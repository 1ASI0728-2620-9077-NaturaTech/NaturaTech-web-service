$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$env:SPRING_PROFILES_ACTIVE = 'local'
if (-not $env:JWT_SECRET) {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
    Write-Host 'JWT temporal generado para esta ejecución local. Las sesiones anteriores caducarán al reiniciar.'
}
& .\mvnw.cmd spring-boot:run
exit $LASTEXITCODE
