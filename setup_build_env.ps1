# Requires -RunAsAdministrator

function Show-TuiPlan {
    Clear-Host
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host "  agentShell - Android Build Env Setup (Admin PowerShell)   " -ForegroundColor White -BackgroundColor DarkBlue
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "PLAN OF ACTION:" -ForegroundColor Yellow
    Write-Host "  [1] Download JDK 17 (Adoptium)"
    Write-Host "  [2] Download Android Command Line Tools"
    Write-Host "  [3] Download Scrcpy (Screen mirroring tool)"
    Write-Host "      (Downloads run in parallel)"
    Write-Host "  [4] Extract components to ~/agentShellEnv"
    Write-Host "  [5] Accept Android SDK licenses automatically"
    Write-Host "  [6] Install ADB (platform-tools), Build-Tools, and API 35"
    Write-Host "  [7] Update SYSTEM Environment Variables (Requires Admin):"
    Write-Host "      -> Set JAVA_HOME, ANDROID_HOME, SCRCPY_HOME"
    Write-Host "      -> Add binaries to Machine PATH"
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Cyan
}

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host ""
    Write-Host "[!] ERROR: This script must be run as Administrator." -ForegroundColor Red
    Write-Host "Please close this window, right-click PowerShell, select 'Run as Administrator', and run the script again." -ForegroundColor Yellow
    Write-Host ""
    exit
}

Show-TuiPlan

$confirm = Read-Host "Press [ENTER] to begin the download and setup, or [CTRL+C] to cancel"

$InstallDir = Join-Path $HOME 'agentShellEnv'
$JavaHome = Join-Path $InstallDir 'jdk'
$AndroidHome = Join-Path $InstallDir 'android_sdk'
$ScrcpyHome = Join-Path $InstallDir 'scrcpy'

if (-not (Test-Path $InstallDir)) { New-Item -ItemType Directory -Path $InstallDir | Out-Null }

$Urls = @{
    'JDK' = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip'
    'AndroidCmd' = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip'
    'Scrcpy' = 'https://github.com/Genymobile/scrcpy/releases/download/v2.4/scrcpy-win64-v2.4.zip'
}

$Files = @{
    'JDK' = Join-Path $InstallDir 'jdk.zip'
    'AndroidCmd' = Join-Path $InstallDir 'cmdline-tools.zip'
    'Scrcpy' = Join-Path $InstallDir 'scrcpy.zip'
}

Write-Host ""
Write-Host "[PHASE 1] Starting parallel downloads..." -ForegroundColor Cyan

Import-Module BitsTransfer -ErrorAction SilentlyContinue

$jobs = @()
foreach ($key in $Urls.Keys) {
    $url = $Urls[$key]
    $dest = $Files[$key]
    Write-Host "  -> Queuing $key"
    
    # Start BITS transfer asynchronously
    $job = Start-BitsTransfer -Source $url -Destination $dest -Asynchronous -DisplayName "Downloading $key"
    $jobs += @{ Name = $key; Job = $job }
}

Write-Host "  -> Downloading (Please wait)..." -ForegroundColor Yellow

$allDone = $false
while (-not $allDone) {
    $allDone = $true
    $statusText = ""
    
    foreach ($item in $jobs) {
        $job = $item.Job
        if ($job.JobState -eq 'Transferring' -or $job.JobState -eq 'Connecting') {
            $allDone = $false
            $pct = 0
            if ($job.BytesTotal -gt 0) {
                $pct = [math]::Round(($job.BytesTransferred / $job.BytesTotal) * 100)
            }
            $statusText += "$($item.Name): $pct%   "
        } elseif ($job.JobState -eq 'Transferred') {
            $statusText += "$($item.Name): 100%   "
        } else {
            $statusText += "$($item.Name): $($job.JobState)   "
        }
    }
    
    # Display the progress bar at the top of the PowerShell window
    if (-not $allDone) {
        Write-Progress -Activity "Downloading Components in Parallel" -Status $statusText
        Start-Sleep -Milliseconds 500
    }
}

# Clear the progress bar
Write-Progress -Activity "Downloading Components in Parallel" -Completed

