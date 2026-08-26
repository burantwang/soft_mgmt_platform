# Build frontend (Vue3+Vite) with local node
$ErrorActionPreference = 'Continue'
$toolsRoot = Join-Path $env:USERPROFILE '.devtools'
$env:Path = "$toolsRoot\node;$env:Path"
$root = Split-Path $PSScriptRoot -Parent
Set-Location (Join-Path $root 'frontend')

$log1 = Join-Path $PSScriptRoot 'frontend-install.log'
$log2 = Join-Path $PSScriptRoot 'frontend-build.log'

Write-Host "npm install at $(Get-Date -Format 'HH:mm:ss') ..."
& npm.cmd install 2>&1 | Out-File -FilePath $log1 -Encoding utf8
Write-Host "INSTALL EXIT=$LASTEXITCODE"

Write-Host "npm run build at $(Get-Date -Format 'HH:mm:ss') ..."
& npm.cmd run build 2>&1 | Out-File -FilePath $log2 -Encoding utf8
Write-Host "BUILD EXIT=$LASTEXITCODE"

Get-ChildItem (Join-Path $root 'frontend\dist') -ErrorAction SilentlyContinue | Select-Object Name, Length
Write-Host '==== FRONTEND BUILD DONE ===='
