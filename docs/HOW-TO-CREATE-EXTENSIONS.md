# How to Create Extensions for NovelLibrary

This guide will walk you through creating a new extension for the NovelLibrary Android app.

## Prerequisites

- Android Studio installed
- Basic knowledge of Kotlin
- Understanding of web scraping concepts (HTML parsing with Jsoup)
- Familiarity with HTTP requests

## Overview

Extensions allow NovelLibrary to fetch novels from different websites. Each extension implements methods to:
- Search for novels
- Fetch novel details
- Retrieve chapter lists
- Parse chapter content

## Step 1: Analyze the Target Website

Before creating an extension, identify these key URLs:

1. **Search URL** - Takes a search query parameter and returns results
2. **Novel Details URL** - Shows information about a specific novel
3. **Chapter List URL** - Returns all chapters for a novel
4. **Chapter Content URL** - Contains the actual chapter text

Example for a site `example.com`:
- Search: `https://example.com/search?q=novel+name`
- Novel: `https://example.com/novel/novel-slug`
- Chapters: `https://example.com/novel/novel-slug/chapters`
- Chapter: `https://example.com/novel/novel-slug/chapter-1`

## Step 2: Create Extension Directory Structure

Navigate to `extensions/individual/en/` (or appropriate language folder) and create your extension folder.

### Directory Structure

```
extensions/individual/en/yoursite/
├── AndroidManifest.xml
├── build.gradle
├── res/
│   ├── mipmap-*/
│   │   └── ic_launcher.png
│   └── values/
│       └── ic_launcher_background.xml
└── src/
    └── io/github/gmathi/novellibrary/extension/en/yoursite/
        └── YourSite.kt
```

### Copy from Existing Extension

The easiest way is to copy an existing extension:

```bash
cp -r extensions/individual/en/boxnovel extensions/individual/en/yoursite
```

## Step 3: Configure build.gradle

Edit `extensions/individual/en/yoursite/build.gradle`:

```gradle
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

ext {
    extName = 'YourSite'           // Display name
    pkgNameSuffix = 'en.yoursite'  // Package suffix
    extClass = '.YourSite'         // Main class name
    extVersionCode = 1             // Version code (increment on updates)
    libVersion = '1.0'             // Library version
}

android {
    namespace = 'io.github.gmathi.novellibrary.extension.en.yoursite'
}

apply from: "$rootDir/common.gradle"
```

## Step 4: Update AndroidManifest.xml

The manifest is minimal:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest package="io.github.gmathi.novellibrary.extension" />
```

## Step 5: Rename Source Files

1. Rename the folder: `src/io/github/gmathi/novellibrary/extension/en/boxnovel/` → `yoursite/`
2. Rename the Kotlin file: `BoxNovel.kt` → `YourSite.kt`

## Step 6: Implement the Extension Class

Edit `YourSite.kt`:

```kotlin
package io.github.gmathi.novellibrary.extension.en.yoursite

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class YourSite : ParsedHttpSource() {

    override val baseUrl: String = "https://yoursite.com"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true
    override val name: String = "Your Site"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    // Implement required methods below...

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}
```

### Implement Search Functionality

```kotlin
override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val url = "$baseUrl/search?q=$encodedQuery&page=$page"
    return GET(url, headers)
}

override fun searchNovelsSelector() = "div.novel-item"

override fun searchNovelsFromElement(element: Element): Novel {
    val titleElement = element.selectFirst("h3.title a")!!
    val novel = Novel(titleElement.text(), titleElement.attr("abs:href"), id)
    novel.imageUrl = element.selectFirst("img")?.attr("abs:src")
    novel.rating = element.selectFirst("span.rating")?.text()
    return novel
}

override fun searchNovelsNextPageSelector() = "a.next-page"
```

### Implement Novel Details

```kotlin
override fun novelDetailsParse(novel: Novel, document: Document): Novel {
    novel.imageUrl = document.selectFirst("div.novel-cover img")?.attr("abs:src")
    novel.longDescription = document.selectFirst("div.description")?.text()
    novel.rating = document.selectFirst("span.rating-value")?.text()
    novel.authors = document.select("div.author a").map { it.text() }
    novel.genres = document.select("div.genres a").map { it.text() }
    return novel
}
```

### Implement Chapter List

```kotlin
override fun chapterListRequest(novel: Novel): Request {
    return GET("${novel.url}/chapters", headers)
}

