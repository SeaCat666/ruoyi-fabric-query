$ErrorActionPreference = 'Stop'

$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$taskPath = '\FabricQuery\'
$currentUser = "$env:USERDOMAIN\$env:USERNAME"

$scheduleService = New-Object -ComObject 'Schedule.Service'
$scheduleService.Connect()
try {
    [void]$scheduleService.GetFolder($taskPath)
} catch {
    [void]$scheduleService.GetFolder('\').CreateFolder('FabricQuery')
}

function New-ProjectAction([string]$scriptName) {
    $scriptPath = Join-Path $projectRoot "deploy\windows\$scriptName"
    return New-ScheduledTaskAction -Execute 'powershell.exe' `
        -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$scriptPath`"" `
        -WorkingDirectory $projectRoot
}

$principal = New-ScheduledTaskPrincipal -UserId $currentUser -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -StartWhenAvailable -MultipleInstances IgnoreNew -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit (New-TimeSpan -Hours 72)

$startTrigger = New-ScheduledTaskTrigger -AtLogOn -User $currentUser
$startTask = New-ScheduledTask -Action (New-ProjectAction 'start-public-stack.ps1') `
    -Trigger $startTrigger -Principal $principal -Settings $settings
Register-ScheduledTask -TaskPath $taskPath -TaskName 'StartProduction' `
    -InputObject $startTask -Force | Out-Null

$watchTrigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) `
    -RepetitionInterval (New-TimeSpan -Minutes 5) `
    -RepetitionDuration (New-TimeSpan -Days 3650)
$watchTask = New-ScheduledTask -Action (New-ProjectAction 'watch-public-stack.ps1') `
    -Trigger $watchTrigger -Principal $principal -Settings $settings
Register-ScheduledTask -TaskPath $taskPath -TaskName 'WatchProduction' `
    -InputObject $watchTask -Force | Out-Null

$backupTrigger = New-ScheduledTaskTrigger -Daily -At '02:00'
$backupTask = New-ScheduledTask -Action (New-ProjectAction 'backup-data.ps1') `
    -Trigger $backupTrigger -Principal $principal -Settings $settings
Register-ScheduledTask -TaskPath $taskPath -TaskName 'DailyBackup' `
    -InputObject $backupTask -Force | Out-Null

Write-Host "Scheduled tasks installed for: $currentUser"
Get-ScheduledTask -TaskPath $taskPath | Select-Object TaskName, State
