const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    args: ['--no-sandbox']
  });

  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

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

  // 提交几个回合直到进入替补阶段
  for (let i = 0; i < 5; i++) {
    console.log(`\n--- 回合 ${i + 1} ---`);

    // 检查状态
    const status = await page.evaluate(() => {
      const el = document.querySelector('.float-status');
      return el ? el.textContent.trim() : 'unknown';
    });
    console.log(`状态: ${status}`);

    // 检查是否有替补按钮
    const replaceBtns = await page.$$('.replace-btn, .replace-card, button:has-text("确认替补")');
    console.log(`替补按钮数量: ${replaceBtns.length}`);

    // 检查所有按钮
    const allButtons = await page.evaluate(() => {
      const btns = document.querySelectorAll('button');
      return Array.from(btns).map(b => ({
        text: b.textContent.trim().substring(0, 30),
        disabled: b.disabled,
        className: b.className.substring(0, 50)
      }));
    });
    console.log('所有按钮:');
    allButtons.forEach(b => {
      console.log(`  ${b.disabled ? '❌' : '✅'} [${b.className}] ${b.text}`);
    });

    // 如果是替补阶段
    if (status.includes('补位')) {
      console.log('\n🔄 检测到替补阶段！');

      // 截图
      await page.screenshot({ path: `test-replacement-${i}.png` });

      // 查找替补选项
      const replaceOptions = await page.evaluate(() => {
        // 查找所有可能是替补的元素
        const elements = document.querySelectorAll('.replace-btn, .replace-card, [class*="replace"]');
        return Array.from(elements).map(el => ({
          tag: el.tagName,
          text: el.textContent.trim().substring(0, 50),
          className: el.className
        }));
      });
      console.log('替补选项:', replaceOptions);

      break;
    }

    // 尝试提交
    const submitBtn = await page.$('button:has-text("提交回合")');
    if (submitBtn) {
      const disabled = await submitBtn.evaluate(el => el.disabled);
      if (!disabled) {
        // 选择招式
        const moveBtns = await page.$$('.move-btn');
        if (moveBtns.length > 0) {
          await moveBtns[0].click();
          await new Promise(r => setTimeout(r, 300));
        }

        // 选择目标
        const targetBtns = await page.$$('.target-btn:not(.active)');
        if (targetBtns.length > 0) {
          await targetBtns[0].click();
          await new Promise(r => setTimeout(r, 200));
        }

        await submitBtn.click();
        console.log('✅ 已提交回合');
        await new Promise(r => setTimeout(r, 3000));
      } else {
        console.log('⚠️ 提交按钮禁用');
        await new Promise(r => setTimeout(r, 1000));
      }
    } else {
      console.log('未找到提交按钮');
      await new Promise(r => setTimeout(r, 1000));
    }
  }

  await browser.close();
})();
