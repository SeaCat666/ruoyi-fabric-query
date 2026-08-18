$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$toolsDir = Join-Path $projectRoot 'tools'
$tempDir = Join-Path ([IO.Path]::GetTempPath()) ("fabric-tools-" + [guid]::NewGuid().ToString('N'))
$caddyZip = Join-Path $tempDir 'caddy.zip'
$cloudflaredFile = Join-Path $toolsDir 'cloudflared.exe'

New-Item -ItemType Directory -Force -Path $toolsDir, $tempDir | Out-Null
try {
    Invoke-WebRequest `
        -Uri 'https://github.com/caddyserver/caddy/releases/download/v2.11.4/caddy_2.11.4_windows_amd64.zip' `
        -OutFile $caddyZip
    Expand-Archive -LiteralPath $caddyZip -DestinationPath $tempDir -Force
    Copy-Item -LiteralPath (Join-Path $tempDir 'caddy.exe') -Destination (Join-Path $toolsDir 'caddy.exe') -Force

    Invoke-WebRequest `
        -Uri 'https://github.com/cloudflare/cloudflared/releases/download/2026.7.3/cloudflared-windows-amd64.exe' `
        -OutFile $cloudflaredFile
} finally {
    $resolvedTemp = [IO.Path]::GetFullPath($tempDir)
    $systemTemp = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
    if ($resolvedTemp.StartsWith($systemTemp, [StringComparison]::OrdinalIgnoreCase) -and
        (Test-Path -LiteralPath $resolvedTemp)) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
}

& (Join-Path $toolsDir 'caddy.exe') version
& $cloudflaredFile --version
Write-Host "Runtime tools installed in: $toolsDir"
