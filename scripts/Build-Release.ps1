param(
    [string[]]$Tasks = @(':app:bundleRelease', ':app:assembleRelease'),
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'TropoSigning')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$keystore = Join-Path $SigningDirectory 'tropo-upload.p12'
$passwordFile = Join-Path $SigningDirectory 'upload-password.dpapi'
if (-not (Test-Path -LiteralPath $keystore) -or -not (Test-Path -LiteralPath $passwordFile)) {
    throw 'Protected upload signing files are missing. Do not place keys/passwords in the repository.'
}
$names = @('TROPO_UPLOAD_STORE_FILE','TROPO_UPLOAD_STORE_PASSWORD','TROPO_UPLOAD_KEY_ALIAS','TROPO_UPLOAD_KEY_PASSWORD')
$original = @{}
foreach ($name in $names) { $original[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
$secure = ConvertTo-SecureString ([System.IO.File]::ReadAllText($passwordFile).Trim())
$pointer = [IntPtr]::Zero
try {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    [Environment]::SetEnvironmentVariable('TROPO_UPLOAD_STORE_FILE', $keystore, 'Process')
    [Environment]::SetEnvironmentVariable('TROPO_UPLOAD_STORE_PASSWORD', $password, 'Process')
    [Environment]::SetEnvironmentVariable('TROPO_UPLOAD_KEY_ALIAS', 'tropo-upload', 'Process')
    [Environment]::SetEnvironmentVariable('TROPO_UPLOAD_KEY_PASSWORD', $password, 'Process')
    & (Join-Path $root 'gradlew.bat') -p $root @Tasks --no-daemon --no-configuration-cache --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Signed release build failed with exit code $LASTEXITCODE" }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $original[$name], 'Process') }
    if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    $password = $null
    $secure.Dispose()
}