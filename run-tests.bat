@echo off
echo ========================================
echo 宝可梦工厂项目 - AI Web Tester 测试
echo ========================================
echo.

echo 1. 运行基本功能测试...
node test-basic.js
echo.

echo 2. 运行简单前后端配合测试...
node test-simple.js
echo.

echo 3. 运行Playwright测试...
node test-playwright.js
echo.

echo ========================================
echo 测试完成！
echo ========================================
pause