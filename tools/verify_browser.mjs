import { createRequire } from 'node:module';
import path from 'node:path';
import fs from 'node:fs/promises';
const require = createRequire(new URL('../frontend/package.json', import.meta.url));
const { chromium, expect } = require('@playwright/test');
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:5175';
const channel = process.env.TEST_BROWSER_CHANNEL || 'chrome';
const browser = await chromium.launch({ headless: true, channel: channel === 'chromium' ? undefined : channel });
const output = 'test-results/screenshots';
const results = [];
const errors = [];
const contexts = [];
await fs.mkdir(output, { recursive: true });

function check(name, value = true) {
  results.push({ name, passed: Boolean(value) });
  console.log(`${value ? 'PASS' : 'FAIL'} ${name}`);
  if (!value) throw new Error(name);
}

async function context() {
  const value = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  contexts.push(value);
  value.on('page', page => page.on('pageerror', error => errors.push(error.message)));
  return value;
}

async function api(ctx, method, endpoint, data) {
  const headers = {};
  if (['post', 'put', 'delete', 'patch'].includes(method)) {
    const csrf = await (await ctx.request.get(`${base}/api/auth/csrf`)).json();
    headers[csrf.headerName] = csrf.token;
  }
  const response = await ctx.request.fetch(`${base}/api${endpoint}`, { method, data, headers });
  if (!response.ok()) throw new Error(`${method} ${endpoint}: ${response.status()} ${await response.text()}`);
  return response;
}

async function login(page, username, password) {
  await page.goto(`${base}/auth/login`);
  await page.waitForLoadState('networkidle');
  await page.getByPlaceholder('请输入用户名').fill(username);
  await page.getByPlaceholder('请输入密码').fill(password);
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL('**/back/dashboard**');
  await page.waitForLoadState('networkidle');
}

async function goto(page, url) {
  await page.goto(`${base}${url}`);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('.el-skeleton')).toHaveCount(0);
}

async function screenshot(page, name) {
  // 等待抽屉、对话框和标签的入场动画结束，避免截到半透明的中间帧。
  await page.waitForTimeout(400);
  await page.screenshot({ path: `${output}/${name}.png`, fullPage: true });
}

