"""Check the delivery archive against its manifest and acceptance records."""
from pathlib import Path
import hashlib
import json
import re
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
archive = root / '交付包' / '宗脉电子族谱-源码与演示包.zip'
prefix = '宗脉电子族谱/'
manifest_name = prefix + '交付清单.json'
required = [
    'README.md',
    'HANDOFF.md',
    'backend/pom.xml',
    'backend/target/family-tree.jar',
    'backend/src/main/resources/db/schema.sql',
    'backend/src/main/resources/db/extensions.sql',
    'backend/src/main/resources/db/demo-data.sql',
    'frontend/dist/index.html',
    'frontend/package.json',
    'tools/Start-Demo.ps1',
    'tools/Start-MySQL.ps1',
    'tools/verify_api.py',
    'tools/verify_mysql.py',
    'tools/verify_browser.mjs',
    'tools/verify_delivery.py',
    '项目文档/交付资料/部署与使用说明.md',
    '项目文档/交付资料/需求数据库与接口说明.md',
    '项目文档/交付资料/测试报告.md',
    '项目文档/交付资料/课程报告.md',
    '项目文档/交付资料/演示脚本.md',
    '项目文档/交付资料/测试记录/api-h2.json',
    '项目文档/交付资料/测试记录/api-mysql.json',
    '项目文档/交付资料/测试记录/browser.json',
]
for index in range(1, 10):
    matches = list((root / '项目文档/交付资料/截图').glob(f'{index:02d}-*.png'))
    if matches:
        required.append('项目文档/交付资料/截图/' + matches[0].name)
forbidden = (
    '__pycache__', '.mv.db', 'node_modules/', '/.git/', 'test-results/',
    'target/classes/', 'target/test-classes/', 'familytree-mysql-',
    'demo-backend.log', 'demo-frontend.log', '.env',
)
failures = []


def fail(message):
    failures.append(message)
    print('FAIL ' + message)


if not archive.is_file():
    fail(f'缺少压缩包 {archive}')
    sys.exit(1)

with zipfile.ZipFile(archive) as bundle:
    broken = bundle.testzip()
    if broken:
        fail(f'压缩包条目损坏 {broken}')
    names = bundle.namelist()
    if len(names) != len(set(names)):
        fail('压缩包存在重名条目')
    if manifest_name not in names:
        fail('缺少交付清单.json')
    else:
        manifest = json.loads(bundle.read(manifest_name).decode('utf-8'))
        entries = {name[len(prefix):]: bundle.read(name) for name in names if name.startswith(prefix) and name != manifest_name}
        listed = {item['path']: item for item in manifest}
        missing = sorted(set(listed) - set(entries))
        extra = sorted(set(entries) - set(listed))
        for path in missing:
            fail(f'清单文件缺失 {path}')
        for path in extra:
            fail(f'清单外文件 {path}')
        if len(manifest) != len(listed):
            fail('清单存在重复路径')
        for path, item in listed.items():
            if path not in entries:
                continue
            payload = entries[path]
            if len(payload) != item['bytes'] or hashlib.sha256(payload).hexdigest() != item['sha256']:
                fail(f'文件大小或哈希不一致 {path}')
        print(f'manifest: {len(manifest)} files, {sum(i["bytes"] for i in manifest):,} bytes')

    for path in required:
        if prefix + path not in names:
            fail(f'缺少必需文件 {path}')

    for name in names:
        lowered = name.lower()
        for token in forbidden:
            if token in lowered:
                fail(f'包含不应交付的内容 {name}')

    for script in ('tools/Start-Demo.ps1', 'tools/Start-MySQL.ps1'):
        if prefix + script in names and not bundle.read(prefix + script).startswith(b'\xef\xbb\xbf'):
            fail(f'PowerShell 脚本缺少 UTF-8 BOM，Windows PowerShell 5.1 可能无法解析 {script}')

    for report_name in ('api-h2.json', 'api-mysql.json', 'browser.json'):
        name = prefix + '项目文档/交付资料/测试记录/' + report_name
        if name not in names:
            continue
        report = json.loads(bundle.read(name).decode('utf-8'))
        if report.get('failed') or not report.get('passed'):
            fail(f'验收记录未通过 {report_name}')
        print(f'{report_name}: {report.get("passed")} passed, {report.get("failed")} failed')

    index = bundle.read(prefix + 'frontend/dist/index.html').decode('utf-8')
    for asset in sorted(set(re.findall(r'(?:src|href)="(/assets/[^"]+)"', index))):
        if prefix + 'frontend/dist' + asset not in names:
            fail(f'生产页面引用的资源缺失 {asset}')

    jar = zipfile.ZipFile(__import__('io').BytesIO(bundle.read(prefix + 'backend/target/family-tree.jar')))
    jar_names = set(jar.namelist())
    for entry in (
        'BOOT-INF/classes/application-demo.yml',
        'BOOT-INF/classes/com/zongmai/familytree/FamilyTreeApplication.class',
        'BOOT-INF/classes/db/demo-data.sql',
    ):
        if entry not in jar_names:
            fail(f'JAR 缺少入口 {entry}')

# Source files in the archive should still match the working tree at packaging time.
with zipfile.ZipFile(archive) as bundle:
    manifest = json.loads(bundle.read(manifest_name).decode('utf-8'))
    drifted = []
    for item in manifest:
        source = root / item['path']
        if source.is_file() and hashlib.sha256(source.read_bytes()).hexdigest() != item['sha256']:
            drifted.append(item['path'])
    if drifted:
        print('note: files changed after packaging: ' + ', '.join(drifted[:10]))

if failures:
    print(f'\nDelivery archive check failed: {len(failures)} problem(s).')
    sys.exit(1)
print(f'\nDelivery archive check passed: {archive.name} ({archive.stat().st_size:,} bytes).')
