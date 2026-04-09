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
