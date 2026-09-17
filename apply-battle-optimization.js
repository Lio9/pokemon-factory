const fs = require('fs');
const path = require('path');

// 应用对战界面优化
class BattleOptimizationApplicator {
  constructor() {
    this.frontendPath = path.join(__dirname, 'frontend', 'src', 'views');
    this.backupPath = path.join(__dirname, 'backup');
  }

  async applyOptimization() {
    console.log('🚀 开始应用对战界面优化...\n');
    
    try {
      // 1. 备份原文件
      await this.backupOriginal();
      
      // 2. 应用优化文件
      await this.applyOptimizedFile();
      
      // 3. 验证优化
      await this.verifyOptimization();
      
      console.log('\n🎉 对战界面优化应用完成！');
      console.log('\n📋 后续步骤:');
      console.log('1. 重启前端服务: cd frontend && npm run dev');
      console.log('2. 访问对战页面: http://localhost:7894/battle');
      console.log('3. 测试主题切换: 点击右上角的🌙/☀️按钮');
      console.log('4. 测试响应式: 调整浏览器窗口大小');
      
    } catch (error) {
      console.error('❌ 优化应用失败:', error.message);
      process.exit(1);
    }
  }

  async backupOriginal() {
    console.log('📦 备份原文件...');
    
    const originalFile = path.join(this.frontendPath, 'Battle.vue');
    const backupFile = path.join(this.backupPath, 'Battle.vue.backup');
    
    // 创建备份目录
    if (!fs.existsSync(this.backupPath)) {
      fs.mkdirSync(this.backupPath, { recursive: true });
    }
    
    // 备份原文件
    if (fs.existsSync(originalFile)) {
      fs.copyFileSync(originalFile, backupFile);
      console.log('  ✅ 原文件已备份到: backup/Battle.vue.backup');
    } else {
      console.log('  ⚠️  原文件不存在，跳过备份');
    }
  }

  async applyOptimizedFile() {
    console.log('\n📝 应用优化文件...');
    
    const optimizedFile = path.join(this.frontendPath, 'BattleOptimized.vue');
    const targetFile = path.join(this.frontendPath, 'Battle.vue');
    
    // 检查优化文件是否存在
    if (!fs.existsSync(optimizedFile)) {
      throw new Error('优化文件不存在: BattleOptimized.vue');
    }
    
    // 复制优化文件
    fs.copyFileSync(optimizedFile, targetFile);
    console.log('  ✅ 优化文件已应用');
  }

  async verifyOptimization() {
    console.log('\n🔍 验证优化...');
    
    const targetFile = path.join(this.frontendPath, 'Battle.vue');
    
    if (!fs.existsSync(targetFile)) {
      throw new Error('目标文件不存在');
    }
    
    const content = fs.readFileSync(targetFile, 'utf8');
    
    // 验证关键优化元素
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
        console.log(`  ✅ ${check.name}`);
        passedChecks++;
      } else {
        console.log(`  ❌ ${check.name}`);
      }
    }
    
    console.log(`\n📊 验证结果: ${passedChecks}/${checks.length} 通过`);
    
    if (passedChecks < checks.length) {
      console.log('⚠️  部分优化元素未找到，请检查优化文件');
    } else {
      console.log('✅ 所有优化元素验证通过');
    }
  }
}

// 运行应用
if (require.main === module) {
  const applicator = new BattleOptimizationApplicator();
  applicator.applyOptimization();
}

module.exports = BattleOptimizationApplicator;