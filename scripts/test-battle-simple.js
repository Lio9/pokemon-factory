/**
 * 简化版完整对战测试
 * 直接使用初始选择，不重新选人
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:7894';
const BATTLE_URL = `${BASE_URL}/battle?mode=guest`;
const BROWSER_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  : undefined;

const MAX_ROUNDS = 25;

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runBattle() {
  console.log('🎮 开始完整对战测试...\n');

  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

  try {
    // 1. 加载页面
    console.log('📋 加载页面...');
    await page.goto(BATTLE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);

    // 2. 关闭教程
    const tutorial = await page.$('.tutorial-overlay');
    if (tutorial) {
      const closeBtn = await page.$('.btn-start');
      if (closeBtn) await closeBtn.click();
      await sleep(500);
    }

    // 3. 开始对战
    console.log('📋 开始对战...');
    const startBtn = await page.$('.btn-primary.btn-large');
    if (startBtn) {
      await startBtn.click();
      await sleep(3000);
    }

    // 4. 确认预览（使用默认选择）
    console.log('📋 确认预览...');
    await sleep(1000);

    // 直接点击确认按钮
    const confirmBtn = await page.$('button:has-text("确认预览")');
    if (confirmBtn) {
      await confirmBtn.click({ force: true });
      await sleep(3000);
      console.log('   ✅ 预览已确认');
    }

    // 5. 战斗循环
    console.log('\n📋 开始战斗...\n');

    let round = 0;
    let finished = false;
    let result = '';

    while (round < MAX_ROUNDS && !finished) {
      round++;
      console.log(`--- 回合 ${round} ---`);

      // 检查是否结束
      const status = await page.evaluate(() => {
        const el = document.querySelector('.float-status');
        return el ? el.textContent : '';
      });

      if (status.includes('胜利') || status.includes('失败') || status.includes('Win') || status.includes('Lose')) {
        result = status;
        finished = true;
        console.log(`\n🏆 对战结束: ${result}`);
        break;
      }

      // 检查结算弹窗
      const settlement = await page.$('.settlement-overlay');
      if (settlement) {
        result = '结算弹窗';
        finished = true;
        console.log(`\n🏆 ${result}`);
        break;
      }

      // 检查是否有招式按钮
      const moveButtons = await page.$$('.move-btn');
      if (moveButtons.length > 0) {
        // 选择第一个招式
        await moveButtons[0].click();
        await sleep(300);

        // 选择目标（如果需要）
        const targetBtns = await page.$$('.target-btn:not(.active)');
        if (targetBtns.length > 0) {
          await targetBtns[0].click();
          await sleep(200);
        }

        // 提交回合（添加超时处理）
        const submitBtn = await page.$('button:has-text("提交回合")');
        if (submitBtn) {
          const disabled = await submitBtn.evaluate(el => el.disabled);
          if (!disabled) {
            await submitBtn.click();
            await sleep(3000);
            console.log('   ✅ 回合已提交');
          } else {
            console.log('   ⚠️ 提交按钮禁用，等待后重试...');
            await sleep(2000);
            // 再次检查
            const stillDisabled = await submitBtn.evaluate(el => el.disabled);
            if (!stillDisabled) {
              await submitBtn.click();
              await sleep(3000);
              console.log('   ✅ 回合已提交（重试成功）');
            } else {
              // 可能需要刷新
              const refreshBtn = await page.$('button:has-text("🔄")');
              if (refreshBtn) {
                await refreshBtn.click();
                await sleep(2000);
              }
            }
          }
        }
      } else {
        // 可能是替补阶段
        const replaceBtns = await page.$$('.replace-card, .replace-btn');
        if (replaceBtns.length > 0) {
          console.log('   检测到替补阶段');
          await replaceBtns[0].click();
          await sleep(300);
          const confirmReplace = await page.$('button:has-text("确认替补")');
          if (confirmReplace) {
            await confirmReplace.click();
            await sleep(2000);
            console.log('   ✅ 替补已确认');
          }
        } else {
          // 刷新状态
          console.log('   刷新状态...');
          const refreshBtn = await page.$('button:has-text("🔄")');
          if (refreshBtn) await refreshBtn.click();
          await sleep(2000);
        }
      }

      console.log(`   状态: ${status}\n`);
    }

    if (!finished) {
      console.log(`\n⚠️ 达到最大回合数 ${MAX_ROUNDS}`);
      result = '超时';
    }

    // 最终截图
    await page.screenshot({ path: 'test-battle-final.png' });

    console.log('\n' + '='.repeat(60));
    console.log('📊 测试结果');
    console.log('='.repeat(60));
    console.log(`🎮 结果: ${result}`);
    console.log(`⏱️ 回合数: ${round}`);
    console.log('='.repeat(60));

  } catch (error) {
    console.error('❌ 错误:', error.message);
    await page.screenshot({ path: 'test-battle-error.png' }).catch(() => {});
  } finally {
    await browser.close();
  }
}

runBattle().catch(console.error);
