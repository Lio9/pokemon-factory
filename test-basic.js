// 基本功能测试脚本
const http = require('http');
const https = require('https');

class BasicTester {
  constructor() {
    this.results = [];
  }

  async testBackendAPI() {
    console.log('🔍 测试后端API...');
    
    const endpoints = [
      // 健康检查端点不存在，跳过测试
      { url: 'http://localhost:8084/api/pokedex/summary', name: '图鉴摘要' },
      { url: 'http://localhost:8084/api/pokedex/pokemon/list?size=5', name: '宝可梦列表' },
      { url: 'http://localhost:8084/api/pokedex/moves/list?size=5', name: '技能列表' },
      { url: 'http://localhost:8084/api/pokedex/abilities/list?size=5', name: '特性列表' },
      { url: 'http://localhost:8084/api/pokedex/items/list?size=5', name: '道具列表' }
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

  async testFrontend() {
    console.log('\n🔍 测试前端...');
    
    try {
      console.log('  测试前端页面...');
      const response = await this.httpGet('http://localhost:7894');
      
      if (response.statusCode === 200) {
        console.log(`    ✅ 前端页面 - 状态码: ${response.statusCode}`);
        this.results.push({ test: '前端页面', status: 'PASS', details: `状态码: ${response.statusCode}` });
        
        // 检查页面内容
        if (response.body.includes('pokemon') || response.body.includes('宝可梦')) {
          console.log('    ✅ 页面内容包含相关内容');
          this.results.push({ test: '页面内容', status: 'PASS', details: '页面包含相关内容' });
        } else {
          console.log('    ⚠️  页面内容可能为空或不相关');
          this.results.push({ test: '页面内容', status: 'WARN', details: '页面内容可能为空或不相关' });
        }
      } else {
        console.log(`    ❌ 前端页面 - 状态码: ${response.statusCode}`);
        this.results.push({ test: '前端页面', status: 'FAIL', details: `状态码: ${response.statusCode}` });
      }
    } catch (error) {
      console.log(`    ❌ 前端测试失败: ${error.message}`);
      this.results.push({ test: '前端页面', status: 'ERROR', details: error.message });
    }
  }

  async testDatabaseConnection() {
    console.log('\n🔍 测试数据库连接...');
    
    try {
      // 测试通过API检查数据库连接
      console.log('  测试数据库连接（通过API）...');
      const response = await this.httpGet('http://localhost:8084/api/pokedex/summary');
      
      if (response.statusCode === 200) {
        const data = JSON.parse(response.body);
        console.log('    ✅ 数据库连接正常');
        console.log(`    数据库统计: ${JSON.stringify(data).substring(0, 100)}...`);
        this.results.push({ test: '数据库连接', status: 'PASS', details: '数据库连接正常' });
      } else {
        console.log('    ❌ 数据库连接可能异常');
        this.results.push({ test: '数据库连接', status: 'FAIL', details: '数据库连接可能异常' });
      }
    } catch (error) {
      console.log(`    ❌ 数据库连接测试失败: ${error.message}`);
      this.results.push({ test: '数据库连接', status: 'ERROR', details: error.message });
    }
  }

  async httpGet(url) {
    return new Promise((resolve, reject) => {
      const client = url.startsWith('https') ? https : http;
      
      const req = client.get(url, (res) => {
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
    console.log('\n📊 基本功能测试报告');
    console.log('=' .repeat(50));
    
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
    console.log('🚀 开始基本功能测试...\n');
    
    await this.testBackendAPI();
    await this.testFrontend();
    await this.testDatabaseConnection();
    
    const report = await this.generateReport();
    
    console.log('\n🎉 基本功能测试完成！');
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const tester = new BasicTester();
  tester.runAllTests().then(report => {
    console.log('\n测试总结:');
    console.log(`成功率: ${report.summary.successRate}`);
    
    if (report.summary.failed > 0 || report.summary.errors > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有基本功能测试通过');
      process.exit(0);
    }
  });
}

module.exports = BasicTester;