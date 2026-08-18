$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$runtimeDir = Join-Path $projectRoot 'runtime'
$logDir = Join-Path $runtimeDir 'logs'
$backendJar = Join-Path $projectRoot 'ruoyi-admin\target\ruoyi-admin.jar'
$frontendIndex = Join-Path $projectRoot 'ruoyi-ui\dist\index.html'
$caddyExe = Join-Path $projectRoot 'tools\caddy.exe'
$cloudflaredExe = Join-Path $projectRoot 'tools\cloudflared.exe'
$caddyConfig = Join-Path $projectRoot 'deploy\windows\Caddyfile'
$tunnelConfig = Join-Path $runtimeDir 'cloudflared-config.yml'

New-Item -ItemType Directory -Force -Path $runtimeDir, $logDir | Out-Null

if (-not (Test-Path -LiteralPath $backendJar)) { throw "Backend JAR is missing: $backendJar" }
if (-not (Test-Path -LiteralPath $frontendIndex)) { throw "Frontend build is missing: $frontendIndex" }
if (-not (Test-Path -LiteralPath $caddyExe)) { throw "Caddy is missing: $caddyExe" }

$tokenSecret = [Environment]::GetEnvironmentVariable('FABRIC_TOKEN_SECRET', 'User')
if ([string]::IsNullOrWhiteSpace($tokenSecret) -or $tokenSecret.Length -lt 48) {
    throw 'FABRIC_TOKEN_SECRET is missing or shorter than 48 characters.'
}
$quickTunnelEnabled = [Environment]::GetEnvironmentVariable('FABRIC_QUICK_TUNNEL_ENABLED', 'User') -eq 'true'
$env:FABRIC_TOKEN_SECRET = $tokenSecret
$env:FABRIC_PROJECT_ROOT = $projectRoot.Replace('\', '/')
$env:FABRIC_DB_PATH = (Join-Path $projectRoot 'ruoyi-admin\data\fabric').Replace('\', '/')

$databaseFile = Join-Path $projectRoot 'ruoyi-admin\data\fabric.mv.db'
if (-not (Test-Path -LiteralPath $databaseFile)) {
    & (Join-Path $PSScriptRoot 'initialize-database.ps1')
}

function Test-Port([int]$port) {
    return $null -ne (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Update-PublicAccessFiles([string]$publicUrl) {
    if ([string]::IsNullOrWhiteSpace($publicUrl)) { return }

    $desktopDir = [Environment]::GetFolderPath('Desktop')
    if ([string]::IsNullOrWhiteSpace($desktopDir) -or -not (Test-Path -LiteralPath $desktopDir)) { return }

    $shortcutName = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('6Z2i5paZ5p+l6K+i57O757uf77yI5YWs572R6K6/6Zeu77yJLnVybA=='))
    $addressName = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String('6Z2i5paZ5p+l6K+i57O757uf5YWs572R5Zyw5Z2ALnR4dA=='))
    $shortcutPath = Join-Path $desktopDir $shortcutName
    $addressPath = Join-Path $desktopDir $addressName
    @(
        '[InternetShortcut]'
        "URL=$publicUrl"
    ) | Set-Content -LiteralPath $shortcutPath -Encoding ASCII
    @(
        'Fabric Query System public URL:'
        $publicUrl
        ''
        'The temporary URL changes after a reboot or tunnel restart. The desktop shortcut is updated automatically.'
    ) | Set-Content -LiteralPath $addressPath -Encoding UTF8
}

if (Test-Port 8080) { throw 'Port 8080 is already in use. Stop the development backend first.' }
if (Test-Port 8088) { throw 'Port 8088 is already in use. Stop the existing public frontend first.' }

$javaExe = (Get-Command java.exe -ErrorAction Stop).Source
$backend = Start-Process -FilePath $javaExe `
    -ArgumentList @('-jar', $backendJar, '--spring.profiles.active=druid,public') `
    -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $logDir 'backend.out.log') `
    -RedirectStandardError (Join-Path $logDir 'backend.err.log')
$backend.Id | Set-Content -LiteralPath (Join-Path $runtimeDir 'backend.pid')

$backendReady = $false
for ($i = 0; $i -lt 90; $i++) {
    if ($backend.HasExited) { throw 'Backend failed to start. Check runtime\logs\backend.err.log.' }
    if (Test-Port 8080) { $backendReady = $true; break }
    Start-Sleep -Seconds 1
}
if (-not $backendReady) { throw 'Backend was not ready within 90 seconds.' }

$caddy = Start-Process -FilePath $caddyExe `
    -ArgumentList @('run', '--config', $caddyConfig, '--adapter', 'caddyfile') `
    -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $logDir 'caddy.out.log') `
    -RedirectStandardError (Join-Path $logDir 'caddy.err.log')
$caddy.Id | Set-Content -LiteralPath (Join-Path $runtimeDir 'caddy.pid')

for ($i = 0; $i -lt 20; $i++) {
    if ($caddy.HasExited) { throw 'Caddy failed to start. Check runtime\logs\caddy.err.log.' }
    if (Test-Port 8088) { break }
    Start-Sleep -Seconds 1
}
if (-not (Test-Port 8088)) { throw 'Caddy was not ready within 20 seconds.' }

$healthStatus = & curl.exe -s -o NUL -w '%{http_code}' 'http://127.0.0.1:8088/'
if ($healthStatus -ne '200') { throw "Local production health check failed: HTTP $healthStatus" }

# cloudflared must connect to the loopback origin directly. Some desktop proxy
# clients expose HTTP_PROXY globally and otherwise turn the proxy handshake into
# an empty HTTP 200 response instead of forwarding the request to Caddy.
foreach ($proxyVariable in @('HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY', 'http_proxy', 'https_proxy', 'all_proxy')) {
    Remove-Item -LiteralPath "Env:$proxyVariable" -ErrorAction SilentlyContinue
}

if (Test-Path -LiteralPath $tunnelConfig) {
    if (-not (Test-Path -LiteralPath $cloudflaredExe)) { throw "cloudflared is missing: $cloudflaredExe" }
    $tunnel = Start-Process -FilePath $cloudflaredExe `
        -ArgumentList @('tunnel', '--config', $tunnelConfig, 'run') `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir 'cloudflared.out.log') `
        -RedirectStandardError (Join-Path $logDir 'cloudflared.err.log')
    $tunnel.Id | Set-Content -LiteralPath (Join-Path $runtimeDir 'cloudflared.pid')
    Write-Host 'Production stack and named Cloudflare Tunnel started.'
} elseif ($quickTunnelEnabled) {
    if (-not (Test-Path -LiteralPath $cloudflaredExe)) { throw "cloudflared is missing: $cloudflaredExe" }
    $quickOutLog = Join-Path $logDir 'cloudflared-quick.out.log'
    $quickErrLog = Join-Path $logDir 'cloudflared-quick.err.log'
    Remove-Item -LiteralPath $quickOutLog, $quickErrLog -Force -ErrorAction SilentlyContinue
    $tunnel = Start-Process -FilePath $cloudflaredExe `
        -ArgumentList @('tunnel', '--no-autoupdate', '--protocol', 'http2', '--url', 'http://127.0.0.1:8088') `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $quickOutLog `
        -RedirectStandardError $quickErrLog
    $tunnel.Id | Set-Content -LiteralPath (Join-Path $runtimeDir 'cloudflared.pid')

    $quickUrl = $null
    for ($i = 0; $i -lt 60; $i++) {
        if ($tunnel.HasExited) { throw 'Quick Tunnel failed to start. Check runtime\logs\cloudflared-quick.err.log.' }
        $logText = ((Get-Content -LiteralPath $quickOutLog, $quickErrLog -Raw -ErrorAction SilentlyContinue) -join "`n")
        $match = [regex]::Match($logText, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($match.Success) { $quickUrl = $match.Value; break }
        Start-Sleep -Seconds 1
    }
    if ([string]::IsNullOrWhiteSpace($quickUrl)) { throw 'Quick Tunnel URL was not available within 60 seconds.' }
    $quickUrl | Set-Content -LiteralPath (Join-Path $runtimeDir 'quick-tunnel-url.txt')
    Update-PublicAccessFiles $quickUrl
    Write-Host "Production stack and temporary Tunnel started: $quickUrl"
} else {
    Write-Host 'Production stack started at http://127.0.0.1:8088; tunnel config is not present yet.'
}
