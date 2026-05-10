# AgentShell Build & Test Automation Script
# Optimized PowerShell TUI for efficient device selection and deployment

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
        } else {
            Write-Host " Available Devices:" -ForegroundColor Cyan
            for ($i = 0; $i -lt $devices.Count; $i++) {
                Write-Host "  $($i + 1)) $($devices[$i])"
            }
            Write-Host ""
            Write-Host " R) Refresh List"
            Write-Host " Q) Quit"
        }
        
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

function Main-Menu {
    param($Device)
    while ($true) {
        Clear-Host
        Write-Host "==========================================" -ForegroundColor Yellow
        Write-Host "   AgentShell: MAIN MENU                  " -ForegroundColor Yellow
        Write-Host "==========================================" -ForegroundColor Yellow
        Write-Host " TARGET: $Device" -ForegroundColor Green
        Write-Host "------------------------------------------"
        Write-Host " [B] Build Debug APK"
        Write-Host " [T] Test (Install & Launch)"
        Write-Host " [A] All (Build + Test)"
        Write-Host ""
        Write-Host " [C] Change Device"
        Write-Host " [Q] Quit"
        Write-Host "==========================================" -ForegroundColor Yellow
        
        $input = Read-Host " Choose Action"
        $action = $input.ToLower()

        switch ($action) {
            "b" {
                Write-Host "`n[TASK] Building project..." -ForegroundColor Blue
                .\gradlew.bat assembleDebug
                Write-Host "`nDone." -ForegroundColor Gray
                Read-Host "Press Enter to return..."
            }
            "t" {
                Write-Host "`n[TASK] Deploying to $Device..." -ForegroundColor Blue
                if (!(Test-Path $ApkPath)) {
                    Write-Host "Error: APK not found at $ApkPath. Build first." -ForegroundColor Red
                } else {
                    adb -s $Device install -r $ApkPath
                    Write-Host "[TASK] Launching $PackageName..." -ForegroundColor Blue
                    adb -s $Device shell am start -n "$PackageName/$MainActivity"
                }
                Read-Host "Press Enter to return..."
            }
            "a" {
                Write-Host "`n[TASK] Starting Full Pipeline..." -ForegroundColor Blue
                .\gradlew.bat assembleDebug
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "`n[TASK] Deploying to $Device..." -ForegroundColor Blue
                    adb -s $Device install -r $ApkPath
                    Write-Host "[TASK] Launching $PackageName..." -ForegroundColor Blue
                    adb -s $Device shell am start -n "$PackageName/$MainActivity"
                } else {
                    Write-Host "`n[!] Build Failed." -ForegroundColor Red
                }
                Read-Host "Press Enter to return..."
            }
            "c" { return "change" }
            "q" { exit }
        }
    }
}

# Main Loop
while ($true) {
    $currentDevice = Select-Device
    $res = Main-Menu -Device $currentDevice
    if ($res -ne "change") { break }
}
