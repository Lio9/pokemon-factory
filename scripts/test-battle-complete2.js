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

const MAX_ROUNDS = 20;

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

    // ========== 步骤 4: 预览阶段 ==========
    console.log('📋 步骤 4: 预览阶段');

    // 获取当前选择状态
    const getSelectionState = async () => {
      return await page.evaluate(() => {
        const cards = document.querySelectorAll('.roster-card');
        const selected = document.querySelectorAll('.roster-card.selected');
        const leads = document.querySelectorAll('.roster-card.lead');
        // 查找确认按钮
        const buttons = document.querySelectorAll('button');
        let confirmBtn = null;
        for (const btn of buttons) {
          if (btn.textContent && btn.textContent.includes('确认预览')) {
            confirmBtn = btn;
            break;
          }
        }
        return {
          total: cards.length,
          selected: selected.length,
          leads: leads.length,
          confirmDisabled: confirmBtn ? confirmBtn.disabled : true
        };
      });
    };

    let state = await getSelectionState();
    console.log(`   初始状态: ${state.total}张卡片, ${state.selected}张选中, ${state.leads}张首发`);

    // 策略: 先取消所有选择，然后重新选择
    // 1. 取消所有已选中的（点击选中的卡片）
    const selectedCards = await page.$$('.roster-card.selected');
    for (const card of selectedCards) {
      await card.click();
      await sleep(200);
    }
    console.log('   已取消所有选择');

    // 2. 选择前4个卡片
    const allCards = await page.$$('.roster-card');
    for (let i = 0; i < Math.min(4, allCards.length); i++) {
      await allCards[i].click();
      await sleep(200);
    }
    console.log('   已选择4个宝可梦');

    // 3. 设置首发（点击前2个选中的卡片，使其变为首发）
    const newSelected = await page.$$('.roster-card.selected');
    if (newSelected.length >= 2) {
      await newSelected[0].click(); // 第一次点击设为首发
      await sleep(200);
      await newSelected[1].click(); // 第一次点击设为首发
      await sleep(200);
    }

    state = await getSelectionState();
    console.log(`   最终状态: ${state.selected}张选中, ${state.leads}张首发, 确认按钮${state.confirmDisabled ? '禁用' : '可用'}`);

    await page.screenshot({ path: 'test-complete-02-preview.png' });

    // 4. 确认预览
    if (!state.confirmDisabled) {
      const confirmBtn = await page.$('button:has-text("确认预览")');
      if (confirmBtn) {
        await confirmBtn.click();
        await sleep(3000);
        console.log('   ✅ 预览确认完成\n');
      }
    } else {
      console.log('   ⚠️ 确认按钮仍然禁用，尝试强制确认...');
      // 直接调用 confirmPreview 函数
      const confirmBtn2 = await page.$('button:has-text("确认预览")');
      if (confirmBtn2) {
        await confirmBtn2.click({ force: true });
      }
      await sleep(2000);
    }

    // ========== 步骤 5: 战斗回合循环 ==========
    console.log('📋 步骤 5: 开始战斗回合\n');

    let round = 0;
    let battleFinished = false;
    let result = '';

    while (round < MAX_ROUNDS && !battleFinished) {
      round++;
      console.log(`--- 回合 ${round} ---`);

      // 检查对战是否结束
      const statusEl = await page.$('.float-status');
      if (statusEl) {
        const statusText = await statusEl.textContent() || '';
        if (statusText.includes('胜利') || statusText.includes('失败') || 
            statusText.includes('Win') || statusText.includes('Lose') ||
            statusText.includes('已结束')) {
          result = statusText;
          console.log(`\n🏆 对战结束: ${result}`);
          battleFinished = true;
          break;
        }
      }

      // 检查结算弹窗
      const settlementModal = await page.$('.settlement-overlay');
      if (settlementModal) {
        result = '结算弹窗出现';
        console.log(`\n🏆 ${result}`);
        battleFinished = true;
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
            // 选择第一个招式（通常是攻击招式）
            await blockMoves[0].click();
            await sleep(300);
            console.log(`   宝可梦 ${i + 1}: 选择了招式 1`);
          }

          // 检查是否需要选择目标
          const targetBtns = await page.$$('.target-btn:not(.active)');
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
            await sleep(3000);
          } else {
            console.log('   ⚠️ 提交按钮禁用');
            // 等待一下再重试
            await sleep(1000);
          }
        }
      } else {
        // 可能在替补阶段
        const replacementBtns = await page.$$('.replace-btn');
        if (replacementBtns.length > 0) {
          console.log('   检测到替补阶段');
          await replacementBtns[0].click();
          await sleep(300);

          const confirmReplaceBtn = await page.$('button:has-text("确认替补")');
          if (confirmReplaceBtn) {
            await confirmReplaceBtn.click();
            await sleep(2000);
            console.log('   ✅ 替补确认完成');
          }
        } else {
          // 尝试刷新
          console.log('   尝试刷新状态...');
          const refreshBtn = await page.$('button:has-text("🔄")');
          if (refreshBtn) {
            await refreshBtn.click();
            await sleep(2000);
          }
        }
      }

      // 获取当前状态
      const currentStatus = await page.evaluate(() => {
        const el = document.querySelector('.float-status');
        return el ? el.textContent : 'unknown';
      });
      console.log(`   状态: ${currentStatus}\n`);
    }

    if (!battleFinished) {
      console.log(`\n⚠️ 达到最大回合数 ${MAX_ROUNDS}，停止测试`);
      result = '超时';
    }

    // ========== 步骤 6: 最终截图 ==========
    console.log('\n📋 步骤 6: 保存最终截图');
    await page.screenshot({ path: 'test-complete-final.png' });

    // 输出结果
    console.log('\n' + '='.repeat(60));
    console.log('📊 完整对战测试结果');
    console.log('='.repeat(60));
    console.log(`🎮 对战结果: ${result}`);
    console.log(`⏱️ 总回合数: ${round}`);
    console.log(`📸 截图已保存到 test-complete-*.png`);
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ 测试出错:', error.message);
    await page.screenshot({ path: 'test-complete-error.png' }).catch(() => {});
  } finally {
    await browser.close();
  }
}

runCompleteBattle().catch(error => {
  console.error('测试失败:', error);
  process.exit(1);
});
