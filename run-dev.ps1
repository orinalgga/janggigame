# 최신 코드를 받고 서버(8080)와 웹(5173)을 각각 새 창에서 띄웁니다.
# 탐색기에서 run-dev.cmd를 더블클릭하거나, PowerShell에서 .\run-dev.cmd 로 실행합니다.
# 이 창은 열어 두면 1분마다 새 코드를 받아 옵니다. 웹은 바로 바뀌고, 서버 코드가 바뀌면 서버 창을 다시 띄웁니다.
# 서버·웹 창을 닫으면 그 부분이 꺼지고, 이 창을 닫으면 자동 반영만 멈춥니다.
$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot
Set-Location $root

Write-Host '최신 코드를 받는 중...'
git pull --ff-only
if ($LASTEXITCODE -ne 0) { Write-Warning '최신 코드를 받지 못했습니다. 지금 있는 코드로 띄웁니다.' }

Write-Host '웹 의존성을 확인하는 중...'
Push-Location web
npm install --no-audit --no-fund
Pop-Location

# .env의 값은 환경변수로만 넣고 화면에 찍지 않는다.
$server = @'
$Host.UI.RawUI.WindowTitle = 'janggi-server'
Set-Location '__ROOT__'
if (Test-Path .env) {
  foreach ($line in Get-Content .env) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(\S.*?)\s*$' -and -not $line.TrimStart().StartsWith('#')) {
      [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
  }
}
.\gradlew.bat --no-daemon :server:bootRun
'@
$server = $server.Replace('__ROOT__', $root)

$web = @'
$Host.UI.RawUI.WindowTitle = 'janggi-web'
Set-Location '__ROOT__\web'
npm run dev
'@
$web = $web.Replace('__ROOT__', $root)

function Start-Window([string]$script) {
  $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($script))
  Start-Process powershell -PassThru -ArgumentList '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded
}

# 창과 그 안에서 띄운 프로그램을 모두 끄고, 혹시 남아 포트를 잡고 있는 프로그램도 끈다.
function Stop-Window($proc, [int]$port) {
  if ($proc -and -not $proc.HasExited) { taskkill /PID $proc.Id /T /F | Out-Null }
  Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

$serverProc = Start-Window $server
$webProc = Start-Window $web
Write-Host '서버와 웹 창을 띄웠습니다. 1분쯤 뒤 http://localhost:5173 을 여세요.'
Write-Host '이 창을 열어 두면 1분마다 새 코드를 확인해서 알아서 반영합니다.'

while ($true) {
  Start-Sleep -Seconds 60
  git fetch --quiet 2>$null
  if ($LASTEXITCODE -ne 0) { continue }
  $behind = git rev-list --count 'HEAD..@{u}' 2>$null
  if ($LASTEXITCODE -ne 0 -or [int]$behind -eq 0) { continue }

  $before = git rev-parse HEAD
  git merge --ff-only --quiet '@{u}' 2>$null
  if ($LASTEXITCODE -ne 0) {
    Write-Warning '이 PC에서 고친 내용이 있어 새 코드를 자동으로 받지 못했습니다.'
    continue
  }
  $changed = git diff --name-only $before HEAD
  Write-Host ("{0} 새 코드를 받았습니다." -f (Get-Date -Format 'HH:mm'))

  # 웹 화면 코드는 Vite가 알아서 다시 읽는다. 의존성이 바뀌었을 때만 다시 띄운다.
  if ($changed | Where-Object { $_ -like 'web/package*.json' }) {
    Write-Host '웹 의존성이 바뀌어 웹을 다시 띄웁니다.'
    Stop-Window $webProc 5173
    Push-Location web
    npm install --no-audit --no-fund
    Pop-Location
    $webProc = Start-Window $web
  }
  if ($changed | Where-Object { $_ -match '^(server|janggi-core|gradle)/' -or $_ -like '*.gradle.kts' }) {
    Write-Host '서버 코드가 바뀌어 서버를 다시 띄웁니다.'
    Stop-Window $serverProc 8080
    $serverProc = Start-Window $server
  }
}
