# 최신 코드를 받고 서버(8080)와 웹(5173)을 각각 새 창에서 띄웁니다.
# 탐색기에서 run-dev.cmd를 더블클릭하거나, PowerShell에서 .\run-dev.cmd 로 실행합니다.
# 두 창을 닫으면 서버와 웹이 꺼집니다.
$ErrorActionPreference = 'Stop'
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
.\gradlew.bat :server:bootRun
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
  Start-Process powershell -ArgumentList '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', $encoded
}

Start-Window $server
Start-Window $web
Write-Host '서버와 웹 창을 띄웠습니다. 1분쯤 뒤 http://localhost:5173 을 여세요.'
