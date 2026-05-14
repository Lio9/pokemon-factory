# Pokemon Factory 数据库自动备份配置指南

## Windows 定时任务配置

### 方法一：使用任务计划程序（推荐）

1. **打开任务计划程序**
   - 按 `Win + R`，输入 `taskschd.msc`，回车

2. **创建基本任务**
   - 点击右侧"创建基本任务"
   - 名称：`Pokemon Factory Database Backup`
   - 描述：`每日凌晨2点自动备份数据库`

3. **设置触发器**
   - 选择"每天"
   - 开始时间：`02:00:00`
   - 每隔：`1` 天

4. **设置操作**
   - 操作：启动程序
   - 程序/脚本：`python`
   - 添加参数：`D:\learn\pokemon-factory\scripts\backup_db.py --db-path D:\learn\pokemon-factory\pokemon-factory-backend\pokemon-factory.db --backup-dir D:\learn\pokemon-factory\backups --retention-days 30`
   - 起始于：`D:\learn\pokemon-factory`

5. **完成配置**
   - 勾选"当单击'完成'时，打开此任务属性的对话框"
   - 在"常规"选项卡中，选择"不管用户是否登录都要运行"
   - 点击"确定"保存

### 方法二：使用 PowerShell 脚本（自动化）

创建文件 `setup_backup_task.ps1`：

```powershell
# 定义任务参数
$taskName = "Pokemon Factory Database Backup"
$scriptPath = "D:\learn\pokemon-factory\scripts\backup_db.py"
$dbPath = "D:\learn\pokemon-factory\pokemon-factory-backend\pokemon-factory.db"
$backupDir = "D:\learn\pokemon-factory\backups"
$retentionDays = 30

# 创建触发器（每天凌晨2点）
$trigger = New-ScheduledTaskTrigger -Daily -At 2am

# 创建动作
$action = New-ScheduledTaskAction -Execute "python" `
    -Argument "$scriptPath --db-path $dbPath --backup-dir $backupDir --retention-days $retentionDays" `
    -WorkingDirectory "D:\learn\pokemon-factory"

# 创建设置
$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable:$false

# 注册任务
Register-ScheduledTask -TaskName $taskName `
    -Trigger $trigger `
    -Action $action `
    -Settings $settings `
    -Description "每日自动备份 Pokemon Factory 数据库" `
    -Force

Write-Host "✅ 定时备份任务已创建: $taskName"
Write-Host "   执行时间: 每天凌晨 2:00"
Write-Host "   备份目录: $backupDir"
Write-Host "   保留天数: $retentionDays 天"
```

运行脚本：
```powershell
# 以管理员身份运行 PowerShell
cd D:\learn\pokemon-factory
.\setup_backup_task.ps1
```

### 方法三：手动测试备份

```powershell
# 立即执行一次备份测试
cd D:\learn\pokemon-factory
python scripts\backup_db.py --db-path pokemon-factory-backend\pokemon-factory.db --backup-dir backups --retention-days 30
```

## Linux/macOS Cron 配置

编辑 crontab：
```bash
crontab -e
```

添加以下行（每天凌晨2点执行）：
```cron
0 2 * * * cd /path/to/pokemon-factory && python scripts/backup_db.py --db-path pokemon-factory-backend/pokemon-factory.db --backup-dir backups --retention-days 30 >> logs/backup.log 2>&1
```

## 备份恢复测试

### 恢复步骤

1. **停止应用服务**
   ```powershell
   # 停止所有后端服务
   ```

2. **备份当前数据库**（以防万一）
   ```powershell
   Copy-Item pokemon-factory.db pokemon-factory.db.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')
   ```

3. **恢复备份**
   ```powershell
   # 假设要恢复到最新的备份
   $latestBackup = Get-ChildItem backups\pokemon-factory_*.db | Sort-Object LastWriteTime -Descending | Select-Object -First 1
   Copy-Item $latestBackup.FullName pokemon-factory.db -Force
   ```

4. **验证恢复**
   ```powershell
   python scripts\verify_sqlite.py
   ```

5. **重启应用服务**

## 监控备份状态

### 检查最近的备份

```powershell
# 列出最近10个备份文件
Get-ChildItem backups\pokemon-factory_*.db | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 10 | 
    Format-Table Name, Length, LastWriteTime
