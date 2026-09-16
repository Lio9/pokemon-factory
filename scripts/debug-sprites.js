const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox']
  });

  const page = await browser.newPage();

  // 监听失败请求
  const failed = [];
  page.on('response', res => {
    if (res.status() >= 400) failed.push(res.url());
  });

  await page.goto('http://localhost:7894/battle?mode=guest', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  // 关闭教程
  const closeBtn = await page.$('.btn-start');
  if (closeBtn) await closeBtn.click();
  await new Promise(r => setTimeout(r, 500));

  // 开始对战
  const startBtn = await page.$('.btn-primary.btn-large');
  if (startBtn) await startBtn.click();
  await new Promise(r => setTimeout(r, 3000));

  // 确认预览
  const confirmBtn = await page.$('button:has-text("确认预览")');
  if (confirmBtn) await confirmBtn.click({ force: true });
  await new Promise(r => setTimeout(r, 3000));

  // 检查所有图片
  const allImgs = await page.evaluate(() => {
    const imgs = document.querySelectorAll('img');
    return Array.from(imgs).map(img => ({
      src: img.src,
      alt: img.alt || 'no-alt',
      width: img.naturalWidth,
      height: img.naturalHeight,
      broken: img.naturalWidth === 0
    }));
  });

  console.log('=== 所有图片状态 ===');
  allImgs.forEach(img => {
    const status = img.broken ? '❌ BROKEN' : '✅ OK';
    console.log(`${status} | ${img.alt} | ${img.width}x${img.height} | ${img.src}`);
  });

  console.log('\n=== 失败的网络请求 ===');
  if (failed.length === 0) {
    console.log('无失败请求');
  } else {
    failed.forEach(u => console.log('❌ ' + u));
  }

  // 截图
  await page.screenshot({ path: 'test-debug-sprites.png' });
  console.log('\n截图已保存: test-debug-sprites.png');

  await browser.close();
})();
