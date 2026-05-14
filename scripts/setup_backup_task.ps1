# Pokemon Factory 数据库备份定时任务安装脚本
# 需要以管理员身份运行 PowerShell

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Pokemon Factory 数据库备份定时任务安装" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "❌ 错误: 此脚本需要管理员权限" -ForegroundColor Red
    Write-Host "   请右键点击 PowerShell，选择'以管理员身份运行'" -ForegroundColor Yellow
    exit 1
}

# 配置参数
$taskName = "Pokemon Factory Database Backup"
$projectRoot = "D:\learn\pokemon-factory"
$scriptPath = Join-Path $projectRoot "scripts\backup_db.py"
$dbPath = Join-Path $projectRoot "pokemon-factory-backend\pokemon-factory.db"
$backupDir = Join-Path $projectRoot "backups"
$retentionDays = 30

# 检查 Python 是否可用
try {
    $pythonVersion = python --version 2>&1
    Write-Host "✅ Python 已安装: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ 错误: 未找到 Python，请先安装 Python 3.8+" -ForegroundColor Red
    exit 1
}

# 检查脚本是否存在
if (-not (Test-Path $scriptPath)) {
    Write-Host "❌ 错误: 备份脚本不存在: $scriptPath" -ForegroundColor Red
    exit 1
}

# 创建备份目录
if (-not (Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
    Write-Host "✅ 已创建备份目录: $backupDir" -ForegroundColor Green
}

# 删除已存在的同名任务（如果有）
$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existingTask) {
    Write-Host "⚠️  检测到已存在的任务，正在删除..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "✅ 旧任务已删除" -ForegroundColor Green
}

# 创建触发器（每天凌晨2点）
$trigger = New-ScheduledTaskTrigger -Daily -At 2am

# 创建动作
$action = New-ScheduledTaskAction `
    -Execute "python" `
    -Argument "$scriptPath --db-path `"$dbPath`" --backup-dir `"$backupDir`" --retention-days $retentionDays" `
    -WorkingDirectory $projectRoot

# 创建设置
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false `
    -ExecutionTimeLimit (New-TimeSpan -Hours 1) `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 5)

# 注册任务
try {
    Register-ScheduledTask `
        -TaskName $taskName `
        -Trigger $trigger `
        -Action $action `
        -Settings $settings `
        -Description "每日自动备份 Pokemon Factory 数据库到 $backupDir，保留最近 $retentionDays 天" `
        -Force | Out-Null
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ 定时备份任务已成功创建！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "任务详情:" -ForegroundColor Cyan
    Write-Host "  任务名称: $taskName"
    Write-Host "  执行时间: 每天凌晨 2:00"
    Write-Host "  备份目录: $backupDir"
    Write-Host "  保留天数: $retentionDays 天"
    Write-Host "  数据库路径: $dbPath"
    Write-Host ""
    Write-Host "常用命令:" -ForegroundColor Cyan
    Write-Host "  查看任务状态: Get-ScheduledTaskInfo -TaskName '$taskName'"
    Write-Host "  立即执行备份: Start-ScheduledTask -TaskName '$taskName'"
    Write-Host "  删除任务: Unregister-ScheduledTask -TaskName '$taskName' -Confirm:`$false"
    Write-Host ""
    
    # 询问是否立即测试一次备份
    $runNow = Read-Host "是否立即执行一次备份测试？(Y/N)"
    if ($runNow -eq 'Y' -or $runNow -eq 'y') {
        Write-Host ""
        Write-Host "🔄 开始执行备份测试..." -ForegroundColor Yellow
        Write-Host ""
        
        python $scriptPath --db-path $dbPath --backup-dir $backupDir --retention-days $retentionDays
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ 备份测试成功！" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "❌ 备份测试失败，请检查错误信息" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ 创建任务失败: $_" -ForegroundColor Red
    exit 1
}
