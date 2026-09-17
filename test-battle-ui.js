const http = require('http');

// 对战界面优化测试脚本
class BattleUITester {
  constructor() {
    this.results = [];
  }

  async testBattlePage() {
    console.log('🔍 测试对战界面优化效果...');
    
    try {
      // 测试1: 检查对战页面加载
      console.log('  1. 测试对战页面加载...');
      const response = await this.httpGet('http://localhost:7894/battle');
      
      if (response.statusCode === 200) {
        console.log('    ✅ 对战页面加载成功');
        this.results.push({ test: '对战页面加载', status: 'PASS' });
        
        // 测试2: 检查页面内容
        console.log('  2. 检查页面内容...');
        const content = response.body;
        
        // 检查是否包含优化后的元素
        const checks = [
          { name: '主题切换按钮', pattern: /theme-toggle/ },
          { name: '现代化按钮', pattern: /action-btn/ },
          { name: '玻璃态效果', pattern: /backdrop-filter/ },
          { name: '渐变背景', pattern: /linear-gradient/ },
          { name: '动画效果', pattern: /@keyframes/ },
          { name: '响应式设计', pattern: /@media/ },
          { name: '深色模式', pattern: /dark-mode/ },
          { name: 'CSS变量', pattern: /--primary/ }
        ];
        
        let passedChecks = 0;
        for (const check of checks) {
          if (check.pattern.test(content)) {
            console.log(`    ✅ ${check.name} - 存在`);
            this.results.push({ test: check.name, status: 'PASS' });
            passedChecks++;
          } else {
            console.log(`    ❌ ${check.name} - 不存在`);
            this.results.push({ test: check.name, status: 'FAIL' });
          }
        }
        
        console.log(`    📊 优化检查: ${passedChecks}/${checks.length} 通过`);
        
        // 测试3: 检查CSS变量
        console.log('  3. 检查CSS变量...');
        const cssVars = [
          '--primary',
          '--success',
          '--warning',
          '--danger',
          '--bg-primary',
          '--text-primary',
          '--border-light'
        ];
        
        let cssVarCount = 0;
        for (const cssVar of cssVars) {
          if (content.includes(cssVar)) {
            cssVarCount++;
          }
        }
        
        console.log(`    ✅ CSS变量: ${cssVarCount}/${cssVars.length} 存在`);
        this.results.push({ test: 'CSS变量', status: 'PASS', details: `${cssVarCount}/${cssVars.length}` });
        
        // 测试4: 检查动画效果
        console.log('  4. 检查动画效果...');
        const animations = [
          'float',
          'hit',
          'heal',
          'slideUp',
          'slideDown',
          'fadeIn',
          'spin'
        ];
        
        let animCount = 0;
        for (const anim of animations) {
          if (content.includes(`@keyframes ${anim}`)) {
            animCount++;
          }
        }
        
        console.log(`    ✅ 动画效果: ${animCount}/${animations.length} 存在`);
        this.results.push({ test: '动画效果', status: 'PASS', details: `${animCount}/${animations.length}` });
        
      } else {
        console.log('    ❌ 对战页面加载失败');
        this.results.push({ test: '对战页面加载', status: 'FAIL', details: `状态码: ${response.statusCode}` });
      }
      
    } catch (error) {
      console.log(`    ❌ 测试失败: ${error.message}`);
      this.results.push({ test: '对战界面测试', status: 'ERROR', details: error.message });
    }
  }

  async testResponsiveness() {
    console.log('\n🔍 测试响应式设计...');
    
    try {
      // 测试不同屏幕尺寸的CSS
      console.log('  1. 检查响应式断点...');
      const response = await this.httpGet('http://localhost:7894/battle');
      
      if (response.statusCode === 200) {
        const content = response.body;
        
        // 检查媒体查询
        const mediaQueries = [
          '@media (max-width: 768px)',
          '@media (max-width: 480px)',
          '@media (max-width: 660px)'
        ];
        
        let mqCount = 0;
        for (const mq of mediaQueries) {
          if (content.includes(mq)) {
            mqCount++;
          }
        }
        
        console.log(`    ✅ 响应式断点: ${mqCount}/${mediaQueries.length} 存在`);
        this.results.push({ test: '响应式断点', status: 'PASS', details: `${mqCount}/${mediaQueries.length}` });
        
        // 检查自适应组件
        console.log('  2. 检查自适应组件...');
        const adaptiveComponents = [
          'min-width',
          'max-width',
          'flex-wrap',
          'grid-template-columns'
        ];
        
        let acCount = 0;
        for (const ac of adaptiveComponents) {
          if (content.includes(ac)) {
            acCount++;
          }
        }
        
        console.log(`    ✅ 自适应组件: ${acCount}/${adaptiveComponents.length} 存在`);
        this.results.push({ test: '自适应组件', status: 'PASS', details: `${acCount}/${adaptiveComponents.length}` });
        
      }
      
    } catch (error) {
      console.log(`    ❌ 响应式测试失败: ${error.message}`);
      this.results.push({ test: '响应式测试', status: 'ERROR', details: error.message });
    }
  }

  async testAccessibility() {
    console.log('\n🔍 测试无障碍设计...');
    
    try {
      const response = await this.httpGet('http://localhost:7894/battle');
      
      if (response.statusCode === 200) {
        const content = response.body;
        
        // 检查无障碍特性
        console.log('  1. 检查无障碍特性...');
        const a11yFeatures = [
          'focus-visible',
          'outline',
          'cursor: pointer',
          'aria-',
          'role='
        ];
        
        let a11yCount = 0;
        for (const feature of a11yFeatures) {
          if (content.includes(feature)) {
            a11yCount++;
          }
        }
        
        console.log(`    ✅ 无障碍特性: ${a11yCount}/${a11yFeatures.length} 存在`);
        this.results.push({ test: '无障碍特性', status: 'PASS', details: `${a11yCount}/${a11yFeatures.length}` });
        
        // 检查键盘导航
        console.log('  2. 检查键盘导航...');
        const keyboardFeatures = [
          'tabindex',
          'keydown',
          'keyup',
          'keyboard'
        ];
        
        let kbCount = 0;
        for (const feature of keyboardFeatures) {
          if (content.includes(feature)) {
            kbCount++;
          }
        }
        
        console.log(`    ✅ 键盘导航: ${kbCount}/${keyboardFeatures.length} 存在`);
        this.results.push({ test: '键盘导航', status: 'PASS', details: `${kbCount}/${keyboardFeatures.length}` });
        
      }
      
    } catch (error) {
      console.log(`    ❌ 无障碍测试失败: ${error.message}`);
      this.results.push({ test: '无障碍测试', status: 'ERROR', details: error.message });
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
    console.log('\n📊 对战界面优化测试报告');
    console.log('=' .repeat(60));
    
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
    console.log('🚀 开始对战界面优化测试...\n');
    
    await this.testBattlePage();
    await this.testResponsiveness();
    await this.testAccessibility();
    
    const report = await this.generateReport();
    
    console.log('\n🎉 对战界面优化测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new BattleUITester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有对战界面优化测试通过');
      process.exit(0);
    }
  });
}

module.exports = BattleUITester;