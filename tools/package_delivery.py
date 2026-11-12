"""Package source, verified builds, and delivery documents without local databases."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
docs = root / '项目文档/交付资料'
records = docs / '测试记录'
screenshots = docs / '截图'
records.mkdir(parents=True,exist_ok=True)
screenshots.mkdir(parents=True,exist_ok=True)
for name in ('api-h2.json','api-mysql.json','browser.json'):
    source = root / 'test-results' / name
    data = json.loads(source.read_text(encoding='utf-8'))
    if data.get('failed') or not data.get('passed'):
        raise RuntimeError(f'Unsuccessful acceptance report: {name}')
    shutil.copy2(source,records/name)
for source in sorted((root/'test-results/screenshots').glob('[0-9][0-9]-*.png')):
    shutil.copy2(source,screenshots/source.name)

paths = set()
for folder in ('backend/src','frontend/src','frontend/public','frontend/dist','tools','项目文档'):
    for source in (root/folder).rglob('*'):
        if source.is_file() and '__pycache__' not in source.parts:
            paths.add(source)
for filename in ('README.md','HANDOFF.md','.gitignore','backend/pom.xml','backend/tools/maven-settings.xml','backend/target/family-tree.jar','frontend/package.json','frontend/package-lock.json','frontend/index.html','frontend/vite.config.js','frontend/tsconfig.json','frontend/tsconfig.app.json','frontend/tsconfig.node.json'):
    paths.add(root/filename)
manifest=[]
destination=root/'交付包'
destination.mkdir(exist_ok=True)
archive=destination/'宗脉电子族谱-源码与演示包.zip'
with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED) as output:
    for source in sorted(paths):
        if not source.is_file():
            raise FileNotFoundError(source)
        name=source.relative_to(root).as_posix()
        payload=source.read_bytes()
        manifest.append({'path':name,'bytes':len(payload),'sha256':hashlib.sha256(payload).hexdigest()})
        output.writestr('宗脉电子族谱/'+name,payload)
    output.writestr('宗脉电子族谱/交付清单.json',json.dumps(manifest,ensure_ascii=False,indent=2))
print(f'Packaged {len(manifest)} files, {archive.stat().st_size:,} bytes')
print(archive)
