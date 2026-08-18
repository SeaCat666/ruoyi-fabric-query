param(
    [switch]$DatabaseOnly
)

$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$backupRoot = Join-Path $projectRoot 'backups'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$dayDir = Join-Path $backupRoot $stamp
$h2Jar = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.m2\repository\com\h2database\h2') -Filter 'h2-*.jar' -Recurse |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1

if ($null -eq $h2Jar) { throw 'H2 driver was not found; backup cannot continue.' }
New-Item -ItemType Directory -Force -Path $dayDir | Out-Null

$databaseZip = (Join-Path $dayDir 'fabric-database.zip').Replace('\', '/')
$sql = "BACKUP TO '$databaseZip'"
$databasePath = (Join-Path $projectRoot 'ruoyi-admin\data\fabric').Replace('\', '/')
& java -cp $h2Jar.FullName org.h2.tools.Shell `
    -url "jdbc:h2:file:$databasePath;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE" `
    -user root -password password -sql $sql
if ($LASTEXITCODE -ne 0) { throw 'H2 database backup failed.' }

$uploadPath = [Environment]::GetEnvironmentVariable('FABRIC_UPLOAD_PATH', 'User')
if ([string]::IsNullOrWhiteSpace($uploadPath)) { $uploadPath = 'D:\ruoyi\uploadPath' }
if (-not $DatabaseOnly -and (Test-Path -LiteralPath $uploadPath)) {
    $uploadBackup = Join-Path $backupRoot 'uploads-current'
    New-Item -ItemType Directory -Force -Path $uploadBackup | Out-Null
    & robocopy $uploadPath $uploadBackup /MIR /COPY:DAT /DCOPY:DAT /FFT /R:2 /W:2 /NP /NFL /NDL
    if ($LASTEXITCODE -gt 7) { throw "Upload mirror failed with robocopy exit code $LASTEXITCODE." }
}

Get-ChildItem -LiteralPath $backupRoot -Directory |
    Where-Object { $_.Name -ne 'uploads-current' } |
    Where-Object { $_.CreationTime -lt (Get-Date).AddDays(-30) } |
    ForEach-Object {
        $resolved = [IO.Path]::GetFullPath($_.FullName)
        if (-not $resolved.StartsWith([IO.Path]::GetFullPath($backupRoot), [StringComparison]::OrdinalIgnoreCase)) {
            throw "Backup cleanup path is outside the backup root: $resolved"
        }
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }

Write-Host "Backup completed: $dayDir"
