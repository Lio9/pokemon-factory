# start-backend 文件说明
# 所属模块：项目脚本目录。
# 文件类型：PowerShell 脚本文件。
# 核心职责：负责 Windows 环境下的启动或运维辅助流程。
# 阅读建议：建议执行前确认当前目录与依赖环境变量。
# 项目注释补全说明：本注释用于帮助后续维护时快速定位文件在整体架构中的职责。

Param(
  [string]$Jar,
  [string]$Db,
  [string]$Java = 'java'
)

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$pythonArgs = @()

if (-not $pythonCommand) {
  $pythonCommand = Get-Command py -ErrorAction SilentlyContinue
  if ($pythonCommand) {
    $pythonArgs += '-3'
  }
}

if (-not $pythonCommand) {
  Write-Host 'Python was not found. Please install Python 3 or run scripts/start-backend.py manually.'
  exit 1
}

$pythonArgs += (Join-Path $scriptRoot 'start-backend.py')

if ($Jar) {
  $pythonArgs += '--jar'
  $pythonArgs += $Jar
}

if ($Db) {
  $pythonArgs += '--db'
  $pythonArgs += $Db
}

if ($Java) {
  $pythonArgs += '--java'
  $pythonArgs += $Java
}

& $pythonCommand.Source @pythonArgs
exit $LASTEXITCODE
