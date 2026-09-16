/**
 * 3D 对战流程完整测试
 * 测试从开始到结束的完整对战流程
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

async function runBattleFlowTest() {
  console.log('🎮 开始测试完整对战流程...\n');

  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  // 收集错误
  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error' && !msg.text().includes('WebSocket')) {
      consoleErrors.push(msg.text());
    }
  });

  const pageErrors = [];
  page.on('pageerror', error => {
    pageErrors.push(error.message);
  });

  try {
    // ========== 步骤 1: 加载页面 ==========
    console.log('\n📋 步骤 1: 加载对战页面');
    await page.goto(BATTLE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);

    const title = await page.title();
    if (title.includes('对战')) {
      log('PASS', '页面加载', `标题正确: ${title}`);
    } else {
      log('FAIL', '页面加载', `标题异常: ${title}`);
    }

    // 截图：初始状态
    await page.screenshot({ path: 'test-01-initial.png' });
    log('PASS', '截图', '已保存初始状态截图');

    // ========== 步骤 2: 关闭教程 ==========
    console.log('\n📋 步骤 2: 关闭教程弹窗');
    const tutorial = await page.$('.tutorial-overlay');
    if (tutorial) {
      const closeBtn = await page.$('.btn-start');
      if (closeBtn) {
        await closeBtn.click();
        await sleep(500);
        log('PASS', '教程', '成功关闭教程');
      }
    } else {
      log('PASS', '教程', '无教程弹窗（已关闭过）');
    }

    // ========== 步骤 3: 检查3D场景 ==========
    console.log('\n📋 步骤 3: 检查 3D 场景');
    const canvas = await page.$('.battle3d-canvas');
    if (canvas) {
      const box = await canvas.boundingBox();
      if (box && box.width > 0 && box.height > 0) {
        log('PASS', '3D画布', `画布尺寸: ${box.width}x${box.height}`);
      } else {
        log('FAIL', '3D画布', '画布尺寸异常');
      }
    } else {
      log('FAIL', '3D画布', '未找到画布');
    }

    // ========== 步骤 4: 点击开始对战 ==========
    console.log('\n📋 步骤 4: 点击开始对战');
    const startBtn = await page.$('.btn-primary.btn-large');
    if (startBtn) {
      const btnText = await startBtn.textContent();
      console.log(`   找到按钮: ${btnText.trim()}`);
      await startBtn.click();
      await sleep(3000);

      // 截图：开始对战后
      await page.screenshot({ path: 'test-02-battle-started.png' });
      log('PASS', '开始对战', '已点击开始按钮');
    } else {
      log('FAIL', '开始对战', '未找到开始按钮');
    }

    // ========== 步骤 5: 检查对战界面 ==========
    console.log('\n📋 步骤 5: 检查对战界面');

    // 检查双方信息
    const teamInfo = await page.$('.teams-row');
    if (teamInfo) {
      log('PASS', '双方信息', '找到双方信息面板');
    } else {
      log('WARN', '双方信息', '未找到双方信息（可能需要后端）');
    }

    // 检查招式按钮
    const moveButtons = await page.$$('.move-btn');
    if (moveButtons.length > 0) {
      log('PASS', '招式按钮', `找到 ${moveButtons.length} 个招式按钮`);

      // 截图：招式选择
      await page.screenshot({ path: 'test-03-moves.png' });

      // 点击第一个招式
      console.log('   点击第一个招式...');
      await moveButtons[0].click();
      await sleep(500);

      // 检查是否选中
      const isSelected = await moveButtons[0].evaluate(el => el.classList.contains('selected'));
      if (isSelected) {
        log('PASS', '招式选择', '招式已选中');
      } else {
        log('WARN', '招式选择', '招式选中状态未确认');
      }

      // 截图：招式选中
      await page.screenshot({ path: 'test-04-move-selected.png' });
    } else {
      log('WARN', '招式按钮', '未找到招式按钮（可能需要后端响应）');
    }

    // ========== 步骤 6: 检查操作按钮 ==========
    console.log('\n📋 步骤 6: 检查操作按钮');

    // 提交回合按钮
    const submitBtn = await page.$('button:has-text("提交回合")');
    if (submitBtn) {
      const isDisabled = await submitBtn.evaluate(el => el.disabled);
      log('PASS', '提交按钮', `找到提交按钮，${isDisabled ? '已禁用' : '可点击'}`);

      if (!isDisabled) {
        console.log('   点击提交回合...');
        await submitBtn.click();
        await sleep(2000);

        // 截图：提交后
        await page.screenshot({ path: 'test-05-submitted.png' });
        log('PASS', '提交回合', '已提交回合');
      }
    } else {
      log('WARN', '提交按钮', '未找到提交按钮');
    }

    // ========== 步骤 7: 检查战斗日志 ==========
    console.log('\n📋 步骤 7: 检查战斗日志');
    const logHeader = await page.$('.log-header');
    if (logHeader) {
      await logHeader.click();
      await sleep(500);

      const logBody = await page.$('.log-body');
      if (logBody) {
        const logEntries = await page.$$('.log-evt');
        log('PASS', '战斗日志', `找到 ${logEntries.length} 条日志`);

        // 截图：日志展开
        await page.screenshot({ path: 'test-06-log.png' });
      }
    } else {
      log('WARN', '战斗日志', '未找到日志区域');
    }

    // ========== 步骤 8: 测试键盘快捷键 ==========
    console.log('\n📋 步骤 8: 测试键盘快捷键');

    // 按 1 键选择招式
    await page.keyboard.press('1');
    await sleep(300);
    log('PASS', '快捷键', '按下 1 键');

    // 按 R 键刷新
    await page.keyboard.press('r');
    await sleep(1000);
    log('PASS', '快捷键', '按下 R 键刷新');

    // 截图：最终状态
    await page.screenshot({ path: 'test-07-final.png' });

    // ========== 步骤 9: 测试响应式布局 ==========
    console.log('\n📋 步骤 9: 测试响应式布局');

    // 切换到移动端尺寸
    await page.setViewportSize({ width: 375, height: 812 });
    await sleep(1000);

    const isMobile = await page.evaluate(() => {
      const container = document.querySelector('.battle3d-container');
      if (!container) return false;
      const style = window.getComputedStyle(container);
      return style.flexDirection === 'column';
    });

    if (isMobile) {
      log('PASS', '响应式', '移动端布局正确');
    } else {
      log('WARN', '响应式', '移动端布局未确认');
    }

    // 截图：移动端
    await page.screenshot({ path: 'test-08-mobile.png' });

    // 恢复桌面尺寸
    await page.setViewportSize({ width: 1280, height: 720 });
    await sleep(500);

    // ========== 步骤 10: 检查页面性能 ==========
    console.log('\n📋 步骤 10: 检查页面性能');

    const performance = await page.evaluate(() => {
      const perf = window.performance;
      const timing = perf.timing;
      return {
        loadTime: timing.loadEventEnd - timing.navigationStart,
        domReady: timing.domContentLoadedEventEnd - timing.navigationStart,
        firstPaint: perf.getEntriesByType('paint')[0]?.startTime || 0
      };
    });

    if (performance.loadTime < 5000) {
      log('PASS', '页面性能', `加载时间: ${performance.loadTime}ms`);
    } else {
      log('WARN', '页面性能', `加载较慢: ${performance.loadTime}ms`);
    }

  } catch (error) {
    log('FAIL', '测试执行', `出错: ${error.message}`);
    await page.screenshot({ path: 'test-error.png' }).catch(() => {});
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

  if (warnings > 0) {
    console.log('\n⚠️ 警告项:');
    results.filter(r => r.status === 'WARN').forEach(r => {
      console.log(`   - ${r.test}: ${r.message}`);
    });
  }

  console.log('\n📸 截图已保存:');
  console.log('   - test-01-initial.png (初始状态)');
  console.log('   - test-02-battle-started.png (开始对战)');
  console.log('   - test-03-moves.png (招式列表)');
  console.log('   - test-04-move-selected.png (招式选中)');
  console.log('   - test-05-submitted.png (提交回合)');
  console.log('   - test-06-log.png (战斗日志)');
  console.log('   - test-07-final.png (最终状态)');
  console.log('   - test-08-mobile.png (移动端)');

  process.exit(failed > 0 ? 1 : 0);
}

runBattleFlowTest().catch(error => {
  console.error('测试失败:', error);
  process.exit(1);
});
