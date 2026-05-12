# AgentShell Build & Test Automation Script
# Optimized PowerShell script for efficient device selection and deployment

$ApkPath = "app\build\outputs\apk\debug\app-debug.apk"
$PackageName = "dev.agentshell.app"
$MainActivity = ".MainActivity"

function Get-AdbDevices {
    $out = adb devices
    $devices = @()
    foreach ($line in $out) {
        # Matches the serial number in "serial\tdevice"
        if ($line -match "^\s*(\S+)\s+device\s*$") {
            $devices += $Matches[1]
        }
    }
    return $devices
}

function Select-Device {
    while ($true) {
        Clear-Host
        Write-Host "==========================================" -ForegroundColor Yellow
        Write-Host "   AgentShell: DEVICE SELECTION           " -ForegroundColor Yellow
        Write-Host "==========================================" -ForegroundColor Yellow
        
        $devices = @(Get-AdbDevices)
        
        if ($devices.Count -eq 0) {
            Write-Host " [!] No devices detected via ADB." -ForegroundColor Red
            Write-Host " Make sure USB Debugging is enabled."
            Write-Host ""
            Write-Host " R) Refresh List"
            Write-Host " Q) Quit"
            $choice = Read-Host " Select Action"
            if ($choice -eq "q") { exit }
            if ($choice -eq "r") { continue }
        } elseif ($devices.Count -eq 1) {
            Write-Host " Auto-selecting only available device: $($devices[0])" -ForegroundColor Green
            Start-Sleep -Seconds 1
            return $devices[0]
        } else {
            Write-Host " Available Devices:" -ForegroundColor Cyan
            for ($i = 0; $i -lt $devices.Count; $i++) {
                Write-Host "  $($i + 1)) $($devices[$i])"
            }
            Write-Host ""
            Write-Host " R) Refresh List"
            Write-Host " Q) Quit"
            
            Write-Host "==========================================" -ForegroundColor Yellow
            $choice = Read-Host " Select a device (1-$($devices.Count)) or Action"
            
            if ($choice -eq "q") { exit }
            if ($choice -eq "r") { continue }
            
            $choiceInt = $choice -as [int]
            if ($null -ne $choiceInt -and $choiceInt -ge 1 -and $choiceInt -le $devices.Count) {
                return $devices[$choiceInt - 1]
            }
        }
    }
}

function Build-And-Deploy {
    param($Device)
    Write-Host "`n[TASK] Starting Build Pipeline..." -ForegroundColor Blue
    .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n[TASK] Deploying to $Device..." -ForegroundColor Blue
        adb -s $Device install -r $ApkPath
        Write-Host "[TASK] Launching $PackageName..." -ForegroundColor Blue
        adb -s $Device shell am start -n "$PackageName/$MainActivity"
        Write-Host "`n[SUCCESS] Build & Deployment Complete!" -ForegroundColor Green
    } else {
        Write-Host "`n[!] Build Failed." -ForegroundColor Red
    }
}

# Main Script Execution
$currentDevice = Select-Device
Build-And-Deploy -Device $currentDevice
