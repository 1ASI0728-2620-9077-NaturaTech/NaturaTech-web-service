param([string]$Image = 'plantsync-api:local')
$ErrorActionPreference = 'Stop'
$suffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$network = "plantsync-check-$suffix"
$database = "plantsync-db-$suffix"
$application = "plantsync-api-$suffix"
$directory = Join-Path ([IO.Path]::GetTempPath()) "plantsync-check-$suffix"
$dbEnv = Join-Path $directory 'mysql.env'
$apiEnv = Join-Path $directory 'api.env'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function New-LocalSecret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
function Wait-Api([string]$url) {
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $health = Invoke-RestMethod "$url/actuator/health" -TimeoutSec 3
            if ($health.status -eq 'UP') { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw 'The application did not become healthy.'
}
try {
    New-Item -ItemType Directory -Path $directory | Out-Null
    $password = New-LocalSecret
    $jwt = New-LocalSecret
    [IO.File]::WriteAllText($dbEnv, "MYSQL_ROOT_PASSWORD=$password`nMYSQL_DATABASE=plantsync`nMYSQL_USER=plantsync`nMYSQL_PASSWORD=$password`n", $utf8)
    # These credentials are disposable and never printed. No external integration keys loaded.
    [IO.File]::WriteAllText($apiEnv, "SPRING_PROFILES_ACTIVE=render`nDB_URL=jdbc:mysql://database:3306/plantsync?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC`nDB_USERNAME=plantsync`nDB_PASSWORD=$password`nJWT_SECRET=$jwt`nIOT_ENVIRONMENT=SIMULATED`nREWARDS_ENABLED=false`n", $utf8)
    docker network create $network | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not create test network.' }
    docker run --detach --name $database --network $network --network-alias database --env-file $dbEnv --health-cmd 'mysqladmin ping -h127.0.0.1 --silent' --health-interval 2s --health-retries 60 mysql:8.4 --default-time-zone=+00:00 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start disposable MySQL.' }
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ((docker inspect --format '{{.State.Health.Status}}' $database) -eq 'healthy') { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw 'MySQL did not become healthy.' }
    docker run --detach --name $application --network $network --env-file $apiEnv --publish '127.0.0.1::10000' $Image | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not start application image.' }
    $binding = docker port $application 10000/tcp
    if ($binding -notmatch '^127\.0\.0\.1:\d+$') { throw 'Unexpected test port binding.' }
    $url = "http://$binding"
    Wait-Api $url
    $email = "docker-$suffix@example.test"
    $userPassword = New-LocalSecret
    $signup = @{name='Docker smoke'; email=$email; password=$userPassword; subscriptionPlan='BASIC'} | ConvertTo-Json
    $null = Invoke-RestMethod "$url/api/v1/authentication/sign-up" -Method Post -ContentType 'application/json' -Body $signup
    $credentials = @{email=$email; password=$userPassword} | ConvertTo-Json
    $login = Invoke-RestMethod "$url/api/v1/authentication/sign-in" -Method Post -ContentType 'application/json' -Body $credentials
    $headers = @{Authorization="Bearer $($login.token)"}
    $me = Invoke-RestMethod "$url/api/v1/me" -Headers $headers
    if ($me.roles -notcontains 'ROLE_USER' -or -not $me.profileId) { throw 'Identity/profile check failed.' }
    $denied = $false
    try { $null = Invoke-RestMethod "$url/api/v1/users" -Headers $headers }
    catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $denied = $true } else { throw } }
    if (-not $denied) { throw 'User accessed global directory.' }
    docker restart $application | Out-Null
    Wait-Api $url
    $restored = Invoke-RestMethod "$url/api/v1/me" -Headers $headers
    if ($restored.id -ne $me.id -or $restored.profileId -ne $me.profileId) { throw 'Persistence after restart failed.' }
    Write-Host 'PASS: Docker render profile, fresh MySQL migration, registration, login, permissions and persistence after restart.'
} finally {
    # Only resources with this execution's unique names are removed.
    $ErrorActionPreference = 'Continue'
    docker rm --force --volumes $application $database 2>$null | Out-Null
    docker network rm $network 2>$null | Out-Null
    if (Test-Path -LiteralPath $dbEnv) { Remove-Item -LiteralPath $dbEnv }
    if (Test-Path -LiteralPath $apiEnv) { Remove-Item -LiteralPath $apiEnv }
    if (Test-Path -LiteralPath $directory) { Remove-Item -LiteralPath $directory }
}
