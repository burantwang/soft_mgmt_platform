# Start backend (dev profile) in background
$java = 'C:\Users\wangwenzhi\.devtools\jdk17\bin\java.exe'
$root = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $root 'backend\target\dev-platform.jar'
$stdout = Join-Path $PSScriptRoot 'backend-run.log'
$stderr = Join-Path $PSScriptRoot 'backend-run.err.log'
$work = Join-Path $root 'backend'
$p = Start-Process -FilePath $java -ArgumentList @('-jar', "`"$jar`"") `
    -WorkingDirectory $work `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -WindowStyle Hidden -PassThru
Write-Host "Backend started, PID=$($p.Id)"
