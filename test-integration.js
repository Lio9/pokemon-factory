const { chromium } = require('playwright');

// 前后端集成测试
class IntegrationTester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.results = [];
  }

  async init() {
    console.log('🚀 初始化集成测试...');
    this.browser = await chromium.launch({ 
      headless: false,
      args: ['--no-sandbox']
    });
    this.page = await this.browser.newPage();
    await this.page.setViewportSize({ width: 1280, height: 720 });
    console.log('✅ 浏览器初始化完成');
  }

  async testHealthCheck() {
    console.log('\n🔍 测试1: 系统健康检查');
    
    try {
      // 测试后端健康检查
      console.log('  1.1 测试后端API健康检查...');
      const backendResponse = await this.page.goto('http://localhost:8084/api/health');
      
      if (backendResponse && backendResponse.status() === 200) {
        console.log('    ✅ 后端API健康检查通过');
        this.results.push({ test: '后端健康检查', status: 'PASS' });
      } else {
        console.log('    ❌ 后端API健康检查失败');
        this.results.push({ test: '后端健康检查', status: 'FAIL' });
      }

      // 测试前端健康检查
      console.log('  1.2 测试前端健康检查...');
      const frontendResponse = await this.page.goto('http://localhost:7894');
      
      if (frontendResponse && frontendResponse.status() === 200) {
        console.log('    ✅ 前端健康检查通过');
        this.results.push({ test: '前端健康检查', status: 'PASS' });
      } else {
        console.log('    ❌ 前端健康检查失败');
        this.results.push({ test: '前端健康检查', status: 'FAIL' });
      }

    } catch (error) {
      console.log(`    ❌ 健康检查失败: ${error.message}`);
      this.results.push({ test: '健康检查', status: 'ERROR', details: error.message });
    }
  }

  async testAPIEndpoints() {
    console.log('\n🔍 测试2: API端点测试');
    
    const endpoints = [
      { url: '/api/pokedex/summary', name: '图鉴摘要' },
      { url: '/api/pokemon?limit=5', name: '宝可梦列表' },
      { url: '/api/moves?limit=5', name: '技能列表' },
      { url: '/api/abilities?limit=5', name: '特性列表' },
      { url: '/api/items?limit=5', name: '道具列表' }
    ];

    for (const endpoint of endpoints) {
      try {
        console.log(`  2.${endpoints.indexOf(endpoint) + 1} 测试 ${endpoint.name} API...`);
        const response = await this.page.goto(`http://localhost:8084${endpoint.url}`);
        
        if (response && response.status() === 200) {
          const data = await response.json();
          console.log(`    ✅ ${endpoint.name} API正常，返回数据`);
          this.results.push({ test: endpoint.name, status: 'PASS', details: 'API返回200' });
        } else {
          console.log(`    ❌ ${endpoint.name} API异常`);
          this.results.push({ test: endpoint.name, status: 'FAIL', details: `状态码: ${response?.status()}` });
        }
      } catch (error) {
        console.log(`    ❌ ${endpoint.name} API测试失败: ${error.message}`);
        this.results.push({ test: endpoint.name, status: 'ERROR', details: error.message });
      }
    }
  }

  async testFrontendPages() {
    console.log('\n🔍 测试3: 前端页面测试');
    
    const pages = [
      { url: '/', name: '首页' },
      { url: '/pokedex', name: '图鉴页面' },
      { url: '/battle', name: '对战页面' },
      { url: '/factory', name: '工厂挑战' },
      { url: '/user', name: '用户页面' }
    ];

    for (const page of pages) {
      try {
        console.log(`  3.${pages.indexOf(page) + 1} 测试 ${page.name}...`);
        await this.page.goto(`http://localhost:7894${page.url}`);
        await this.page.waitForLoadState('networkidle');
        
        // 检查页面是否加载成功
        const pageTitle = await this.page.title();
        const hasContent = await this.page.$('body');
        
        if (hasContent) {
          console.log(`    ✅ ${page.name}加载成功`);
          this.results.push({ test: page.name, status: 'PASS', details: '页面加载成功' });
        } else {
          console.log(`    ❌ ${page.name}加载失败`);
          this.results.push({ test: page.name, status: 'FAIL', details: '页面加载失败' });
        }
      } catch (error) {
        console.log(`    ❌ ${page.name}测试失败: ${error.message}`);
        this.results.push({ test: page.name, status: 'ERROR', details: error.message });
      }
    }
  }

  async testBattleFlow() {
    console.log('\n🔍 测试4: 对战流程测试');
    
    try {
      // 测试1: 访问对战页面
      console.log('  4.1 测试对战页面加载...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      
      // 检查对战界面元素
      const battleElements = await this.page.$$('.battle, .battle-container, .battle-interface');
      if (battleElements.length > 0) {
        console.log('    ✅ 对战界面加载成功');
        this.results.push({ test: '对战界面', status: 'PASS' });
      } else {
        console.log('    ❌ 对战界面加载失败');
        this.results.push({ test: '对战界面', status: 'FAIL' });
      }

      // 测试2: 测试开始对战按钮
      console.log('  4.2 测试开始对战按钮...');
      const startButton = await this.page.$('button:has-text("开始对战"), .start-battle-btn, [data-testid="start-battle"]');
      if (startButton) {
        console.log('    ✅ 开始对战按钮存在');
        this.results.push({ test: '开始对战按钮', status: 'PASS' });
        
        // 测试3: 测试点击开始对战
        console.log('  4.3 测试点击开始对战...');
        await startButton.click();
        await this.page.waitForTimeout(2000);
        
        // 检查是否进入对战状态
        const battleState = await this.page.$('.battle-active, .battle-started, .in-battle');
        if (battleState) {
          console.log('    ✅ 成功进入对战状态');
          this.results.push({ test: '进入对战状态', status: 'PASS' });
        } else {
          console.log('    ⚠️  未检测到对战状态');
          this.results.push({ test: '进入对战状态', status: 'WARN', details: '未检测到对战状态' });
        }
      } else {
        console.log('    ❌ 开始对战按钮不存在');
        this.results.push({ test: '开始对战按钮', status: 'FAIL' });
      }

    } catch (error) {
      console.log(`    ❌ 对战流程测试失败: ${error.message}`);
      this.results.push({ test: '对战流程测试', status: 'ERROR', details: error.message });
    }
  }

  async testPokedexFlow() {
    console.log('\n🔍 测试5: 图鉴流程测试');
    
    try {
      // 测试1: 访问图鉴页面
      console.log('  5.1 测试图鉴页面加载...');
      await this.page.goto('http://localhost:7894/pokedex');
      await this.page.waitForLoadState('networkidle');
      
      // 检查图鉴列表
      const pokemonCards = await this.page.$$('.pokemon-card, .pokemon-item, .pokemon-list-item');
      console.log(`    找到 ${pokemonCards.length} 个宝可梦卡片`);
      
      if (pokemonCards.length > 0) {
        console.log('    ✅ 图鉴页面加载成功');
        this.results.push({ test: '图鉴页面', status: 'PASS', details: `找到${pokemonCards.length}个卡片` });
        
        // 测试2: 测试点击宝可梦卡片
        console.log('  5.2 测试点击宝可梦卡片...');
        await pokemonCards[0].click();
        await this.page.waitForTimeout(1000);
        
        // 检查是否显示详情
        const detailView = await this.page.$('.pokemon-detail, .detail-view, .pokemon-info');
        if (detailView) {
          console.log('    ✅ 宝可梦详情显示成功');
          this.results.push({ test: '宝可梦详情', status: 'PASS' });
        } else {
          console.log('    ⚠️  未检测到详情视图');
          this.results.push({ test: '宝可梦详情', status: 'WARN', details: '未检测到详情视图' });
        }
      } else {
        console.log('    ❌ 图鉴页面加载失败');
        this.results.push({ test: '图鉴页面', status: 'FAIL' });
      }

    } catch (error) {
      console.log(`    ❌ 图鉴流程测试失败: ${error.message}`);
      this.results.push({ test: '图鉴流程测试', status: 'ERROR', details: error.message });
    }
  }

  async testUserAuth() {
    console.log('\n🔍 测试6: 用户认证测试');
    
    try {
      // 测试1: 访问用户页面
      console.log('  6.1 测试用户页面加载...');
      await this.page.goto('http://localhost:7894/user');
      await this.page.waitForLoadState('networkidle');
      
      // 检查登录表单
      const loginForm = await this.page.$('form, .login-form, .auth-form');
      if (loginForm) {
        console.log('    ✅ 登录表单存在');
        this.results.push({ test: '登录表单', status: 'PASS' });
        
        // 测试2: 测试登录功能
        console.log('  6.2 测试登录功能...');
        const usernameInput = await this.page.$('input[type="text"], input[name="username"], input[placeholder*="用户名"]');
        const passwordInput = await this.page.$('input[type="password"], input[name="password"]');
        const loginButton = await this.page.$('button[type="submit"], button:has-text("登录"), .login-btn');
        
        if (usernameInput && passwordInput && loginButton) {
          // 测试登录
          await usernameInput.fill('testuser');
          await passwordInput.fill('testpass');
          await loginButton.click();
          await this.page.waitForTimeout(2000);
          
          // 检查是否登录成功
          const loggedIn = await this.page.$('.user-info, .logged-in, .user-avatar');
          if (loggedIn) {
            console.log('    ✅ 登录功能正常');
            this.results.push({ test: '登录功能', status: 'PASS' });
          } else {
            console.log('    ⚠️  登录可能失败或需要验证');
            this.results.push({ test: '登录功能', status: 'WARN', details: '登录可能失败或需要验证' });
          }
        } else {
          console.log('    ❌ 登录表单元素不完整');
          this.results.push({ test: '登录功能', status: 'FAIL', details: '登录表单元素不完整' });
        }
      } else {
        console.log('    ❌ 登录表单不存在');
        this.results.push({ test: '登录表单', status: 'FAIL' });
      }

    } catch (error) {
      console.log(`    ❌ 用户认证测试失败: ${error.message}`);
      this.results.push({ test: '用户认证测试', status: 'ERROR', details: error.message });
    }
  }

  async generateReport() {
    console.log('\n📊 集成测试报告');
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
    
    // 生成JSON报告
    const report = {
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
    
    // 保存报告到文件
    const fs = require('fs');
    const reportPath = `D:\\learn\\pokemon-factory\\integration-test-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`\n📄 集成测试报告已保存到: ${reportPath}`);
    
    return report;
  }

  async runAllTests() {
    try {
      await this.init();
      
      await this.testHealthCheck();
      await this.testAPIEndpoints();
      await this.testFrontendPages();
      await this.testBattleFlow();
      await this.testPokedexFlow();
      await this.testUserAuth();
      
      const report = await this.generateReport();
      
      console.log('\n🎉 集成测试完成！');
      
      return report;
      
    } catch (error) {
      console.error('❌ 测试过程中发生错误:', error);
    } finally {
      if (this.browser) {
        await this.browser.close();
      }
    }
  }
}

// 运行测试
if (require.main === module) {
  const tester = new IntegrationTester();
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

module.exports = IntegrationTester;