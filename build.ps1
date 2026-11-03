[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$PomFile = ".\pom.xml",
    [string]$RuntimeFolder = "C:\ETO-PRACTICE\appassembler"
)

if (-not (Test-Path $PomFile)) {
    throw "POM file not found: $PomFile"
}

$content = Get-Content $PomFile -Raw

if ($content -notmatch '(?s)(<version>)(\d+)\.(\d+)\.(\d+)(</version>)') {
    throw "Could not locate a version tag in major.minor.patch format."
}

$major = [int]$matches[2]
$minor = [int]$matches[3]
$patch = [int]$matches[4] + 1

$newVersion = "$major.$minor.$patch"
$buildTime = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssK")

$buildInfoFile = "src/main/java/com/surftools/utils/BuildInfo.java" 
if ($PSCmdlet.ShouldProcess($buildInfoFile, "Write BuildInfo.java file, version: $newVersion, time: $buildTime")) {
@"
package com.surftools.utils;

public final class BuildInfo {
    public static final String VERSION = "$newVersion";
    public static final String BUILD_TIME = "$buildTime";

    private BuildInfo() {}
}
"@ | Set-Content $buildInfoFile
}

$match = [regex]::Match(
    $content,
    '(?s)(<version>)(\d+\.\d+\.\d+)(</version>)'
)

if (-not $match.Success) {
    throw "Could not find project version"
}

$newContent =
    $content.Substring(0, $match.Index) +
    "<version>$newVersion</version>" +
    $content.Substring($match.Index + $match.Length)

if ($PSCmdlet.ShouldProcess($PomFile, "Update version to $newVersion")) {
    Set-Content -Path $PomFile -Value $newContent -NoNewline
}

if ($PSCmdlet.ShouldProcess("Maven Project", "mvn package")) {
    & mvn package
    if ($LASTEXITCODE -ne 0) {
        throw "mvn package failed."
    }
}

$jar = Get-ChildItem ".\target" -Filter "*jar-with-dependencies*.jar" |
       Sort-Object LastWriteTime -Descending |
       Select-Object -First 1

if (-not $jar) {
    $jar = Get-ChildItem ".\target" -Filter "*.jar" |
           Sort-Object LastWriteTime -Descending |
           Select-Object -First 1
}

if (-not $jar) {
    throw "No JAR found in target folder."
}

if ($PSCmdlet.ShouldProcess($jar.FullName, "Copy to $RuntimeFolder")) {
    New-Item -ItemType Directory -Force -Path $RuntimeFolder | Out-Null
    Copy-Item $jar.FullName $RuntimeFolder -Force
}

Write-Host "New version: $newVersion"