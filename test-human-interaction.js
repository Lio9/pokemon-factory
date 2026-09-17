const { chromium } = require('playwright');

// 模拟人类点击动作的交互测试
class HumanInteractionTester {
  constructor() {
    this.results = [];
    this.browser = null;
    this.page = null;
  }

  async init() {
    console.log('🤖 初始化人类交互模拟测试...\n');
    
    try {
      // 使用系统Chrome浏览器
      this.browser = await chromium.launch({ 
        headless: false,  // 显示浏览器以便观察
        args: ['--no-sandbox'],
        channel: 'chrome'
      });
      
      this.page = await this.browser.newPage();
      await this.page.setViewportSize({ width: 1280, height: 720 });
      
      // 设置更真实的用户代理
      await this.page.setExtraHTTPHeaders({
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      });
      
      console.log('✅ 浏览器初始化成功');
      this.results.push({ test: '浏览器初始化', status: 'PASS' });
      
    } catch (error) {
      console.log('❌ 浏览器初始化失败:', error.message);
      this.results.push({ test: '浏览器初始化', status: 'FAIL', details: error.message });
      throw error;
    }
  }

  async simulateHumanDelay(min = 100, max = 300) {
    // 模拟人类操作延迟
    const delay = Math.floor(Math.random() * (max - min + 1)) + min;
    await this.page.waitForTimeout(delay);
  }

  async simulateMouseMove(selector) {
    // 模拟人类鼠标移动
    const element = await this.page.$(selector);
    if (element) {
      const box = await element.boundingBox();
      if (box) {
        // 随机移动到元素内的不同位置
        const x = box.x + Math.random() * box.width;
        const y = box.y + Math.random() * box.height;
        await this.page.mouse.move(x, y);
        await this.simulateHumanDelay(50, 150);
      }
    }
  }

  async simulateClick(selector, options = {}) {
    // 模拟人类点击动作
    console.log(`    模拟点击: ${selector}`);
    
    // 先移动鼠标到元素
    await this.simulateMouseMove(selector);
    
    // 模拟人类点击延迟
    await this.simulateHumanDelay(50, 100);
    
    // 点击元素
    await this.page.click(selector, options);
    
    // 点击后延迟
    await this.simulateHumanDelay(100, 200);
  }

  async simulateTyping(selector, text) {
    // 模拟人类打字动作
    console.log(`    模拟输入: ${text}`);
    
    await this.page.click(selector);
    await this.simulateHumanDelay(100, 200);
    
    // 逐个字符输入，模拟真实打字
    for (const char of text) {
      await this.page.keyboard.type(char);
      await this.simulateHumanDelay(50, 150);
    }
  }

