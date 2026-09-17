const http = require('http');

// 智能测试脚本 - 自动检测环境并选择最佳测试方式
class SmartTester {
  constructor() {
    this.results = [];
    this.hasPlaywright = false;
  }

  async checkEnvironment() {
    console.log('🔍 检查测试环境...');
    
    // 检查后端服务
    try {
      const response = await this.httpGet('http://localhost:8084/api/pokedex/summary');
      if (response.statusCode === 200) {
        console.log('  ✅ 后端服务运行正常 (端口 8084)');
        this.results.push({ test: '后端服务', status: 'PASS', details: '端口 8084' });
      } else {
        console.log('  ❌ 后端服务异常');
        this.results.push({ test: '后端服务', status: 'FAIL', details: `状态码: ${response.statusCode}` });
      }
    } catch (error) {
      console.log('  ❌ 后端服务未启动');
      this.results.push({ test: '后端服务', status: 'ERROR', details: error.message });
    }

    // 检查前端服务
    try {
      const response = await this.httpGet('http://localhost:7894');
      if (response.statusCode === 200) {
        console.log('  ✅ 前端服务运行正常 (端口 7894)');
        this.results.push({ test: '前端服务', status: 'PASS', details: '端口 7894' });
      } else {
        console.log('  ❌ 前端服务异常');
        this.results.push({ test: '前端服务', status: 'FAIL', details: `状态码: ${response.statusCode}` });
      }
    } catch (error) {
      console.log('  ❌ 前端服务未启动');
      this.results.push({ test: '前端服务', status: 'ERROR', details: error.message });
    }

    // 检查 Playwright
    try {
      const { chromium } = require('playwright');
      console.log('  ✅ Playwright 模块已安装');
      this.results.push({ test: 'Playwright模块', status: 'PASS' });
      this.hasPlaywright = true;
    } catch (error) {
      console.log('  ❌ Playwright 模块未安装');
      this.results.push({ test: 'Playwright模块', status: 'FAIL', details: error.message });
      this.hasPlaywright = false;
    }
  }

  async testBackendAPI() {
    console.log('\n🔍 测试后端API...');
    
    const endpoints = [
      { url: 'http://localhost:8084/api/pokedex/summary', name: '图鉴摘要' },
      { url: 'http://localhost:8084/api/pokedex/pokemon/list?size=5', name: '宝可梦列表' },
      { url: 'http://localhost:8084/api/pokedex/moves/list?size=5', name: '技能列表' },
      { url: 'http://localhost:8084/api/pokedex/abilities/list?size=5', name: '特性列表' },
      { url: 'http://localhost:8084/api/pokedex/items/list?size=5', name: '道具列表' },
      { url: 'http://localhost:8084/api/pokedex/types', name: '属性列表' }
    ];

    for (const endpoint of endpoints) {
      try {
        console.log(`  测试 ${endpoint.name}...`);
        const response = await this.httpGet(endpoint.url);
        
        if (response.statusCode === 200) {
          const data = JSON.parse(response.body);
          console.log(`    ✅ ${endpoint.name} - 状态码: ${response.statusCode}`);
          this.results.push({ test: endpoint.name, status: 'PASS', details: `状态码: ${response.statusCode}` });
        } else {
          console.log(`    ❌ ${endpoint.name} - 状态码: ${response.statusCode}`);
          this.results.push({ test: endpoint.name, status: 'FAIL', details: `状态码: ${response.statusCode}` });
        }
      } catch (error) {
        console.log(`    ❌ ${endpoint.name} - 错误: ${error.message}`);
        this.results.push({ test: endpoint.name, status: 'ERROR', details: error.message });
      }
    }
  }

  async testFrontendPages() {
    console.log('\n🔍 测试前端页面...');
    
    const pages = [
      { url: 'http://localhost:7894/', name: '首页' },
      { url: 'http://localhost:7894/pokedex', name: '图鉴页面' },
      { url: 'http://localhost:7894/battle', name: '对战页面' },
      { url: 'http://localhost:7894/factory', name: '工厂挑战' },
      { url: 'http://localhost:7894/user', name: '用户页面' }
    ];

    for (const page of pages) {
      try {
        console.log(`  测试 ${page.name}...`);
        const response = await this.httpGet(page.url);
        
        if (response.statusCode === 200) {
          console.log(`    ✅ ${page.name} - 状态码: ${response.statusCode}`);
          this.results.push({ test: page.name, status: 'PASS', details: `状态码: ${response.statusCode}` });
          
          // 检查页面内容
          if (response.body && response.body.length > 100) {
            console.log(`    ✅ ${page.name} - 页面内容丰富`);
            this.results.push({ test: `${page.name}内容`, status: 'PASS', details: `页面大小: ${response.body.length}字节` });
          } else {
            console.log(`    ⚠️  ${page.name} - 页面内容可能为空`);
            this.results.push({ test: `${page.name}内容`, status: 'WARN', details: '页面内容可能为空' });
          }
        } else {
          console.log(`    ❌ ${page.name} - 状态码: ${response.statusCode}`);
          this.results.push({ test: page.name, status: 'FAIL', details: `状态码: ${response.statusCode}` });
        }
      } catch (error) {
        console.log(`    ❌ ${page.name} - 错误: ${error.message}`);
        this.results.push({ test: page.name, status: 'ERROR', details: error.message });
      }
    }
  }