try {
  const demo = await context();
  const page = await demo.newPage();
  await login(page, 'demo_admin', 'Demo123!');
  await expect(page.getByRole('button', { name: '创建家族', exact: true })).toBeVisible();
  await screenshot(page, '01-family-overview');
  await expect(page.locator('.brand-logo img')).toBeVisible();
  check('production build includes the sidebar brand image',await page.locator('.brand-logo img').evaluate(image=>image.complete && image.naturalWidth>0));
  check('administrator login and live family overview');
  await page.getByRole('button', { name: '查看族谱', exact: true }).first().click();
  await page.waitForLoadState('networkidle');
  await expect(page.locator('.tree-canvas canvas').first()).toBeVisible();
  await screenshot(page, '02-family-tree');
  const familyId = new URL(page.url()).searchParams.get('familyId');
  const tree = await (await api(demo, 'get', `/families/${familyId}/tree`)).json();
  check('graph loads all 36 people and confirmed relationships', tree.persons.length === 36);
  const search = page.locator('.graph-tools .el-select').nth(2);
  await search.click();
  await search.locator('input').fill('林守诚');
  await page.getByRole('option', { name: `林守诚（${tree.family.ancestor_person_id}）`, exact: true }).click();
  await page.getByRole('button', { name: '查看档案', exact: true }).click();
  const drawer = page.locator('.el-drawer');
  await expect(drawer.getByRole('heading', { name: '林守诚', exact: true })).toBeVisible();
  await expect(drawer.getByText('兄弟姐妹（共享父母）', { exact: true })).toBeVisible();
  await screenshot(page, '03-person-details');
  check('search, person detail drawer, and relatives');
  await drawer.locator('.el-drawer__close-btn').click();
  await expect(drawer).toBeHidden();
  const canvasBox=await page.locator('.tree-canvas').boundingBox();
  await page.locator('.tree-canvas').click({position:{x:canvasBox.width/2,y:canvasBox.height/2}});
  await expect(drawer.getByRole('heading',{name:'林守诚',exact:true})).toBeVisible();
  check('clicking graph node opens its corresponding record');
  await drawer.getByRole('button',{name:'折叠后代',exact:true}).click();
  await expect(page.getByRole('button',{name:'展开全部',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'展开全部',exact:true}).click();
  await expect(page.getByRole('button',{name:'展开全部',exact:true})).toHaveCount(0);
  check('collapse and expand descendant branches');
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: '导出图片', exact: true }).click();
  const download = await downloadPromise;
  await download.saveAs('test-results/tree-export.png');
  check('whole graph PNG export', (await fs.stat('test-results/tree-export.png')).size > 1000);
  await goto(page, `/back/consultations?familyId=${familyId}`);
  await expect(page.locator('.el-table__body-wrapper').getByText('林守诚', { exact: true })).toBeVisible();
  await screenshot(page, '04-member-records');
  await page.getByRole('button', { name: '后代', exact: true }).first().click();
  await expect(page.locator('.el-dialog').getByText('林承安', { exact: true })).toBeVisible();
  check('member page lineage dialog');
  await page.locator('.el-dialog__headerbtn').click();
  await goto(page, `/back/emotional?familyId=${familyId}`);
  await expect(page.getByRole('heading', { name: '始迁清溪', exact: true })).toBeVisible();
  await screenshot(page, '05-family-events');
  check('event timeline displays real events');
  await goto(page, `/back/album?familyId=${familyId}`);
  await page.getByRole('button', { name: '上传档案', exact: true }).click();
  await expect(page.getByRole('dialog').getByText('关联事件', { exact: true })).toBeVisible();
  await screenshot(page, '06-media-upload');
  await page.getByRole('dialog').getByRole('button', { name: '取消', exact: true }).click();
  await goto(page, `/back/settings?familyId=${familyId}`);
  await page.getByRole('tab', { name: '账号与角色', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'demo_member', exact: true })).toBeVisible();
  await screenshot(page, '07-member-permissions');
  await page.getByRole('tab', { name: '始祖与数据导出', exact: true }).click();
  const csvPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: '导出成员 CSV', exact: true }).click();
  const csv = await csvPromise;
  await csv.saveAs('test-results/members-export.csv');
  check('settings and CSV download', (await fs.readFile('test-results/members-export.csv')).subarray(0,3).equals(Buffer.from([239,187,191])));
  await page.getByRole('tab', { name: '系统账号', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'demo_outsider', exact: true })).toBeVisible();
  check('system administrator sees system user controls');

  const qa = await context();
  const username = `web_${Date.now()}`;
  await api(qa, 'post', '/auth/register', { username, password: 'TestOnly123!' });
  const editorPage = await qa.newPage();
  await login(editorPage, username, 'TestOnly123!');
  await editorPage.getByRole('button', { name: '创建家族', exact: true }).click();
  let dialog = editorPage.getByRole('dialog');
  await dialog.getByLabel('家族名称', { exact: true }).fill('页面验收家族');
  await dialog.getByLabel('姓氏', { exact: true }).fill('测');
  await dialog.getByRole('button', { name: '保存', exact: true }).click();
  await expect(editorPage.getByRole('cell', { name: '页面验收家族', exact: true })).toBeVisible();
  check('create family through normal UI');
  await editorPage.getByRole('button', { name: '成员档案', exact: true }).click();
  await editorPage.waitForURL(/\/back\/consultations\?familyId=/);
  await editorPage.waitForLoadState('networkidle');
  const qaId = new URL(editorPage.url()).searchParams.get('familyId');
  for (const name of ['测甲', '测乙', '测丙']) {
    await editorPage.getByRole('button', { name: '新增成员', exact: true }).click();
    dialog = editorPage.getByRole('dialog');
    await dialog.getByLabel('姓名', { exact: true }).fill(name);
    await dialog.getByRole('button', { name: '保存', exact: true }).click();
    await expect(dialog).toBeHidden();
    await expect(editorPage.getByRole('cell', { name, exact: true })).toBeVisible();
  }
  check('create three member records through normal UI');
  await editorPage.getByRole('button', { name: '维护亲属关系', exact: true }).click();
  dialog = editorPage.getByRole('dialog');
  const people = await (await api(qa,'get',`/families/${qaId}/persons`)).json();
  for (const names of [['测甲','测乙'],['测乙','测丙']]) {
    for (let i=0;i<2;i++) {
      const label = i===0 ? '父亲或母亲' : '子女';
      const item = people.find(p=>p.name===names[i]);
      const control=dialog.getByLabel(label,{exact:true});
      const listId=await control.getAttribute('aria-controls');
      await control.click();
      await editorPage.locator(`#${listId}`).getByRole('option',{name:`${item.name}（${item.id}）`,exact:true}).click();
    }
    await dialog.getByRole('button',{name:'添加关系',exact:true}).click();
    await expect(dialog.locator('.el-table__body-wrapper tr')).toHaveCount(names[0]==='测甲'?1:2);
  }
  check('create parent links through normal UI');
  await dialog.locator('.el-dialog__headerbtn').click();
  await goto(editorPage,`/back/emotional?familyId=${qaId}`);
  await editorPage.getByRole('button',{name:'发布事件',exact:true}).click();
  dialog=editorPage.getByRole('dialog');
  await dialog.getByLabel('事件标题',{exact:true}).fill('页面验收事件');
  await dialog.getByLabel('事件内容',{exact:true}).fill('虚构测试事件。');
  await dialog.getByRole('button',{name:'保存',exact:true}).click();
  await expect(editorPage.getByRole('heading',{name:'页面验收事件',exact:true})).toBeVisible();
  check('create event through normal UI');
  await goto(editorPage,`/back/album?familyId=${qaId}`);
  await editorPage.getByRole('button',{name:'上传档案',exact:true}).click();
  dialog=editorPage.getByRole('dialog');
  await dialog.getByLabel('标题',{exact:true}).fill('页面验收图片');
  await dialog.locator('input[type=file]').setInputFiles({name:'test.png',mimeType:'image/png',buffer:await fs.readFile('test-results/tree-export.png')});
  await dialog.getByRole('button',{name:'上传',exact:true}).click();
  await expect(editorPage.getByRole('heading',{name:'页面验收图片',exact:true})).toBeVisible();
  check('upload and display image through normal UI');
  await goto(editorPage,`/back/consultations?familyId=${qaId}`);
  await editorPage.getByRole('button',{name:'编辑',exact:true}).first().click();
  dialog=editorPage.getByRole('dialog');
  const avatarControl=dialog.getByLabel('头像',{exact:true});
  const avatarList=await avatarControl.getAttribute('aria-controls');
  await dialog.locator('.el-form-item').filter({hasText:'头像'}).locator('.el-select').click();
  await editorPage.locator(`#${avatarList}`).getByRole('option',{name:'页面验收图片',exact:true}).click();
  await dialog.getByRole('button',{name:'保存',exact:true}).click();
  await expect(dialog).toBeHidden();
  const avatarRequest=editorPage.waitForResponse(response=>response.url().includes(`/api/families/${qaId}/media/`) && response.url().endsWith('/file') && response.status()===200);
  await goto(editorPage,`/back/knowledge?familyId=${qaId}`);
  await avatarRequest;
  check('person avatar is selected from album and loaded by graph card');

  const member = await context();
  const memberPage = await member.newPage();
  await login(memberPage,'demo_member','Demo123!');
  await goto(memberPage,`/back/consultations?familyId=${familyId}`);
  await expect(memberPage.getByRole('button',{name:'新增成员',exact:true})).toHaveCount(0);
  await expect(memberPage.getByRole('button',{name:'申请修改',exact:true}).first()).toBeVisible();
  await memberPage.getByRole('button',{name:'申请修改',exact:true}).first().click();
  dialog=memberPage.getByRole('dialog');
  const originalName=await dialog.getByLabel('姓名',{exact:true}).inputValue();
  await dialog.getByLabel('人物简介',{exact:true}).fill('浏览器申请审核验收。');
  await dialog.getByRole('button',{name:'提交审核',exact:true}).click();
  await expect(dialog).toBeHidden();
  check('member read-only controls and change proposal');
  await goto(page,`/back/settings?familyId=${familyId}`);
  const pendingRow=page.locator('.el-table__body tr').filter({hasText:'demo_member'}).filter({hasText:'待审核'}).first();
  await pendingRow.getByRole('button',{name:'通过',exact:true}).click();
  await page.locator('.el-message-box').getByRole('button',{name:'确定',exact:true}).click();
  await expect(page.locator('.el-table__body tr').filter({hasText:'demo_member'}).filter({hasText:'已通过'}).first()).toBeVisible();
  check('administrator approves member change through normal UI');
  await screenshot(page,'08-change-review');
  const updated=await (await api(demo,'get',`/families/${familyId}/persons`)).json();
  const changed=updated.find(p=>p.name===originalName);
  changed.biography='虚构人物，用于电子族谱教学与验收。';
  await api(demo,'put',`/families/${familyId}/persons/${changed.id}`,changed);
  await memberPage.setViewportSize({width:390,height:844});
  await goto(memberPage,`/back/knowledge?familyId=${familyId}`);
  await expect(memberPage.locator('.tree-canvas canvas').first()).toBeVisible();
  await screenshot(memberPage,'09-mobile-tree');
  check('mobile layout keeps usable graph area',await memberPage.locator('.tree-canvas').evaluate(node=>node.getBoundingClientRect().width)>200);
  check('no browser script errors',errors.length===0);
} catch(error) {
  results.push({name:error.message,passed:false});
  for(let i=0;i<contexts.length;i++) for(const page of contexts[i].pages()) await page.screenshot({path:`${output}/failure-${i}.png`,fullPage:true}).catch(()=>{});
  throw error;
} finally {
  for(const ctx of contexts) await ctx.close();
  await browser.close();
  await fs.writeFile('test-results/browser.json',JSON.stringify({checks:results,errors,passed:results.filter(r=>r.passed).length,failed:results.filter(r=>!r.passed).length},null,2));
}
