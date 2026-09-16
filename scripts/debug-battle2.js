const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox']
  });
  const page = await browser.newPage();

  // 收集控制台日志
  const logs = [];
  page.on('console', msg => {
    logs.push(`[${msg.type()}] ${msg.text()}`);
  });

  await page.goto('http://localhost:7894/battle?mode=guest', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  // 关闭教程
  const closeBtn = await page.$('.btn-start');
  if (closeBtn) await closeBtn.click();
  await new Promise(r => setTimeout(r, 500));

  // 点击开始
  const startBtn = await page.$('.btn-primary.btn-large');
  if (startBtn) await startBtn.click();
  await new Promise(r => setTimeout(r, 5000));

  // 获取控制台日志
  console.log('=== Console Logs ===');
  logs.forEach(l => console.log(l));

  // 获取完整页面状态
  const state = await page.evaluate(() => {
    // 尝试获取 Vue 组件状态
    const app = document.querySelector('#app');
    return {
      url: window.location.href,
      title: document.title,
      hasPanel: !!document.querySelector('.battle3d-panel'),
      hasTeams: !!document.querySelector('.teams-row'),
      teamContent: document.querySelector('.teams-row')?.textContent?.trim(),
      hasMoves: !!document.querySelector('.moves-section'),
      moveCount: document.querySelectorAll('.move-btn').length,
      hasConfirm: !!document.querySelector('button:has-text("确认预览")'),
      actionButtons: Array.from(document.querySelectorAll('.action-row button')).map(b => b.textContent?.trim())
    };
  });

  console.log('\n=== Page State ===');
  console.log(JSON.stringify(state, null, 2));

  await page.screenshot({ path: 'test-debug2.png' });
  console.log('\nScreenshot saved to test-debug2.png');

  await browser.close();
})();
