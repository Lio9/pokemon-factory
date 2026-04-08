Param(
  [string]$Jar = 'pokemon-factory-backend\common\target\common-0.0.1-SNAPSHOT.jar'
)

# 解析 SQLite 文件路径：如果外部已经指定环境变量，则优先复用该路径。
if ($env:SQLITE_DB_PATH) {
  $dbPath = $env:SQLITE_DB_PATH
} else {
  $dbPath = Join-Path (Get-Location) 'pokemon-factory-backend\pokemon-factory.db'
}

# 确保数据库所在目录存在，避免 common 首次启动时因为目录缺失无法创建数据库文件。
$dir = Split-Path $dbPath -Parent
if (-not (Test-Path $dir)) {
  Write-Host "Creating directory $dir"
  New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

Write-Host "Using SQLITE DB path: $dbPath"

# 如果 common 的可执行 JAR 尚未构建出来，这里直接失败，避免用户误以为已经完成初始化。
if (-not (Test-Path $Jar)) {
  Write-Host "JAR not found: $Jar"
  exit 1
}

# 启动 common JAR，由 common 统一完成数据库初始化。
# 这里不再直接启动 pokeDex/battleFactory，因为数据库职责已经全部收敛到 common。
Start-Process -FilePath 'java' -ArgumentList "-jar", $Jar -WorkingDirectory (Get-Location) -NoNewWindow

Write-Host "Started common jar (detached). Check application logs for database initialization output."
