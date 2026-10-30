#requires -Version 5.1
<#
.SYNOPSIS
Prepares a dedicated GPG key for Maven Central release signing.

.DESCRIPTION
This script never stores or prints a passphrase. GPG's own pinentry prompts
for the passphrase during key generation. The private key is exported outside
the repository, under the user's profile, so it cannot be committed by accident.

The script:
  1. Locates or installs GnuPG.
  2. Optionally starts interactive key generation.
  3. Finds the release key by email.
  4. Exports public and private ASCII-armored key files.
  5. Uploads the public key to public keyservers.
  6. Prints the GitHub Actions secret setup commands.

.PARAMETER Name
GPG user ID name. Default: UnitSeek Release.

.PARAMETER Email
GPG user ID email. Default: unitseek@outlook.com.

.PARAMETER Fingerprint
Use an existing key fingerprint instead of discovering it by email.

.PARAMETER OutputDirectory
Directory for exported key files. Default: ~/.knitid-release.

.PARAMETER SkipKeyGeneration
Do not offer to generate a new key.

.PARAMETER SkipUpload
Do not send the public key to keyservers.

.EXAMPLE
pwsh -File scripts/setup-gpg-release.ps1

.EXAMPLE
pwsh -File scripts/setup-gpg-release.ps1 -Fingerprint 0123456789ABCDEF...
#>
[CmdletBinding()]
param(
    [string]$Name = 'UnitSeek Release',
    [string]$Email = 'unitseek@outlook.com',
    [string]$Fingerprint,
    [string]$OutputDirectory = (Join-Path $env:USERPROFILE '.knitid-release'),
    [switch]$SkipKeyGeneration,
    [switch]$SkipUpload
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Resolve-Gpg {
    $command = Get-Command gpg -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        'C:\Program Files\GnuPG\bin\gpg.exe',
        'C:\Program Files (x86)\GnuPG\bin\gpg.exe'
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw 'GnuPG not found. Install it with: winget install --id GnuPG.GnuPG -e'
}

function Find-SecretKeyFingerprint {
    param(
        [string]$Gpg,
        [string]$EmailAddress
    )

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    $lines = & $Gpg --list-secret-keys --with-colons 2>$null
    $ErrorActionPreference = $previousErrorAction
    $currentFingerprint = $null
    $found = @()

    foreach ($line in $lines) {
        $fields = $line -split ':'
        switch ($fields[0]) {
            'sec' {
                $currentFingerprint = $null
            }
            'fpr' {
                if ($null -eq $currentFingerprint -and $fields.Count -gt 9) {
                    $currentFingerprint = $fields[9]
                }
            }
            'uid' {
                if ($null -ne $currentFingerprint -and $fields.Count -gt 9 -and
                    $fields[9] -like "*<$EmailAddress>*") {
                    $found += $currentFingerprint
                }
            }
        }
    }

    return $found | Select-Object -Unique
}

function Invoke-Checked {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
    }
}

$gpg = Resolve-Gpg
Write-Host "GnuPG: $gpg"
& $gpg --version | Select-Object -First 1

if (-not $Fingerprint) {
    $existing = @(Find-SecretKeyFingerprint -Gpg $gpg -EmailAddress $Email)

    if ($existing.Count -gt 1) {
        Write-Host 'Multiple secret keys match this email:'
        $existing | ForEach-Object { Write-Host "  $_" }
        throw 'Pass -Fingerprint to select one of the keys above.'
    }

    if ($existing.Count -eq 1) {
        $Fingerprint = $existing[0]
    }
    elseif (-not $SkipKeyGeneration) {
        Write-Host ''
        Write-Host 'No release key found. GPG will now ask for key type, size, expiry, identity and passphrase.'
        Write-Host 'Recommended choices: RSA and RSA, 4096 bits, expiry 0, then your release identity.'
        Write-Host ''
        Invoke-Checked -FilePath $gpg -Arguments @('--full-generate-key')

        $existing = @(Find-SecretKeyFingerprint -Gpg $gpg -EmailAddress $Email)
        if ($existing.Count -eq 0) {
            throw "No secret key found for $Email after generation. Pass -Fingerprint explicitly."
        }
        $Fingerprint = $existing[0]
    }
    else {
        throw 'No release key found and -SkipKeyGeneration was specified.'
    }
}

$Fingerprint = $Fingerprint.Trim()
Write-Host ''
Write-Host "Release fingerprint: $Fingerprint"
& $gpg --list-keys --fingerprint $Fingerprint

if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
}

$privateKeyFile = Join-Path $OutputDirectory 'knitid-release-private.asc'
$publicKeyFile = Join-Path $OutputDirectory 'knitid-release-public.asc'

Write-Host ''
Write-Host "Exporting keys to: $OutputDirectory"
Invoke-Checked -FilePath $gpg -Arguments @(
    '--batch', '--yes', '--armor', '--output', $privateKeyFile,
    '--export-secret-keys', $Fingerprint
)
Invoke-Checked -FilePath $gpg -Arguments @(
    '--batch', '--yes', '--armor', '--output', $publicKeyFile,
    '--export', $Fingerprint
)

if (-not $SkipUpload) {
    Write-Host ''
    Write-Host 'Uploading public key to keyservers...'
    Invoke-Checked -FilePath $gpg -Arguments @(
        '--send-keys', '--keyserver', 'hkps://keyserver.ubuntu.com', $Fingerprint
    )
    Invoke-Checked -FilePath $gpg -Arguments @(
        '--send-keys', '--keyserver', 'hkps://keys.openpgp.org', $Fingerprint
    )
}

Write-Host ''
Write-Host '============================================================'
Write-Host 'GPG preparation complete.'
Write-Host "Fingerprint: $Fingerprint"
Write-Host "Private key: $privateKeyFile"
Write-Host "Public key:  $publicKeyFile"
Write-Host ''
Write-Host 'Next: add these GitHub Environment secrets under "release":'
Write-Host '  GPG_PRIVATE_KEY       = contents of knitid-release-private.asc'
Write-Host '  MAVEN_GPG_PASSPHRASE  = the passphrase chosen during key generation'
Write-Host '  MAVEN_CENTRAL_USERNAME'
Write-Host '  MAVEN_CENTRAL_PASSWORD'
Write-Host ''
Write-Host 'If GitHub CLI is authenticated, you can use:'
Write-Host ('  gh secret set GPG_PRIVATE_KEY --env release --body-file "' + $privateKeyFile + '"')
Write-Host '  gh secret set MAVEN_GPG_PASSPHRASE --env release'
Write-Host ''
Write-Host 'After the secret is configured, delete the private key file:'
Write-Host ('  Remove-Item "' + $privateKeyFile + '"')
Write-Host '============================================================'
