# Extension Runtime Test Results

**Last Updated:** 2026-02-24 11:35:45

This document contains the results of end-to-end runtime testing for all extensions.

**Test Flow:** Search → Novel Details → Chapter List

**Search Terms Used:** god, magic, I, is

## Summary

- **Total Extensions Tested:** 13
- **Fully Working (E2E):** 4 (30%)
- **Failed All Terms:** 9

## Extension Status

| Extension | Status | Search Term | Results | Novel | Chapters | Error |
|-----------|--------|-------------|---------|-------|----------|-------|
| JPMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTPSConnectionPool(host='jpmtl.com', port=443): Max retries exceeded with url: /v2/book/show/browse?query=is&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories= (Caused by NameResolutionError("HTTPSConnection(host='jpmtl.com', port=443): Failed to resolve 'jpmtl.com' ([Errno 11001] getaddrinfo failed)")) |
| RoyalRoad | ✅ PASS | `god` | 20 | God | 6 | - |
| BoxNovel | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |
| FanMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTP 404 |
| NovelBuddy | ✅ PASS | `god` | 48 | Reincarnation Of The Strongest Sword God (Web Novel) | 4533 | - |
| Ranobes | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |
| EmpireNovel | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTP 403 |
| ScribbleHub | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTP 500 |
| NovelBin | ✅ PASS | `god` | 31 | 100X Returns System: I Dominate the Age of Gods | 167 | - |
| LNMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | Script doesn't contain novel data |
| NovelFull | ✅ PASS | `god` | 31 | Super Insane Doctor of the Goddess | 3386 | - |
| WuxiaWorldSite | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |
| Neovel | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | 'NoneType' object has no attribute 'replace' |

## Detailed Results

### JPMTL

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: HTTPSConnectionPool(host='jpmtl.com', port=443): Max retries exceeded with url: /v2/book/show/browse?query=god&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories= (Caused by NameResolutionError("HTTPSConnection(host='jpmtl.com', port=443): Failed to resolve 'jpmtl.com' ([Errno 11001] getaddrinfo failed)"))
- URL: `https://jpmtl.com/v2/book/show/browse?query=god&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: HTTPSConnectionPool(host='jpmtl.com', port=443): Max retries exceeded with url: /v2/book/show/browse?query=magic&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories= (Caused by NameResolutionError("HTTPSConnection(host='jpmtl.com', port=443): Failed to resolve 'jpmtl.com' ([Errno 11001] getaddrinfo failed)"))
- URL: `https://jpmtl.com/v2/book/show/browse?query=magic&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 3 (search: `I`):**
- ❌ Search failed: HTTPSConnectionPool(host='jpmtl.com', port=443): Max retries exceeded with url: /v2/book/show/browse?query=I&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories= (Caused by NameResolutionError("HTTPSConnection(host='jpmtl.com', port=443): Failed to resolve 'jpmtl.com' ([Errno 11001] getaddrinfo failed)"))
- URL: `https://jpmtl.com/v2/book/show/browse?query=I&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 4 (search: `is`):**
- ❌ Search failed: HTTPSConnectionPool(host='jpmtl.com', port=443): Max retries exceeded with url: /v2/book/show/browse?query=is&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories= (Caused by NameResolutionError("HTTPSConnection(host='jpmtl.com', port=443): Failed to resolve 'jpmtl.com' ([Errno 11001] getaddrinfo failed)"))
- URL: `https://jpmtl.com/v2/book/show/browse?query=is&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

### RoyalRoad

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 20
- **Novel Title:** God
- **Novel URL:** https://www.royalroad.com/fiction/33681/god
- **Chapters Found:** 6
- **Search Time:** 4576ms
- **Details Time:** 638ms
- **Chapters Time:** 0ms

### BoxNovel

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=god&post_type=wp-manga`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: ('Connection aborted.', RemoteDisconnected('Remote end closed connection without response'))
- URL: `https://boxnovel.com/?s=magic&post_type=wp-manga`

