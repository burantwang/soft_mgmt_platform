# ============================================================
# 开发工具链一键部署脚本（Windows）
# 安装到 $HOME\.devtools（无需管理员权限），并配置用户级环境变量
# 包含：Node 20.18.0 / OpenJDK 17.0.13 / Maven 3.9.16
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File setup-env.ps1
# ============================================================
$ErrorActionPreference = 'Stop'
$toolsRoot = Join-Path $env:USERPROFILE '.devtools'
$dlDir = Join-Path $toolsRoot 'downloads'
$logFile = Join-Path $PSScriptRoot 'setup-env.log'
New-Item -ItemType Directory -Force -Path $dlDir | Out-Null

function Log([string]$msg) {
    $line = ("[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $msg)
    Write-Output $line
    Add-Content -Path $logFile -Value $line
}

$targets = @(
    @{
        Name = 'node'
        Zip  = Join-Path $dlDir 'node.zip'
        Url  = 'https://npmmirror.com/mirrors/node/v20.18.0/node-v20.18.0-win-x64.zip'
        Bin  = Join-Path $toolsRoot 'node'
    },
    @{
        Name = 'jdk17'
        Zip  = Join-Path $dlDir 'jdk.zip'
        Url  = 'https://download.visualstudio.microsoft.com/download/pr/26480204-af7a-4cfe-b22e-d123159ca2c7/8e14867d6280442d69ab9adc7fdc23ef/microsoft-jdk-17.0.13-windows-x64.zip'
        Bin  = Join-Path $toolsRoot 'jdk17'
    },
    @{
        Name = 'maven'
        Zip  = Join-Path $dlDir 'maven.zip'
        Url  = 'https://mirrors.aliyun.com/apache/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip'
        Bin  = Join-Path $toolsRoot 'maven'
    }
)

foreach ($t in $targets) {
    Log "==== 处理工具: $($t.Name) ===="
    if (Test-Path $t.Bin) {
        Log "已存在 $($t.Bin)，跳过"
        continue
    }
    if (-not (Test-Path $t.Zip)) {
        Log "下载: $($t.Url)"
        & curl.exe -sL -o $t.Zip --retry 3 --connect-timeout 30 $t.Url
        if ($LASTEXITCODE -ne 0) { throw "下载失败: $($t.Name) ($($t.Url))" }
        Log "下载完成: $((Get-Item $t.Zip).Length / 1MB) MB"
    } else {
        Log "使用已缓存压缩包: $($t.Zip)"
    }

    $ex = Join-Path $dlDir ("ex_" + $t.Name)
    if (Test-Path $ex) { Remove-Item $ex -Recurse -Force }
    New-Item -ItemType Directory -Path $ex | Out-Null

    Log "解压: $($t.Zip)"
    & tar.exe -xf $t.Zip -C $ex
    if ($LASTEXITCODE -ne 0) { throw "解压失败: $($t.Name)" }

    $inner = Get-ChildItem $ex -Directory | Select-Object -First 1
    if (-not $inner) { throw "解压后未找到目录: $($t.Name)" }

    Log "重命名: $($inner.FullName) -> $($t.Bin)"
    Move-Item $inner.FullName $t.Bin
}

$nodeBin = Join-Path $toolsRoot 'node'
$jdkBin  = Join-Path $toolsRoot 'jdk17'
$mvnBin  = Join-Path $toolsRoot 'maven'

# 配置用户级环境变量
[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdkBin, 'User')
[Environment]::SetEnvironmentVariable('MAVEN_HOME', $mvnBin, 'User')

$cur = [Environment]::GetEnvironmentVariable('Path', 'User')
$add = @($nodeBin, (Join-Path $jdkBin 'bin'), (Join-Path $mvnBin 'bin')) -join ';'
if ($cur -notlike "*$nodeBin*") {
    [Environment]::SetEnvironmentVariable('Path', "$add;$cur", 'User')
}

# 同步到当前进程，便于后续命令直接使用
$env:Path = "$add;$env:Path"
$env:JAVA_HOME = $jdkBin
$env:MAVEN_HOME = $mvnBin

Log "==== 部署完成，版本验证 ===="
Log "node: $(& (Join-Path $nodeBin 'node.exe') -v)"
Log "npm : $(& (Join-Path $nodeBin 'npm.cmd') -v)"
Log "java: $(& (Join-Path $jdkBin 'bin\java.exe') -version 2>&1 | Select-Object -First 1)"
Log "mvn : $(& (Join-Path $mvnBin 'bin\mvn.cmd') -v 2>&1 | Select-Object -First 1)"

$doneFile = Join-Path $toolsRoot 'setup-done.txt'
"deployed at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Set-Content -Path $doneFile
Log "完成标志: $doneFile"
Log "==== SETUP DONE ===="
