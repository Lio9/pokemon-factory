/**
 * 3D 对战页面自动化测试脚本
 * 使用 Playwright 测试页面功能
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:7894';
const BATTLE_URL = `${BASE_URL}/battle?mode=guest`;

// 使用已安装的浏览器
const BROWSER_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  : undefined;

// 测试结果
const results = [];

// 辅助函数
function log(status, test, message) {
  const icon = status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️';
  console.log(`${icon} [${status}] ${test}: ${message}`);
  results.push({ status, test, message });
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// 测试用例
async function runTests() {
  console.log('🚀 开始测试 3D 对战页面...\n');

  const browser = await chromium.launch({
    headless: true,
    executablePath: BROWSER_PATH,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  // 收集控制台错误
  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });

  // 收集页面错误
  const pageErrors = [];
  page.on('pageerror', error => {
    pageErrors.push(error.message);
  });

  try {
    // 测试 1: 页面加载
    console.log('📋 测试 1: 页面加载');
    await page.goto(BATTLE_URL, { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);
    log('PASS', '页面加载', '页面成功加载');

    // 测试 2: 检查标题
    console.log('📋 测试 2: 检查标题');
    const title = await page.title();
    if (title.includes('对战')) {
      log('PASS', '页面标题', `标题包含"对战": ${title}`);
    } else {
      log('FAIL', '页面标题', `标题不包含"对战": ${title}`);
    }

    // 测试 3: 检查 3D 画布
    console.log('📋 测试 3: 检查 3D 画布');
    const canvas = await page.$('.battle3d-canvas');
    if (canvas) {
      log('PASS', '3D 画布', '找到 3D 画布元素');
    } else {
      log('FAIL', '3D 画布', '未找到 3D 画布元素');
    }

    // 测试 4: 检查控制面板
    console.log('📋 测试 4: 检查控制面板');
    const panel = await page.$('.battle3d-panel');
    if (panel) {
      log('PASS', '控制面板', '找到控制面板元素');
    } else {
      log('FAIL', '控制面板', '未找到控制面板元素');
    }

    // 测试 5: 检查开始按钮
    console.log('📋 测试 5: 检查开始按钮');
    const startButton = await page.$('.btn-primary.btn-large');
    if (startButton) {
      const buttonText = await startButton.textContent();
      log('PASS', '开始按钮', `找到开始按钮: ${buttonText.trim()}`);
    } else {
      log('FAIL', '开始按钮', '未找到开始按钮');
    }

    // 测试 5.5: 关闭教程（如果存在）
    console.log('📋 测试 5.5: 关闭教程');
    const tutorialOverlay = await page.$('.tutorial-overlay');
    if (tutorialOverlay) {
      const closeBtn = await page.$('.btn-start');
      if (closeBtn) {
        await closeBtn.click();
        await sleep(500);
        log('PASS', '关闭教程', '成功关闭教程弹窗');
      }
    }

    // 测试 6: 点击开始对战
    console.log('📋 测试 6: 点击开始对战');
    if (startButton) {
      await startButton.click();
      await sleep(3000);

      // 检查是否进入对战
      const teamInfo = await page.$('.teams-row');
      if (teamInfo) {
        log('PASS', '开始对战', '成功进入对战界面');
      } else {
        log('WARN', '开始对战', '点击后未检测到对战界面（可能需要登录）');
      }
    }

    // 测试 7: 检查招式按钮
    console.log('📋 测试 7: 检查招式按钮');
    const moveButtons = await page.$$('.move-btn');
    if (moveButtons.length > 0) {
      log('PASS', '招式按钮', `找到 ${moveButtons.length} 个招式按钮`);
    } else {
      log('WARN', '招式按钮', '未找到招式按钮（可能需要先开始对战）');
    }

    // 测试 8: 检查音效按钮
    console.log('📋 测试 8: 检查音效按钮');
    const muteButton = await page.$('.icon-btn');
    if (muteButton) {
      log('PASS', '音效按钮', '找到音效切换按钮');
    } else {
      log('FAIL', '音效按钮', '未找到音效切换按钮');
    }

    // 测试 9: 检查性能按钮
    console.log('📋 测试 9: 检查性能按钮');
    const perfButton = await page.$('button:has-text("⚡")');
    if (perfButton) {
      log('PASS', '性能按钮', '找到性能切换按钮');
    } else {
      log('FAIL', '性能按钮', '未找到性能切换按钮');
    }

    // 测试 10: 检查调试按钮
    console.log('📋 测试 10: 检查调试按钮');
    const debugButton = await page.$('button:has-text("🐛")');
    if (debugButton) {
      log('PASS', '调试按钮', '找到调试按钮');
    } else {
      log('FAIL', '调试按钮', '未找到调试按钮');
    }

    // 测试 11: 检查相机按钮
    console.log('📋 测试 11: 检查相机按钮');
    const cameraButton = await page.$('button:has-text("📷")');
    if (cameraButton) {
      log('PASS', '相机按钮', '找到相机切换按钮');
    } else {
      log('FAIL', '相机按钮', '未找到相机切换按钮');
    }

    // 测试 12: 检查教程弹窗
    console.log('📋 测试 12: 检查教程弹窗');
    const tutorial = await page.$('.tutorial-overlay');
    if (tutorial) {
      log('PASS', '教程弹窗', '检测到教程弹窗');
      // 关闭教程
      const closeButton = await page.$('.btn-start');
      if (closeButton) {
        await closeButton.click();
        await sleep(500);
        log('PASS', '关闭教程', '成功关闭教程弹窗');
      }
    } else {
      log('WARN', '教程弹窗', '未检测到教程弹窗（可能已关闭或未触发）');
    }

    // 测试 13: 检查控制台错误
    console.log('📋 测试 13: 检查控制台错误');
    const criticalErrors = consoleErrors.filter(e =>
      !e.includes('WebSocket') &&
      !e.includes('favicon') &&
      !e.includes('third-party') &&
      !e.includes('extension')
    );
    if (criticalErrors.length === 0) {
      log('PASS', '控制台错误', '无严重控制台错误');
    } else {
      log('FAIL', '控制台错误', `发现 ${criticalErrors.length} 个错误`);
      criticalErrors.slice(0, 3).forEach(e => {
        console.log(`   - ${e.substring(0, 100)}...`);
      });
    }

    // 测试 14: 检查页面错误
    console.log('📋 测试 14: 检查页面错误');
    if (pageErrors.length === 0) {
      log('PASS', '页面错误', '无页面错误');
    } else {
      log('FAIL', '页面错误', `发现 ${pageErrors.length} 个错误`);
      pageErrors.slice(0, 3).forEach(e => {
        console.log(`   - ${e.substring(0, 100)}...`);
      });
    }

    // 测试 15: 响应式检查
    console.log('📋 测试 15: 响应式检查');
    await page.setViewportSize({ width: 375, height: 812 }); // iPhone 尺寸
    await sleep(1000);

    const isMobileLayout = await page.evaluate(() => {
      const container = document.querySelector('.battle3d-container');
      if (!container) return false;
      const style = window.getComputedStyle(container);
      return style.flexDirection === 'column';
    });

    if (isMobileLayout) {
      log('PASS', '响应式布局', '移动端布局正确');
    } else {
      log('WARN', '响应式布局', '移动端布局可能未正确应用');
    }

    // 截图
    await page.setViewportSize({ width: 1280, height: 720 });
    await sleep(500);
    await page.screenshot({ path: 'test-screenshot.png', fullPage: false });
    log('PASS', '截图', '已保存测试截图: test-screenshot.png');

  } catch (error) {
    log('FAIL', '测试执行', `测试过程中出错: ${error.message}`);
  } finally {
    await browser.close();
  }

  // 输出测试报告
  console.log('\n' + '='.repeat(50));
  console.log('📊 测试报告');
  console.log('='.repeat(50));

  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  const warnings = results.filter(r => r.status === 'WARN').length;

  console.log(`✅ 通过: ${passed}`);
  console.log(`❌ 失败: ${failed}`);
  console.log(`⚠️ 警告: ${warnings}`);
  console.log(`📊 总计: ${results.length}`);
  console.log('='.repeat(50));

  if (failed > 0) {
    console.log('\n❌ 失败的测试:');
    results.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`   - ${r.test}: ${r.message}`);
    });
  }

  process.exit(failed > 0 ? 1 : 0);
}

// 运行测试
runTests().catch(error => {
  console.error('测试运行失败:', error);
  process.exit(1);
});