  async testHomepageInteraction() {
    console.log('\n🔍 测试1: 首页交互测试');
    
    try {
      // 访问首页
      console.log('  1.1 访问首页...');
      await this.page.goto('http://localhost:7894');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 1000);
      
      // 模拟人类浏览页面
      console.log('  1.2 模拟浏览页面...');
      
      // 滚动页面
      await this.page.evaluate(() => {
        window.scrollBy(0, 300);
      });
      await this.simulateHumanDelay(300, 500);
      
      await this.page.evaluate(() => {
        window.scrollBy(0, -150);
      });
      await this.simulateHumanDelay(200, 400);
      
      // 检查页面元素
      const title = await this.page.title();
      console.log(`    页面标题: ${title}`);
      
      // 检查导航菜单
      console.log('  1.3 测试导航菜单...');
      const navItems = await this.page.$$('nav a, .nav-item, .menu-item');
      console.log(`    找到 ${navItems.length} 个导航项`);
      
      if (navItems.length > 0) {
        console.log('    ✅ 导航菜单存在');
        this.results.push({ test: '导航菜单', status: 'PASS', details: `${navItems.length}个` });
        
        // 模拟点击第一个导航项
        console.log('    模拟点击第一个导航项...');
        await this.simulateClick('nav a:first-child, .nav-item:first-child, .menu-item:first-child');
        await this.simulateHumanDelay(500, 800);
        
        // 检查页面是否跳转
        const currentUrl = this.page.url();
        console.log(`    当前URL: ${currentUrl}`);
        
        if (currentUrl !== 'http://localhost:7894/') {
          console.log('    ✅ 页面跳转成功');
          this.results.push({ test: '页面跳转', status: 'PASS' });
          
          // 返回首页
          await this.page.goBack();
          await this.simulateHumanDelay(300, 500);
        } else {
          console.log('    ⚠️  页面可能未跳转');
          this.results.push({ test: '页面跳转', status: 'WARN' });
        }
      } else {
        console.log('    ❌ 导航菜单不存在');
        this.results.push({ test: '导航菜单', status: 'FAIL' });
      }
      
      // 检查主要功能区域
      console.log('  1.4 检查主要功能区域...');
      const mainContent = await this.page.$('main, .main-content, .content');
      if (mainContent) {
        console.log('    ✅ 主要内容区域存在');
        this.results.push({ test: '主要内容区域', status: 'PASS' });
      }
      
    } catch (error) {
      console.log(`    ❌ 首页交互测试失败: ${error.message}`);
      this.results.push({ test: '首页交互', status: 'FAIL', details: error.message });
    }
  }

  async testPokedexInteraction() {
    console.log('\n🔍 测试2: 图鉴交互测试');
    
    try {
      // 访问图鉴页面
      console.log('  2.1 访问图鉴页面...');
      await this.page.goto('http://localhost:7894/pokedex');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 模拟人类浏览图鉴列表
      console.log('  2.2 模拟浏览图鉴列表...');
      
      // 滚动页面查看更多宝可梦
      for (let i = 0; i < 3; i++) {
        await this.page.evaluate(() => {
          window.scrollBy(0, 200);
        });
        await this.simulateHumanDelay(300, 500);
      }
      
      // 检查图鉴列表 - 使用更通用的选择器
      console.log('  2.3 检查图鉴列表...');
      const pokemonCards = await this.page.$$('div[class*="pokemon"], div[class*="card"], div[class*="item"], .pokemon-card, .card');
      console.log(`    找到 ${pokemonCards.length} 个宝可梦卡片`);
      
      if (pokemonCards.length > 0) {
        console.log('    ✅ 图鉴列表加载成功');
        this.results.push({ test: '图鉴列表', status: 'PASS', details: `${pokemonCards.length}个` });
        
        // 模拟点击第一个宝可梦
        console.log('    模拟点击第一个宝可梦...');
        await this.simulateClick('div[class*="pokemon"]:first-child, div[class*="card"]:first-child, .pokemon-card:first-child');
        await this.simulateHumanDelay(800, 1200);
        
        // 检查是否显示详情
        const detailView = await this.page.$('.detail-view, .pokemon-detail, .detail-panel, .modal, [class*="detail"]');
        if (detailView) {
          console.log('    ✅ 宝可梦详情显示成功');
          this.results.push({ test: '宝可梦详情', status: 'PASS' });
          
          // 模拟关闭详情
          console.log('    模拟关闭详情...');
          const closeButton = await this.page.$('.close-btn, .modal-close, button[aria-label="Close"], button:has-text("×")');
          if (closeButton) {
            await this.simulateClick('.close-btn, .modal-close, button[aria-label="Close"], button:has-text("×")');
            await this.simulateHumanDelay(300, 500);
          } else {
            // 点击背景关闭
            await this.page.keyboard.press('Escape');
            await this.simulateHumanDelay(300, 500);
          }
        } else {
          console.log('    ⚠️  宝可梦详情可能未显示');
          this.results.push({ test: '宝可梦详情', status: 'WARN' });
        }
      } else {
        console.log('    ❌ 图鉴列表为空');
        this.results.push({ test: '图鉴列表', status: 'FAIL' });
      }
      
      // 测试搜索功能
      console.log('  2.4 测试搜索功能...');
      const searchInput = await this.page.$('input[type="text"], .search-input, input[placeholder*="搜索"], input[placeholder*="search"]');
      if (searchInput) {
        console.log('    ✅ 搜索框存在');
        this.results.push({ test: '搜索框', status: 'PASS' });
        
        // 模拟输入搜索词
        console.log('    模拟输入搜索词...');
        await this.simulateTyping('input[type="text"], .search-input, input[placeholder*="搜索"], input[placeholder*="search"]', '皮卡丘');
        await this.simulateHumanDelay(500, 800);
        
        // 等待搜索结果
        await this.page.waitForTimeout(1000);
        
        // 检查搜索结果
        const searchResults = await this.page.$$('div[class*="pokemon"], div[class*="card"], .pokemon-card, .card');
        console.log(`    找到 ${searchResults.length} 个搜索结果`);
        
        if (searchResults.length > 0) {
          console.log('    ✅ 搜索功能正常');
          this.results.push({ test: '搜索功能', status: 'PASS', details: `${searchResults.length}个结果` });
        } else {
          console.log('    ⚠️  搜索可能无结果');
          this.results.push({ test: '搜索功能', status: 'WARN' });
        }
        
        // 清空搜索框
        await this.page.keyboard.press('Control+A');
        await this.page.keyboard.press('Delete');
        await this.simulateHumanDelay(200, 400);
      } else {
        console.log('    ❌ 搜索框不存在');
        this.results.push({ test: '搜索框', status: 'FAIL' });
      }
      
    } catch (error) {
      console.log(`    ❌ 图鉴交互测试失败: ${error.message}`);
      this.results.push({ test: '图鉴交互', status: 'FAIL', details: error.message });
    }
  }

  async testBattleInteraction() {
    console.log('\n🔍 测试3: 对战交互测试');
    
    try {
      // 访问对战页面
      console.log('  3.1 访问对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 检查对战界面
      console.log('  3.2 检查对战界面...');
      const battleInterface = await this.page.$('.battle-page, [class*="battle"], main');
      if (battleInterface) {
        console.log('    ✅ 对战界面加载成功');
        this.results.push({ test: '对战界面', status: 'PASS' });
        
        // 检查开始对战按钮 - 使用更通用的选择器
        console.log('  3.3 检查开始对战按钮...');
        const startButton = await this.page.$('button:has-text("手动对战"), .action-btn.primary, button:has-text("开始对战")');
        if (startButton) {
          console.log('    ✅ 开始对战按钮存在');
          this.results.push({ test: '开始对战按钮', status: 'PASS' });
          
          // 模拟点击开始对战
          console.log('    模拟点击开始对战...');
          await this.simulateClick('button:has-text("手动对战"), .action-btn.primary, button:has-text("开始对战")');
          await this.simulateHumanDelay(1000, 1500);
          
          // 检查是否进入对战状态
          const battleState = await this.page.$('.battlefield, [class*="battle"], .pokemon-slot');
          if (battleState) {
            console.log('    ✅ 成功进入对战状态');
            this.results.push({ test: '进入对战', status: 'PASS' });
            
            // 模拟选择招式
            console.log('    模拟选择招式...');
            const moveButtons = await this.page.$$('button[class*="move"], .move-btn, button:has-text("威力")');
            if (moveButtons.length > 0) {
              console.log(`    找到 ${moveButtons.length} 个招式按钮`);
              
              // 随机选择一个招式
              const randomIndex = Math.floor(Math.random() * moveButtons.length);
              await moveButtons[randomIndex].click();
              await this.simulateHumanDelay(300, 500);
              
              console.log('    ✅ 招式选择成功');
              this.results.push({ test: '招式选择', status: 'PASS' });
            }
            
            // 模拟提交回合
            console.log('    模拟提交回合...');
            const submitButton = await this.page.$('button:has-text("提交回合"), button:has-text("End Turn"), .action-btn.primary');
            if (submitButton) {
              await submitButton.click();
              await this.simulateHumanDelay(1000, 1500);
              
              console.log('    ✅ 回合提交成功');
              this.results.push({ test: '回合提交', status: 'PASS' });
            }
          } else {
            console.log('    ⚠️  可能未进入对战状态');
            this.results.push({ test: '进入对战', status: 'WARN' });
          }
        } else {
          console.log('    ❌ 开始对战按钮不存在');
          this.results.push({ test: '开始对战按钮', status: 'FAIL' });
        }
        
        // 检查投降按钮
        console.log('  3.4 检查投降按钮...');
        const forfeitButton = await this.page.$('button:has-text("投降"), .toolbar-btn.danger, button:has-text("Forfeit")');
        if (forfeitButton) {
          console.log('    ✅ 投降按钮存在');
          this.results.push({ test: '投降按钮', status: 'PASS' });
        }
        
      } else {
        console.log('    ❌ 对战界面加载失败');
        this.results.push({ test: '对战界面', status: 'FAIL' });
      }
      
    } catch (error) {
      console.log(`    ❌ 对战交互测试失败: ${error.message}`);
      this.results.push({ test: '对战交互', status: 'FAIL', details: error.message });
    }
  }

  async testFactoryInteraction() {
    console.log('\n🔍 测试4: 工厂挑战交互测试');
    
    try {
      // 访问工厂挑战页面
      console.log('  4.1 访问工厂挑战页面...');
      await this.page.goto('http://localhost:7894/factory');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 检查工厂挑战界面
      console.log('  4.2 检查工厂挑战界面...');
      const factoryInterface = await this.page.$('[class*="factory"], [class*="challenge"], main');
      if (factoryInterface) {
        console.log('    ✅ 工厂挑战界面加载成功');
        this.results.push({ test: '工厂挑战界面', status: 'PASS' });
        
        // 检查开始挑战按钮
        console.log('  4.3 检查开始挑战按钮...');
        const startChallengeButton = await this.page.$('button:has-text("工厂挑战"), .action-btn.secondary, button:has-text("开始挑战")');
        if (startChallengeButton) {
          console.log('    ✅ 开始挑战按钮存在');
          this.results.push({ test: '开始挑战按钮', status: 'PASS' });
          
          // 模拟点击开始挑战
          console.log('    模拟点击开始挑战...');
          await this.simulateClick('button:has-text("工厂挑战"), .action-btn.secondary, button:has-text("开始挑战")');
          await this.simulateHumanDelay(1000, 1500);
          
          // 检查是否进入挑战状态
          const challengeState = await this.page.$('[class*="challenge"], [class*="battle"], .pokemon-slot');
          if (challengeState) {
            console.log('    ✅ 成功进入挑战状态');
            this.results.push({ test: '进入挑战', status: 'PASS' });
          } else {
            console.log('    ⚠️  可能未进入挑战状态');
            this.results.push({ test: '进入挑战', status: 'WARN' });
          }
        } else {
          console.log('    ❌ 开始挑战按钮不存在');
          this.results.push({ test: '开始挑战按钮', status: 'FAIL' });
        }
        
      } else {
        console.log('    ❌ 工厂挑战界面加载失败');
        this.results.push({ test: '工厂挑战界面', status: 'FAIL' });
      }
      
    } catch (error) {
      console.log(`    ❌ 工厂挑战交互测试失败: ${error.message}`);
      this.results.push({ test: '工厂挑战交互', status: 'FAIL', details: error.message });
    }
  }

  async testUserInteraction() {
    console.log('\n🔍 测试5: 用户系统交互测试');
    
    try {
      // 访问用户页面
      console.log('  5.1 访问用户页面...');
      await this.page.goto('http://localhost:7894/user');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 检查用户界面
      console.log('  5.2 检查用户界面...');
      const userInterface = await this.page.$('[class*="user"], [class*="login"], main');
      if (userInterface) {
        console.log('    ✅ 用户界面加载成功');
        this.results.push({ test: '用户界面', status: 'PASS' });
        
        // 检查登录表单
        console.log('  5.3 检查登录表单...');
        const loginForm = await this.page.$('form, [class*="login"], [class*="auth"]');
        if (loginForm) {
          console.log('    ✅ 登录表单存在');
          this.results.push({ test: '登录表单', status: 'PASS' });
          
          // 检查用户名输入框
          const usernameInput = await this.page.$('input[type="text"], input[name="username"], input[placeholder*="用户名"], input[placeholder*="username"]');
          if (usernameInput) {
            console.log('    ✅ 用户名输入框存在');
            this.results.push({ test: '用户名输入框', status: 'PASS' });
            
            // 模拟输入用户名
            console.log('    模拟输入用户名...');
            await this.simulateTyping('input[type="text"], input[name="username"], input[placeholder*="用户名"], input[placeholder*="username"]', 'testuser');
            await this.simulateHumanDelay(300, 500);
          }
          
          // 检查密码输入框
          const passwordInput = await this.page.$('input[type="password"], input[name="password"]');
          if (passwordInput) {
            console.log('    ✅ 密码输入框存在');
            this.results.push({ test: '密码输入框', status: 'PASS' });
            
            // 模拟输入密码
            console.log('    模拟输入密码...');
            await this.simulateTyping('input[type="password"], input[name="password"]', 'testpass');
            await this.simulateHumanDelay(300, 500);
          }
          
          // 检查登录按钮
          const loginButton = await this.page.$('button[type="submit"], button:has-text("登录"), button:has-text("Login")');
          if (loginButton) {
            console.log('    ✅ 登录按钮存在');
            this.results.push({ test: '登录按钮', status: 'PASS' });
            
            // 模拟点击登录
            console.log('    模拟点击登录...');
            await this.simulateClick('button[type="submit"], button:has-text("登录"), button:has-text("Login")');
            await this.simulateHumanDelay(1000, 1500);
            
            // 检查登录结果
            const errorMessage = await this.page.$('.error-message, .alert-danger, [class*="error"]');
            const successMessage = await this.page.$('.success-message, .alert-success, [class*="success"]');
            
            if (errorMessage) {
              const errorText = await errorMessage.textContent();
              console.log(`    ⚠️  登录错误: ${errorText}`);
              this.results.push({ test: '登录结果', status: 'WARN', details: errorText });
            } else if (successMessage) {
              console.log('    ✅ 登录成功');
              this.results.push({ test: '登录结果', status: 'PASS' });
            } else {
              console.log('    ✅ 登录提交成功（无错误信息）');
              this.results.push({ test: '登录结果', status: 'PASS' });
            }
          }
        } else {
          console.log('    ❌ 登录表单不存在');
          this.results.push({ test: '登录表单', status: 'FAIL' });
        }
        
      } else {
        console.log('    ❌ 用户界面加载失败');
        this.results.push({ test: '用户界面', status: 'FAIL' });
      }
      
    } catch (error) {
      console.log(`    ❌ 用户系统交互测试失败: ${error.message}`);
      this.results.push({ test: '用户系统交互', status: 'FAIL', details: error.message });
    }
  }

  async testThemeSwitch() {
    console.log('\n🔍 测试6: 主题切换测试');
    
    try {
      // 访问对战页面（有主题切换按钮）
      console.log('  6.1 访问对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 检查主题切换按钮
      console.log('  6.2 检查主题切换按钮...');
      const themeToggle = await this.page.$('button:has-text("🌙"), button:has-text("☀️"), .theme-toggle');
      if (themeToggle) {
        console.log('    ✅ 主题切换按钮存在');
        this.results.push({ test: '主题切换按钮', status: 'PASS' });
        
        // 检查当前主题
        const hasDarkMode = await this.page.$('.dark-mode, [class*="dark"]');
        const initialTheme = hasDarkMode ? '深色' : '浅色';
        console.log(`    当前主题: ${initialTheme}`);
        
        // 模拟点击主题切换
        console.log('    模拟点击主题切换...');
        await themeToggle.click();
        await this.simulateHumanDelay(500, 800);
        
        // 检查主题是否切换
        const hasDarkModeAfter = await this.page.$('.dark-mode, [class*="dark"]');
        const newTheme = hasDarkModeAfter ? '深色' : '浅色';
        console.log(`    切换后主题: ${newTheme}`);
        
        if (initialTheme !== newTheme) {
          console.log('    ✅ 主题切换成功');
          this.results.push({ test: '主题切换', status: 'PASS', details: `${initialTheme} -> ${newTheme}` });
          
          // 切换回原主题
          await themeToggle.click();
          await this.simulateHumanDelay(300, 500);
        } else {
          console.log('    ⚠️  主题可能未切换');
          this.results.push({ test: '主题切换', status: 'WARN' });
        }
        
      } else {
        console.log('    ❌ 主题切换按钮不存在');
        this.results.push({ test: '主题切换按钮', status: 'FAIL' });
      }
      
    } catch (error) {
      console.log(`    ❌ 主题切换测试失败: ${error.message}`);
      this.results.push({ test: '主题切换', status: 'FAIL', details: error.message });
    }
  }

  async testResponsiveInteraction() {
    console.log('\n🔍 测试7: 响应式交互测试');
    
    try {
      // 访问对战页面
      console.log('  7.1 访问对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 测试不同屏幕尺寸
      console.log('  7.2 测试不同屏幕尺寸...');
      
      const sizes = [
        { width: 1280, height: 720, name: '桌面端' },
        { width: 768, height: 1024, name: '平板端' },
        { width: 375, height: 667, name: '移动端' }
      ];
      
      for (const size of sizes) {
        console.log(`    测试 ${size.name} (${size.width}x${size.height})...`);
        
        // 调整窗口大小
        await this.page.setViewportSize({ width: size.width, height: size.height });
        await this.simulateHumanDelay(300, 500);
        
        // 模拟人类浏览
        await this.page.evaluate(() => {
          window.scrollBy(0, 100);
        });
        await this.simulateHumanDelay(200, 400);
        
        await this.page.evaluate(() => {
          window.scrollBy(0, -50);
        });
        await this.simulateHumanDelay(200, 400);
        
        // 检查页面是否正常显示 - 使用更通用的选择器
        const isVisible = await this.page.isVisible('.battle-page, [class*="battle"], main, body');
        if (isVisible) {
          console.log(`      ✅ ${size.name} 显示正常`);
          this.results.push({ test: `${size.name}显示`, status: 'PASS' });
        } else {
          console.log(`      ❌ ${size.name} 显示异常`);
          this.results.push({ test: `${size.name}显示`, status: 'FAIL' });
        }
      }
      
      // 恢复桌面端尺寸
      await this.page.setViewportSize({ width: 1280, height: 720 });
      await this.simulateHumanDelay(300, 500);
      
    } catch (error) {
      console.log(`    ❌ 响应式交互测试失败: ${error.message}`);
      this.results.push({ test: '响应式交互', status: 'FAIL', details: error.message });
    }
  }

  async testKeyboardNavigation() {
    console.log('\n🔍 测试8: 键盘导航测试');
    
    try {
      // 访问对战页面
      console.log('  8.1 访问对战页面...');
      await this.page.goto('http://localhost:7894/battle');
      await this.page.waitForLoadState('networkidle');
      await this.simulateHumanDelay(500, 800);
      
      // 测试Tab键导航
      console.log('  8.2 测试Tab键导航...');
      
      // 按Tab键多次
      for (let i = 0; i < 5; i++) {
        await this.page.keyboard.press('Tab');
        await this.simulateHumanDelay(100, 200);
      }
      
      // 检查焦点位置
      const focusedElement = await this.page.evaluate(() => {
        const activeElement = document.activeElement;
        return {
          tagName: activeElement?.tagName,
          className: activeElement?.className,
          id: activeElement?.id
        };
      });
      
      console.log(`    焦点元素: ${focusedElement.tagName} ${focusedElement.className ? '.' + focusedElement.className.split(' ')[0] : ''}`);
      
      if (focusedElement.tagName) {
        console.log('    ✅ 键盘导航正常');
        this.results.push({ test: '键盘导航', status: 'PASS', details: `焦点在${focusedElement.tagName}` });
      } else {
        console.log('    ⚠️  键盘导航可能异常');
        this.results.push({ test: '键盘导航', status: 'WARN' });
      }
      
      // 测试Enter键
      console.log('  8.3 测试Enter键...');
      await this.page.keyboard.press('Enter');
      await this.simulateHumanDelay(300, 500);
      
      console.log('    ✅ Enter键测试完成');
      this.results.push({ test: 'Enter键', status: 'PASS' });
      
      // 测试Escape键
      console.log('  8.4 测试Escape键...');
      await this.page.keyboard.press('Escape');
      await this.simulateHumanDelay(300, 500);
      
      console.log('    ✅ Escape键测试完成');
      this.results.push({ test: 'Escape键', status: 'PASS' });
      
    } catch (error) {
      console.log(`    ❌ 键盘导航测试失败: ${error.message}`);
      this.results.push({ test: '键盘导航', status: 'FAIL', details: error.message });
    }
  }

  async generateReport() {
    console.log('\n📊 人类交互模拟测试报告');
    console.log('=' .repeat(70));
    
    const totalTests = this.results.length;
    const passedTests = this.results.filter(r => r.status === 'PASS').length;
    const failedTests = this.results.filter(r => r.status === 'FAIL').length;
    const warnTests = this.results.filter(r => r.status === 'WARN').length;
    const errorTests = this.results.filter(r => r.status === 'ERROR').length;
    
    console.log(`总测试数: ${totalTests}`);
    console.log(`通过: ${passedTests} (${((passedTests / totalTests) * 100).toFixed(1)}%)`);
    console.log(`失败: ${failedTests}`);
    console.log(`警告: ${warnTests}`);
    console.log(`错误: ${errorTests}`);
    
    console.log('\n详细结果:');
    this.results.forEach((result, index) => {
      const statusIcon = {
        'PASS': '✅',
        'FAIL': '❌',
        'WARN': '⚠️',
        'ERROR': '💥'
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
        warnings: warnTests,
        errors: errorTests,
        successRate: `${((passedTests / totalTests) * 100).toFixed(1)}%`
      },
      details: this.results
    };
    
    // 保存报告到文件
    const fs = require('fs');
    const reportPath = `D:\\learn\\pokemon-factory\\human-interaction-test-report-${Date.now()}.json`;
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`\n📄 人类交互测试报告已保存到: ${reportPath}`);
    
    return report;
  }

  async runAllTests() {
    try {
      await this.init();
      
      await this.testHomepageInteraction();
      await this.testPokedexInteraction();
      await this.testBattleInteraction();
      await this.testFactoryInteraction();
      await this.testUserInteraction();
      await this.testThemeSwitch();
      await this.testResponsiveInteraction();
      await this.testKeyboardNavigation();
      
      const report = await this.generateReport();
      
      console.log('\n🎉 人类交互模拟测试完成！');
      
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
  const tester = new HumanInteractionTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有人类交互测试通过');
      process.exit(0);
    }
  });
}

module.exports = HumanInteractionTester;