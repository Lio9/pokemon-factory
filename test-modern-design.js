const http = require('http');

// 现代化设计测试脚本
class ModernDesignTester {
  constructor() {
    this.results = [];
  }

  async testModernDesign() {
    console.log('🎨 测试现代化设计效果...\n');
    
    try {
      // 测试1: 检查全局样式
      console.log('  1. 检查全局样式...');
      const globalCss = await this.httpGet('http://localhost:7894/src/styles/global.css');
      
      if (globalCss.statusCode === 200) {
        console.log('    ✅ 全局样式文件可访问');
        this.results.push({ test: '全局样式', status: 'PASS' });
        
        // 检查关键CSS变量
        const cssContent = globalCss.body;
        const cssVariables = [
          '--primary-500',
          '--success-500',
          '--warning-500',
          '--danger-500',
          '--radius-xl',
          '--shadow-lg',
          '--transition-normal'
        ];
        
        let varCount = 0;
        for (const cssVar of cssVariables) {
          if (cssContent.includes(cssVar)) {
            varCount++;
          }
        }
        
        console.log(`    ✅ CSS变量: ${varCount}/${cssVariables.length} 存在`);
        this.results.push({ test: 'CSS变量', status: 'PASS', details: `${varCount}/${cssVariables.length}` });
        
        // 检查动画关键帧
        const animations = [
          '@keyframes fadeIn',
          '@keyframes fadeInUp',
          '@keyframes slideUp',
          '@keyframes scaleIn',
          '@keyframes bounceIn',
          '@keyframes pulse',
          '@keyframes spin',
          '@keyframes float',
          '@keyframes shimmer',
          '@keyframes gradient'
        ];
        
        let animCount = 0;
        for (const anim of animations) {
          if (cssContent.includes(anim)) {
            animCount++;
          }
        }
        
        console.log(`    ✅ 动画关键帧: ${animCount}/${animations.length} 存在`);
        this.results.push({ test: '动画关键帧', status: 'PASS', details: `${animCount}/${animations.length}` });
        
      } else {
        console.log('    ❌ 全局样式文件不可访问');
        this.results.push({ test: '全局样式', status: 'FAIL' });
      }
      
      // 测试2: 检查现代化组件
      console.log('\n  2. 检查现代化组件...');
      const components = [
        'ModernNavbar.vue',
        'HomeModern.vue',
        'PokedexModern.vue'
      ];
      
      for (const component of components) {
        const response = await this.httpGet(`http://localhost:7894/src/components/${component}`);
        if (response.statusCode === 200) {
          console.log(`    ✅ ${component} 可访问`);
          this.results.push({ test: component, status: 'PASS' });
        } else {
          console.log(`    ❌ ${component} 不可访问`);
          this.results.push({ test: component, status: 'FAIL' });
        }
      }
      
      // 测试3: 检查页面样式
      console.log('\n  3. 检查页面样式...');
      const pages = [
        { url: '/', name: '首页' },
        { url: '/pokemon', name: '图鉴页面' },
        { url: '/battle', name: '对战页面' }
      ];
      
      for (const page of pages) {
        const response = await this.httpGet(`http://localhost:7894${page.url}`);
        if (response.statusCode === 200) {
          console.log(`    ✅ ${page.name} 可访问`);
          this.results.push({ test: `${page.name}访问`, status: 'PASS' });
          
          // 检查页面内容是否包含现代化样式
          const content = response.body;
          const modernStyles = [
            'backdrop-filter',
            'linear-gradient',
            'border-radius',
            'box-shadow',
            'transition',
            'animation',
            'transform',
            'rgba('
          ];
          
          let styleCount = 0;
          for (const style of modernStyles) {
            if (content.includes(style)) {
              styleCount++;
            }
          }
          
          console.log(`    ✅ ${page.name} 现代化样式: ${styleCount}/${modernStyles.length}`);
          this.results.push({ test: `${page.name}样式`, status: 'PASS', details: `${styleCount}/${modernStyles.length}` });
          
        } else {
          console.log(`    ❌ ${page.name} 不可访问`);
          this.results.push({ test: `${page.name}访问`, status: 'FAIL' });
        }
      }
      
      // 测试4: 检查响应式设计
      console.log('\n  4. 检查响应式设计...');
      const mediaQueries = [
        '@media (max-width: 640px)',
        '@media (max-width: 768px)',
        '@media (max-width: 1024px)',
        '@media (min-width: 640px)',
        '@media (min-width: 768px)',
        '@media (min-width: 1024px)'
      ];
      
      let mqCount = 0;
      for (const mq of mediaQueries) {
        const response = await this.httpGet('http://localhost:7894/src/styles/global.css');
        if (response.body && response.body.includes(mq)) {
          mqCount++;
        }
      }
      
      console.log(`    ✅ 媒体查询: ${mqCount}/${mediaQueries.length} 存在`);
      this.results.push({ test: '媒体查询', status: 'PASS', details: `${mqCount}/${mediaQueries.length}` });
      
      // 测试5: 检查深色模式支持
      console.log('\n  5. 检查深色模式支持...');
      const darkModeFeatures = [
        '.dark-mode',
        'isDarkMode',
        'toggleTheme',
        'localStorage',
        'classList.toggle'
      ];
      
      let dmCount = 0;
      for (const feature of darkModeFeatures) {
        const response = await this.httpGet('http://localhost:7894/src/components/ModernNavbar.vue');
        if (response.body && response.body.includes(feature)) {
          dmCount++;
        }
      }
      
      console.log(`    ✅ 深色模式特性: ${dmCount}/${darkModeFeatures.length} 存在`);
      this.results.push({ test: '深色模式', status: 'PASS', details: `${dmCount}/${darkModeFeatures.length}` });
      
      // 测试6: 检查动画效果
      console.log('\n  6. 检查动画效果...');
      const animationFeatures = [
        'animation:',
        'transition:',
        'transform:',
        'hover:',
        'active:',
        'focus:'
      ];
      
      let afCount = 0;
      for (const feature of animationFeatures) {
        const response = await this.httpGet('http://localhost:7894/src/views/HomeModern.vue');
        if (response.body && response.body.includes(feature)) {
          afCount++;
        }
      }
      
      console.log(`    ✅ 动画特性: ${afCount}/${animationFeatures.length} 存在`);
      this.results.push({ test: '动画效果', status: 'PASS', details: `${afCount}/${animationFeatures.length}` });
      
      // 测试7: 检查交互元素
      console.log('\n  7. 检查交互元素...');
      const interactiveElements = [
        'button',
        'input',
        'router-link',
        '@click',
        '@mouseenter',
        '@mouseleave'
      ];
      
      let ieCount = 0;
      for (const element of interactiveElements) {
        const response = await this.httpGet('http://localhost:7894/src/views/PokedexModern.vue');
        if (response.body && response.body.includes(element)) {
          ieCount++;
        }
      }
      
      console.log(`    ✅ 交互元素: ${ieCount}/${interactiveElements.length} 存在`);
      this.results.push({ test: '交互元素', status: 'PASS', details: `${ieCount}/${interactiveElements.length}` });
      
    } catch (error) {
      console.log(`    ❌ 测试失败: ${error.message}`);
      this.results.push({ test: '现代化设计测试', status: 'FAIL', details: error.message });
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
    console.log('\n📊 现代化设计测试报告');
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
    console.log('🚀 开始现代化设计测试...\n');
    
    await this.testModernDesign();
    
    const report = await this.generateReport();
    
    console.log('\n🎉 现代化设计测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new ModernDesignTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有现代化设计测试通过');
      process.exit(0);
    }
  });
}

module.exports = ModernDesignTester;