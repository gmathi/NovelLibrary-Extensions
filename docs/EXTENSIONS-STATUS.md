# Extension Runtime Test Results

**Last Updated:** 2026-02-28 01:08:45

This document contains the results of end-to-end runtime testing for all extensions.

**Test Flow:** Search → Novel Details → Chapter List

**Search Terms Used:** god, magic, I, is

## Summary

- **Total Extensions Tested:** 13
- **Fully Working (E2E):** 6 (46%)
- **Failed All Terms:** 7

## Extension Status

| Extension | Status | Search Term | Results | Novel | Chapters | Error |
|-----------|--------|-------------|---------|-------|----------|-------|
| RoyalRoad | ✅ PASS | `god` | 20 | God | 6 | - |
| PurrFiction | ✅ PASS | `god` | 25 | Gods End — The Chosen Who Was Never Meant to Exist | 24 | - |
| BoxNovel | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |
| JPMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | Failed to parse JSON: Expecting value: line 1 column 1 (char 0) |
| EmpireNovel | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTP 403 |
| FanMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | HTTP 404 |
| NovelFull | ✅ PASS | `god` | 31 | Journey To Become A True God | 4379 | - |
| LNMTL | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | Script doesn't contain novel data |
| ScribbleHub | ✅ PASS | `god` | 25 | God Of Ice and snow [BL] | 1 | - |
| NovelBin | ✅ PASS | `god` | 31 | 100X Returns System: I Dominate the Age of Gods | 172 | - |
| WuxiaWorldSite | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |
| NovelBuddy | ✅ PASS | `god` | 48 | Reincarnation Of The Strongest Sword God (Web Novel) | 4533 | - |
| Ranobes | ❌ FAIL | `god`, `magic`, `I`, `is` | - | - | - | No results found |

## Detailed Results

### RoyalRoad

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 20
- **Novel Title:** God
- **Novel URL:** https://www.royalroad.com/fiction/33681/god
- **Chapters Found:** 6
- **Search Time:** 844ms
- **Details Time:** 236ms
- **Chapters Time:** 0ms

### PurrFiction

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 25
- **Novel Title:** Gods End — The Chosen Who Was Never Meant to Exist
- **Novel URL:** https://purrfiction.io/V1/page/book?bookId=1147&language=EN
- **Chapters Found:** 24
- **Search Time:** 747ms
- **Details Time:** 665ms
- **Chapters Time:** 958ms

### BoxNovel

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=god&post_type=wp-manga`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=magic&post_type=wp-manga`

**Attempt 3 (search: `I`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=I&post_type=wp-manga`

**Attempt 4 (search: `is`):**
- ❌ Search failed: No results found
- URL: `https://boxnovel.com/?s=is&post_type=wp-manga`

### JPMTL

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: ('Connection aborted.', RemoteDisconnected('Remote end closed connection without response'))
- URL: `https://jpmtl.com/v2/book/show/browse?query=god&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: Failed to parse JSON: Expecting value: line 1 column 1 (char 0)
- URL: `https://jpmtl.com/v2/book/show/browse?query=magic&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 3 (search: `I`):**
- ❌ Search failed: Failed to parse JSON: Expecting value: line 1 column 1 (char 0)
- URL: `https://jpmtl.com/v2/book/show/browse?query=I&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

**Attempt 4 (search: `is`):**
- ❌ Search failed: Failed to parse JSON: Expecting value: line 1 column 1 (char 0)
- URL: `https://jpmtl.com/v2/book/show/browse?query=is&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=`

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

### NovelFull

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 31
- **Novel Title:** Journey To Become A True God
- **Novel URL:** https://novelfull.com/journey-to-become-a-true-god.html
- **Chapters Found:** 4379
- **Search Time:** 417ms
- **Details Time:** 814ms
- **Chapters Time:** 1812ms

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

### ScribbleHub

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 25
- **Novel Title:** God Of Ice and snow [BL]
- **Novel URL:** https://www.scribblehub.com/series/24501/god-of-ice-and-snow-bl/
- **Chapters Found:** 1
- **Search Time:** 1558ms
- **Details Time:** 1044ms
- **Chapters Time:** 564ms

### NovelBin

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 31
- **Novel Title:** 100X Returns System: I Dominate the Age of Gods
- **Novel URL:** https://novelbin.me/novel-book/100x-returns-system-i-dominate-the-age-of-gods
- **Chapters Found:** 172
- **Search Time:** 995ms
- **Details Time:** 1789ms
- **Chapters Time:** 998ms

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

### NovelBuddy

**Status:** ✅ PASS

- **Search Term:** `god`
- **Search Results:** 48
- **Novel Title:** Reincarnation Of The Strongest Sword God (Web Novel)
- **Novel URL:** https://novelbuddy.com/novel/reincarnation-of-the-strongest-sword-god
- **Chapters Found:** 4533
- **Search Time:** 2176ms
- **Details Time:** 1446ms
- **Chapters Time:** 1554ms

### Ranobes

**Status:** ❌ FAIL

Tried 4 search terms: `god`, `magic`, `I`, `is`

**Attempt 1 (search: `god`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 2 (search: `magic`):**
- ❌ Search failed: HTTPSConnectionPool(host='ranobes.net', port=443): Read timed out.
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 3 (search: `I`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

**Attempt 4 (search: `is`):**
- ❌ Search failed: No results found
- URL: `https://ranobes.net/index.php?do=search`

---

*This file is automatically generated by `test-extension-runtime.py`*
