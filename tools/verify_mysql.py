"""Run acceptance checks against a separate, temporary MySQL 8 instance."""
import argparse
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request

parser = argparse.ArgumentParser()
parser.add_argument('--mysql-bin',default=r'C:\Program Files\MySQL\MySQL Server 8.4\bin')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
output = root / 'test-results'
output.mkdir(exist_ok=True)
data = Path(tempfile.mkdtemp(prefix='familytree-mysql-'))
mysql_bin = Path(args.mysql_bin)
flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
mysql = None
backend = None
completed = False
password = secrets.token_urlsafe(32)
env = dict(os.environ)

def client(sql, authenticated=False):
    client_env = dict(env)
    if authenticated:
        client_env['MYSQL_PWD'] = password
    return subprocess.run([str(mysql_bin/'mysql.exe'),'--no-defaults','--protocol=TCP','--host=127.0.0.1','--port=13306','--user=root','--default-character-set=utf8mb4'],input=sql,encoding='utf-8',capture_output=True,env=client_env,creationflags=flags,timeout=30)

with (output/'mysql-server.log').open('w',encoding='utf-8') as mysql_log, (output/'mysql-backend.log').open('w',encoding='utf-8') as backend_log:
    try:
        subprocess.run([str(mysql_bin/'mysqld.exe'),'--no-defaults','--initialize-insecure',f'--basedir={mysql_bin.parent}',f'--datadir={data}'],stdout=mysql_log,stderr=subprocess.STDOUT,check=True,creationflags=flags,timeout=120)
        mysql = subprocess.Popen([str(mysql_bin/'mysqld.exe'),'--no-defaults',f'--basedir={mysql_bin.parent}',f'--datadir={data}','--port=13306','--bind-address=127.0.0.1','--mysqlx-port=13360','--mysqlx-bind-address=127.0.0.1','--console'],cwd=mysql_bin,stdout=mysql_log,stderr=subprocess.STDOUT,creationflags=flags)
        deadline=time.monotonic()+60
        while time.monotonic()<deadline:
            if mysql.poll() is not None:
                raise RuntimeError(f'Isolated MySQL exited {mysql.returncode}. Data directory {data}. See test-results/mysql-server.log')
            if client('SELECT 1;').returncode==0:
                break
            time.sleep(0.5)
        else:
            raise TimeoutError('Isolated MySQL start timed out')
        sql=(root/'backend/src/main/resources/db/schema.sql').read_text(encoding='utf-8')
        sql += '\n' + (root/'backend/src/main/resources/db/extensions.sql').read_text(encoding='utf-8')
        sql += f"\nCREATE USER 'family_app'@'localhost' IDENTIFIED BY '{password}'; GRANT ALL ON family_tree.* TO 'family_app'@'localhost'; ALTER USER 'root'@'localhost' IDENTIFIED BY '{password}';"
        initialized=client(sql)
        if initialized.returncode:
            raise RuntimeError(initialized.stderr)
        app_env=dict(env,SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:13306/family_tree?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8',SPRING_DATASOURCE_USERNAME='family_app',SPRING_DATASOURCE_PASSWORD=password,SPRING_DATASOURCE_DRIVER_CLASS_NAME='com.mysql.cj.jdbc.Driver',SERVER_PORT='9081')
        backend=subprocess.Popen(['java','-jar','target/family-tree.jar','--spring.profiles.active=demo','--spring.sql.init.schema-locations=classpath:db/extensions.sql','--logging.level.root=WARN'],cwd=root/'backend',env=app_env,stdout=backend_log,stderr=subprocess.STDOUT,creationflags=flags)
        deadline=time.monotonic()+70
        while time.monotonic()<deadline:
            if backend.poll() is not None:
                raise RuntimeError('MySQL backend failed to start. See test-results/mysql-backend.log')
            try:
                urllib.request.urlopen('http://127.0.0.1:9081/api/auth/csrf',timeout=2).close()
                break
            except OSError:
                time.sleep(0.5)
        else:
            raise TimeoutError('Backend start timed out')
        result=subprocess.run([sys.executable,str(root/'tools/verify_api.py'),'--base-url','http://127.0.0.1:9081/api','--report',str(output/'api-mysql.json'),'--demo-system-checks'],cwd=root,creationflags=flags)
        if result.returncode:
            raise RuntimeError('MySQL HTTP acceptance failed')
        seed_sql=(root/'backend/src/main/resources/db/demo-data.sql').read_text(encoding='utf-8')
        seeded=client('USE family_tree;\n'+seed_sql,authenticated=True)
        if seeded.returncode:
            raise RuntimeError('Demo SQL failed: '+seeded.stderr)
        seeded_counts=client("USE family_tree; SELECT COUNT(*),COUNT(DISTINCT generation) FROM person WHERE family_id=(SELECT MAX(id) FROM family);",authenticated=True)
        if '36\t5' not in seeded_counts.stdout:
            raise AssertionError('SQL seed did not create 36 people across five generations')
        print('PASS portable MySQL demo SQL creates 36 people across five generations',flush=True)
        completed = True
        print('MySQL 8 acceptance passed. The isolated processes are shutting down.',flush=True)
    finally:
        if backend and backend.poll() is None:
            backend.terminate()
            backend.wait(timeout=20)
        if mysql and mysql.poll() is None:
            shutdown_env=dict(env,MYSQL_PWD=password)
            subprocess.run([str(mysql_bin/'mysqladmin.exe'),'--no-defaults','--protocol=TCP','--host=127.0.0.1','--port=13306','--user=root','shutdown'],env=shutdown_env,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=flags,timeout=20)
            try:
                mysql.wait(timeout=20)
            except subprocess.TimeoutExpired:
                mysql.terminate()
                mysql.wait(timeout=20)
        if completed:
            temporary_root=Path(tempfile.gettempdir()).resolve()
            resolved_data=data.resolve()
            if resolved_data.parent != temporary_root or not resolved_data.name.startswith('familytree-mysql-'):
                raise RuntimeError('Refusing to clean a directory outside this test temporary scope')
            shutil.rmtree(resolved_data)
