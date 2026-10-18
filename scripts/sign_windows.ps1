$ErrorActionPreference = 'Stop'
$installer = Get-ChildItem -LiteralPath 'apps/desktop-client/build/packaging/installers' -Filter '*.exe'
if (@($installer).Count -ne 1) { throw 'Expected exactly one Windows installer.' }
if ($env:WINDOWS_SIGNING_POLICY -eq 'unsigned-pilot') {
    if ([string]::IsNullOrWhiteSpace($env:WINDOWS_UNSIGNED_PILOT_REASON)) { throw 'Unsigned pilot requires an explicit published reason.' }
    Write-Output 'Windows candidate is an explicitly designated unsigned pilot.'
    exit 0
}
if ($env:WINDOWS_SIGNING_POLICY -ne 'authenticode' -or !$env:WINDOWS_CERT_SHA256 -or !$env:WINDOWS_CERT_BASE64 -or !$env:WINDOWS_CERT_PASSWORD) {
    throw 'Authenticode requires the certificate, password and trusted SHA-256; otherwise explicitly select unsigned-pilot.'
}
$pfxPath = Join-Path $env:RUNNER_TEMP 'securelan-windows-signing.pfx'
$imported = $null
try {
    [IO.File]::WriteAllBytes($pfxPath, [Convert]::FromBase64String($env:WINDOWS_CERT_BASE64))
    $password = ConvertTo-SecureString $env:WINDOWS_CERT_PASSWORD -AsPlainText -Force
    $imported = Import-PfxCertificate -FilePath $pfxPath -CertStoreLocation 'Cert:\CurrentUser\My' -Password $password
    $certificate = @($imported | Where-Object HasPrivateKey)
    if ($certificate.Count -ne 1) { throw 'Expected one signing identity.' }
    $actualHash = [BitConverter]::ToString($certificate[0].GetCertHash([Security.Cryptography.HashAlgorithmName]::SHA256)).Replace('-', '')
    if ($actualHash -ne $env:WINDOWS_CERT_SHA256.Replace(':', '')) { throw 'Unexpected Windows signing identity.' }
    $signed = Set-AuthenticodeSignature -LiteralPath $installer.FullName -Certificate $certificate[0] -HashAlgorithm SHA256 -TimestampServer 'http://timestamp.digicert.com'
    if ($signed.Status -ne 'Valid' -or !$signed.TimeStamperCertificate) { throw 'Installer Authenticode signature/timestamp is invalid.' }
} finally {
    if (Test-Path -LiteralPath $pfxPath) { Remove-Item -LiteralPath $pfxPath -Force }
    foreach ($certificate in @($imported)) { Remove-Item -LiteralPath (Join-Path 'Cert:\CurrentUser\My' $certificate.Thumbprint) -ErrorAction SilentlyContinue }
}
