/**
 * 完整对战流程测试 - 从开始到结束
 * 自动完成整个对战，直到胜利或失败
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:7894';
const BATTLE_URL = `${BASE_URL}/battle?mode=guest`;

const BROWSER_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  : undefined;

const MAX_ROUNDS = 20; // 最大回合数，防止无限循环

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runCompleteBattle() {
  console.log('🎮 开始完整对战测试（从开始到结束）...\n');

  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  const logs = [];
  page.on('console', msg => {
    if (msg.type() === 'error' && !msg.text().includes('WebSocket') && !msg.text().includes('favicon')) {
      logs.push(`[${msg.type()}] ${msg.text()}`);
    }
  });

  try {
    // ========== 步骤 1: 加载页面 ==========
    console.log('📋 步骤 1: 加载对战页面');
    await page.goto(BATTLE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);
    console.log('   ✅ 页面加载完成\n');

    // ========== 步骤 2: 关闭教程 ==========
    console.log('📋 步骤 2: 关闭教程');
    const tutorial = await page.$('.tutorial-overlay');
    if (tutorial) {
      const closeBtn = await page.$('.btn-start');
      if (closeBtn) {
        await closeBtn.click();
        await sleep(500);
        console.log('   ✅ 教程已关闭\n');
      }
    }

    // ========== 步骤 3: 开始对战 ==========
    console.log('📋 步骤 3: 开始对战');
    const startBtn = await page.$('.btn-primary.btn-large');
    if (startBtn) {
      await startBtn.click();
      await sleep(3000);
      console.log('   ✅ 对战已开始\n');
    }

    await page.screenshot({ path: 'test-complete-01-started.png' });

    // ========== 步骤 4: 预览阶段 - 选择宝可梦 ==========
    console.log('📋 步骤 4: 预览阶段 - 选择宝可梦');

    // 等待预览界面加载
    await sleep(1000);

    // 检查是否在预览阶段
    const previewPhase = await page.$('.preview-section');
    if (previewPhase) {
      // 自动选择宝可梦（点击未选中的卡片）
      const unselectedCards = await page.$$('.roster-card:not(.selected)');
      console.log(`   找到 ${unselectedCards.length} 个未选中的宝可梦`);

      // 点击所有未选中的卡片
      for (const card of unselectedCards) {
        await card.click();
        await sleep(200);
      }

      // 设置首发（点击前2个选中的卡片）
      const selectedCards = await page.$$('.roster-card.selected');
      if (selectedCards.length >= 2) {
        // 第一个设为首发
        await selectedCards[0].click();
        await sleep(200);
        await selectedCards[0].click();
        await sleep(200);
      }

      console.log('   ✅ 宝可梦选择完成');
      await page.screenshot({ path: 'test-complete-02-selected.png' });

      // 确认预览
      const confirmBtn = await page.$('button:has-text("确认预览")');
      if (confirmBtn) {
        const isDisabled = await confirmBtn.evaluate(el => el.disabled);
        if (!isDisabled) {
          await confirmBtn.click();
          await sleep(2000);
          console.log('   ✅ 预览确认完成\n');
        } else {
          console.log('   ⚠️ 确认按钮禁用，尝试直接继续...\n');
        }
      }
    } else {
      console.log('   ⚠️ 未检测到预览阶段，可能已跳过\n');
    }

    // ========== 步骤 5: 战斗回合循环 ==========
    console.log('📋 步骤 5: 开始战斗回合\n');

    let round = 0;
    let battleFinished = false;

    while (round < MAX_ROUNDS && !battleFinished) {
      round++;
      console.log(`--- 回合 ${round} ---`);

      // 检查对战是否结束
      const statusEl = await page.$('.float-status');
      if (statusEl) {
        const statusText = await statusEl.textContent();
        if (statusText.includes('胜利') || statusText.includes('失败') || statusText.includes('Win') || statusText.includes('Lose')) {
          console.log(`\n🏆 对战结束: ${statusText}`);
          battleFinished = true;
          break;
        }
      }

      // 检查是否有结算弹窗
      const settlementModal = await page.$('.settlement-overlay');
      if (settlementModal) {
        console.log('\n🏆 检测到结算弹窗，对战结束！');
        battleFinished = true;
        await page.screenshot({ path: 'test-complete-result.png' });
        break;
      }

      // 检查是否在行动阶段
      const moveButtons = await page.$$('.move-btn');
      if (moveButtons.length > 0) {
        console.log(`   找到 ${moveButtons.length} 个招式按钮`);

        // 为每个活跃的宝可梦选择招式
        const moveBlocks = await page.$$('.move-block');
        for (let i = 0; i < moveBlocks.length; i++) {
          const blockMoves = await moveBlocks[i].$$('.move-btn');
          if (blockMoves.length > 0) {
            // 随机选择一个招式（或选择第一个）
            const moveIdx = Math.floor(Math.random() * blockMoves.length);
            await blockMoves[moveIdx].click();
            await sleep(300);
            console.log(`   宝可梦 ${i + 1}: 选择了招式 ${moveIdx + 1}`);
          }

          // 检查是否需要选择目标
          const targetBtns = await page.$$('.target-btn');
          if (targetBtns.length > 0) {
            await targetBtns[0].click();
            await sleep(200);
            console.log(`   选择了目标`);
          }
        }

        await page.screenshot({ path: `test-complete-round-${round}.png` });

        // 提交回合
        const submitBtn = await page.$('button:has-text("提交回合")');
        if (submitBtn) {
          const isDisabled = await submitBtn.evaluate(el => el.disabled);
          if (!isDisabled) {
            await submitBtn.click();
            console.log('   ✅ 回合已提交');
            await sleep(3000); // 等待回合结算
          } else {
            console.log('   ⚠️ 提交按钮禁用，尝试刷新...');
            const refreshBtn = await page.$('button:has-text("🔄")');
            if (refreshBtn) {
              await refreshBtn.click();
              await sleep(2000);
            }
          }
        }
      } else {
        // 可能在替补阶段
        const replacementBtns = await page.$$('.replace-btn');
        if (replacementBtns.length > 0) {
          console.log('   检测到替补阶段');
          // 选择第一个替补
          await replacementBtns[0].click();
          await sleep(300);

          const confirmReplaceBtn = await page.$('button:has-text("确认替补")');
          if (confirmReplaceBtn) {
            await confirmReplaceBtn.click();
            await sleep(2000);
            console.log('   ✅ 替补确认完成');
          }
        } else {
          // 可能在等待中，点击刷新
          console.log('   未检测到招式按钮，尝试刷新...');
          const refreshBtn = await page.$('button:has-text("🔄")');
          if (refreshBtn) {
            await refreshBtn.click();
            await sleep(2000);
          }
        }
      }

      // 检查对战状态
      const status = await page.evaluate(() => {
        const el = document.querySelector('.float-status');
        return el ? el.textContent : 'unknown';
      });
      console.log(`   当前状态: ${status}\n`);
    }

    if (!battleFinished) {
      console.log(`\n⚠️ 达到最大回合数 ${MAX_ROUNDS}，停止测试`);
    }

    // ========== 步骤 6: 最终截图 ==========
    console.log('\n📋 步骤 6: 保存最终截图');
    await page.screenshot({ path: 'test-complete-final.png' });

    // 检查控制台错误
    const criticalErrors = logs.filter(e => !e.includes('500') && !e.includes('404'));
    if (criticalErrors.length > 0) {
      console.log('\n⚠️ 控制台错误:');
      criticalErrors.slice(0, 3).forEach(e => console.log(`   - ${e.substring(0, 100)}`));
    } else {
      console.log('\n✅ 无严重控制台错误');
    }

  } catch (error) {
    console.error('\n❌ 测试出错:', error.message);
    await page.screenshot({ path: 'test-complete-error.png' }).catch(() => {});
  } finally {
    await browser.close();
  }

  console.log('\n' + '='.repeat(60));
  console.log('📊 完整对战测试完成');
  console.log('='.repeat(60));
  console.log(`📸 截图已保存到 test-complete-*.png`);
}

runCompleteBattle().catch(error => {
  console.error('测试失败:', error);
  process.exit(1);
});
