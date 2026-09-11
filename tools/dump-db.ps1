# ============================================================
# One-click backup of dev_platform database -> sql/dev_platform_backup.sql
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/dump-db.ps1
#   powershell -ExecutionPolicy Bypass -File tools/dump-db.ps1 -Password <your-password>
# Notes:
#   - Password priority: -Password arg > env DB_PASSWORD > default 'root'
#   - Output file style matches other sql scripts in this repo (no BOM), ready to commit.
# ============================================================
param(
    [string]$HostName = '127.0.0.1',
    [string]$Port     = '3306',
    [string]$User     = 'root',
    [string]$Password = '',
    [string]$DbName   = 'dev_platform',
    [string]$OutFile  = ''
)

$ErrorActionPreference = 'Continue'

# Password resolution: -Password arg > env DB_PASSWORD > default 'root'
if ([string]::IsNullOrEmpty($Password)) {
    if ($env:DB_PASSWORD) { $Password = $env:DB_PASSWORD } else { $Password = 'root' }
}

if ([string]::IsNullOrEmpty($OutFile)) {
    $OutFile = Join-Path $PSScriptRoot '..\sql\dev_platform_backup.sql'
}
$OutFile = [System.IO.Path]::GetFullPath($OutFile)

$dump = (Get-Command mysqldump -ErrorAction Stop).Source
Write-Host "==> Dumping: ${User}@${HostName}:${Port}/${DbName}"
Write-Host "==> Output : $OutFile"

# 重要：必须使用 mysqldump 的 --result-file 直接写文件。
# 若改用 PowerShell 的 ">" 重定向，Windows PowerShell 5.1 会把输出编码为 UTF-16LE，
# 该文件在 Linux/容器内导入时会报 "ASCII '\0' appeared in the statement"。
$dumpArgs = @(
    '-h', $HostName, '-P', $Port, '-u', $User, ('-p' + $Password),
    '--single-transaction', '--no-tablespaces', '--routines', '--triggers',
    '--default-character-set=utf8mb4', '--set-charset',
    "--result-file=$OutFile", $DbName
)
& $dump @dumpArgs 2>&1 | Out-String -Stream |
    Where-Object { $_ -and ($_ -notmatch 'password on the command line') } | Write-Host

if ($LASTEXITCODE -ne 0) {
    Write-Error "mysqldump failed (exit=$LASTEXITCODE). Check MySQL is running and credentials."
    exit $LASTEXITCODE
}

$size = [math]::Round((Get-Item $OutFile).Length / 1KB, 1)
Write-Host "==> Backup done: $size KB"
Write-Host "==> Next step:  git add sql/dev_platform_backup.sql ; git commit -m 'chore(db): refresh backup'"
