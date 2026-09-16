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
    if (response.status() >= 400) {
      failedRequests.push({
        url: response.url(),
        status: response.status()
      });
    }
  });

  // 测试图鉴页
  console.log('📖 测试图鉴页...');
  await page.goto('http://localhost:7894/pokemon', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  // 测试伤害计算器
  console.log('📊 测试伤害计算器...');
  await page.goto('http://localhost:7894/damage-calculator', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  // 测试对战页
  console.log('⚔️ 测试对战页...');
  await page.goto('http://localhost:7894/battle?mode=guest', { waitUntil: 'networkidle', timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000));

  console.log('\n❌ 失败请求:');
  failedRequests.forEach(r => {
    console.log(`   [${r.status}] ${r.url}`);
  });

  if (failedRequests.length === 0) {
    console.log('   无失败请求');
  }

  await browser.close();
})();
