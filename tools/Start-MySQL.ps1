param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location (Join-Path $projectRoot 'backend')
try {
    if ($Build -or -not (Test-Path -LiteralPath 'target\family-tree.jar')) {
        & mvn package
        if ($LASTEXITCODE -ne 0) { throw '后端打包失败，请查看 Maven 输出。' }
    }
    if (-not $env:DB_PASSWORD) {
        $databasePassword = Read-Host -Prompt '请输入 MySQL 账号 family_app 的密码' -AsSecureString
        $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $databasePassword).Password
    }
    & java -jar target/family-tree.jar
} finally {
    Pop-Location
}
