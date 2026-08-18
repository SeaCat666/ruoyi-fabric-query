$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$runtimeDir = Join-Path $projectRoot 'runtime'
$watchLog = Join-Path $runtimeDir 'logs\watchdog.log'
$components = @(
    @{ Name = 'backend'; Expected = 'ruoyi-admin.jar' },
    @{ Name = 'caddy'; Expected = 'caddy.exe' },
    @{ Name = 'cloudflared'; Expected = 'cloudflared.exe' }
)

$healthy = $true
foreach ($component in $components) {
    $pidFile = Join-Path $runtimeDir "$($component.Name).pid"
    if (-not (Test-Path -LiteralPath $pidFile)) {
        $healthy = $false
        continue
    }

    $targetPid = [int](Get-Content -LiteralPath $pidFile | Select-Object -First 1)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$targetPid" -ErrorAction SilentlyContinue
    if ($null -eq $process -or $process.CommandLine -notlike "*$($component.Expected)*") {
        # The PID is stale or has been reused. Never stop an unrelated process.
        Remove-Item -LiteralPath $pidFile -Force
        $healthy = $false
    }
}

if ($healthy) {
    $backendPort = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalAddress -eq '127.0.0.1' } | Select-Object -First 1
    $webPort = Get-NetTCPConnection -State Listen -LocalPort 8088 -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalAddress -eq '127.0.0.1' } | Select-Object -First 1
    $healthy = $null -ne $backendPort -and $null -ne $webPort
}

if ($healthy) { exit 0 }

"$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') A component was unavailable; restarting the public stack." |
    Add-Content -LiteralPath $watchLog -Encoding UTF8

try {
    & (Join-Path $projectRoot 'deploy\windows\stop-public-stack.ps1')
} catch {
    "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') Stop warning: $($_.Exception.Message)" |
        Add-Content -LiteralPath $watchLog -Encoding UTF8
}

& (Join-Path $projectRoot 'deploy\windows\start-public-stack.ps1')
