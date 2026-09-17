# 宝可梦工厂项目 - 测试指南

## 快速开始

### 1. 运行所有测试
```bash
node run-all-tests.js
```

### 2. 运行单个测试脚本
```bash
# 基本功能测试
node test-basic.js

# 简单前后端配合测试
node test-simple.js

# 智能测试脚本
node test-smart.js

# 系统Chrome测试脚本
node test-chrome.js
```

## 测试脚本说明

### test-basic.js - 基本功能测试
- 测试后端API端点
- 测试前端页面加载
- 测试数据库连接
- **用例数**: 8个
- **执行时间**: ~0.3秒

### test-simple.js - 简单前后端配合测试
- 测试所有后端API端点
- 测试所有前端页面
- 测试数据库集成
- 测试API响应格式
- 测试错误处理
- **用例数**: 20个
- **执行时间**: ~0.3秒

### test-smart.js - 智能测试脚本
- 自动检测测试环境
- 测试后端API端点
- 测试前端页面
- 测试数据库集成
- 尝试Playwright UI测试（如果可用）
- **用例数**: 22个
- **执行时间**: ~0.5秒

### test-chrome.js - 系统Chrome测试脚本
- 使用系统Chrome浏览器
- 测试Vue应用挂载
- 测试导航菜单
- 测试API请求
- 测试图鉴页面
- **用例数**: 6个
- **执行时间**: ~10秒

## 测试环境要求

### 必需环境
- Node.js 20+
- 后端服务运行在端口 8084
- 前端服务运行在端口 7894

### 可选环境
- Playwright (用于UI自动化测试)
- 系统Chrome浏览器 (用于真实浏览器测试)

## 测试结果说明

### 状态图标
- ✅ **PASS**: 测试通过
- ❌ **FAIL**: 测试失败
- 💥 **ERROR**: 测试错误
- ⚠️ **WARN**: 测试警告

### 成功率计算
- **100%**: 所有测试通过
- **95%+**: 基本通过，有少量警告
- **90%+**: 大部分通过，需要检查失败项
- **<90%**: 需要修复问题

## 常见问题解决

### 1. Playwright浏览器未安装
```bash
npx playwright install chromium
```

### 2. 后端服务未启动
```bash
cd backend
mvn package -pl battle -am -DskipTests
java -jar backend/battle/target/battle-0.0.1-SNAPSHOT.jar
```

### 3. 前端服务未启动
```bash
cd frontend
npm run dev
```

### 4. 数据库连接失败
```bash
python scripts/setup.py
```

## 测试报告

测试执行后会生成以下报告：
- `test-execution-report-{timestamp}.json` - JSON格式测试报告
- `COMPREHENSIVE-TEST-REPORT.md` - 综合测试报告
- `TEST-SUMMARY-FINAL.md` - 最终测试总结

## 自定义测试

### 添加新测试用例
1. 在相应的测试脚本中添加测试方法
2. 在 `runAllTests()` 方法中调用新测试方法
3. 在 `generateReport()` 方法中处理测试结果

### 创建新测试脚本
1. 复制现有测试脚本作为模板
2. 修改测试逻辑和断言
3. 在 `run-all-tests.js` 中添加新脚本

## 最佳实践

### 1. 定期运行测试
- 每次代码提交前运行测试
- 每次部署前运行完整测试
- 定期运行性能测试

### 2. 维护测试环境
- 保持测试环境与生产环境一致
- 定期更新测试数据
- 清理测试产生的临时文件

### 3. 分析测试结果
- 关注失败和错误的测试用例
- 分析性能测试结果
- 根据测试结果优化代码

## 技术支持

如果遇到测试问题：
1. 检查测试环境是否满足要求
2. 查看测试报告中的详细错误信息
3. 参考常见问题解决方案
4. 联系开发团队获取支持

---

**文档版本**: 1.0  
**最后更新**: 2026年9月17日  
**维护者**: AI Web Tester