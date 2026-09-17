const { chromium } = require('playwright');

// AI Web Tester - 测试宝可梦工厂项目的整体功能
class AIWebTester {
  constructor() {
    this.browser = null;
    this.page = null;
    this.results = [];
  }

  async init() {
    console.log('🚀 初始化 AI Web Tester...');
    this.browser = await chromium.launch({ 
      headless: false,  // 显示浏览器以便观察
      args: ['--no-sandbox']
    });
    this.page = await this.browser.newPage();
    
    // 设置视口大小
    await this.page.setViewportSize({ width: 1280, height: 720 });
    
    console.log('✅ 浏览器初始化完成');
  }

  async testFrontend() {
    console.log('\n🔍 测试前端功能...');
    
    try {
      // 测试1: 访问前端页面
      console.log('1. 测试前端页面加载...');
      await this.page.goto('http://localhost:7894');
      await this.page.waitForLoadState('networkidle');
      
      // 检查页面标题
      const title = await this.page.title();
      console.log(`   页面标题: ${title}`);
      
      // 检查是否有Vue应用挂载
      const appElement = await this.page.$('#app');
      if (appElement) {
        console.log('   ✅ Vue应用成功挂载');
        this.results.push({ test: '前端页面加载', status: 'PASS', details: 'Vue应用成功挂载' });
      } else {
        console.log('   ❌ Vue应用未挂载');
        this.results.push({ test: '前端页面加载', status: 'FAIL', details: 'Vue应用未挂载' });
      }

      // 测试2: 检查导航菜单
      console.log('2. 测试导航菜单...');
      const navItems = await this.page.$$('nav a, .nav-item, .menu-item');
      console.log(`   找到 ${navItems.length} 个导航项`);
      
      if (navItems.length > 0) {
        console.log('   ✅ 导航菜单存在');
        this.results.push({ test: '导航菜单', status: 'PASS', details: `找到${navItems.length}个导航项` });
      } else {
        console.log('   ❌ 导航菜单不存在');
        this.results.push({ test: '导航菜单', status: 'FAIL', details: '导航菜单不存在' });
      }

      // 测试3: 检查主要功能区域
      console.log('3. 测试主要功能区域...');
      const mainContent = await this.page.$('main, .main-content, .content');
      if (mainContent) {
        console.log('   ✅ 主要内容区域存在');
        this.results.push({ test: '主要功能区域', status: 'PASS', details: '主要内容区域存在' });
      } else {
        console.log('   ❌ 主要内容区域不存在');
        this.results.push({ test: '主要功能区域', status: 'FAIL', details: '主要内容区域不存在' });
      }

    } catch (error) {
      console.log(`   ❌ 前端测试失败: ${error.message}`);
      this.results.push({ test: '前端测试', status: 'ERROR', details: error.message });
    }
  }

  async testBackendAPI() {
    console.log('\n🔍 测试后端API...');
    
    try {
      // 测试1: 检查后端API是否可访问
      console.log('1. 测试后端API连接...');
      const response = await this.page.goto('http://localhost:8084/api/pokedex/summary');
      
      if (response && response.status() === 200) {
        console.log('   ✅ 后端API可访问');
        const data = await response.json();
        console.log(`   API返回数据: ${JSON.stringify(data).substring(0, 100)}...`);
        this.results.push({ test: '后端API连接', status: 'PASS', details: 'API返回200状态码' });
      } else {
        console.log('   ❌ 后端API不可访问');
        this.results.push({ test: '后端API连接', status: 'FAIL', details: `状态码: ${response?.status()}` });
      }

      // 测试2: 测试宝可梦列表API
      console.log('2. 测试宝可梦列表API...');
      const pokemonResponse = await this.page.goto('http://localhost:8084/api/pokemon?limit=5');
      
      if (pokemonResponse && pokemonResponse.status() === 200) {
        const pokemonData = await pokemonResponse.json();
        console.log(`   ✅ 宝可梦列表API正常，返回${pokemonData.length || 0}条数据`);
        this.results.push({ test: '宝可梦列表API', status: 'PASS', details: `返回${pokemonData.length || 0}条数据` });
      } else {
        console.log('   ❌ 宝可梦列表API异常');
        this.results.push({ test: '宝可梦列表API', status: 'FAIL', details: `状态码: ${pokemonResponse?.status()}` });
      }

    } catch (error) {
      console.log(`   ❌ 后端API测试失败: ${error.message}`);
      this.results.push({ test: '后端API测试', status: 'ERROR', details: error.message });
    }
  }

