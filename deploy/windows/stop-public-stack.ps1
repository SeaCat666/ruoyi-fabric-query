$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$runtimeDir = Join-Path $projectRoot 'runtime'

foreach ($name in @('cloudflared', 'caddy', 'backend')) {
    $pidFile = Join-Path $runtimeDir "$name.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $targetPid = [int](Get-Content -LiteralPath $pidFile | Select-Object -First 1)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$targetPid" -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        $expected = if ($name -eq 'backend') { 'ruoyi-admin.jar' } else { "$name.exe" }
        if ($process.CommandLine -notlike "*$expected*") {
            throw "PID $targetPid does not match $name; refusing to stop it."
        }
        Stop-Process -Id $targetPid -Force
        Write-Host "Stopped $name (PID $targetPid)."
    }
    Remove-Item -LiteralPath $pidFile -Force
}
