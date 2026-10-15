param(
    [string]$Apk = (Join-Path $env:LOCALAPPDATA 'gradle-builds\tropo\app\outputs\apk\release\app-release.apk'),
    [string]$NdkVersion = '28.2.13676358'
)

$ErrorActionPreference = 'Stop'
$pageSize = 16384
$readelf = Join-Path $env:LOCALAPPDATA "Android\Sdk\ndk\$NdkVersion\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe"
if (-not (Test-Path -LiteralPath $readelf)) { throw "NDK readelf missing: $readelf" }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$directory = Join-Path $env:TEMP ('tropo-native-check-' + [Guid]::NewGuid().ToString('N'))
$archive = [IO.Compression.ZipFile]::OpenRead($Apk)
try {
    $results = @(
        foreach ($entry in $archive.Entries | Where-Object { $_.FullName -match '^lib/(arm64-v8a|x86_64)/.*\.so$' }) {
            $path = Join-Path $directory $entry.FullName
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $path)
            $headers = & $readelf -W -l $path
            if ($LASTEXITCODE -ne 0) { throw "ELF read failed: $($entry.FullName)" }
            $loads = @($headers | Where-Object { $_ -match '^\s*LOAD\s' })
            $loadAligned = $loads.Count -gt 0
            foreach ($load in $loads) {
                $alignment = ($load.Trim() -split '\s+')[-1]
                if ([Convert]::ToInt64($alignment.Substring(2), 16) -lt $pageSize) { $loadAligned = $false }
            }
            $relro = $headers | Where-Object { $_ -match '^\s*GNU_RELRO\s' } | Select-Object -First 1
            $relroAligned = $true
            if ($relro) {
                $parts = $relro.Trim() -split '\s+'
                $end = [Convert]::ToInt64($parts[2].Substring(2), 16) + [Convert]::ToInt64($parts[5].Substring(2), 16)
                $relroAligned = ($end % $pageSize) -eq 0
            }
            [pscustomobject]@{ Library = $entry.FullName; Load16K = $loadAligned; Relro16K = $relroAligned }
        }
    )
    $results | Format-Table -AutoSize
    if ($results | Where-Object { -not $_.Load16K -or -not $_.Relro16K }) {
        throw '64-bit native libraries failed the 16 KB LOAD/RELRO check.'
    }
    Write-Output "PASS: $($results.Count) 64-bit native libraries checked."
} finally {
    $archive.Dispose()
    if (Test-Path -LiteralPath $directory) { Remove-Item -LiteralPath $directory -Recurse -Force }
}