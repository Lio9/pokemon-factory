# 批量修复行尾空格和换行符问题
# 使用方法: 在 PowerShell 中运行 .\fix_whitespace.ps1

$ErrorActionPreference = "Continue"

# 定义需要修复的文件列表
$files = @(
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleAISupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleConditionSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleDamageSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleFlowSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/engine/BattleTurnCleanupSupport.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/service/AIService.java",
    "pokemon-factory-backend/battle-factory/src/main/java/com/lio9/battle/service/OpponentPoolService.java",
    "pokemon-factory-frontend/src/components/BattleArena.vue",
    "pokemon-factory-frontend/src/composables/useLocale.js"
)

$fixedCount = 0
$errorCount = 0

foreach ($file in $files) {
    if (Test-Path $file) {
        try {
            # 读取文件内容
            $content = Get-Content $file -Raw -Encoding UTF8
            
            # 修复行尾空格（移除每行末尾的空格和制表符）
            $content = $content -replace '[ \t]+(\r?\n)', '$1'
            
            # 统一换行符为 LF
            $content = $content -replace '\r\n', "`n"
            
            # 确保文件末尾有换行符
            if (-not $content.EndsWith("`n")) {
                $content += "`n"
            }
            
            # 写回文件
            Set-Content $file -Value $content -Encoding UTF8 -NoNewline
            
            Write-Host "✅ Fixed: $file" -ForegroundColor Green
            $fixedCount++
        }
        catch {
            Write-Host "❌ Error fixing $file : $_" -ForegroundColor Red
            $errorCount++
        }
    }
    else {
        Write-Host "⚠️  File not found: $file" -ForegroundColor Yellow
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "修复完成！" -ForegroundColor Cyan
Write-Host "成功修复: $fixedCount 个文件" -ForegroundColor Green
Write-Host "失败: $errorCount 个文件" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "========================================" -ForegroundColor Cyan
