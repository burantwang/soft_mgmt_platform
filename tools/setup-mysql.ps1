# ============================================================
# MySQL 8.0 portable deploy script (no admin required)
# Install to $HOME\.devtools\mysql, data dir $HOME\.devtools\mysql-data
# ============================================================
$ErrorActionPreference = 'Stop'
$toolsRoot = Join-Path $env:USERPROFILE '.devtools'
$dlDir = Join-Path $toolsRoot 'downloads'
$logFile = Join-Path $PSScriptRoot 'setup-mysql.log'
New-Item -ItemType Directory -Force -Path $dlDir | Out-Null

function Log([string]$msg) {
    $line = ("[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $msg)
    Write-Output $line
    Add-Content -Path $logFile -Value $line
}

$mysqlDir = Join-Path $toolsRoot 'mysql'
$dataDir  = Join-Path $toolsRoot 'mysql-data'
$zip      = Join-Path $dlDir 'mysql.zip'
$url      = 'https://mirrors.huaweicloud.com/mysql/Downloads/MySQL-8.0/mysql-8.0.29-winx64.zip'

# 1) download & extract
if (-not (Test-Path $mysqlDir)) {
    if (-not (Test-Path $zip)) {
        Log "Downloading MySQL 8.0.29 (~213MB)..."
        & curl.exe -sL -o $zip --retry 3 --connect-timeout 30 $url
        if ($LASTEXITCODE -ne 0) { throw "MySQL download failed" }
        Log ("Downloaded: {0} MB" -f [math]::Round((Get-Item $zip).Length / 1MB, 1))
    }
    $ex = Join-Path $dlDir 'ex_mysql'
    if (Test-Path $ex) { Remove-Item $ex -Recurse -Force }
    New-Item -ItemType Directory -Path $ex | Out-Null
    Log 'Extracting...'
    & tar.exe -xf $zip -C $ex
    if ($LASTEXITCODE -ne 0) { throw 'MySQL extract failed' }
    $inner = Get-ChildItem $ex -Directory | Select-Object -First 1
    Move-Item $inner.FullName $mysqlDir
    Log "MySQL installed to $mysqlDir"
} else {
    Log "MySQL already exists: $mysqlDir"
}

# 2) write my.ini (use forward slashes)
$myIni = Join-Path $mysqlDir 'my.ini'
$iniBase = $mysqlDir -replace '\\', '/'
$iniData = $dataDir -replace '\\', '/'
$ini = @"
[mysqld]
basedir=$iniBase
datadir=$iniData
port=3306
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
max_connections=200
default-time-zone=+08:00
[client]
port=3306
default-character-set=utf8mb4
"@
Set-Content -Path $myIni -Value $ini -Encoding ASCII
Log "my.ini written: $myIni"

# 3) initialize data dir (root empty password, insecure)
if (-not (Test-Path (Join-Path $dataDir 'mysql'))) {
    Log 'Initializing MySQL data directory...'
    # NOTE: do NOT use --console nor *>> here: mysqld writes progress to stderr,
    # and with $ErrorActionPreference='Stop' PS 5.1 throws NativeCommandError,
    # aborting init midway (data dir left with only auto.cnf).
    $ErrorActionPreference = 'Continue'
    & (Join-Path $mysqlDir 'bin\mysqld.exe') --defaults-file=$myIni --initialize-insecure
    $initCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($initCode -ne 0) { throw "MySQL initialize failed (exit=$initCode), see $dataDir\*.err" }
    Log 'Data directory initialized (root with empty password)'
} else {
    Log 'Data directory already initialized'
}

# 4) add bin to user PATH
$mysqlBin = Join-Path $mysqlDir 'bin'
$cur = [Environment]::GetEnvironmentVariable('Path', 'User')
if ($cur -notlike "*$mysqlBin*") {
    [Environment]::SetEnvironmentVariable('Path', "$mysqlBin;$cur", 'User')
    Log 'MySQL bin added to user PATH'
}

# 5) start mysqld as background process
$running = Get-Process mysqld -ErrorAction SilentlyContinue
if (-not $running) {
    Log 'Starting mysqld...'
    $p = Start-Process (Join-Path $mysqlDir 'bin\mysqld.exe') -ArgumentList "--defaults-file=$myIni" -WindowStyle Hidden -PassThru
    Log ("mysqld started, PID=" + $p.Id)
} else {
    Log 'mysqld already running'
}

$doneFile = Join-Path $toolsRoot 'mysql-done.txt'
"mysql deployed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Set-Content -Path $doneFile
Log '==== MYSQL SETUP DONE ===='