  async testDatabaseIntegration() {
    console.log('\n🔍 测试数据库集成...');
    
    try {
      console.log('  测试数据库连接（通过API）...');
      const response = await this.httpGet('http://localhost:8084/api/pokedex/summary');
      
      if (response.statusCode === 200) {
        const data = JSON.parse(response.body);
        console.log('    ✅ 数据库连接正常');
        console.log(`    数据库统计: 宝可梦 ${data.data?.pokemonCount || 0} 个, 技能 ${data.data?.moveCount || 0} 个`);
        this.results.push({ test: '数据库连接', status: 'PASS', details: '数据库连接正常' });
        
        // 检查数据完整性
        if (data.data?.pokemonCount > 0) {
          console.log('    ✅ 宝可梦数据存在');
          this.results.push({ test: '宝可梦数据', status: 'PASS', details: `${data.data.pokemonCount}个宝可梦` });
        } else {
          console.log('    ❌ 宝可梦数据为空');
          this.results.push({ test: '宝可梦数据', status: 'FAIL', details: '宝可梦数据为空' });
        }
      } else {
        console.log('    ❌ 数据库连接可能异常');
        this.results.push({ test: '数据库连接', status: 'FAIL', details: '数据库连接可能异常' });
      }
    } catch (error) {
      console.log(`    ❌ 数据库集成测试失败: ${error.message}`);
      this.results.push({ test: '数据库集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testWithPlaywright() {
    if (!this.hasPlaywright) {
      console.log('\n⚠️  跳过 Playwright 测试（模块未安装）');
      return;
    }

    console.log('\n🔍 使用 Playwright 进行UI测试...');
    
    try {
      const { chromium } = require('playwright');
      
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
      
      // 如果是浏览器未安装的错误，给出具体建议并标记为警告
      if (error.message.includes("Executable doesn't exist")) {
        console.log('  💡 建议: 运行 "npx playwright install chromium" 安装浏览器');
        this.results.push({ test: 'Playwright测试', status: 'WARN', details: '浏览器未安装，跳过UI测试' });
      } else {
        this.results.push({ test: 'Playwright测试', status: 'ERROR', details: error.message });
      }
    }
  }

  async httpGet(url) {
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
    console.log('\n📊 智能测试报告');
    console.log('=' .repeat(60));
    
    const totalTests = this.results.length;
    const passedTests = this.results.filter(r => r.status === 'PASS').length;
    const failedTests = this.results.filter(r => r.status === 'FAIL').length;
    const errorTests = this.results.filter(r => r.status === 'ERROR').length;
    const warnTests = this.results.filter(r => r.status === 'WARN').length;
    
    console.log(`总测试数: ${totalTests}`);
    console.log(`通过: ${passedTests} (${((passedTests / totalTests) * 100).toFixed(1)}%)`);
    console.log(`失败: ${failedTests}`);
    console.log(`错误: ${errorTests}`);
    console.log(`警告: ${warnTests}`);
    
    console.log('\n详细结果:');
    this.results.forEach((result, index) => {
      const statusIcon = {
        'PASS': '✅',
        'FAIL': '❌',
        'ERROR': '💥',
        'WARN': '⚠️'
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
        warnings: warnTests,
        successRate: `${((passedTests / totalTests) * 100).toFixed(1)}%`
      },
      details: this.results
    };
  }

  async runAllTests() {
    console.log('🚀 开始智能测试...\n');
    
    // 检查环境
    await this.checkEnvironment();
    
    // 运行基本测试
    await this.testBackendAPI();
    await this.testFrontendPages();
    await this.testDatabaseIntegration();
    
    // 如果有 Playwright，运行UI测试
    if (this.hasPlaywright) {
      try {
        await this.testWithPlaywright();
      } catch (error) {
        console.log('  ⚠️  Playwright 测试失败，跳过UI测试');
        this.results.push({ test: 'Playwright测试', status: 'WARN', details: '浏览器未安装，跳过UI测试' });
      }
    }
    
    const report = await this.generateReport();
    
    console.log('\n🎉 智能测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new SmartTester();
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

module.exports = SmartTester;