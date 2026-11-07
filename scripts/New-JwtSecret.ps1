param([string]$EnvFile = (Join-Path $PSScriptRoot '..\.env'))
$ErrorActionPreference = 'Stop'
$existingConfig = ''
if (Test-Path -LiteralPath $EnvFile) {
    $existingConfig = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $EnvFile))
    $existingKey = [regex]::Match($existingConfig, '(?m)^JWT_SECRET=([^\r\n]*)')
    if ($existingKey.Success -and $existingKey.Groups[1].Value.Trim() -ne '' -and
            !$existingKey.Groups[1].Value.StartsWith('replace-with-')) {
        Write-Host 'JWT_SECRET already exists; preserved the current signing key.'
        exit 0
    }
}
$randomBytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $generator.GetBytes($randomBytes) } finally { $generator.Dispose() }
$jwtSecretValue = [Convert]::ToBase64String($randomBytes)
if ($existingConfig -match '(?m)^JWT_SECRET=') {
    $updatedConfig = [regex]::Replace($existingConfig, '(?m)^JWT_SECRET=[^\r\n]*', "JWT_SECRET=$jwtSecretValue")
} else {
    $updatedConfig = $existingConfig.TrimEnd() + "`nJWT_SECRET=$jwtSecretValue`n"
}
[System.IO.File]::WriteAllText($EnvFile, $updatedConfig, [System.Text.UTF8Encoding]::new($false))
Write-Host 'Generated a 32-byte signing key in the private .env file. Keep this file across restarts.'