  async testFrontendBackendIntegration() {
    console.log('\n🔍 测试前后端集成...');
    
    try {
      // 测试1: 从前端页面测试API调用
      console.log('1. 测试前端页面API调用...');
      await this.page.goto('http://localhost:7894');
      await this.page.waitForLoadState('networkidle');
      
      // 监听网络请求
      const apiCalls = [];
      this.page.on('request', request => {
        if (request.url().includes('/api/')) {
          apiCalls.push(request.url());
        }
      });

      // 等待一段时间让API调用完成
      await this.page.waitForTimeout(2000);
      
      console.log(`   前端发起的API调用: ${apiCalls.length}个`);
      if (apiCalls.length > 0) {
        console.log('   ✅ 前端成功调用后端API');
        this.results.push({ test: '前后端集成', status: 'PASS', details: `前端发起${apiCalls.length}个API调用` });
      } else {
        console.log('   ⚠️  前端未发起API调用');
        this.results.push({ test: '前后端集成', status: 'WARN', details: '前端未发起API调用' });
      }

      // 测试2: 测试用户认证流程
      console.log('2. 测试用户认证流程...');
      const loginButton = await this.page.$('button:has-text("登录"), .login-btn, [data-testid="login"]');
      if (loginButton) {
        console.log('   ✅ 登录按钮存在');
        this.results.push({ test: '用户认证', status: 'PASS', details: '登录按钮存在' });
      } else {
        console.log('   ⚠️  登录按钮不存在');
        this.results.push({ test: '用户认证', status: 'WARN', details: '登录按钮不存在' });
      }

    } catch (error) {
      console.log(`   ❌ 前后端集成测试失败: ${error.message}`);
      this.results.push({ test: '前后端集成测试', status: 'ERROR', details: error.message });
    }
  }

