# Build backend (SpringBoot) with local maven
$ErrorActionPreference = 'Continue'
$toolsRoot = Join-Path $env:USERPROFILE '.devtools'
$env:Path = "$toolsRoot\maven\bin;$toolsRoot\jdk17\bin;$env:Path"
$root = Split-Path $PSScriptRoot -Parent
$logFile = Join-Path $PSScriptRoot 'backend-build.log'
Set-Location (Join-Path $root 'backend')
Write-Host "Building backend at $(Get-Date -Format 'HH:mm:ss') ..."
& mvn.cmd clean package -DskipTests 2>&1 | Out-File -FilePath $logFile -Encoding utf8
Write-Host "MVN EXIT=$LASTEXITCODE"
Get-Item (Join-Path $root 'backend\target\dev-platform.jar') -ErrorAction SilentlyContinue | Select-Object FullName, Length
Write-Host '==== BACKEND BUILD DONE ===='
