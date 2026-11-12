import { createRequire } from 'node:module'
import path from 'node:path'
import fs from 'node:fs/promises'
const require = createRequire(new URL('../frontend/package.json', import.meta.url))
const { chromium } = require('playwright')
const browser = await chromium.launch({ headless:true, channel:'chrome' })
try {
  const page = await browser.newPage({ viewport:{ width:1440,height:1000 } })
  const errors = []
  page.on('pageerror',error => errors.push(error.message))
  page.on('response', async response => {
    if (response.url().includes('/api/') && !response.url().includes('/csrf')) {
      const info = { url:response.url(),status:response.status() }
      if (response.status() >= 400) info.body = (await response.text()).slice(0,600)
      console.log('HTTP', JSON.stringify(info))
    }
  })
  await page.goto('http://127.0.0.1:5175/auth/login')
  await page.waitForLoadState('networkidle')
  console.log((await page.locator('input').evaluateAll(nodes => nodes.map(n=>({type:n.type,placeholder:n.placeholder})))))
  await page.getByPlaceholder('请输入用户名').fill('demo_admin')
  await page.getByPlaceholder('请输入密码').fill('Demo123!')
  await page.getByRole('button',{ name:'登录',exact:true }).click()
  await page.waitForURL('**/back/dashboard**').catch(async error => {
    await fs.mkdir('test-results/screenshots',{recursive:true})
    await page.screenshot({path:'test-results/screenshots/login-failure.png',fullPage:true})
    console.log('BROWSER ERRORS',JSON.stringify(errors))
    console.log((await page.locator('body').innerText()).slice(0,4000))
    throw error
  })
  await page.waitForLoadState('networkidle')
  await fs.mkdir('test-results/screenshots',{recursive:true})
  await page.screenshot({path:'test-results/screenshots/dashboard.png',fullPage:true})
  console.log((await page.locator('body').innerText()).slice(0,7000))
  await page.getByRole('button',{name:'查看族谱',exact:true}).first().click()
  await page.waitForLoadState('networkidle')
  await page.locator('.tree-canvas canvas').first().waitFor()
  await page.screenshot({path:'test-results/screenshots/tree.png',fullPage:true})
  console.log('ERRORS',JSON.stringify(errors))
  console.log((await page.locator('body').innerText()).slice(0,5000))
} finally { await browser.close() }
