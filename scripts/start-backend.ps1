Param(
  [string]$Jar = 'pokemon-factory-backend\pokeDex\target\pokeDex-0.0.1-SNAPSHOT.jar'
)

# Determine DB path
if ($env:SQLITE_DB_PATH) {
  $dbPath = $env:SQLITE_DB_PATH
} else {
  $dbPath = Join-Path (Get-Location) 'pokemon-factory-backend\pokemon-factory.db'
}

$dir = Split-Path $dbPath -Parent
if (-not (Test-Path $dir)) {
  Write-Host "Creating directory $dir"
  New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

Write-Host "Using SQLITE DB path: $dbPath"

if (-not (Test-Path $Jar)) {
  Write-Host "JAR not found: $Jar"
  exit 1
}

# Start the backend JAR in detached process
# Pass spring.flyway.mixed=true explicitly to ensure Flyway allows mixed statements for SQLite migrations
Start-Process -FilePath 'java' -ArgumentList "-Dspring.flyway.mixed=true","-jar", $Jar -WorkingDirectory (Get-Location) -NoNewWindow

Write-Host "Started backend jar (detached). Check application logs for Flyway output."