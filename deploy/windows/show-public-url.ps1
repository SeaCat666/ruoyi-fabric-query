$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$urlFile = Join-Path $projectRoot 'runtime\quick-tunnel-url.txt'

if (-not (Test-Path -LiteralPath $urlFile)) {
    throw 'The public URL is not ready. Sign in to Windows and wait about one minute.'
}

$url = Get-Content -LiteralPath $urlFile | Select-Object -First 1
Set-Clipboard -Value $url
$status = & curl.exe -sS -o NUL -w '%{http_code}' --max-time 20 "$url/"
Write-Host "Current public URL: $url"
Write-Host 'The URL has been copied to the clipboard.'
if ($status -eq '200') {
    Write-Host 'Status: healthy (HTTP 200)'
} else {
    Write-Warning "Status: unhealthy (HTTP $status). Wait one minute and try again."
}
Write-Host 'The temporary URL changes after a reboot or tunnel restart. The desktop shortcut is updated automatically.'