```

### 备份健康检查脚本

创建 `check_backup_health.ps1`：

```powershell
$backupDir = "D:\learn\pokemon-factory\backups"
$maxAgeHours = 26  # 超过26小时没有备份视为异常

if (-not (Test-Path $backupDir)) {
    Write-Host "❌ 备份目录不存在" -ForegroundColor Red
    exit 1
}

$latestBackup = Get-ChildItem "$backupDir\pokemon-factory_*.db" | 
    Sort-Object LastWriteTime -Descending | 
    Select-Object -First 1

if (-not $latestBackup) {
    Write-Host "❌ 未找到任何备份文件" -ForegroundColor Red
    exit 1
}

$age = (Get-Date) - $latestBackup.LastWriteTime
$ageHours = [math]::Round($age.TotalHours, 2)

if ($ageHours -gt $maxAgeHours) {
    Write-Host "⚠️  警告: 最新备份已超过 ${ageHours} 小时" -ForegroundColor Yellow
    Write-Host "   文件: $($latestBackup.Name)" -ForegroundColor Yellow
    Write-Host "   时间: $($latestBackup.LastWriteTime)" -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "✅ 备份状态正常" -ForegroundColor Green
    Write-Host "   最新备份: $($latestBackup.Name)" -ForegroundColor Green
    Write-Host "   备份时间: $($latestBackup.LastWriteTime)" -ForegroundColor Green
    Write-Host "   距今: ${ageHours} 小时" -ForegroundColor Green
    
    # 显示备份统计
    $allBackups = Get-ChildItem "$backupDir\pokemon-factory_*.db"
    $totalSize = ($allBackups | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "   备份总数: $($allBackups.Count)" -ForegroundColor Cyan
    Write-Host "   总大小: $([math]::Round($totalSize, 2)) MB" -ForegroundColor Cyan
    exit 0
}
```

## 云存储备份（可选）

### AWS S3 配置

1. **安装 AWS CLI**
   ```powershell
   pip install awscli
   aws configure
   ```

2. **修改 backup_db.py**，在末尾添加：
   ```python
   def upload_to_s3(backup_path: Path, bucket_name: str):
       """上传备份到 S3"""
       import subprocess
       
       s3_key = f"backups/{backup_path.name}"
       cmd = [
           "aws", "s3", "cp",
           str(backup_path),
           f"s3://{bucket_name}/{s3_key}"
       ]
       
       print(f"☁️  上传到 S3: {s3_key}")
       result = subprocess.run(cmd, capture_output=True, text=True)
       
       if result.returncode == 0:
           print(f"✅ 上传成功")
       else:
           print(f"❌ 上传失败: {result.stderr}")
   
   # 在 main() 函数最后调用
   # upload_to_s3(backup_path, "your-bucket-name")
   ```

### 阿里云 OSS 配置

使用 `ossutil` 工具：
```bash
# 安装 ossutil
# 配置 credentials
ossutil config

# 上传备份
ossutil cp backups/pokemon-factory_20260514_020000.db oss://your-bucket/backups/
```

## 故障排除

### 问题1：备份时数据库被锁定

**解决方案：**
- 确保使用 SQLite 在线备份 API（脚本已实现）
- 避免在备份高峰期进行大量写入操作

### 问题2：备份文件过大

**解决方案：**
1. 启用 WAL 模式压缩：
   ```sql
   PRAGMA wal_checkpoint(TRUNCATE);
   ```

2. 定期清理无用数据

3. 考虑增量备份方案

### 问题3：定时任务未执行

**检查清单：**
- [ ] 任务计划程序中任务状态是否为"就绪"
- [ ] Python 是否在系统 PATH 中
- [ ] 脚本路径是否正确
- [ ] 是否有足够的权限

查看任务历史：
```powershell
Get-ScheduledTaskInfo -TaskName "Pokemon Factory Database Backup"
```

## 最佳实践

1. **3-2-1 备份原则**
   - 至少 3 份备份
   - 2 种不同介质（本地 + 云端）
   - 1 份异地备份

2. **定期恢复演练**
   - 每月至少进行一次恢复测试
   - 验证备份完整性

3. **监控告警**
   - 设置备份失败告警
   - 监控备份文件大小变化

4. **文档更新**
   - 记录每次重大变更
   - 保持恢复流程文档最新

---

**最后更新**: 2026-05-14  
**维护者**: Pokemon Factory Team