**Attempt 3 (search: `I`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=I&post_type=wp-manga`

**Attempt 4 (search: `is`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=is&post_type=wp-manga`

### FanMTL

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: HTTP 404
- URL: `https://www.fanmtl.com/search?status=all&sort=views&q=god&page=1`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: HTTP 404
- URL: `https://www.fanmtl.com/search?status=all&sort=views&q=magic&page=1`

**Attempt 3 (search: `I`):**
- ❌ Search failed: HTTP 404
- URL: `https://www.fanmtl.com/search?status=all&sort=views&q=I&page=1`

**Attempt 4 (search: `is`):**
- ❌ Search failed: HTTP 404
- URL: `https://www.fanmtl.com/search?status=all&sort=views&q=is&page=1`

### NovelBuddy

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 48
- **Novel Title:** Reincarnation Of The Strongest Sword God (Web Novel)
- **Novel URL:** https://novelbuddy.com/novel/reincarnation-of-the-strongest-sword-god
- **Chapters Found:** 4533
- **Search Time:** 4681ms
- **Details Time:** 4328ms
- **Chapters Time:** 1150ms

### Ranobes

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 3 (search: `I`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 4 (search: `is`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

### EmpireNovel

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: HTTP 403
- URL: `https://www.empirenovel.com/?s=god&post_type=wp-manga`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: HTTP 403
- URL: `https://www.empirenovel.com/?s=magic&post_type=wp-manga`

**Attempt 3 (search: `I`):**
- ❌ Search failed: HTTP 403
- URL: `https://www.empirenovel.com/?s=I&post_type=wp-manga`

**Attempt 4 (search: `is`):**
- ❌ Search failed: HTTP 403
- URL: `https://www.empirenovel.com/?s=is&post_type=wp-manga`

### ScribbleHub

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: HTTP 500
- URL: `https://www.scribblehub.com/?s=god&post_type=fictionposts&paged=1`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: HTTP 500
- URL: `https://www.scribblehub.com/?s=magic&post_type=fictionposts&paged=1`

**Attempt 3 (search: `I`):**
- ❌ Search failed: HTTP 500
- URL: `https://www.scribblehub.com/?s=I&post_type=fictionposts&paged=1`

**Attempt 4 (search: `is`):**
- ❌ Search failed: HTTP 500
- URL: `https://www.scribblehub.com/?s=is&post_type=fictionposts&paged=1`

### NovelBin

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 31
- **Novel Title:** 100X Returns System: I Dominate the Age of Gods
- **Novel URL:** https://novelbin.me/novel-book/100x-returns-system-i-dominate-the-age-of-gods
- **Chapters Found:** 167
- **Search Time:** 4600ms
- **Details Time:** 4637ms
- **Chapters Time:** 2844ms

### LNMTL

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: Script doesn't contain novel data
- URL: `https://lnmtl.com/home`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: Script doesn't contain novel data
- URL: `https://lnmtl.com/home`

**Attempt 3 (search: `I`):**
- ❌ Search failed: Script doesn't contain novel data
- URL: `https://lnmtl.com/home`

**Attempt 4 (search: `is`):**
- ❌ Search failed: Script doesn't contain novel data
- URL: `https://lnmtl.com/home`

### NovelFull

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 31
- **Novel Title:** Super Insane Doctor of the Goddess
- **Novel URL:** https://novelfull.com/super-insane-doctor-of-the-goddess.html
- **Chapters Found:** 3386
- **Search Time:** 4301ms
- **Details Time:** 4310ms
- **Chapters Time:** 3599ms

### WuxiaWorldSite

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: No results found
- URL: `https://wuxiaworldsite.co/search/god&page=1`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: No results found
- URL: `https://wuxiaworldsite.co/search/magic&page=1`

**Attempt 3 (search: `I`):**
- ❌ Search failed: No results found
- URL: `https://wuxiaworldsite.co/search/I&page=1`

**Attempt 4 (search: `is`):**
- ❌ Search failed: No results found
- URL: `https://wuxiaworldsite.co/search/is&page=1`

### Neovel

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: 'NoneType' object has no attribute 'replace'
- URL: `https://neoread.neovel.io/V2/books/search?language=ALL&filter=0&name=god&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: 'NoneType' object has no attribute 'replace'
- URL: `https://neoread.neovel.io/V2/books/search?language=ALL&filter=0&name=magic&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000`

**Attempt 3 (search: `I`):**
- ❌ Search failed: 'NoneType' object has no attribute 'replace'
- URL: `https://neoread.neovel.io/V2/books/search?language=ALL&filter=0&name=I&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000`

**Attempt 4 (search: `is`):**
- ❌ Search failed: 'NoneType' object has no attribute 'replace'
- URL: `https://neoread.neovel.io/V2/books/search?language=ALL&filter=0&name=is&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000`

---

*This file is automatically generated by `test-extension-runtime.py`*
