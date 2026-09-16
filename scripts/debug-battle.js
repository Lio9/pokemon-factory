const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox']
  });
  const page = await browser.newPage();
  await page.goto('http://localhost:7894/battle?mode=guest', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  // 关闭教程
  const closeBtn = await page.$('.btn-start');
  if (closeBtn) await closeBtn.click();
  await new Promise(r => setTimeout(r, 500));

  // 点击开始
  const startBtn = await page.$('.btn-primary.btn-large');
  if (startBtn) await startBtn.click();
  await new Promise(r => setTimeout(r, 3000));

  // 获取页面HTML
  const html = await page.evaluate(() => {
    const panel = document.querySelector('.battle3d-panel');
    return panel ? panel.innerHTML.substring(0, 5000) : 'No panel found';
  });
  console.log('=== Panel HTML ===');
  console.log(html);

  // 截图
  await page.screenshot({ path: 'test-debug.png' });
  console.log('\nScreenshot saved to test-debug.png');

  await browser.close();
})();
