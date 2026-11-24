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
            $health = Invoke-RestMethod "$url/actuator/health" -UseBasicParsing -TimeoutSec 3
            if ($health.status -eq 'UP') { return }
        } catch { $lastHealthError = $_.Exception.Message }
        Start-Sleep -Seconds 2
    }
    throw "The application did not become healthy at $url. Last error: $lastHealthError"
}
try {
    New-Item -ItemType Directory -Path $directory | Out-Null
    $password = New-LocalSecret
    $jwt = New-LocalSecret
    $edge = New-LocalSecret
    $nodeSecret = New-LocalSecret
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $nodeHash = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($nodeSecret)))).Replace('-', '').ToLowerInvariant() } finally { $sha.Dispose() }
    $nodeHashes = @{ 'SMOKE-NODE'=$nodeHash } | ConvertTo-Json -Compress
    [IO.File]::WriteAllText($dbEnv, "MYSQL_ROOT_PASSWORD=$password`nMYSQL_DATABASE=plantsync`nMYSQL_USER=plantsync`nMYSQL_PASSWORD=$password`n", $utf8)
    # These credentials are disposable and never printed. No external integration keys loaded.
    [IO.File]::WriteAllText($apiEnv, "SPRING_PROFILES_ACTIVE=render`nDB_URL=jdbc:mysql://database:3306/plantsync?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC`nDB_USERNAME=plantsync`nDB_PASSWORD=$password`nJWT_SECRET=$jwt`nEDGE_API_KEY=$edge`nIOT_NODE_KEY_HASHES=$nodeHashes`nIOT_ENVIRONMENT=SIMULATED`nREWARDS_ENABLED=false`nCORS_ORIGINS=https://plantsync-app.netlify.app`nWALLET_DOMAIN=plantsync-app.netlify.app`n", $utf8)
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
    $preflight = Invoke-WebRequest "$url/api/v1/authentication/sign-in" -UseBasicParsing -Method Options -Headers @{Origin='https://plantsync-app.netlify.app'; 'Access-Control-Request-Method'='POST'; 'Access-Control-Request-Headers'='content-type'}
    if ($preflight.Headers['Access-Control-Allow-Origin'] -ne 'https://plantsync-app.netlify.app') { throw 'Netlify CORS preflight failed.' }
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
    $plantBody = @{name='Smoke plant'; species='Monstera'; description='Disposable test'; acquisitionDate='2026-01-01'; humidity='MEDIA'; nextWateringDate='2026-12-01'; notificationsEnabled=$true; profileId=$me.profileId; humidityThresholdMin=30; humidityThresholdMax=70; lightThresholdMin=20} | ConvertTo-Json
    $plant = Invoke-RestMethod "$url/api/v1/plants" -Headers $headers -Method Post -ContentType 'application/json' -Body $plantBody
    $nodeBody = @{nodeCode='SMOKE-NODE'; plantId=$plant.id; profileId=$me.profileId} | ConvertTo-Json
    $node = Invoke-RestMethod "$url/api/v1/iot/nodes" -Headers $headers -Method Post -ContentType 'application/json' -Body $nodeBody
    $taskBody = @{action='Water'; scheduledDate='2026-12-01'; plantId=$plant.id; profileId=$me.profileId; notes='Smoke test'} | ConvertTo-Json
    $task = Invoke-RestMethod "$url/api/v1/tasks" -Headers $headers -Method Post -ContentType 'application/json' -Body $taskBody
    $completed = Invoke-RestMethod "$url/api/v1/tasks/$($task.id)/complete" -Headers $headers -Method Patch -ContentType 'application/json' -Body '{"humidity":45,"notes":"Verified"}'
    if (-not $completed.completedAt) { throw 'Task completion failed.' }
    $otherSignup = @{name='Other user'; email="other-$suffix@example.test"; password=$userPassword; subscriptionPlan='BASIC'} | ConvertTo-Json
    $null = Invoke-RestMethod "$url/api/v1/authentication/sign-up" -Method Post -ContentType 'application/json' -Body $otherSignup
    $otherLogin = Invoke-RestMethod "$url/api/v1/authentication/sign-in" -Method Post -ContentType 'application/json' -Body (@{email="other-$suffix@example.test"; password=$userPassword} | ConvertTo-Json)
    $otherHeaders = @{Authorization="Bearer $($otherLogin.token)"}
    $isolated = $false
    try { $null = Invoke-RestMethod "$url/api/v1/plants/$($plant.id)" -Headers $otherHeaders }
    catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $isolated = $true } else { throw } }
    if (-not $isolated) { throw 'Another user accessed the plant.' }
    $edgeHeaders = @{'X-Edge-Api-Key'=$edge; 'X-Node-Key'=$nodeSecret}
    $reading = '[{"nodeId":' + $node.id + ',"humidityPercent":45,"lightPercent":40,"gasPercent":10,"airTemperature":23}]'
    $null = Invoke-RestMethod "$url/api/v1/iot/edge/sensor-readings/batch" -Headers $edgeHeaders -Method Post -ContentType 'application/json' -Body $reading
    $latest = Invoke-RestMethod "$url/api/v1/iot/plants/$($plant.id)/readings/latest" -Headers $headers
    if ($latest.humidityPercent -ne 45 -or $latest.nodeId -ne $node.id) { throw 'Telemetry roundtrip failed.' }
    $wrongHeaders = @{'X-Edge-Api-Key'=$edge; 'X-Node-Key'=('x' * 48)}
    $rejected = $false
    try { $null = Invoke-RestMethod "$url/api/v1/iot/edge/sensor-readings/batch" -Headers $wrongHeaders -Method Post -ContentType 'application/json' -Body $reading }
    catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $rejected = $true } else { throw } }
    if (-not $rejected) { throw 'Wrong node secret accepted.' }
    docker restart $application | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restart application image.' }
    # Docker may assign a different host port when restarting an ephemeral binding.
    $binding = docker port $application 10000/tcp
    if ($binding -notmatch '^127\.0\.0\.1:\d+$') { throw 'Unexpected restarted test port binding.' }
    $url = "http://$binding"
    Wait-Api $url
    $restored = Invoke-RestMethod "$url/api/v1/me" -Headers $headers
    if ($restored.id -ne $me.id -or $restored.profileId -ne $me.profileId) { throw 'Persistence after restart failed.' }
    $persistedReading = Invoke-RestMethod "$url/api/v1/iot/plants/$($plant.id)/readings/latest" -Headers $headers
    if ($persistedReading.id -ne $latest.id) { throw 'Telemetry persistence after restart failed.' }
    Write-Host 'PASS: Docker render profile, fresh MySQL migration, Netlify CORS, registration, login, permissions, cross-user isolation, task completion, authenticated telemetry, invalid node secret rejection and persistence after restart.'
} finally {
    # Only resources with this execution's unique names are removed.
    $ErrorActionPreference = 'Continue'
    docker rm --force --volumes $application $database 2>$null | Out-Null
    docker network rm $network 2>$null | Out-Null
    if (Test-Path -LiteralPath $dbEnv) { Remove-Item -LiteralPath $dbEnv }
    if (Test-Path -LiteralPath $apiEnv) { Remove-Item -LiteralPath $apiEnv }
    if (Test-Path -LiteralPath $directory) { Remove-Item -LiteralPath $directory }
}