foreach ($item in $jobs) {
    if ($item.Job.JobState -eq 'Transferred') {
        Complete-BitsTransfer -BitsJob $item.Job
        Write-Host "  [v] $($item.Name) downloaded." -ForegroundColor Green
    } else {
        Write-Host "  [!] $($item.Name) failed: $($item.Job.JobState)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "[PHASE 2] Extracting files..." -ForegroundColor Cyan

# Extract JDK
Write-Host "  -> Extracting JDK 17..."
Expand-Archive -Path $Files['JDK'] -DestinationPath (Join-Path $InstallDir 'temp_jdk') -Force
$jdkFolder = Get-ChildItem (Join-Path $InstallDir 'temp_jdk') | Select-Object -First 1
if (Test-Path $JavaHome) { Remove-Item $JavaHome -Recurse -Force }
Move-Item $jdkFolder.FullName $JavaHome
Remove-Item (Join-Path $InstallDir 'temp_jdk') -Force
Remove-Item $Files['JDK']

# Extract Android Tools
Write-Host "  -> Extracting Android Tools..."
Expand-Archive -Path $Files['AndroidCmd'] -DestinationPath (Join-Path $InstallDir 'temp_cmd') -Force
$latestDir = Join-Path $AndroidHome 'cmdline-tools\latest'
if (Test-Path $latestDir) { Remove-Item $latestDir -Recurse -Force }
New-Item -ItemType Directory -Path $latestDir -Force | Out-Null
Move-Item (Join-Path $InstallDir 'temp_cmd\cmdline-tools\*') $latestDir
Remove-Item (Join-Path $InstallDir 'temp_cmd') -Force
Remove-Item $Files['AndroidCmd']

# Extract Scrcpy
Write-Host "  -> Extracting Scrcpy..."
Expand-Archive -Path $Files['Scrcpy'] -DestinationPath (Join-Path $InstallDir 'temp_scrcpy') -Force
$scrcpyContents = Get-ChildItem (Join-Path $InstallDir 'temp_scrcpy')
if (Test-Path $ScrcpyHome) { Remove-Item $ScrcpyHome -Recurse -Force }
if ($scrcpyContents.Count -eq 1 -and $scrcpyContents[0].PSIsContainer) {
    Move-Item $scrcpyContents[0].FullName $ScrcpyHome
} else {
    Rename-Item -Path (Join-Path $InstallDir 'temp_scrcpy') -NewName $ScrcpyHome
}
Remove-Item (Join-Path $InstallDir 'temp_scrcpy') -Force -ErrorAction SilentlyContinue
Remove-Item $Files['Scrcpy']

Write-Host ""
Write-Host "[PHASE 3] Configuring Android SDK..." -ForegroundColor Cyan
$sdkmanager = Join-Path $latestDir 'bin\sdkmanager.bat'

Write-Host "  -> Accepting licenses..."
$yesInput = ("y" + [Environment]::NewLine) * 50
$yesInput | & $sdkmanager '--licenses' | Out-Null

Write-Host "  -> Downloading platforms, build-tools, and platform-tools (ADB)..."
& $sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'

Write-Host ""
Write-Host "[PHASE 4] Setting System Environment Variables..." -ForegroundColor Cyan
[Environment]::SetEnvironmentVariable('JAVA_HOME', $JavaHome, 'Machine')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $AndroidHome, 'Machine')
[Environment]::SetEnvironmentVariable('SCRCPY_HOME', $ScrcpyHome, 'Machine')

$machinePath = [Environment]::GetEnvironmentVariable('PATH', 'Machine')
$pathsToAdd = @(
    (Join-Path $JavaHome 'bin'),
    (Join-Path $AndroidHome 'platform-tools'),
    (Join-Path $AndroidHome 'cmdline-tools\latest\bin'),
    $ScrcpyHome
)

$pathUpdated = $false
foreach ($p in $pathsToAdd) {
    if ($machinePath -notmatch [regex]::Escape($p)) {
        $machinePath += ";$p"
        $pathUpdated = $true
    }
}

if ($pathUpdated) {
    [Environment]::SetEnvironmentVariable('PATH', $machinePath, 'Machine')
    Write-Host "  [v] System PATH updated." -ForegroundColor Green
} else {
    Write-Host "  [i] System PATH already contains necessary paths." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "SUCCESS: Environment Setup is Complete!" -ForegroundColor Green
Write-Host "Location: $InstallDir"
Write-Host "Please CLOSE this PowerShell window and open a NEW one to use 'adb', 'scrcpy', and 'java'." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
