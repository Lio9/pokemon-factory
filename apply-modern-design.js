const fs = require('fs');
const path = require('path');

// 现代化设计应用脚本
class ModernDesignApplicator {
  constructor() {
    this.frontendPath = path.join(__dirname, 'frontend', 'src');
    this.backupPath = path.join(__dirname, 'backup-modern');
  }

  async applyModernDesign() {
    console.log('🎨 开始应用现代化设计...\n');
    
    try {
      // 1. 备份原文件
      await this.backupOriginalFiles();
      
      // 2. 应用全局样式
      await this.applyGlobalStyles();
      
      // 3. 应用现代化组件
      await this.applyModernComponents();
      
      // 4. 更新主应用
      await this.updateMainApp();
      
      // 5. 验证设计
      await this.verifyDesign();
      
      console.log('\n🎉 现代化设计应用完成！');
      console.log('\n📋 后续步骤:');
      console.log('1. 重启前端服务: cd frontend && npm run dev');
      console.log('2. 访问首页: http://localhost:7894');
      console.log('3. 测试主题切换: 点击右上角的🌙/☀️按钮');
      console.log('4. 测试响应式: 调整浏览器窗口大小');
      console.log('5. 测试动画效果: 滚动页面观察动画');
      
    } catch (error) {
      console.error('❌ 设计应用失败:', error.message);
      process.exit(1);
    }
  }

  async backupOriginalFiles() {
    console.log('📦 备份原文件...');
    
    // 创建备份目录
    if (!fs.existsSync(this.backupPath)) {
      fs.mkdirSync(this.backupPath, { recursive: true });
    }
    
    // 备份主要文件
    const filesToBackup = [
      'App.vue',
      'views/Home.vue',
      'views/PokemonList.vue',
      'views/Battle.vue'
    ];
    
    for (const file of filesToBackup) {
      const sourcePath = path.join(this.frontendPath, file);
      const backupFilePath = path.join(this.backupPath, `${file}.backup`);
      
      // 确保备份目录存在
      const backupDir = path.dirname(backupFilePath);
      if (!fs.existsSync(backupDir)) {
        fs.mkdirSync(backupDir, { recursive: true });
      }
      
      if (fs.existsSync(sourcePath)) {
        fs.copyFileSync(sourcePath, backupFilePath);
        console.log(`  ✅ 备份 ${file}`);
      } else {
        console.log(`  ⚠️  ${file} 不存在，跳过备份`);
      }
    }
  }

  async applyGlobalStyles() {
    console.log('\n🎨 应用全局样式...');
    
    // 创建样式目录
    const stylesDir = path.join(this.frontendPath, 'styles');
    if (!fs.existsSync(stylesDir)) {
      fs.mkdirSync(stylesDir, { recursive: true });
    }
    
    // 复制全局样式文件
    const globalCssPath = path.join(__dirname, 'frontend', 'src', 'styles', 'global.css');
    if (fs.existsSync(globalCssPath)) {
      console.log('  ✅ 全局样式文件已存在');
    } else {
      console.log('  ⚠️  全局样式文件不存在，请先创建');
    }
  }

  async applyModernComponents() {
    console.log('\n🧩 应用现代化组件...');
    
    // 复制现代化组件
    const components = [
      'ModernNavbar.vue',
      'HomeModern.vue',
      'PokedexModern.vue'
    ];
    
    for (const component of components) {
      const sourcePath = path.join(__dirname, 'frontend', 'src', 'components', component);
      const targetPath = path.join(this.frontendPath, 'components', component);
      
      if (fs.existsSync(sourcePath)) {
        fs.copyFileSync(sourcePath, targetPath);
        console.log(`  ✅ 应用 ${component}`);
      } else {
        console.log(`  ⚠️  ${component} 不存在，请先创建`);
      }
    }
  }

  async updateMainApp() {
    console.log('\n📱 更新主应用...');
    
    // 读取App.vue
    const appPath = path.join(this.frontendPath, 'App.vue');
    let appContent = fs.readFileSync(appPath, 'utf8');
    
    // 添加全局样式导入
    if (!appContent.includes('global.css')) {
      const styleImport = `\n<style>\n@import './styles/global.css';\n</style>\n`;
      
      // 在现有样式标签前添加
      if (appContent.includes('<style')) {
        appContent = appContent.replace('<style', `${styleImport}<style`);
      } else {
        appContent += styleImport;
      }
      
      fs.writeFileSync(appPath, appContent);
      console.log('  ✅ 添加全局样式导入');
    }
    
    // 更新路由使用现代化组件
    const routerPath = path.join(this.frontendPath, 'router', 'index.js');
    if (fs.existsSync(routerPath)) {
      let routerContent = fs.readFileSync(routerPath, 'utf8');
      
      // 更新首页路由
      if (routerContent.includes("Home.vue")) {
        routerContent = routerContent.replace(
          "component: () => import('../views/Home.vue')",
          "component: () => import('../views/HomeModern.vue')"
        );
        console.log('  ✅ 更新首页路由');
      }
      
      // 更新图鉴路由
      if (routerContent.includes("PokemonList.vue")) {
        routerContent = routerContent.replace(
          "component: () => import('../views/PokemonList.vue')",
          "component: () => import('../views/PokedexModern.vue')"
        );
        console.log('  ✅ 更新图鉴路由');
      }
      
      fs.writeFileSync(routerPath, routerContent);
    }
  }

  async verifyDesign() {
    console.log('\n🔍 验证设计...');
    
    // 检查关键文件
    const requiredFiles = [
      'styles/global.css',
      'components/ModernNavbar.vue',
      'views/HomeModern.vue',
      'views/PokedexModern.vue'
    ];
    
    let allFilesExist = true;
    for (const file of requiredFiles) {
      const filePath = path.join(this.frontendPath, file);
      if (fs.existsSync(filePath)) {
        console.log(`  ✅ ${file} 存在`);
      } else {
        console.log(`  ❌ ${file} 不存在`);
        allFilesExist = false;
      }
    }
    
    if (allFilesExist) {
      console.log('  ✅ 所有设计文件验证通过');
    } else {
      console.log('  ⚠️  部分设计文件缺失');
    }
  }
}

// 运行应用
if (require.main === module) {
  const applicator = new ModernDesignApplicator();
  applicator.applyModernDesign();
}

module.exports = ModernDesignApplicator;