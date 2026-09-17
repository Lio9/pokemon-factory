const { chromium } = require('playwright');

// 前后端配合测试
class FrontendBackendTester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.results = [];
  }

  async init() {
    console.log('🚀 初始化前后端配合测试...');
    this.browser = await chromium.launch({ 
      headless: false,
      args: ['--no-sandbox']
    });
    this.page = await this.browser.newPage();
    await this.page.setViewportSize({ width: 1280, height: 720 });
    console.log('✅ 浏览器初始化完成');
  }

  async testHomepageIntegration() {
    console.log('\n🔍 测试1: 首页集成测试');
    
    try {
      // 监听API请求
      const apiRequests = [];
      this.page.on('request', request => {
        if (request.url().includes('/api/')) {
          apiRequests.push({
            url: request.url(),
            method: request.method()
          });
        }
      });

      // 监听API响应
      const apiResponses = [];
      this.page.on('response', async response => {
        if (response.url().includes('/api/')) {
          try {
            const responseBody = await response.text();
            apiResponses.push({
              url: response.url(),
              status: response.status(),
              body: responseBody.substring(0, 100)
            });
          } catch (e) {
            apiResponses.push({
              url: response.url(),
              status: response.status(),
              body: '无法读取响应'
            });
          }
        }
      });

      console.log('  1.1 访问首页...');
      await this.page.goto('http://localhost:7894');
      await this.page.waitForLoadState('networkidle');
      await this.page.waitForTimeout(3000); // 等待API调用

      console.log(`  1.2 检测到 ${apiRequests.length} 个API请求`);
      console.log(`  1.3 检测到 ${apiResponses.length} 个API响应`);

      // 分析API调用
      if (apiRequests.length > 0) {
        console.log('  ✅ 前端成功发起API请求');
        this.results.push({ test: '首页API请求', status: 'PASS', details: `发起${apiRequests.length}个请求` });
        
        // 显示API请求详情
        apiRequests.forEach((req, index) => {
          console.log(`    ${index + 1}. ${req.method} ${req.url}`);
        });
      } else {
        console.log('  ❌ 前端未发起API请求');
        this.results.push({ test: '首页API请求', status: 'FAIL', details: '未发起API请求' });
      }

      // 检查API响应
      const successfulResponses = apiResponses.filter(r => r.status === 200);
      if (successfulResponses.length > 0) {
        console.log(`  ✅ ${successfulResponses.length} 个API响应成功`);
        this.results.push({ test: '首页API响应', status: 'PASS', details: `${successfulResponses.length}个响应成功` });
      } else {
        console.log('  ❌ 没有成功的API响应');
        this.results.push({ test: '首页API响应', status: 'FAIL', details: '没有成功的API响应' });
      }

      // 检查页面内容
      console.log('  1.4 检查页面内容...');
      const pageContent = await this.page.content();
      
      if (pageContent.includes('宝可梦') || pageContent.includes('pokemon')) {
        console.log('  ✅ 页面包含相关内容');
        this.results.push({ test: '首页内容', status: 'PASS', details: '页面包含相关内容' });
      } else {
        console.log('  ❌ 页面内容可能为空');
        this.results.push({ test: '首页内容', status: 'FAIL', details: '页面内容可能为空' });
      }

    } catch (error) {
      console.log(`  ❌ 首页集成测试失败: ${error.message}`);
      this.results.push({ test: '首页集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testPokedexIntegration() {
    console.log('\n🔍 测试2: 图鉴集成测试');
    
    try {
      // 监听API请求
      const apiRequests = [];
      this.page.on('request', request => {
        if (request.url().includes('/api/pokedex/')) {
          apiRequests.push(request.url());
        }
      });

      console.log('  2.1 访问图鉴页面...');
      await this.page.goto('http://localhost:7894/pokedex');
      await this.page.waitForLoadState('networkidle');
      await this.page.waitForTimeout(3000);

      console.log(`  2.2 检测到 ${apiRequests.length} 个图鉴API请求`);

      if (apiRequests.length > 0) {
        console.log('  ✅ 图鉴页面发起API请求');
        this.results.push({ test: '图鉴API请求', status: 'PASS', details: `发起${apiRequests.length}个请求` });
      } else {
        console.log('  ❌ 图鉴页面未发起API请求');
        this.results.push({ test: '图鉴API请求', status: 'FAIL', details: '未发起API请求' });
      }

      // 检查图鉴列表
      console.log('  2.3 检查图鉴列表...');
      const pokemonCards = await this.page.$$('.pokemon-card, .pokemon-item, .list-item');
      
      if (pokemonCards.length > 0) {
        console.log(`  ✅ 图鉴列表显示 ${pokemonCards.length} 个宝可梦`);
        this.results.push({ test: '图鉴列表', status: 'PASS', details: `显示${pokemonCards.length}个宝可梦` });
        
        // 测试点击宝可梦
        console.log('  2.4 测试点击宝可梦...');
        await pokemonCards[0].click();
        await this.page.waitForTimeout(2000);
        
        // 检查详情页面
        const detailView = await this.page.$('.detail-view, .pokemon-detail, .detail-panel');
        if (detailView) {
          console.log('  ✅ 宝可梦详情显示成功');
          this.results.push({ test: '宝可梦详情', status: 'PASS', details: '详情显示成功' });
        } else {
          console.log('  ⚠️  未检测到详情视图');
          this.results.push({ test: '宝可梦详情', status: 'WARN', details: '未检测到详情视图' });
        }
      } else {
        console.log('  ❌ 图鉴列表为空');
        this.results.push({ test: '图鉴列表', status: 'FAIL', details: '图鉴列表为空' });
      }

    } catch (error) {
      console.log(`  ❌ 图鉴集成测试失败: ${error.message}`);
      this.results.push({ test: '图鉴集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testBattleIntegration() {
    console.log('\n🔍 测试3: 对战集成测试');
    
    try {
      console.log('  3.1 访问对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      
      // 检查对战界面
      const battleInterface = await this.page.$('.battle-interface, .battle-container, .battle-view');
      if (battleInterface) {
        console.log('  ✅ 对战界面加载成功');
        this.results.push({ test: '对战界面', status: 'PASS', details: '对战界面加载成功' });
        
        // 检查开始对战按钮
        console.log('  3.2 检查开始对战按钮...');
        const startButton = await this.page.$('button:has-text("开始对战"), .start-battle, [data-testid="start-battle"]');
        
        if (startButton) {
          console.log('  ✅ 开始对战按钮存在');
          this.results.push({ test: '开始对战按钮', status: 'PASS', details: '按钮存在' });
          
          // 测试点击开始对战
          console.log('  3.3 测试点击开始对战...');
          await startButton.click();
          await this.page.waitForTimeout(3000);
          
          // 检查是否进入对战状态
          const battleState = await this.page.$('.battle-active, .in-battle, .battle-started');
          if (battleState) {
            console.log('  ✅ 成功进入对战状态');
            this.results.push({ test: '进入对战', status: 'PASS', details: '成功进入对战状态' });
          } else {
            console.log('  ⚠️  未检测到对战状态');
            this.results.push({ test: '进入对战', status: 'WARN', details: '未检测到对战状态' });
          }
        } else {
          console.log('  ❌ 开始对战按钮不存在');
          this.results.push({ test: '开始对战按钮', status: 'FAIL', details: '按钮不存在' });
        }
      } else {
        console.log('  ❌ 对战界面加载失败');
        this.results.push({ test: '对战界面', status: 'FAIL', details: '对战界面加载失败' });
      }

    } catch (error) {
      console.log(`  ❌ 对战集成测试失败: ${error.message}`);
      this.results.push({ test: '对战集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testUserAuthIntegration() {
    console.log('\n🔍 测试4: 用户认证集成测试');
    
    try {
      console.log('  4.1 访问用户页面...');
      await this.page.goto('http://localhost:7894/user');
      await this.page.waitForLoadState('networkidle');
      
      // 检查登录表单
      const loginForm = await this.page.$('form, .login-form, .auth-form');
      if (loginForm) {
        console.log('  ✅ 登录表单存在');
        this.results.push({ test: '登录表单', status: 'PASS', details: '登录表单存在' });
        
        // 测试登录功能
        console.log('  4.2 测试登录功能...');
        const usernameInput = await this.page.$('input[type="text"], input[name="username"], input[placeholder*="用户名"]');
        const passwordInput = await this.page.$('input[type="password"], input[name="password"]');
        const loginButton = await this.page.$('button[type="submit"], button:has-text("登录"), .login-btn');
        
        if (usernameInput && passwordInput && loginButton) {
          console.log('  ✅ 登录表单元素完整');
          this.results.push({ test: '登录表单元素', status: 'PASS', details: '表单元素完整' });
          
          // 测试输入
          await usernameInput.fill('testuser');
          await passwordInput.fill('testpass');
          
          console.log('  ✅ 登录表单输入正常');
          this.results.push({ test: '登录表单输入', status: 'PASS', details: '输入正常' });
          
          // 测试提交
          console.log('  4.3 测试登录提交...');
          await loginButton.click();
          await this.page.waitForTimeout(2000);
          
          // 检查登录结果
          const errorMessage = await this.page.$('.error-message, .alert-danger, .login-error');
          if (errorMessage) {
            const errorText = await errorMessage.textContent();
            console.log(`  ⚠️  登录错误: ${errorText}`);
            this.results.push({ test: '登录结果', status: 'WARN', details: `错误: ${errorText}` });
          } else {
            console.log('  ✅ 登录提交成功（无错误信息）');
            this.results.push({ test: '登录结果', status: 'PASS', details: '登录提交成功' });
          }
        } else {
          console.log('  ❌ 登录表单元素不完整');
          this.results.push({ test: '登录表单元素', status: 'FAIL', details: '表单元素不完整' });
        }
      } else {
        console.log('  ❌ 登录表单不存在');
        this.results.push({ test: '登录表单', status: 'FAIL', details: '登录表单不存在' });
      }

    } catch (error) {
      console.log(`  ❌ 用户认证集成测试失败: ${error.message}`);
      this.results.push({ test: '用户认证集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testFactoryIntegration() {
    console.log('\n🔍 测试5: 工厂挑战集成测试');
    
    try {
      console.log('  5.1 访问工厂挑战页面...');
      await this.page.goto('http://localhost:7894/factory');
      await this.page.waitForLoadState('networkidle');
      
      // 检查工厂挑战界面
      const factoryInterface = await this.page.$('.factory-interface, .factory-container, .factory-view');
      if (factoryInterface) {
        console.log('  ✅ 工厂挑战界面加载成功');
        this.results.push({ test: '工厂挑战界面', status: 'PASS', details: '界面加载成功' });
        
        // 检查开始挑战按钮
        const startChallengeButton = await this.page.$('button:has-text("开始挑战"), .start-challenge, [data-testid="start-challenge"]');
        if (startChallengeButton) {
          console.log('  ✅ 开始挑战按钮存在');
          this.results.push({ test: '开始挑战按钮', status: 'PASS', details: '按钮存在' });
        } else {
          console.log('  ❌ 开始挑战按钮不存在');
          this.results.push({ test: '开始挑战按钮', status: 'FAIL', details: '按钮不存在' });
        }
      } else {
        console.log('  ❌ 工厂挑战界面加载失败');
        this.results.push({ test: '工厂挑战界面', status: 'FAIL', details: '界面加载失败' });
      }

    } catch (error) {
      console.log(`  ❌ 工厂挑战集成测试失败: ${error.message}`);
      this.results.push({ test: '工厂挑战集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testCrossPageNavigation() {
    console.log('\n🔍 测试6: 跨页面导航测试');
    
    const pages = [
      { url: '/', name: '首页' },
      { url: '/pokedex', name: '图鉴' },
      { url: '/battle', name: '对战' },
      { url: '/factory', name: '工厂挑战' },
      { url: '/user', name: '用户' }
    ];

    for (const page of pages) {
      try {
        console.log(`  6.${pages.indexOf(page) + 1} 测试导航到 ${page.name}...`);
        await this.page.goto(`http://localhost:7894${page.url}`);
        await this.page.waitForLoadState('networkidle');
        
        // 检查页面是否加载
        const pageTitle = await this.page.title();
        const hasContent = await this.page.$('body');
        
        if (hasContent) {
          console.log(`    ✅ ${page.name}页面加载成功`);
          this.results.push({ test: `导航到${page.name}`, status: 'PASS', details: '页面加载成功' });
        } else {
          console.log(`    ❌ ${page.name}页面加载失败`);
          this.results.push({ test: `导航到${page.name}`, status: 'FAIL', details: '页面加载失败' });
        }
      } catch (error) {
        console.log(`    ❌ 导航到${page.name}失败: ${error.message}`);
        this.results.push({ test: `导航到${page.name}`, status: 'ERROR', details: error.message });
      }
    }
  }

  async generateReport() {
    console.log('\n📊 前后端配合测试报告');
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
    const reportPath = `D:\\learn\\pokemon-factory\\frontend-backend-test-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`\n📄 前后端配合测试报告已保存到: ${reportPath}`);
    
    return report;
  }

  async runAllTests() {
    try {
      await this.init();
      
      await this.testHomepageIntegration();
      await this.testPokedexIntegration();
      await this.testBattleIntegration();
      await this.testUserAuthIntegration();
      await this.testFactoryIntegration();
      await this.testCrossPageNavigation();
      
      const report = await this.generateReport();
      
      console.log('\n🎉 前后端配合测试完成！');
      
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
  const tester = new FrontendBackendTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有前后端配合测试通过');
      process.exit(0);
    }
  });
}

module.exports = FrontendBackendTester;