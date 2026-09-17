const http = require('http');

// 简单的前后端配合测试
class SimpleTester {
  constructor() {
    this.results = [];
  }

  async testBackendEndpoints() {
    console.log('🔍 测试后端API端点...');
    
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

  async testAPIResponseFormat() {
    console.log('\n🔍 测试API响应格式...');
    
    try {
      console.log('  测试API响应格式...');
      const response = await this.httpGet('http://localhost:8084/api/pokedex/summary');
      
      if (response.statusCode === 200) {
        const data = JSON.parse(response.body);
        
        // 检查响应格式
        if (data.code === 200 && data.data) {
          console.log('    ✅ API响应格式正确');
          this.results.push({ test: 'API响应格式', status: 'PASS', details: '响应格式正确' });
        } else {
          console.log('    ❌ API响应格式异常');
          this.results.push({ test: 'API响应格式', status: 'FAIL', details: '响应格式异常' });
        }
      }
    } catch (error) {
      console.log(`    ❌ API响应格式测试失败: ${error.message}`);
      this.results.push({ test: 'API响应格式测试', status: 'ERROR', details: error.message });
    }
  }

  async testErrorHandling() {
    console.log('\n🔍 测试错误处理...');
    
    try {
      console.log('  测试不存在的API端点...');
      const response = await this.httpGet('http://localhost:8084/api/nonexistent');
      
      if (response.statusCode === 404) {
        console.log('    ✅ 错误处理正常（返回404）');
        this.results.push({ test: '错误处理', status: 'PASS', details: '返回404状态码' });
      } else {
        console.log(`    ⚠️  错误处理可能异常（状态码: ${response.statusCode}）`);
        this.results.push({ test: '错误处理', status: 'WARN', details: `状态码: ${response.statusCode}` });
      }
    } catch (error) {
      console.log(`    ❌ 错误处理测试失败: ${error.message}`);
      this.results.push({ test: '错误处理测试', status: 'ERROR', details: error.message });
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
    console.log('\n📊 简单前后端配合测试报告');
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
    console.log('🚀 开始简单前后端配合测试...\n');
    
    await this.testBackendEndpoints();
    await this.testFrontendPages();
    await this.testDatabaseIntegration();
    await this.testAPIResponseFormat();
    await this.testErrorHandling();
    
    const report = await this.generateReport();
    
    console.log('\n🎉 简单前后端配合测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new SimpleTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有简单前后端配合测试通过');
      process.exit(0);
    }
  });
}

module.exports = SimpleTester;