#!/usr/bin/env node

// 宝可梦工厂项目 - 综合测试执行脚本
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

class TestRunner {
  constructor() {
    this.results = [];
    this.startTime = new Date();
  }

  async runTest(scriptName, description) {
    console.log(`\n🚀 运行 ${description}...`);
    console.log('=' .repeat(50));
    
    try {
      const startTime = Date.now();
      const result = execSync(`node ${scriptName}`, { 
        encoding: 'utf8',
        cwd: __dirname,
        timeout: 60000 // 60秒超时
      });
      
      const endTime = Date.now();
      const duration = (endTime - startTime) / 1000;
      
      console.log(result);
      console.log(`✅ ${description} 完成 (耗时: ${duration.toFixed(2)}秒)`);
      
      this.results.push({
        script: scriptName,
        description: description,
        status: 'PASS',
        duration: duration,
        output: result
      });
      
      return true;
    } catch (error) {
      console.log(`❌ ${description} 失败: ${error.message}`);
      
      this.results.push({
        script: scriptName,
        description: description,
        status: 'FAIL',
        error: error.message,
        output: error.stdout || ''
      });
      
      return false;
    }
  }

  async runAllTests() {
    console.log('🎯 宝可梦工厂项目 - 综合测试执行');
    console.log('=' .repeat(60));
    console.log(`开始时间: ${this.startTime.toLocaleString()}`);
    
    // 定义测试脚本
    const testScripts = [
      { script: 'test-basic.js', description: '基本功能测试' },
      { script: 'test-simple.js', description: '简单前后端配合测试' },
      { script: 'test-smart.js', description: '智能测试脚本' },
      { script: 'test-chrome.js', description: '系统Chrome测试脚本' }
    ];
    
    // 运行所有测试
    let passedTests = 0;
    let failedTests = 0;
    
    for (const test of testScripts) {
      const success = await this.runTest(test.script, test.description);
      if (success) {
        passedTests++;
      } else {
        failedTests++;
      }
    }
    
    // 生成测试报告
    const endTime = new Date();
    const totalDuration = (endTime - this.startTime) / 1000;
    
    console.log('\n📊 综合测试报告');
    console.log('=' .repeat(60));
    console.log(`结束时间: ${endTime.toLocaleString()}`);
    console.log(`总耗时: ${totalDuration.toFixed(2)}秒`);
    console.log(`测试脚本数: ${testScripts.length}`);
    console.log(`通过: ${passedTests}`);
    console.log(`失败: ${failedTests}`);
    console.log(`成功率: ${((passedTests / testScripts.length) * 100).toFixed(1)}%`);
    
    console.log('\n详细结果:');
    this.results.forEach((result, index) => {
      const statusIcon = result.status === 'PASS' ? '✅' : '❌';
      const duration = result.duration ? ` (${result.duration.toFixed(2)}秒)` : '';
      console.log(`${index + 1}. ${statusIcon} ${result.description}${duration}`);
    });
    
    // 生成JSON报告
    const report = {
      timestamp: new Date().toISOString(),
      startTime: this.startTime.toISOString(),
      endTime: endTime.toISOString(),
      totalDuration: totalDuration,
      summary: {
        totalScripts: testScripts.length,
        passedScripts: passedTests,
        failedScripts: failedTests,
        successRate: `${((passedTests / testScripts.length) * 100).toFixed(1)}%`
      },
      details: this.results
    };
    
    // 保存报告到文件
    const reportPath = path.join(__dirname, `test-execution-report-${Date.now()}.json`);
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    
    console.log(`\n📄 测试执行报告已保存到: ${reportPath}`);
    
    return report;
  }
}

// 运行测试
if (require.main === module) {
  const runner = new TestRunner();
  runner.runAllTests().then(report => {
    console.log('\n🎉 综合测试执行完成！');
    
    if (report.summary.failedScripts > 0) {
      console.log('\n⚠️  存在测试失败，请检查相关功能');
      process.exit(1);
    } else {
      console.log('\n✅ 所有测试脚本执行成功');
      process.exit(0);
    }
  });
}

module.exports = TestRunner;