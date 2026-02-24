# Script to check all extensions for basic structure

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Extension Structure Check" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

$extensionsDir = "extensions/individual/en"
$total = 0
$valid = 0
$invalid = 0

# Get all extension directories
$extensions = Get-ChildItem -Path $extensionsDir -Directory

foreach ($ext in $extensions) {
    $total++
    $extName = $ext.Name
    
    Write-Host "[$total] Checking: $extName" -ForegroundColor Yellow
    
    # Check for required files
    $hasManifest = Test-Path "$($ext.FullName)/AndroidManifest.xml"
    $hasBuildGradle = Test-Path "$($ext.FullName)/build.gradle"
    
    # Check for source files
    $sourceFiles = Get-ChildItem -Path "$($ext.FullName)/src" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue
    $hasSource = $sourceFiles.Count -gt 0
    
    # Report status
    if ($hasManifest -and $hasBuildGradle -and $hasSource) {
        Write-Host "  ✓ Structure: VALID" -ForegroundColor Green
        Write-Host "    - AndroidManifest.xml: ✓"
        Write-Host "    - build.gradle: ✓"
        Write-Host "    - Source files: ✓ ($($sourceFiles.Count) file(s))"
        $valid++
    } else {
        Write-Host "  ✗ Structure: INVALID" -ForegroundColor Red
        Write-Host "    - AndroidManifest.xml: $(if ($hasManifest) { '✓' } else { '✗' })"
        Write-Host "    - build.gradle: $(if ($hasBuildGradle) { '✓' } else { '✗' })"
        Write-Host "    - Source files: $(if ($hasSource) { "✓ ($($sourceFiles.Count))" } else { '✗' })"
        $invalid++
    }
    Write-Host ""
}

Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Summary" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host "Total Extensions: $total"
Write-Host "Valid: $valid" -ForegroundColor Green
Write-Host "Invalid: $invalid" -ForegroundColor $(if ($invalid -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($invalid -eq 0) {
    Write-Host "✓ All extensions have valid structure!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ Some extensions have structural issues" -ForegroundColor Red
    exit 1
}
