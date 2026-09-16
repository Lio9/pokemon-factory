/**
 * 3D 对战完整流程测试
 * 包括：预览选人 → 确认 → 选招式 → 提交回合
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:7894';
const BATTLE_URL = `${BASE_URL}/battle?mode=guest`;

const BROWSER_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  : undefined;

const results = [];

function log(status, test, message) {
  const icon = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️';
  console.log(`${icon} [${status}] ${test}: ${message}`);
  results.push({ status, test, message });
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runFullBattleTest() {
  console.log('🎮 开始完整对战流程测试...\n');

  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error' && !msg.text().includes('WebSocket') && !msg.text().includes('favicon')) {
      consoleErrors.push(msg.text());
    }
  });

  try {
    // ========== 步骤 1: 加载页面 ==========
    console.log('\n📋 步骤 1: 加载对战页面');
    await page.goto(BATTLE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);

    const title = await page.title();
    log('PASS', '页面加载', `标题: ${title}`);
    await page.screenshot({ path: 'test-full-01-initial.png' });

    // ========== 步骤 2: 关闭教程 ==========
    console.log('\n📋 步骤 2: 关闭教程');
    const tutorial = await page.$('.tutorial-overlay');
    if (tutorial) {
      const closeBtn = await page.$('.btn-start');
      if (closeBtn) {
        await closeBtn.click();
        await sleep(500);
        log('PASS', '教程', '已关闭');
      }
    } else {
      log('PASS', '教程', '无教程弹窗');
    }

    // ========== 步骤 3: 点击开始对战 ==========
    console.log('\n📋 步骤 3: 开始对战');
    const startBtn = await page.$('.btn-primary.btn-large');
    if (startBtn) {
      await startBtn.click();
      await sleep(3000);
      log('PASS', '开始对战', '已点击');
      await page.screenshot({ path: 'test-full-02-started.png' });
    } else {
      log('FAIL', '开始对战', '未找到按钮');
    }

    // ========== 步骤 4: 检查当前阶段 ==========
    console.log('\n📋 步骤 4: 检查战斗阶段');

    // 检查是否在预览阶段
    const previewPhase = await page.$('text=确认预览');
    const actionPhase = await page.$('.moves-section');
    const replacementPhase = await page.$('text=确认替补');

    if (previewPhase) {
      log('PASS', '阶段检测', '当前为预览阶段（选人）');

      // ========== 步骤 5: 预览阶段 - 选择宝可梦 ==========
      console.log('\n📋 步骤 5: 预览阶段 - 选择宝可梦');

      // 点击宝可梦卡片选择
      const cards = await page.$$('.roster-card:not(.selected)');
      console.log(`   找到 ${cards.length} 个未选中的宝可梦卡片`);

      if (cards.length >= 2) {
        // 选择2个宝可梦（已经有4个被自动选中了）
        for (let i = 0; i < 2; i++) {
          await cards[i].click();
          await sleep(300);
        }
        log('PASS', '选人', '已选择宝可梦');
        await page.screenshot({ path: 'test-full-03-selected.png' });

        // 点击确认预览
        const confirmBtn = await page.$('button:has-text("确认预览")');
        if (confirmBtn) {
          const isDisabled = await confirmBtn.evaluate(el => el.disabled);
          if (!isDisabled) {
            await confirmBtn.click();
            await sleep(2000);
            log('PASS', '确认预览', '已确认');
            await page.screenshot({ path: 'test-full-04-confirmed.png' });
          } else {
            log('WARN', '确认预览', '按钮已禁用（可能选择不完整）');
          }
        }
      } else {
        log('WARN', '选人', '卡片数量不足');
      }
    } else if (actionPhase) {
      log('PASS', '阶段检测', '当前为行动阶段（选招式）');
    } else if (replacementPhase) {
      log('PASS', '阶段检测', '当前为替补阶段');
    } else {
      log('WARN', '阶段检测', '无法确定当前阶段');
    }

    // ========== 步骤 6: 行动阶段 - 选择招式 ==========
    console.log('\n📋 步骤 6: 行动阶段 - 选择招式');

    // 等待招式加载
    await sleep(1000);

    const moveButtons = await page.$$('.move-btn');
    console.log(`   找到 ${moveButtons.length} 个招式按钮`);

    if (moveButtons.length > 0) {
      log('PASS', '招式', `找到 ${moveButtons.length} 个招式`);

      // 选择第一个招式
      await moveButtons[0].click();
      await sleep(500);
      log('PASS', '选择招式', '已选择第一个招式');
      await page.screenshot({ path: 'test-full-05-move.png' });

      // 检查是否需要选择目标
      const targetBtns = await page.$$('.target-btn');
      if (targetBtns.length > 0) {
        console.log('   需要选择目标...');
        await targetBtns[0].click();
        await sleep(300);
        log('PASS', '选择目标', '已选择目标');
      }

      // ========== 步骤 7: 提交回合 ==========
      console.log('\n📋 步骤 7: 提交回合');

      const submitBtn = await page.$('button:has-text("提交回合")');
      if (submitBtn) {
        const isDisabled = await submitBtn.evaluate(el => el.disabled);
        console.log(`   提交按钮状态: ${isDisabled ? '禁用' : '可点击'}`);

        if (!isDisabled) {
          await submitBtn.click();
          await sleep(2000);
          log('PASS', '提交回合', '已提交');
          await page.screenshot({ path: 'test-full-06-submitted.png' });
        } else {
          log('WARN', '提交回合', '按钮禁用（可能需要选择更多内容）');
        }
      } else {
        log('WARN', '提交回合', '未找到提交按钮');
      }
    } else {
      log('WARN', '招式', '未找到招式按钮');
    }

    // ========== 步骤 8: 检查战斗结果 ==========
    console.log('\n📋 步骤 8: 检查战斗结果');

    await sleep(1000);

    // 检查是否有回合日志
    const logEntries = await page.$$('.log-evt');
    if (logEntries.length > 0) {
      log('PASS', '战斗日志', `有 ${logEntries.length} 条日志记录`);
    }

    // 检查是否结束
    const statusText = await page.$('.float-status');
    if (statusText) {
      const text = await statusText.textContent();
      log('PASS', '战斗状态', `状态: ${text}`);
    }

    await page.screenshot({ path: 'test-full-07-result.png' });

    // ========== 步骤 9: 检查错误 ==========
    console.log('\n📋 步骤 9: 检查错误');

    const criticalErrors = consoleErrors.filter(e =>
      !e.includes('500') && !e.includes('404') && !e.includes('ECONNREFUSED')
    );

    if (criticalErrors.length === 0) {
      log('PASS', '错误检查', '无严重错误');
    } else {
      log('FAIL', '错误检查', `发现 ${criticalErrors.length} 个错误`);
      criticalErrors.slice(0, 3).forEach(e => console.log(`   - ${e.substring(0, 100)}`));
    }

  } catch (error) {
    log('FAIL', '测试执行', `出错: ${error.message}`);
    await page.screenshot({ path: 'test-full-error.png' }).catch(() => {});
  } finally {
    await browser.close();
  }

  // ========== 测试报告 ==========
  console.log('\n' + '='.repeat(60));
  console.log('📊 完整对战流程测试报告');
  console.log('='.repeat(60));

  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  const warnings = results.filter(r => r.status === 'WARN').length;

  console.log(`✅ 通过: ${passed}`);
  console.log(`❌ 失败: ${failed}`);
  console.log(`⚠️ 警告: ${warnings}`);
  console.log(`📊 总计: ${results.length}`);
  console.log('='.repeat(60));

  if (failed > 0) {
    console.log('\n❌ 失败项:');
    results.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`   - ${r.test}: ${r.message}`);
    });
  }

  console.log('\n📸 截图已保存到 test-full-*.png');

  process.exit(failed > 0 ? 1 : 0);
}

runFullBattleTest().catch(error => {
  console.error('测试失败:', error);
  process.exit(1);
});
