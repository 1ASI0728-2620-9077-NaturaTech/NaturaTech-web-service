param([string]$BaseUrl = 'http://127.0.0.1:8080/api/v1')
$ErrorActionPreference = 'Stop'
if ($BaseUrl -notmatch '^http://(127\.0\.0\.1|localhost):\d+/api/v1$') {
    throw 'Esta prueba solo crea cuentas en un servidor local.'
}
# Creates one disposable local account; never prints password or bearer token.
$suffix = [Guid]::NewGuid().ToString('N')
$email = "smoke-$suffix@example.test"
$password = [Guid]::NewGuid().ToString('N') + '!a9'
$signup = @{name='Verificacion local'; email=$email; password=$password; subscriptionPlan='BASIC'} | ConvertTo-Json
$null = Invoke-RestMethod "$BaseUrl/authentication/sign-up" -Method Post -ContentType 'application/json' -Body $signup
$body = @{email=$email; password=$password} | ConvertTo-Json
$login = Invoke-RestMethod "$BaseUrl/authentication/sign-in" -Method Post -ContentType 'application/json' -Body $body
$headers = @{Authorization="Bearer $($login.token)"}
$me = Invoke-RestMethod "$BaseUrl/me" -Headers $headers
if ($me.id -ne $login.id -or $me.roles -notcontains 'ROLE_USER' -or -not $me.profileId) { throw 'Identidad o perfil incorrectos' }
$profile = Invoke-RestMethod "$BaseUrl/profiles/by-user-id?userId=$($me.id)" -Headers $headers
if ($profile.id -ne $me.profileId) { throw 'Perfil incorrecto' }
$null = Invoke-RestMethod "$BaseUrl/capabilities" -Headers $headers
$denied = $false
try { $null = Invoke-RestMethod "$BaseUrl/users" -Headers $headers }
catch { if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $denied = $true } else { throw } }
if (-not $denied) { throw 'El directorio global no rechazo al usuario' }
Write-Host 'OK: registro, login, identidad, perfil, capacidades y aislamiento del directorio.'
Write-Host 'Queda una cuenta de prueba sin datos personales en la base local. No se probaron proveedores externos.'
