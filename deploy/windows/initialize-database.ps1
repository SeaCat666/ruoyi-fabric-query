param(
    [string]$DatabasePath
)

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
if ([string]::IsNullOrWhiteSpace($DatabasePath)) {
    $DatabasePath = Join-Path $projectRoot 'ruoyi-admin\data\fabric'
}
$DatabasePath = [IO.Path]::GetFullPath($DatabasePath)
$databaseFile = "$DatabasePath.mv.db"
$schemaFile = Join-Path $projectRoot 'ruoyi-admin\src\main\resources\sql\init-h2.sql'
$h2Jar = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.m2\repository\com\h2database\h2') `
    -Filter 'h2-*.jar' -Recurse -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (Test-Path -LiteralPath $databaseFile) {
    Write-Host "Database already exists: $databaseFile"
    exit 0
}
if ($null -eq $h2Jar) { throw 'H2 driver was not found. Run Maven build first.' }
if (-not (Test-Path -LiteralPath $schemaFile)) { throw "Schema file is missing: $schemaFile" }

$databaseDir = Split-Path -Parent $DatabasePath
New-Item -ItemType Directory -Force -Path $databaseDir | Out-Null
$databaseUrlPath = $DatabasePath.Replace('\', '/')
$schemaUrlPath = $schemaFile.Replace('\', '/')
$runScriptSql = "RUNSCRIPT FROM '$schemaUrlPath' CHARSET 'UTF-8'"

& java -cp $h2Jar.FullName org.h2.tools.Shell `
    -url "jdbc:h2:file:$databaseUrlPath;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE" `
    -user root -password password -sql $runScriptSql
if ($LASTEXITCODE -ne 0) { throw 'H2 database initialization failed.' }

Write-Host "Database initialized: $databaseFile"
