[CmdletBinding()]
param(
    [Parameter(Mandatory, Position=0)]
    [ValidateSet('1.20.1-forge', '1.20.1-fabric', '1.21.1-neoforge', '1.21.1-fabric', '26.2-fabric', '26.2-neoforge', '26.3-fabric', '26.3-neoforge')]
    [string]$Target,
    [string[]]$Tasks = @('test', 'build'),
    [string]$GradleUserHome
)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
$dependency = Join-Path (Split-Path $repository -Parent) 'stacks-not-slots'
if ($Target -ne '1.21.1-neoforge') {
    $repository = Join-Path $repository "ports/$Target"
    $dependency = Join-Path $dependency "ports/$Target"
}
foreach ($project in @($repository, $dependency)) {
    if (-not (Test-Path -LiteralPath (Join-Path $project 'gradlew.bat'))) {
        throw "Missing project: $project. Keep the matching stacks-not-slots repository beside bundled-not-siloed."
    }
}
$common = @('--console=plain', '--max-workers=2')
if ($GradleUserHome) { $common += @('--gradle-user-home', $GradleUserHome) }
# Older Loom resolves composite mod metadata before scheduling dependency tasks.
& (Join-Path $dependency 'gradlew.bat') -p $dependency @common test build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $repository 'gradlew.bat') -p $repository @common @Tasks
exit $LASTEXITCODE
