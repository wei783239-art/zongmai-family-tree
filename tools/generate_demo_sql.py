"""Generate portable MySQL seed SQL with runtime-generated primary keys."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
people = []
relations = []

def person(name, generation, male):
    index = len(people)
    people.append((name, generation, male, index))
    return index

ancestor = person('林守诚', 1, True)
partner = person('陈淑宁', 1, False)
relations.append((ancestor,partner,'SPOUSE'))
previous = [(ancestor,partner)]
for generation, count in enumerate([3,4,5,10], start=2):
    following = []
    for index in range(count):
        male = index % 2 == 0
        child = person('林' + ['承','景','文','知'][generation-2] + ['安','宁','远','和','清','明','悦','新','澄','初'][index],generation,male)
        parents = previous[index % len(previous)]
        relations.extend([(parents[0],child,'PARENT_CHILD'),(parents[1],child,'PARENT_CHILD')])
        if generation < 5:
            mate = person(('周' if male else '许') + ['若','思','云','书','晓'][index] + ('兰' if male else '川'),generation,not male)
            relations.append((child,mate,'SPOUSE'))
            following.append((child,mate))
    previous = following

lines = [
    '-- 五代虚构演示数据，共36人。先运行 schema.sql 和 extensions.sql 并注册实际管理账号。',
    '-- 将末尾 CALL 中的 demo_admin 改为已注册的用户名。每次调用创建一个新家族。',
    'SET NAMES utf8mb4;',
    'DELIMITER $$',
    'CREATE PROCEDURE zongmai_seed_demo_v1(IN owner_username VARCHAR(64))',
    'BEGIN',
    '  DECLARE owner_id BIGINT;',
    '  DECLARE family_id_value BIGINT;',
    '  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;',
    "  SELECT id INTO owner_id FROM sys_user WHERE username=owner_username AND status='ACTIVE';",
    "  IF owner_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Register the owner account first'; END IF;",
    '  START TRANSACTION;',
    "  INSERT INTO family(name,surname,ancestral_hall,hometown,description,motto) VALUES('林氏五代演示家族','林','积善堂','虚构·清溪镇','全部人物和事件均为虚构，仅用于课程演示。','敦亲睦族，诚实向学。');",
    '  SET family_id_value=LAST_INSERT_ID();',
    "  INSERT INTO family_user(family_id,user_id,role) VALUES(family_id_value,owner_id,'FAMILY_ADMIN');"
]
for name,generation,male,index in people:
    year = 1888 + (generation-1)*27 + index%7
    birth = f'{year:04d}-{index%12+1:02d}-{index%25+1:02d}'
    death = f"'{1968+index}-06-15'" if generation == 1 else 'NULL'
    gender = 'MALE' if male else 'FEMALE'
    nickname = ['守','承','景','文','知'][generation-1]
    lines.extend([
        f"  INSERT INTO person(family_id,name,gender,generation,birth_date,death_date,birth_place,hometown,nickname,biography) VALUES(family_id_value,'{name}','{gender}',{generation},'{birth}',{death},'虚构·清溪镇','虚构·江南','{nickname}','虚构人物，用于电子族谱教学与验收。');",
        f'  SET @zongmai_p{index}=LAST_INSERT_ID();'
    ])
for start,end,kind in relations:
    first,second = (min(start,end),max(start,end)) if kind == 'SPOUSE' else (start,end)
    lines.append(f"  INSERT INTO person_relation(family_id,from_person_id,to_person_id,relation_type) VALUES(family_id_value,@zongmai_p{first},@zongmai_p{second},'{kind}');")
lines.extend([
    '  UPDATE family SET ancestor_person_id=@zongmai_p0 WHERE id=family_id_value;',
    "  INSERT INTO family_event(family_id,title,event_date,location,content,person_id) VALUES(family_id_value,'始迁清溪','1928-03-12','虚构·清溪镇','演示事件，全部内容均为虚构。',@zongmai_p0);",
    "  INSERT INTO family_event(family_id,title,event_date,location,content) VALUES(family_id_value,'家族修谱','2026-10-01','虚构·积善堂','汇集五代档案并建立电子族谱。');",
    '  COMMIT;',
    '  SELECT family_id_value AS created_family_id;',
    'END$$',
    'DELIMITER ;',
    "CALL zongmai_seed_demo_v1('demo_admin');",
    'DROP PROCEDURE zongmai_seed_demo_v1;',
    ''
])
destination = root / 'backend/src/main/resources/db/demo-data.sql'
destination.write_text('\n'.join(lines),encoding='utf-8')
print(f'Generated {len(people)} people and {len(relations)} relations in {destination}')
