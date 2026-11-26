[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path ".env")) {
    throw "Missing .env. Copy .env.example to .env and set DB_PASSWORD."
}

$demoDataPath = Join-Path $PSScriptRoot "src\main\resources\demo-data.sql"
if (-not (Test-Path $demoDataPath)) {
    throw "Missing demo data script: $demoDataPath"
}

Write-Host "Starting PostgreSQL with existing data..." -ForegroundColor Cyan
docker compose up -d --wait
if ($LASTEXITCODE -ne 0) {
    throw "Failed to start PostgreSQL."
}

Write-Host "Loading demo data..." -ForegroundColor Cyan
$databaseUser = docker compose exec -T postgres printenv POSTGRES_USER
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read POSTGRES_USER from PostgreSQL container."
}
$databaseUser = $databaseUser.Trim()

$databaseName = docker compose exec -T postgres printenv POSTGRES_DB
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read POSTGRES_DB from PostgreSQL container."
}
$databaseName = $databaseName.Trim()

$containerDataPath = "/tmp/learning-platform-demo-data.sql"
docker compose cp $demoDataPath "postgres:$containerDataPath"
if ($LASTEXITCODE -ne 0) {
    throw "Failed to copy demo data into PostgreSQL container."
}

docker compose exec -T postgres psql --set ON_ERROR_STOP=1 --username $databaseUser --dbname $databaseName --file $containerDataPath
$loadExitCode = $LASTEXITCODE

docker compose exec -T postgres rm -f $containerDataPath
$cleanupExitCode = $LASTEXITCODE

if ($loadExitCode -ne 0) {
    throw "Failed to load demo data. Start the application once to apply Flyway migrations, then retry."
}
if ($cleanupExitCode -ne 0) {
    throw "Demo data loaded, but the temporary container file could not be removed."
}

Write-Host "Demo data loaded successfully." -ForegroundColor Green
