param([int]$FrontendPort = 5175, [int]$BackendPort = 9080, [switch]$Development)
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDirectory = Join-Path $projectRoot 'test-results'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

function Test-LocalPort([int]$Port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try { return $client.ConnectAsync('127.0.0.1', $Port).Wait(500) -and $client.Connected }
    catch { return $false }
    finally { $client.Dispose() }
}

if (-not (Test-LocalPort $BackendPort)) {
    $jarPath = Join-Path $projectRoot 'backend\target\family-tree.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) { throw '请先在 backend 目录执行 mvn package。' }
    $backend = Start-Process -FilePath 'java' -ArgumentList @('-jar','target/family-tree.jar','--spring.profiles.active=demo',"--server.port=$BackendPort") -WorkingDirectory (Join-Path $projectRoot 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'demo-backend.log') -RedirectStandardError (Join-Path $outputDirectory 'demo-backend-error.log')
    Write-Output "演示后端进程 $($backend.Id)"
}

if (-not (Test-LocalPort $FrontendPort)) {
    $viteArguments = @('node_modules/vite/bin/vite.js')
    if (-not $Development) {
        if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'frontend\dist\index.html'))) { throw '请先在 frontend 目录执行 npm run build。' }
        $viteArguments += 'preview'
    }
    $viteArguments += @('--host','127.0.0.1','--port',"$FrontendPort",'--strictPort')
    $oldTarget = $env:API_TARGET
    try {
        $env:API_TARGET = "http://127.0.0.1:$BackendPort"
        $frontend = Start-Process -FilePath 'node' -ArgumentList $viteArguments -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $outputDirectory 'demo-frontend.log') -RedirectStandardError (Join-Path $outputDirectory 'demo-frontend-error.log')
        Write-Output "演示前端进程 $($frontend.Id)"
    } finally { $env:API_TARGET = $oldTarget }
}

$deadline = [DateTime]::UtcNow.AddSeconds(40)
while ([DateTime]::UtcNow -lt $deadline) {
    if ((Test-LocalPort $BackendPort) -and (Test-LocalPort $FrontendPort)) {
        Write-Output "浏览器访问 http://127.0.0.1:$FrontendPort/auth/login"
        Write-Output '演示账号 demo_admin 或 demo_member，初始密码 Demo123!'
        return
    }
    Start-Sleep -Milliseconds 500
}
throw '演示服务启动超时，请查看 test-results 目录中的服务日志。'
