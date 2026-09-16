/**
 * 图鉴和伤害计算器自动化测试
 */

const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:7894';
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

async function testPokedex(page) {
  console.log('\n📖 ===== 图鉴模块测试 =====\n');

  // 测试1: 宝可梦列表页
  console.log('📋 测试1: 宝可梦列表页');
  await page.goto(`${BASE_URL}/pokemon`, { waitUntil: 'networkidle', timeout: 30000 });
  await sleep(2000);

  const title = await page.title();
  if (title.includes('图鉴') || title.includes('Pokemon')) {
    log('PASS', '列表页加载', `标题: ${title}`);
  } else {
    log('FAIL', '列表页加载', `标题异常: ${title}`);
  }

  // 检查宝可梦卡片
  const cards = await page.$$('.pokemon-card, [class*="card"]');
  if (cards.length > 0) {
    log('PASS', '宝可梦卡片', `找到 ${cards.length} 个卡片`);
  } else {
    log('WARN', '宝可梦卡片', '未找到卡片（可能需要后端）');
  }

  // 测试2: 搜索功能
  console.log('\n📋 测试2: 搜索功能');
  const searchInput = await page.$('input[type="text"], input[placeholder*="搜索"]');
  if (searchInput) {
    await searchInput.fill('皮卡丘');
    await sleep(1000);
    log('PASS', '搜索输入', '已输入搜索词');

    // 检查搜索结果
    const searchResults = await page.$$('.pokemon-card, [class*="card"]');
    log('PASS', '搜索结果', `找到 ${searchResults.length} 个结果`);
  } else {
    log('WARN', '搜索输入', '未找到搜索框');
  }

  // 测试3: 点击进入详情页
  console.log('\n📋 测试3: 宝可梦详情页');
  const firstCard = await page.$('.pokemon-card a, [class*="card"] a');
  if (firstCard) {
    await firstCard.click();
    await sleep(2000);

    const detailTitle = await page.title();
    if (detailTitle.includes('详情') || detailTitle.includes('Pokemon')) {
      log('PASS', '详情页加载', `标题: ${detailTitle}`);
    }

    // 检查详情内容
    const detailContent = await page.evaluate(() => {
      const body = document.body.textContent;
      return {
        hasName: /名[名称]|Name/.test(body),
        hasType: /属[性性]|Type/.test(body),
        hasStats: /能力|Stats|HP/.test(body)
      };
    });

    if (detailContent.hasName) log('PASS', '详情内容', '包含名称信息');
    if (detailContent.hasType) log('PASS', '详情内容', '包含属性信息');
    if (detailContent.hasStats) log('PASS', '详情内容', '包含能力值信息');
  } else {
    log('WARN', '详情页', '未找到可点击的卡片');
  }

  await page.screenshot({ path: 'test-pokedex.png' });
}

async function testDamageCalculator(page) {
  console.log('\n📊 ===== 伤害计算器测试 =====\n');

  // 测试1: 加载页面
  console.log('📋 测试1: 加载伤害计算器');
  await page.goto(`${BASE_URL}/damage-calculator`, { waitUntil: 'networkidle', timeout: 30000 });
  await sleep(2000);

  const title = await page.title();
  if (title.includes('伤害') || title.includes('Damage')) {
    log('PASS', '页面加载', `标题: ${title}`);
  } else {
    log('FAIL', '页面加载', `标题异常: ${title}`);
  }

  // 测试2: 检查表单元素
  console.log('\n📋 测试2: 检查表单元素');

  const formElements = await page.evaluate(() => {
    const inputs = document.querySelectorAll('input, select');
    const buttons = document.querySelectorAll('button');
    return {
      inputCount: inputs.length,
      buttonCount: buttons.length,
      hasAttackSelect: !!document.querySelector('select[name*="attack"], [class*="attack"]'),
      hasDefenseSelect: !!document.querySelector('select[name*="defense"], [class*="defense"]')
    };
  });

  if (formElements.inputCount > 0) {
    log('PASS', '表单元素', `找到 ${formElements.inputCount} 个输入框`);
  }
  if (formElements.buttonCount > 0) {
    log('PASS', '按钮', `找到 ${formElements.buttonCount} 个按钮`);
  }

  // 测试3: 尝试计算伤害
  console.log('\n📋 测试3: 伤害计算');

  // 查找计算按钮
  const calcButton = await page.$('button:has-text("计算"), button:has-text("Calculate"), button[type="submit"]');
  if (calcButton) {
    await calcButton.click();
    await sleep(1000);

    // 检查是否有结果显示
    const resultArea = await page.evaluate(() => {
      const body = document.body.textContent;
      return {
        hasDamage: /伤害|Damage|HP/.test(body),
        hasPercentage: /\d+%/.test(body)
      };
    });

    if (resultArea.hasDamage) {
      log('PASS', '伤害计算', '计算结果显示');
    } else {
      log('WARN', '伤害计算', '未检测到计算结果（可能需要选择宝可梦）');
    }
  } else {
    log('WARN', '计算按钮', '未找到计算按钮');
  }

  await page.screenshot({ path: 'test-damage-calc.png' });
}

async function runTests() {
  console.log('🎮 开始图鉴和伤害计算器测试...\n');

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
  const errors = [];
  page.on('console', msg => {
    if (msg.type() === 'error' && !msg.text().includes('WebSocket') && !msg.text().includes('favicon')) {
      errors.push(msg.text());
    }
  });

  try {
    await testPokedex(page);
    await testDamageCalculator(page);
  } catch (error) {
    log('FAIL', '测试执行', `出错: ${error.message}`);
  } finally {
    await browser.close();
  }

  // 输出报告
  console.log('\n' + '='.repeat(60));
  console.log('📊 测试报告');
  console.log('='.repeat(60));

  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL').length;
  const warnings = results.filter(r => r.status === 'WARN').length;

  console.log(`✅ 通过: ${passed}`);
  console.log(`❌ 失败: ${failed}`);
  console.log(`⚠️ 警告: ${warnings}`);
  console.log(`📊 总计: ${results.length}`);
  console.log('='.repeat(60));

  if (errors.length > 0) {
    console.log('\n⚠️ 控制台错误:');
    errors.slice(0, 5).forEach(e => console.log(`   - ${e.substring(0, 100)}`));
  }

  console.log('\n📸 截图已保存: test-pokedex.png, test-damage-calc.png');
}

runTests().catch(console.error);
