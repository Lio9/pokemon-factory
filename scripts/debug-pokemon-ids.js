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

  // 开始对战
  const startBtn = await page.$('.btn-primary.btn-large');
  if (startBtn) await startBtn.click();
  await new Promise(r => setTimeout(r, 3000));

  // 确认预览
  const confirmBtn = await page.$('button:has-text("确认预览")');
  if (confirmBtn) await confirmBtn.click({ force: true });
  await new Promise(r => setTimeout(r, 3000));

  // 检查图片 src
  const imgData = await page.evaluate(() => {
    const imgs = document.querySelectorAll('.mon-sprite');
    return Array.from(imgs).map(img => ({
      src: img.src,
      alt: img.alt,
      naturalWidth: img.naturalWidth,
      complete: img.complete
    }));
  });

  console.log('=== 精灵图数据 ===');
  imgData.forEach((img, i) => {
    console.log(`\n图片 ${i + 1}:`);
    console.log(`  src: ${img.src}`);
    console.log(`  alt: ${img.alt}`);
    console.log(`  naturalWidth: ${img.naturalWidth}`);
    console.log(`  complete: ${img.complete}`);
  });

  // 检查原始数据
  const pokemonData = await page.evaluate(() => {
    // 尝试获取 Vue 组件数据
    const app = document.querySelector('#app');
    if (app && app.__vue_app__) {
      // 尝试从 DOM 中提取数据
      const cards = document.querySelectorAll('.mon-card');
      return Array.from(cards).map(card => {
        const name = card.querySelector('.mon-name')?.textContent;
        const img = card.querySelector('.mon-sprite');
        return {
          name: name,
          imgSrc: img?.src
        };
      });
    }
    return [];
  });

  console.log('\n=== 宝可梦卡片数据 ===');
  pokemonData.forEach(p => {
    console.log(`${p.name}: ${p.imgSrc}`);
  });

  await browser.close();
})();
