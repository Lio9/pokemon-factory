# 定时备份任务安装指南

**最后更新**: 2026-05-14  
**版本**: v1.0  
**状态**: ✅ 备份脚本已验证通过，待安装定时任务

---

## 📋 前置条件检查

### ✅ 已完成
- [x] 备份脚本存在: `scripts/backup_db.py`
- [x] Python环境可用
- [x] 备份目录已创建: `backups/`
- [x] 手动备份测试成功（2026-05-14 17:14）

### ⏳ 待执行
- [ ] 以管理员身份运行PowerShell
- [ ] 执行定时任务安装脚本

---

## 🚀 快速安装（推荐）

### 步骤1: 以管理员身份打开PowerShell

1. 按 `Win + X` 键
2. 选择 **"Windows PowerShell (管理员)"** 或 **"终端(管理员)"**
3. 点击"是"确认UAC提示

### 步骤2: 执行安装脚本

```powershell
cd D:\learn\pokemon-factory
.\scripts\setup_backup_task.ps1
```

### 步骤3: 验证安装

```powershell
# 查看任务信息
Get-ScheduledTaskInfo -TaskName "Pokemon Factory Database Backup"

# 立即测试一次备份
Start-ScheduledTask -TaskName "Pokemon Factory Database Backup"
```

---

## 📖 详细说明

### 备份配置

| 参数 | 值 | 说明 |
|------|-----|------|
| **任务名称** | Pokemon Factory Database Backup | Windows任务计划程序中的名称 |
| **执行时间** | 每天凌晨 2:00 | 可修改为其他时间 |
| **数据库路径** | `D:\learn\pokemon-factory\pokemon-factory-backend\pokemon-factory.db` | SQLite数据库文件 |
| **备份目录** | `D:\learn\pokemon-factory\backups` | 备份文件存储位置 |
| **保留天数** | 30天 | 自动删除30天前的旧备份 |
| **重试次数** | 3次 | 失败后每5分钟重试一次 |

### 备份文件命名规则

```
pokemon-factory_YYYYMMDD_HHMMSS.db
```

**示例**:
- `pokemon-factory_20260514_171405.db` (2026年5月14日 17:14:05)
- `pokemon-factory_20260515_020000.db` (2026年5月15日 02:00:00)

---

## 🔧 自定义配置

### 修改备份时间

如果要改为每天凌晨3点备份：

```powershell
# 1. 删除现有任务
Unregister-ScheduledTask -TaskName "Pokemon Factory Database Backup" -Confirm:$false

# 2. 重新创建（修改 -At 参数）
$action = New-ScheduledTaskAction -Execute "python" `
    -Argument "D:\learn\pokemon-factory\scripts\backup_db.py --db-path `"D:\learn\pokemon-factory\pokemon-factory-backend\pokemon-factory.db`" --backup-dir `"D:\learn\pokemon-factory\backups`" --retention-days 30" `
    -WorkingDirectory "D:\learn\pokemon-factory"

$trigger = New-ScheduledTaskTrigger -Daily -At 3am

Register-ScheduledTask `
    -TaskName "Pokemon Factory Database Backup" `
    -Trigger $trigger `
    -Action $action `
    -Description "每日自动备份 Pokemon Factory 数据库" `
    -Force
```

### 修改保留天数

编辑 `scripts/setup_backup_task.ps1` 第24行：

```powershell
$retentionDays = 30  # 改为 60、90 等
```

---

## 📊 监控和维护

### 查看备份历史

```powershell
# 列出所有备份文件
ls D:\learn\pokemon-factory\backups\*.db | Sort-Object LastWriteTime -Descending

# 查看备份总大小
(ls D:\learn\pokemon-factory\backups\*.db | Measure-Object -Property Length -Sum).Sum / 1MB
```

### 查看任务执行日志

```powershell
# 查看最近一次执行结果
Get-ScheduledTaskInfo -TaskName "Pokemon Factory Database Backup" | Select-Object LastRunTime, LastTaskResult, NextRunTime

# 查看完整历史记录
Get-WinEvent -LogName Microsoft-Windows-TaskScheduler/Operational | Where-Object {$_.Message -like "*Pokemon Factory*"} | Select-Object TimeCreated, Message -First 10
```

### 手动触发备份

```powershell
Start-ScheduledTask -TaskName "Pokemon Factory Database Backup"
```

### 暂停/恢复任务

```powershell
# 暂停
Disable-ScheduledTask -TaskName "Pokemon Factory Database Backup"

# 恢复
Enable-ScheduledTask -TaskName "Pokemon Factory Database Backup"
```

### 删除任务

```powershell
Unregister-ScheduledTask -TaskName "Pokemon Factory Database Backup" -Confirm:$false
```

---

## ❓ 常见问题

### Q1: 为什么需要管理员权限？

**A**: Windows任务计划程序的API需要管理员权限才能创建和修改定时任务。这是Windows的安全机制。

### Q2: 如果忘记以管理员身份运行会怎样？

**A**: 脚本会检测到并显示错误提示：
```
❌ 错误: 此脚本需要管理员权限
   请右键点击 PowerShell，选择'以管理员身份运行'
```

### Q3: 备份会影响数据库性能吗？

**A**: 不会。SQLite的备份是通过文件复制实现的，不会影响正在运行的应用。备份过程通常在几秒内完成。

### Q4: 如何验证备份文件是否有效？

**A**: 备份脚本会自动验证完整性（PRAGMA integrity_check）。你也可以手动验证：

```powershell
python scripts\verify_sqlite.py D:\learn\pokemon-factory\backups\pokemon-factory_20260514_171405.db
```

### Q5: 备份文件占用多少空间？

**A**: 当前数据库大小约3MB，每次备份约3MB。保留30天的话，最多占用约90MB空间。

---

## 🎯 验收标准

安装完成后，请验证以下条件：

- [ ] 任务已成功注册到Windows任务计划程序
- [ ] 下次运行时间显示正确（明天凌晨2:00）
- [ ] 手动触发备份成功执行
- [ ] 备份目录中生成了新的备份文件
- [ ] 备份文件大小 > 0 且完整性验证通过

---

## 📞 技术支持

如果遇到问题，请检查：

1. **Python是否正确安装**
   ```powershell
   python --version
   ```

2. **备份脚本是否存在**
   ```powershell
   Test-Path D:\learn\pokemon-factory\scripts\backup_db.py
   ```

3. **数据库文件是否可访问**
   ```powershell
   Test-Path D:\learn\pokemon-factory\pokemon-factory-backend\pokemon-factory.db
   ```

4. **查看任务计划程序GUI**
   - 按 `Win + R`
   - 输入 `taskschd.msc`
   - 查找 "Pokemon Factory Database Backup" 任务

---

## 📝 相关文档

- [TODO.md](./TODO.md) - 项目待办事项清单
- [OPTIMIZATION_SUMMARY.md](./OPTIMIZATION_SUMMARY.md) - v2.0优化总结
- [optimization_roadmap.md](./optimization_roadmap.md) - 项目路线图

---

**预计安装时间**: 5-10分钟  
**难度级别**: ⭐ (简单)  
**风险等级**: 🟢 (低风险)