  async testBattleSystem() {
    console.log('\n🔍 测试对战系统...');
    
    try {
      // 测试1: 访问对战页面
      console.log('1. 测试对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      
      // 检查对战界面元素
      const battleElements = await this.page.$$('.battle, .battle-container, [data-testid="battle"]');
      if (battleElements.length > 0) {
        console.log('   ✅ 对战页面加载成功');
        this.results.push({ test: '对战页面', status: 'PASS', details: '对战页面加载成功' });
      } else {
        console.log('   ❌ 对战页面加载失败');
        this.results.push({ test: '对战页面', status: 'FAIL', details: '对战页面加载失败' });
      }

      // 测试2: 测试对战按钮
      console.log('2. 测试对战按钮...');
      const battleButton = await this.page.$('button:has-text("开始对战"), .battle-btn, [data-testid="start-battle"]');
      if (battleButton) {
        console.log('   ✅ 对战按钮存在');
        this.results.push({ test: '对战按钮', status: 'PASS', details: '对战按钮存在' });
      } else {
        console.log('   ❌ 对战按钮不存在');
        this.results.push({ test: '对战按钮', status: 'FAIL', details: '对战按钮不存在' });
      }

    } catch (error) {
      console.log(`   ❌ 对战系统测试失败: ${error.message}`);
      this.results.push({ test: '对战系统测试', status: 'ERROR', details: error.message });
    }
  }

  async testPokedex() {
    console.log('\n🔍 测试图鉴系统...');
    
    try {
      // 测试1: 访问图鉴页面
      console.log('1. 测试图鉴页面...');
      await this.page.goto('http://localhost:7894/pokedex');
      await this.page.waitForLoadState('networkidle');
      
      // 检查图鉴列表
      const pokemonCards = await this.page.$$('.pokemon-card, .pokemon-item, [data-testid="pokemon"]');
      console.log(`   找到 ${pokemonCards.length} 个宝可梦卡片`);
      
      if (pokemonCards.length > 0) {
        console.log('   ✅ 图鉴页面加载成功');
        this.results.push({ test: '图鉴页面', status: 'PASS', details: `找到${pokemonCards.length}个宝可梦卡片` });
      } else {
        console.log('   ❌ 图鉴页面加载失败');
        this.results.push({ test: '图鉴页面', status: 'FAIL', details: '图鉴页面加载失败' });
      }

      // 测试2: 测试搜索功能
      console.log('2. 测试搜索功能...');
      const searchInput = await this.page.$('input[type="text"], .search-input, [data-testid="search"]');
      if (searchInput) {
        await searchInput.fill('皮卡丘');
        await this.page.waitForTimeout(1000);
        console.log('   ✅ 搜索功能可用');
        this.results.push({ test: '搜索功能', status: 'PASS', details: '搜索功能可用' });
      } else {
        console.log('   ❌ 搜索功能不可用');
        this.results.push({ test: '搜索功能', status: 'FAIL', details: '搜索功能不可用' });
      }

    } catch (error) {
      console.log(`   ❌ 图鉴系统测试失败: ${error.message}`);
      this.results.push({ test: '图鉴系统测试', status: 'ERROR', details: error.message });
    }
  }

  async testFactoryChallenge() {
    console.log('\n🔍 测试工厂挑战...');
    
    try {
      // 测试1: 访问工厂挑战页面
      console.log('1. 测试工厂挑战页面...');
      await this.page.goto('http://localhost:7894/factory');
      await this.page.waitForLoadState('networkidle');
      
      // 检查工厂挑战界面
      const factoryElements = await this.page.$$('.factory, .factory-container, [data-testid="factory"]');
      if (factoryElements.length > 0) {
        console.log('   ✅ 工厂挑战页面加载成功');
        this.results.push({ test: '工厂挑战页面', status: 'PASS', details: '工厂挑战页面加载成功' });
      } else {
        console.log('   ❌ 工厂挑战页面加载失败');
        this.results.push({ test: '工厂挑战页面', status: 'FAIL', details: '工厂挑战页面加载失败' });
      }

    } catch (error) {
      console.log(`   ❌ 工厂挑战测试失败: ${error.message}`);
      this.results.push({ test: '工厂挑战测试', status: 'ERROR', details: error.message });
    }
  }

  async testUserSystem() {
    console.log('\n🔍 测试用户系统...');
    
    try {
      // 测试1: 访问用户页面
      console.log('1. 测试用户页面...');
      await this.page.goto('http://localhost:7894/user');
      await this.page.waitForLoadState('networkidle');
      
      // 检查用户界面
      const userElements = await this.page.$$('.user, .user-container, [data-testid="user"]');
      if (userElements.length > 0) {
        console.log('   ✅ 用户页面加载成功');
        this.results.push({ test: '用户页面', status: 'PASS', details: '用户页面加载成功' });
      } else {
        console.log('   ❌ 用户页面加载失败');
        this.results.push({ test: '用户页面', status: 'FAIL', details: '用户页面加载失败' });
      }

      // 测试2: 测试注册功能
      console.log('2. 测试注册功能...');
      const registerButton = await this.page.$('button:has-text("注册"), .register-btn, [data-testid="register"]');
      if (registerButton) {
        console.log('   ✅ 注册功能可用');
        this.results.push({ test: '注册功能', status: 'PASS', details: '注册功能可用' });
      } else {
        console.log('   ❌ 注册功能不可用');
        this.results.push({ test: '注册功能', status: 'FAIL', details: '注册功能不可用' });
      }

    } catch (error) {
      console.log(`   ❌ 用户系统测试失败: ${error.message}`);
      this.results.push({ test: '用户系统测试', status: 'ERROR', details: error.message });
    }
  }

  async generateReport() {
    console.log('\n📊 测试报告');
    console.log('=' .repeat(50));
    
    const totalTests = this.results.length;
    const passedTests = this.results.filter(r => r.status === 'PASS').length;
    const failedTests = this.results.filter(r => r.status === 'FAIL').length;
    const errorTests = this.results.filter(r => r.status === 'ERROR').length;
    const warnTests = this.results.filter(r => r.status === 'WARN').length;
    
    console.log(`总测试数: ${totalTests}`);
    console.log(`通过: ${passedTests}`);
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
      
      console.log(`${index + 1}. ${statusIcon} ${result.test}: ${result.details}`);
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
    const reportPath = `D:\\learn\\pokemon-factory\\test-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`\n📄 测试报告已保存到: ${reportPath}`);
    
    return report;
  }

  async runAllTests() {
    try {
      await this.init();
      
      await this.testFrontend();
      await this.testBackendAPI();
      await this.testFrontendBackendIntegration();
      await this.testBattleSystem();
      await this.testPokedex();
      await this.testFactoryChallenge();
      await this.testUserSystem();
      
      const report = await this.generateReport();
      
      console.log('\n🎉 测试完成！');
      
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
  const tester = new AIWebTester();
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

module.exports = AIWebTester;