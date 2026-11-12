"""HTTP acceptance checks. Creates uniquely named synthetic accounts and families."""
import argparse
import base64
import concurrent.futures
import http.cookiejar
import json
from pathlib import Path
import struct
import time
import urllib.error
import urllib.request
import uuid
import zlib

parser = argparse.ArgumentParser()
parser.add_argument('--base-url', default='http://127.0.0.1:9080/api')
parser.add_argument('--report', default='test-results/api.json')
parser.add_argument('--demo-system-checks', action='store_true')
args = parser.parse_args()
results = []

def check(name, condition):
    results.append({'name': name, 'passed': bool(condition)})
    print(('PASS ' if condition else 'FAIL ') + name, flush=True)
    if not condition:
        raise AssertionError(name)

class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def call(self, method, path, data=None, expected=200, csrf=True, content_type=None):
        headers = {}
        if method in ('POST', 'PUT', 'PATCH', 'DELETE') and csrf:
            token = self.call('GET', '/auth/csrf')
            headers[token['headerName']] = token['token']
        if isinstance(data, (dict, list)):
            data = json.dumps(data, ensure_ascii=False).encode('utf-8')
            headers['Content-Type'] = 'application/json'
        if content_type:
            headers['Content-Type'] = content_type
        request = urllib.request.Request(args.base_url + path, data=data, headers=headers, method=method)
        try:
            response = self.opener.open(request, timeout=25)
        except urllib.error.HTTPError as failure:
            response = failure
        body = response.read()
        allowed = (expected,) if isinstance(expected, int) else expected
        if response.status not in allowed:
            raise AssertionError(f'{method} {path}: expected {expected}, got {response.status}: {body[:1000]!r}')
        if isinstance(expected, tuple):
            return response.status
        if 'application/json' in response.headers.get('Content-Type', '') and body:
            return json.loads(body)
        return body

    def login(self, username, password='TestOnly123!'):
        self.call('POST', '/auth/login', {'username': username, 'password': password})

def payload(person):
    return {key: person.get(key) for key in ('name', 'gender', 'generation', 'birth_date', 'death_date', 'birth_place', 'biography', 'nickname', 'hometown', 'visibility', 'avatar_url')}

def png():
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!IIBBBBB', 2, 2, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(b'\x00\x38\x78\xa0\x38\x78\xa0' * 2)) + chunk(b'IEND', b'')

def multipart(fields, filename='sample.png', content=None):
    boundary = 'FamilyTree' + uuid.uuid4().hex
    parts = []
    for key, value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: image/png\r\n\r\n'.encode() + (png() if content is None else content) + b'\r\n')
    parts.append(f'--{boundary}--\r\n'.encode())
    return b''.join(parts), 'multipart/form-data; boundary=' + boundary

