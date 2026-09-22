$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = Join-Path $projectRoot "backend"
$frontendPath = Join-Path $projectRoot "frontend"

Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$backendPath'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$frontendPath'; if (-not (Test-Path node_modules)) { npm install }; npm run dev"

Write-Host "Backend and frontend are starting in separate windows."
Write-Host "Open http://localhost:5173 after both are ready."