override fun chapterListSelector() = "ul.chapter-list li a"

override fun chapterFromElement(element: Element) = 
    WebPage(element.absUrl("href"), element.text())

override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
    val document = response.asJsoup()
    return document.select(chapterListSelector()).mapIndexed { index, element ->
        val chapter = chapterFromElement(element)
        chapter.orderId = index.toLong()
        chapter
    }
}
```

### Stub Unused Methods

If you don't support certain features, stub them:

```kotlin
override fun latestUpdatesRequest(page: Int): Request = throw Exception(NOT_USED)
override fun latestUpdatesSelector(): String = throw Exception(NOT_USED)
override fun latestUpdatesFromElement(element: Element): Novel = throw Exception(NOT_USED)
override fun latestUpdatesNextPageSelector(): String = throw Exception(NOT_USED)

override fun popularNovelsRequest(page: Int): Request = throw Exception(NOT_USED)
override fun popularNovelsSelector(): String = throw Exception(NOT_USED)
override fun popularNovelsFromElement(element: Element): Novel = throw Exception(NOT_USED)
override fun popularNovelNextPageSelector(): String = throw Exception(NOT_USED)
```

## Step 7: Register Extension in settings.gradle

Add your extension to the root `settings.gradle`:

```gradle
include ':extensions:individual:en:yoursite'
```

## Step 8: Build and Test

### Build the Extension

```bash
# Build all extensions
./gradlew assembleRelease

# Build specific extension
./gradlew :extensions:individual:en:yoursite:assembleRelease
```

The APK will be generated in:
`extensions/individual/en/yoursite/build/outputs/apk/release/`

### Test the Extension

Use the testing scripts:

```bash
# Test runtime functionality
python test-extension-runtime.py

# Validate structure
pwsh validate-extension-structure.ps1
```

## Tips and Best Practices

### CSS Selectors

Use browser DevTools to find the right selectors:
1. Right-click element → Inspect
2. Note the class names and structure
3. Test selectors in browser console: `document.querySelector("your.selector")`

### Error Handling

Always use safe calls (`?.`) when selecting elements:

```kotlin
novel.imageUrl = element.selectFirst("img")?.attr("abs:src")
```

### Cloudflare Protection

If the site uses Cloudflare, use the cloudflare client:

```kotlin
override val client: OkHttpClient
    get() = network.cloudflareClient
```

### POST Requests

For AJAX chapter loading:

```kotlin
override fun chapterListRequest(novel: Novel): Request {
    val formBody = FormBody.Builder()
        .add("action", "get_chapters")
        .add("novel_id", novel.externalNovelId!!)
        .build()
    return POST("${novel.url}/ajax/chapters", headers, formBody)
}
```

### Metadata Storage

Store additional data in the novel's metadata:

```kotlin
novel.metadata["PostId"] = document.select("input#post-id").attr("value")
novel.externalNovelId = postId
```

### Debugging

Add logging to help debug:

```kotlin
println("DEBUG: Search URL = $url")
println("DEBUG: Found ${elements.size} results")
```

## Common Issues

### Issue: No search results found
- Check if the selector matches the HTML structure
- Verify the search URL format
- Check if the site requires authentication

### Issue: Images not loading
- Use `attr("abs:src")` instead of `attr("src")` for absolute URLs
- Check if images require referer header

### Issue: Chapters in wrong order
- Use `.reversed()` if chapters are listed newest-first
- Set `orderId` to maintain correct order

### Issue: Cloudflare blocking requests
- Use `network.cloudflareClient` instead of default client
- Add proper User-Agent and Referer headers

## Example: Complete Extension

See `extensions/individual/en/boxnovel/` for a complete working example.

## Resources

- [Jsoup Documentation](https://jsoup.org/) - HTML parsing
- [OkHttp Documentation](https://square.github.io/okhttp/) - HTTP client
- [CSS Selectors Reference](https://www.w3schools.com/cssref/css_selectors.asp)

## Getting Help

- Check existing extensions for similar site structures
- Review [TESTING-GUIDE.md](TESTING-GUIDE.md) for testing procedures
- Check [EXTENSIONS-STATUS.md](EXTENSIONS-STATUS.md) for working examples
