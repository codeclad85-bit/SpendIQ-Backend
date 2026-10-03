$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$jdk = 'C:\Program Files\Java\jdk-25.0.4.1'
if (!(Test-Path "$jdk\bin\java.exe")) { throw 'JDK 25 folder missing. Update $jdk in this script.' }
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($mavenCommand) { $mavenPath = $mavenCommand.Source }
else {
    $cache = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
    $found = Get-ChildItem $cache -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$found) { throw 'Maven wrapper files were not in Local History, and cached Maven was not found. Send this error for the next setup step.' }
    $mavenPath = $found.FullName
}
& $mavenPath spring-boot:run
exit $LASTEXITCODE
