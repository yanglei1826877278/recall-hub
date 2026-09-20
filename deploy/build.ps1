$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location "$Root\web"
pnpm install --frozen-lockfile
pnpm build
Remove-Item "$Root\server\src\main\resources\static" -Recurse -Force -ErrorAction SilentlyContinue
New-Item "$Root\server\src\main\resources\static" -ItemType Directory | Out-Null
Copy-Item ".\dist\*" "$Root\server\src\main\resources\static" -Recurse
Set-Location "$Root\server"
mvn clean package

