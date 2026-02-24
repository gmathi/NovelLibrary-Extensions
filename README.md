# NovelLibrary-Extensions

Extensions for NovelLibrary Android app.

---

## 📊 Current Status

**Total Extensions:** 13

### ✅ Tested & Working (4 - 31%)
- NovelFull
- NovelBuddy
- NovelBin
- RoyalRoad

### ❌ Tested & Not Working (9 - 69%)
- ScribbleHub (HTTP 500)
- EmpireNovel (HTTP 403)
- LNMTL (Parsing issue)
- BoxNovel (No results found)
- FanMTL (HTTP 404)
- Ranobes (No results found)
- WuxiaWorldSite (No results found)
- Neovel (Parsing error)
- JPMTL (DNS resolution failed)

See [docs/EXTENSIONS-STATUS.md](docs/EXTENSIONS-STATUS.md) for detailed test results with URLs and error information.

---

## 📚 Documentation

- **[docs/EXTENSIONS-STATUS.md](docs/EXTENSIONS-STATUS.md)** - Current status of all extensions with automated test results
- **[docs/TESTING-GUIDE.md](docs/TESTING-GUIDE.md)** - Complete guide for testing extensions (runtime, structure, manual)
- **[docs/HOW-TO-CREATE-EXTENSIONS.md](docs/HOW-TO-CREATE-EXTENSIONS.md)** - Step-by-step guide for creating new extensions

---

## 🚀 Quick Start

### Testing Extensions

```bash
# Test runtime functionality (search, details, chapters) - runs in parallel
python test-extension-runtime.py

# Validate extension structure (files, manifests)
pwsh validate-extension-structure.ps1
```

Results are saved to `docs/EXTENSIONS-STATUS.md`.

### Building Extensions

```bash
# Build all extensions
./gradlew assembleRelease

# Build specific extension
./gradlew :extensions:individual:en:novelfull:assembleRelease
```

---

## 🛠️ Creating Extensions

For detailed instructions on creating new extensions, see [docs/HOW-TO-CREATE-EXTENSIONS.md](docs/HOW-TO-CREATE-EXTENSIONS.md).

### Quick Overview

1. Identify the source website URLs (search, novel details, chapters)
2. Copy an existing extension folder as a template
3. Update package names and configuration
4. Implement the search, details, and chapter fetching logic
5. Test using the automated test scripts
6. Build and install on device for final testing

See the full guide for step-by-step instructions with screenshots.
