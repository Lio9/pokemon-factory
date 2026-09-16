const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox']
  });

  const page = await browser.newPage();

  // 监听所有请求
  const failedRequests = [];
  page.on('response', response => {
    if (response.status() === 404) {
      failedRequests.push({
        url: response.url(),
        status: response.status()
      });
    }
  });

  // 测试图鉴页
  console.log('📖 测试图鉴页...');
  await page.goto('http://localhost:7894/pokemon', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 3000));

  console.log('\n❌ 404 请求:');
  failedRequests.forEach(r => {
    console.log(`   - ${r.url}`);
  });

  if (failedRequests.length === 0) {
    console.log('   无404请求');
  }

  await browser.close();
})();