def main():
    suffix = uuid.uuid4().hex[:10]
    names = ['qa_admin_' + suffix, 'qa_member_' + suffix, 'qa_outside_' + suffix]
    anonymous, admin, member, outsider = [Client() for _ in range(4)]
    anonymous.call('GET', '/families', expected=401)
    check('anonymous protected read is 401', True)
    anonymous.call('POST', '/auth/register', {'username':'x', 'password':'12345'}, expected=400)
    check('registration validates username and password', True)
    for client, name in zip((admin, member, outsider), names):
        client.call('POST', '/auth/register', {'username':name, 'password':'TestOnly123!'}, expected=201)
        client.login(name)
    admin.call('POST', '/auth/register', {'username':names[0], 'password':'TestOnly123!'}, expected=409)
    check('duplicate registration is 409', True)
    check('fresh account has no families', admin.call('GET', '/families') == [])
    admin.call('POST', '/families', {'name':'CSRF check'}, expected=403, csrf=False)
    check('missing CSRF is rejected', True)
    family_id = admin.call('POST', '/demo-families')['id']
    root = f'/families/{family_id}'
    tree = admin.call('GET', root + '/tree')
    people = tree['persons']
    check('36 fictional people across five generations', len(people) == 36 and len({p['generation'] for p in people}) == 5)
    admin.call('PUT', root + '/members', {'username':names[1], 'role':'MEMBER'})
    check('family account association', len(admin.call('GET', root + '/members')) == 2)
    check('member can read all family people', len(member.call('GET', root + '/persons')) == 36)
    member.call('POST', root + '/persons', {'name':'forbidden'}, expected=403)
    outsider.call('GET', root + '/tree', expected=403)
    outsider.call('GET', root + '/media', expected=403)
    member.call('GET', root + '/export', expected=403)
    check('member write and cross-family reads are forbidden', True)
    page = admin.call('GET', root + '/persons/page?generation=3&size=5&page=1')
    check('server generation filter and pagination', page['total'] == 8 and len(page['records']) == 5 and all(p['generation'] == 3 for p in page['records']))
    person = people[0]
    year = str(person['birth_date'])[:4]
    page = admin.call('GET', root + '/persons/page?birthYear=' + year)
    check('birth year filter', page['total'] > 0 and all(str(p['birth_date']).startswith(year) for p in page['records']))
    admin.call('GET', root + '/persons/page?size=101', expected=400)
    check('pagination bounds', True)
    admin.call('POST', root + '/persons', {'name':'bad dates', 'birth_date':'2000-01-01', 'death_date':'1999-01-01'}, expected=400)
    admin.call('POST', root + '/persons', {'name':'bad date', 'birth_date':'2026-02-30'}, expected=400)
    check('date ordering and strict calendar validation', True)
    relation = next(r for r in tree['relations'] if r['relation_type'] == 'PARENT_CHILD')
    admin.call('POST', root + '/relations', {'parentPersonId':relation['from_person_id'], 'childPersonId':relation['from_person_id']}, expected=400)
    admin.call('POST', root + '/relations', {'parentPersonId':relation['from_person_id'], 'childPersonId':relation['to_person_id']}, expected=409)
    admin.call('POST', root + '/relations', {'parentPersonId':relation['to_person_id'], 'childPersonId':relation['from_person_id']}, expected=409)
    admin.call('POST', root + '/relations/adoptive', {'parentPersonId':relation['to_person_id'], 'childPersonId':relation['from_person_id']}, expected=409)
    check('self relations, duplicates and biological/adoptive cycles rejected', True)
    spouse = next(r for r in tree['relations'] if r['relation_type'] == 'SPOUSE')
    admin.call('POST', root + '/relations/spouse', {'personId':spouse['to_person_id'], 'spousePersonId':spouse['from_person_id']}, expected=409)
    check('reversed spouse duplicate rejected', True)
    other = admin.call('POST', '/families', {'name':'Cross-family QA'}, expected=201)['id']
    admin.call('POST', f'/families/{other}/relations', {'parentPersonId':relation['from_person_id'], 'childPersonId':relation['to_person_id']}, expected=404)
    check('cross-family relation rejected', True)
    descendants = admin.call('GET', root + f'/persons/{person["id"]}/descendants')
    check('multi-level descendants are unique and exclude root', len(descendants) >= 20 and len({p['id'] for p in descendants}) == len(descendants) and person['id'] not in {p['id'] for p in descendants})
    leaf = next(p for p in people if p['generation'] == 5)
    ancestors = admin.call('GET', root + f'/persons/{leaf["id"]}/ancestors')
    check('multi-level ancestors', person['id'] in {p['id'] for p in ancestors})
    private_data = payload(leaf); private_data['visibility'] = 'PRIVATE'
    admin.call('PUT', root + f'/persons/{leaf["id"]}', private_data)
    member.call('GET', root + f'/persons/{leaf["id"]}', expected=404)
    member_tree = member.call('GET', root + '/tree')
    check('private people and their edge names are filtered', len(member_tree['persons']) == 35 and all(leaf['id'] not in (r['from_person_id'],r['to_person_id']) for r in member_tree['relations']))
    admin.call('POST',root+'/events',{'title':'Private person event','person_id':leaf['id']})
    hidden_event=next(e['id'] for e in admin.call('GET',root+'/events') if e['title']=='Private person event')
    hidden_file,hidden_type=multipart({'title':'Indirect private event image','eventId':hidden_event})
    admin.call('POST',root+'/media',hidden_file,content_type=hidden_type)
    hidden_media=admin.call('GET',root+'/media')[0]['id']
    member.call('GET',root+f'/media/{hidden_media}/file',expected=404)
    visible_events=member.call('GET',root+'/events')
    check('private event attachments and statistics follow visible scope',all(e['id']!=hidden_event for e in visible_events) and member.call('GET',root+'/stats')['events']==len(visible_events))
    admin.call('DELETE',root+f'/media/{hidden_media}')
    admin.call('DELETE',root+f'/events/{hidden_event}')
    member.call('GET', root + f'/persons/{person["id"]}/descendants')
    admin.call('PUT', root + '/members', {'username':names[0], 'role':'MEMBER'}, expected=409)
    admin_id = admin.call('GET', '/auth/me')['id']
    admin.call('DELETE', root + f'/members/{admin_id}', expected=409)
    check('last active administrator protected', True)
    target = people[2]
    change = payload(target); change['biography'] = 'Approved QA biography'
    member.call('POST', root + '/requests', {'kind':'PERSON_UPDATE', 'targetId':target['id'], 'payload':change})
    request_id = admin.call('GET', root + '/requests')[0]['id']
    member.call('POST', root + f'/requests/{request_id}/review', {'approve':True}, expected=403)
    admin.call('POST', root + f'/requests/{request_id}/review', {}, expected=400)
    check('missing approval decision cannot silently reject proposal',True)
    admin.call('POST', root + f'/requests/{request_id}/review', {'approve':True, 'note':'QA approved'})
    check('member proposal and administrator approval', admin.call('GET', root + f'/persons/{target["id"]}')['biography'] == 'Approved QA biography')
    admin.call('POST', root + f'/requests/{request_id}/review', {'approve':True}, expected=409)
    check('review is single-use', True)
    change = payload(admin.call('GET', root + f'/persons/{target["id"]}'))
    member.call('POST', root + '/requests', {'kind':'PERSON_UPDATE', 'targetId':target['id'], 'payload':change})
    stale = admin.call('GET', root + '/requests')[0]['id']
    change['biography'] = 'Changed after proposal'
    admin.call('PUT', root + f'/persons/{target["id"]}', change)
    admin.call('POST', root + f'/requests/{stale}/review', {'approve':True}, expected=409)
    admin.call('POST', root + f'/requests/{stale}/review', {'approve':False, 'note':'stale'})
    check('stale proposal conflict and rejection', True)
    admin.call('POST', root + '/events', {'title':'QA event', 'event_date':'2026-11-11', 'person_id':target['id'], 'content':'QA only'})
    event_id = next(e['id'] for e in admin.call('GET', root + '/events') if e['title'] == 'QA event')
    data, content_type = multipart({'title':'QA image', 'personId':target['id'], 'eventId':event_id, 'visibility':'FAMILY'})
    admin.call('POST', root + '/media', data, content_type=content_type)
    media_id = admin.call('GET', root + '/media')[0]['id']
    check('authenticated image upload and read', member.call('GET', root + f'/media/{media_id}/file') == png())
    bad, bad_type = multipart({'title':'invalid'}, content=b'<script>test</script>')
    admin.call('POST', root + '/media', bad, expected=400, content_type=bad_type)
    check('executable or invalid upload rejected', True)
    data, content_type = multipart({'title':'Private QA image', 'visibility':'PRIVATE'})
    admin.call('POST', root + '/media', data, content_type=content_type)
    private_media = admin.call('GET', root + '/media')[0]['id']
    member.call('GET', root + f'/media/{private_media}/file', expected=404)
    check('private media access', all(m['id'] != private_media for m in member.call('GET', root + '/media')))
    change['avatar_url'] = f'/api/families/{family_id}/media/{media_id}/file'
    admin.call('PUT', root + f'/persons/{target["id"]}', change)
    check('avatar references family image', admin.call('GET', root + f'/persons/{target["id"]}')['avatar_url'] == change['avatar_url'])
    change['avatar_url'] = 'https://example.com/external.png'
    admin.call('PUT', root + f'/persons/{target["id"]}', change, expected=400)
    check('external avatar address rejected', True)
    exported = admin.call('GET', root + '/export?format=json')
    check('complete JSON export includes binary media', len(exported['persons']) == 36 and len(exported['media']) == 2 and base64.b64decode(exported['media'][0]['file_data']) == png())
    csv = admin.call('GET', root + '/export?format=csv')
    check('CSV export is UTF-8 with BOM', csv.startswith(b'\xef\xbb\xbf') and len(csv.splitlines()) == 37)
    admin.call('DELETE', root + f'/events/{event_id}')
    check('event deletion clears media reference', next(m for m in admin.call('GET', root + '/media') if m['id'] == media_id)['event_id'] is None)
    admin.call('DELETE', root + f'/media/{media_id}')
    check('deleting avatar image clears person reference', admin.call('GET', root + f'/persons/{target["id"]}')['avatar_url'] is None)
    admin.call('DELETE', root, expected=409)
    check('failed family delete preserves memberships', len(admin.call('GET', root + '/members')) == 2)
    check('audit records successful writes', len(admin.call('GET', root + '/audit')) >= 8)
    original_family = admin.call('GET', root)
    original_family['published'] = True
    admin.call('PUT', root, original_family)
    published = next(f for f in anonymous.call('GET', '/public/families') if f['id'] == family_id)
    check('public family summary exposes only authorized summary fields', set(published) == {'id','name','surname','description'})
    original_family['published'] = False
    admin.call('PUT', root, original_family)
    check('public summary can be withdrawn', all(f['id'] != family_id for f in anonymous.call('GET', '/public/families')))
    for name in ('Concurrent A', 'Concurrent B'):
        admin.call('POST', root + '/persons', {'name':name})
    parallel_people = [p for p in admin.call('GET', root + '/persons') if p['name'].startswith('Concurrent ')]
    first, second = parallel_people[-2:]
    concurrent_clients = [Client(), Client()]
    for client in concurrent_clients:
        client.login(names[0])
    def create_opposite(index):
        pair = (first, second) if index == 0 else (second, first)
        return concurrent_clients[index].call('POST', root + '/relations', {'parentPersonId':pair[0]['id'],'childPersonId':pair[1]['id']}, expected=(200,409))
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        statuses = list(pool.map(create_opposite, (0,1)))
    check('concurrent opposite parent links cannot create a cycle', sorted(statuses) == [200,409])
    if args.demo_system_checks:
        system = Client()
        system.login('demo_admin','Demo123!')
        outside_id = outsider.call('GET','/auth/me')['id']
        system.call('PATCH',f'/system/users/{outside_id}',{'status':'DISABLED'})
        outsider.call('GET','/auth/me',expected=403)
        outsider.call('GET','/families',expected=403)
        system.call('PATCH',f'/system/users/{outside_id}',{'status':'ACTIVE'})
        check('system administrator can disable account and block existing session', True)
    admin.call('GET', '/system/users', expected=403)
    check('family admin cannot administer system users', True)
    admin.call('POST', '/auth/logout')
    admin.call('GET', '/auth/me', expected=401)
    check('logout invalidates session', True)

try:
    main()
except Exception as failure:
    if not results or results[-1]['passed']:
        results.append({'name':str(failure),'passed':False})
    raise
finally:
    report = Path(args.report)
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({'baseUrl':args.base_url,'checks':results,'passed':sum(r['passed'] for r in results),'failed':sum(not r['passed'] for r in results)},ensure_ascii=False,indent=2),encoding='utf-8')
