# Extension Testing Guide

This guide explains how to test NovelLibrary extensions to ensure they work correctly.

## Table of Contents

- [Overview](#overview)
- [Testing Tools](#testing-tools)
- [Runtime Testing](#runtime-testing)
- [Structure Validation](#structure-validation)
- [Manual Testing](#manual-testing)
- [Troubleshooting](#troubleshooting)

---

## Overview

Extensions need to be tested at multiple levels:

1. **Runtime Testing** - Verify the extension can search, fetch novel details, and load chapters
2. **Structure Validation** - Ensure all required files and configurations are present
3. **Manual Testing** - Test the extension in the actual NovelLibrary app

---

## Testing Tools

### 1. Runtime Test Script

**File:** `test-extension-runtime.py`

**Purpose:** Tests the core functionality of extensions (search, details, chapters) using multiple search terms.

**Features:**
- Parallel testing of all extensions
- Rate limiting (1 second between requests to same host)
- Multiple search term fallback
- Detailed error reporting with URLs
- Generates `docs/EXTENSIONS-STATUS.md` report

**Usage:**
```bash
# Test all extensions
python test-extension-runtime.py

# Test a single extension
python test-extension-runtime.py NovelFull

# Test with JSON output (useful for CI/CD)
python test-extension-runtime.py NovelFull --json

# List available extensions
python test-extension-runtime.py --list

# Show help
python test-extension-runtime.py --help
```

**Output:**
- Console output with real-time test progress
- `docs/EXTENSIONS-STATUS.md` - Detailed markdown report (only when testing all extensions)

**Testing Single Extensions:**
When testing a single extension, the status file is not updated. This allows you to quickly test individual extensions during development without affecting the overall status report.

### 2. Structure Validation Script

**File:** `validate-extension-structure.ps1`

**Purpose:** Validates that extensions have all required files and proper configuration.

**Usage:**
```powershell
pwsh validate-extension-structure.ps1
```

---

## Runtime Testing

### What Gets Tested

The runtime test performs an end-to-end flow:

1. **Search Test**
   - Sends search query to the extension's search URL
   - Verifies results are returned
   - Extracts first novel from results

2. **Novel Details Test**
   - Fetches the novel details page
   - Checks for description, author, and genres
   - Validates page structure

3. **Chapter List Test**
   - Fetches chapter list (via AJAX if needed)
   - Counts available chapters
   - Verifies chapter links are valid

### Search Terms

The test uses multiple search terms to avoid edge cases:
- `god`
- `magic`
- `I`
- `is`

If one term fails, the test tries the next term. An extension passes if ANY search term succeeds.

### Understanding Results

**✅ PASS** - Extension completed all three tests successfully
- Search returned results
- Novel details loaded correctly
- Chapter list was fetched

**❌ FAIL** - Extension failed with all search terms
- Check the error message and URL
- Common issues: HTTP errors (403, 500), parsing failures, site changes

### Example Output

```
✅ NovelFull - PASS
   Search Term: 'god'
   Search Results: 31
   Novel: Super Insane Doctor of the Goddess
   Chapters: 3386

❌ ScribbleHub - FAIL
   Tried 4 search terms: 'god', 'magic', 'I', 'is'
   Search Error: HTTP 500
   URL: https://www.scribblehub.com/?s=god&post_type=fictionposts&paged=1
```

---

## Structure Validation

### Required Files

Each extension must have:

1. **AndroidManifest.xml** - Defines the extension package and metadata
2. **build.gradle** - Build configuration
3. **Source file** - Kotlin implementation (e.g., `NovelFull.kt`)
4. **Resources** - Icons and drawables

### Validation Checks

The structure validator checks:
- All required files exist
- Package names match folder structure
- Build configuration is correct
- Icons are present

---

## Manual Testing

### Prerequisites

1. Android Studio installed
2. NovelLibrary app installed on device/emulator
3. Extension built and installed

### Steps

1. **Build the Extension**
   ```bash
   ./gradlew :extensions:individual:en:novelfull:assembleRelease
   ```

2. **Install on Device**
   - Locate the APK in `extensions/individual/en/[extension]/build/outputs/apk/release/`
   - Install via ADB or file manager

3. **Test in NovelLibrary**
   - Open NovelLibrary app
   - Go to Browse → Sources
   - Enable the extension
   - Test search functionality
   - Open a novel and verify details
   - Load chapters and verify content

### What to Test

- [ ] Search returns relevant results
- [ ] Novel details display correctly (title, author, cover, description)
- [ ] Genres/tags are shown
- [ ] Chapter list loads completely
- [ ] Chapters open and display content
- [ ] Images load (if applicable)
- [ ] Navigation works (next/previous chapter)

---

## Troubleshooting

### Common Issues

#### HTTP 403 Forbidden
**Cause:** Website blocking automated requests or requiring authentication

**Solutions:**
- Check if the website has changed their anti-bot protection
- Update user agent in the extension
- Add necessary headers or cookies

#### HTTP 500 Server Error
**Cause:** Website is down or having issues

**Solutions:**
- Wait and retry later
- Check if the website is accessible in a browser
- Verify the URL format hasn't changed

#### No Results Found
**Cause:** Search selector changed or search term too specific

**Solutions:**
- Try different search terms
- Inspect the website HTML to verify selectors
- Update selectors in the extension code

#### Parsing Errors
**Cause:** Website HTML structure changed

**Solutions:**
- Inspect the current website HTML
- Update CSS selectors in the extension
- Check for JavaScript-rendered content (may need different approach)

### Debugging Tips

1. **Check the URL**
   - The test report includes the failing URL
   - Open it in a browser to see what's returned

2. **Inspect HTML**
   - Use browser DevTools to check element selectors
   - Verify the CSS selectors match current structure

3. **Test Manually**
   - Use tools like Postman or curl to test requests
   - Check response headers and content

4. **Review Extension Code**
   - Compare with working extensions
   - Verify base URL is correct
   - Check selector syntax

### Getting Help

If you're stuck:
1. Check `docs/EXTENSIONS-STATUS.md` for current status
2. Review the extension's source code
3. Compare with similar working extensions
4. Check if the source website is accessible

---

## Best Practices

### When Creating Extensions

1. **Use Robust Selectors**
   - Prefer class names over complex CSS paths
   - Use multiple fallback selectors when possible

2. **Handle Errors Gracefully**
   - Check for null/empty results
   - Provide meaningful error messages

3. **Test Thoroughly**
   - Run runtime tests before committing
   - Test with multiple search terms
   - Verify on actual device

4. **Document Changes**
   - Note any website-specific quirks
   - Document required headers or special handling

### When Updating Extensions

1. **Verify Website Changes**
   - Check if the website structure changed
   - Test the old selectors first

2. **Update Selectors Carefully**
   - Test each selector individually
   - Ensure backward compatibility if possible

3. **Run Full Test Suite**
   - Runtime tests
   - Structure validation
   - Manual testing on device

---

## Continuous Testing

### Automated Testing

Run tests regularly to catch breaking changes:

```bash
# Weekly or after website updates
python test-extension-runtime.py
```

### Monitoring

- Check `docs/EXTENSIONS-STATUS.md` for current status
- Monitor for HTTP errors (403, 500)
- Watch for parsing failures

### Maintenance

- Update extensions when websites change
- Remove extensions for permanently dead sites
- Document known issues in status file

---

*Last Updated: 2026-02-24*
