Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    throw "Missing .env. Copy .env.example to .env and set DB_PASSWORD."
}

Write-Host "Removing the existing PostgreSQL container and data volume..." -ForegroundColor Cyan
docker compose down --volumes --remove-orphans
if ($LASTEXITCODE -ne 0) {
    throw "Failed to reset PostgreSQL."
}

Write-Host "Starting PostgreSQL with an empty database..." -ForegroundColor Cyan
docker compose up -d --wait
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start PostgreSQL."
}

Write-Host "Starting Learning Platform..." -ForegroundColor Cyan
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
