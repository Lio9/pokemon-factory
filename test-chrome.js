const { chromium } = require('playwright');

// 使用系统Chrome浏览器的测试脚本
class ChromeTester {
  constructor() {
    this.results = [];
  }

  async testWithSystemChrome() {
    console.log('🚀 使用系统Chrome浏览器进行测试...');
    
    try {
      // 尝试使用系统Chrome浏览器
      console.log('  尝试启动系统Chrome浏览器...');
      const browser = await chromium.launch({ 
        headless: true,
        args: ['--no-sandbox'],
        channel: 'chrome'  // 使用系统安装的Chrome
      });
      
      console.log('  ✅ 系统Chrome浏览器启动成功');
      this.results.push({ test: '系统Chrome启动', status: 'PASS' });
      
      const page = await browser.newPage();
      await page.setViewportSize({ width: 1280, height: 720 });
      
      // 测试1: 访问首页
      console.log('  测试1: 访问首页...');
      await page.goto('http://localhost:7894');
      await page.waitForLoadState('networkidle');
      
      const title = await page.title();
      console.log(`    页面标题: ${title}`);
      
      // 检查页面内容
      const content = await page.content();
      if (content.includes('pokemon') || content.includes('宝可梦')) {
        console.log('    ✅ 首页内容正常');
        this.results.push({ test: '首页内容', status: 'PASS' });
      } else {
        console.log('    ❌ 首页内容异常');
        this.results.push({ test: '首页内容', status: 'FAIL' });
      }
      
      // 测试2: 检查Vue应用挂载
      console.log('  测试2: 检查Vue应用挂载...');
      const appElement = await page.$('#app');
      if (appElement) {
        console.log('    ✅ Vue应用成功挂载');
        this.results.push({ test: 'Vue应用挂载', status: 'PASS' });
      } else {
        console.log('    ❌ Vue应用未挂载');
        this.results.push({ test: 'Vue应用挂载', status: 'FAIL' });
      }
      
      // 测试3: 检查导航菜单
      console.log('  测试3: 检查导航菜单...');
      const navItems = await page.$$('nav a, .nav-item, .menu-item');
      console.log(`    找到 ${navItems.length} 个导航项`);
      
      if (navItems.length > 0) {
        console.log('    ✅ 导航菜单存在');
        this.results.push({ test: '导航菜单', status: 'PASS' });
      } else {
        console.log('    ❌ 导航菜单不存在');
        this.results.push({ test: '导航菜单', status: 'FAIL' });
      }
      
      // 测试4: 检查API请求
      console.log('  测试4: 检查API请求...');
      const apiRequests = [];
      page.on('request', request => {
        if (request.url().includes('/api/')) {
          apiRequests.push(request.url());
        }
      });
      
      await page.reload();
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);
      
      console.log(`    检测到 ${apiRequests.length} 个API请求`);
      if (apiRequests.length > 0) {
        console.log('    ✅ 前端发起API请求');
        this.results.push({ test: 'API请求', status: 'PASS' });
      } else {
        console.log('    ❌ 前端未发起API请求');
        this.results.push({ test: 'API请求', status: 'FAIL' });
      }
      
      // 测试5: 测试图鉴页面
      console.log('  测试5: 测试图鉴页面...');
      await page.goto('http://localhost:7894/pokedex');
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(3000); // 等待更长时间让页面加载
      
      // 检查页面内容
      const pageContent = await page.content();
      console.log(`    页面内容大小: ${pageContent.length} 字节`);
      
      // 尝试多种选择器
      let pokemonCards = await page.$$('.pokemon-card, .pokemon-item, .list-item, .card, .item');
      console.log(`    找到 ${pokemonCards.length} 个宝可梦卡片`);
      
      // 如果没找到，尝试其他选择器
      if (pokemonCards.length === 0) {
        pokemonCards = await page.$$('div[class*="pokemon"], div[class*="card"], div[class*="item"]');
        console.log(`    尝试其他选择器，找到 ${pokemonCards.length} 个元素`);
      }
      
      if (pokemonCards.length > 0) {
        console.log('    ✅ 图鉴页面加载成功');
        this.results.push({ test: '图鉴页面', status: 'PASS' });
      } else {
        // 检查是否有错误信息
        const errorElement = await page.$('.error, .alert, .message');
        if (errorElement) {
          const errorText = await errorElement.textContent();
          console.log(`    ❌ 图鉴页面加载失败: ${errorText}`);
          this.results.push({ test: '图鉴页面', status: 'FAIL', details: errorText });
        } else {
          console.log('    ❌ 图鉴页面加载失败');
          this.results.push({ test: '图鉴页面', status: 'FAIL' });
        }
      }
      
      await browser.close();
      console.log('  ✅ 浏览器关闭');
      
    } catch (error) {
      console.log(`  ❌ 系统Chrome测试失败: ${error.message}`);
      this.results.push({ test: '系统Chrome测试', status: 'ERROR', details: error.message });
      
      // 如果是浏览器未安装的错误，给出具体建议
      if (error.message.includes("Executable doesn't exist") || error.message.includes("channel")) {
        console.log('  💡 建议: 确保系统已安装Chrome浏览器，或运行 "npx playwright install chromium"');
      }
    }
  }

  async generateReport() {
    console.log('\n📊 系统Chrome测试报告');
    console.log('=' .repeat(50));
    
    const totalTests = this.results.length;
    const passedTests = this.results.filter(r => r.status === 'PASS').length;
    const failedTests = this.results.filter(r => r.status === 'FAIL').length;
    const errorTests = this.results.filter(r => r.status === 'ERROR').length;
    
    console.log(`总测试数: ${totalTests}`);
    console.log(`通过: ${passedTests} (${((passedTests / totalTests) * 100).toFixed(1)}%)`);
    console.log(`失败: ${failedTests}`);
    console.log(`错误: ${errorTests}`);
    
    console.log('\n详细结果:');
    this.results.forEach((result, index) => {
      const statusIcon = {
        'PASS': '✅',
        'FAIL': '❌',
        'ERROR': '💥'
      }[result.status];
      
      const details = result.details ? ` - ${result.details}` : '';
      console.log(`${index + 1}. ${statusIcon} ${result.test}${details}`);
    });
    
    return {
      timestamp: new Date().toISOString(),
      summary: {
        total: totalTests,
        passed: passedTests,
        failed: failedTests,
        errors: errorTests,
        successRate: `${((passedTests / totalTests) * 100).toFixed(1)}%`
      },
      details: this.results
    };
  }

  async runAllTests() {
    console.log('🚀 开始系统Chrome测试...\n');
    
    await this.testWithSystemChrome();
    
    const report = await this.generateReport();
    
    console.log('\n🎉 系统Chrome测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new ChromeTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有测试通过');
      process.exit(0);
    }
  });
}

module.exports = ChromeTester;