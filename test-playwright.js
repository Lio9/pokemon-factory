const { chromium } = require('playwright');

// Playwright 测试脚本（即使浏览器未安装也能测试基本功能）
class PlaywrightTester {
  constructor() {
    this.results = [];
  }

  async testWithPlaywright() {
    console.log('🚀 使用 Playwright 进行测试...');
    
    try {
      // 尝试启动浏览器
      console.log('  尝试启动浏览器...');
      const browser = await chromium.launch({ 
        headless: true,
        args: ['--no-sandbox']
      });
      
      console.log('  ✅ 浏览器启动成功');
      this.results.push({ test: '浏览器启动', status: 'PASS' });
      
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
      
      await browser.close();
      console.log('  ✅ 浏览器关闭');
      
    } catch (error) {
      console.log(`  ❌ Playwright 测试失败: ${error.message}`);
      this.results.push({ test: 'Playwright测试', status: 'ERROR', details: error.message });
      
      // 如果是浏览器未安装的错误，给出具体建议
      if (error.message.includes("Executable doesn't exist")) {
        console.log('  💡 建议: 运行 "npx playwright install chromium" 安装浏览器');
      }
    }
  }

  async testWithoutPlaywright() {
    console.log('\n🚀 不使用 Playwright 进行基本测试...');
    
    // 测试后端API
    console.log('  测试后端API...');
    const endpoints = [
      'http://localhost:8084/api/pokedex/summary',
      'http://localhost:8084/api/pokedex/pokemon/list?size=5'
    ];
    
    for (const endpoint of endpoints) {
      try {
        const response = await this.httpGet(endpoint);
        if (response.statusCode === 200) {
          console.log(`    ✅ ${endpoint} - 状态码: ${response.statusCode}`);
          this.results.push({ test: endpoint, status: 'PASS' });
        } else {
          console.log(`    ❌ ${endpoint} - 状态码: ${response.statusCode}`);
          this.results.push({ test: endpoint, status: 'FAIL' });
        }
      } catch (error) {
        console.log(`    ❌ ${endpoint} - 错误: ${error.message}`);
        this.results.push({ test: endpoint, status: 'ERROR' });
      }
    }
    
    // 测试前端页面
    console.log('  测试前端页面...');
    const pages = [
      'http://localhost:7894/',
      'http://localhost:7894/pokedex',
      'http://localhost:7894/battle'
    ];
    
    for (const page of pages) {
      try {
        const response = await this.httpGet(page);
        if (response.statusCode === 200) {
          console.log(`    ✅ ${page} - 状态码: ${response.statusCode}`);
          this.results.push({ test: page, status: 'PASS' });
        } else {
          console.log(`    ❌ ${page} - 状态码: ${response.statusCode}`);
          this.results.push({ test: page, status: 'FAIL' });
        }
      } catch (error) {
        console.log(`    ❌ ${page} - 错误: ${error.message}`);
        this.results.push({ test: page, status: 'ERROR' });
      }
    }
  }

  async httpGet(url) {
    const http = require('http');
    return new Promise((resolve, reject) => {
      const req = http.get(url, (res) => {
        let body = '';
        
        res.on('data', (chunk) => {
          body += chunk;
        });
        
        res.on('end', () => {
          resolve({
            statusCode: res.statusCode,
            headers: res.headers,
            body: body
          });
        });
      });
      
      req.on('error', (error) => {
        reject(error);
      });
      
      req.setTimeout(5000, () => {
        req.destroy();
        reject(new Error('请求超时'));
      });
    });
  }

  async generateReport() {
    console.log('\n📊 Playwright 测试报告');
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
    console.log('🚀 开始 Playwright 测试...\n');
    
    // 尝试使用 Playwright
    await this.testWithPlaywright();
    
    // 如果 Playwright 失败，使用基本测试
    if (this.results.some(r => r.status === 'ERROR')) {
      console.log('\n⚠️  Playwright 测试失败，使用基本测试...');
      await this.testWithoutPlaywright();
    }
    
    const report = await this.generateReport();
    
    console.log('\n🎉 Playwright 测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new PlaywrightTester();
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

module.exports = PlaywrightTester;