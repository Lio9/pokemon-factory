# 批量格式化所有 Java 核心文件
# 使用方法: 在 VSCode 中打开这些文件后按 Ctrl+S 保存

$files = @(
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleEngine.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleAISupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleSetupSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleConditionSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleDamageSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleFlowSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleTurnCleanupSupport.java",
    "pokemon-factory-backend/battle-factory/src/test/java/com/lio9/battle/engine/BattleEngineRegressionBaselineTest.java"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "请按照以下步骤操作：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

foreach ($file in $files) {
    Write-Host "📄 $file" -ForegroundColor Green
}

Write-Host ""
Write-Host "操作步骤：" -ForegroundColor Yellow
Write-Host "1. 在 VSCode 中依次打开上述文件" -ForegroundColor White
Write-Host "2. 对每个文件按 Ctrl+S 保存（会自动格式化）" -ForegroundColor White
Write-Host "3. 或者按 Shift+Alt+F 手动格式化" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
