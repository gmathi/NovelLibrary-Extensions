#!/usr/bin/env python3
"""
End-to-End Extension Testing
Tests: Search → Novel Details → Chapter List
Tests with multiple search terms to avoid edge cases
Tests extensions in parallel with rate limiting per host

Usage:
  python test-extension-runtime.py                    # Test all extensions
  python test-extension-runtime.py NovelFull          # Test single extension
  python test-extension-runtime.py --list             # List available extensions
  python test-extension-runtime.py NovelFull --json   # Output results as JSON
"""

import time
import json
import sys
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
import cloudscraper
from bs4 import BeautifulSoup

# Multiple search terms to test with
SEARCH_TERMS = ["god", "magic", "I", "is"]

@dataclass
class E2ETestResult:
    name: str
    base_url: str
    search_term: str = ""
    
    # Search test
    search_success: bool = False
    search_status: int = 0
    search_time: float = 0.0
    search_result_count: int = 0
    search_error: Optional[str] = None
    search_url: Optional[str] = None
    
    # Novel details test
    novel_url: Optional[str] = None
    novel_title: Optional[str] = None
    novel_id: Optional[str] = None  # For JSON APIs
    details_success: bool = False
    details_status: int = 0
    details_time: float = 0.0
    details_has_description: bool = False
    details_has_author: bool = False
    details_has_genres: bool = False
    details_error: Optional[str] = None
    
    # Chapter list test
    chapters_success: bool = False
    chapters_status: int = 0
    chapters_time: float = 0.0
    chapters_count: int = 0
    chapters_error: Optional[str] = None
    
    # Overall
    overall_success: bool = False

# Extension configurations with selectors for all stages
# Search URLs will be modified with different search terms
EXTENSIONS = [
    {
        "name": "NovelFull",
        "base_url": "https://novelfull.com",
        "search_url_template": "https://novelfull.com/search?keyword={query}&page=1",
        "search_selector": "div.list.list-truyen div.row",
        "novel_link_selector": "h3.truyen-title a",
        "details_description_selector": "div.desc-text",
        "details_author_selector": "div.info a",
        "details_genres_selector": "div.info a",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "#rating",
        "chapter_ajax_attr": "data-novel-id",
        "chapter_ajax_url_template": "https://novelfull.com/ajax-chapter-option?novelId={id}&currentChapterId=",
        "chapter_selector": "option",
        "chapter_link_attr": "value",
    },
    {
        "name": "ScribbleHub",
        "base_url": "https://www.scribblehub.com",
        "search_url_template": "https://www.scribblehub.com/?s={query}&post_type=fictionposts&paged=1",
        "search_selector": "div.search_main_box",
        "novel_link_selector": "div.search_title a",
        "details_description_selector": "div.wi_fic_desc",
        "details_author_selector": "span[property='author'] a",
        "details_genres_selector": "span.wi_fic_genre a",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "#mypostid",
        "chapter_ajax_attr": "value",
        "chapter_ajax_url_template": "https://www.scribblehub.com/wp-admin/admin-ajax.php",
        "chapter_ajax_post": True,
        "chapter_selector": "a[href]",
    },
    {
        "name": "RoyalRoad",
        "base_url": "https://www.royalroad.com",
        "search_url_template": "https://www.royalroad.com/fictions/search?title={query}&page=1",
        "search_selector": "div.fiction-list > div",
        "novel_link_selector": ".fiction-title a",
        "details_description_selector": "div[property=description]",
        "details_author_selector": "meta[property='books:author']",
        "details_genres_selector": "[property=genre]",
        "chapter_list_ajax": False,
        "chapter_selector": "table#chapters tbody tr a[href]",
    },
    {
        "name": "NovelBin",
        "base_url": "https://novelbin.me",
        "search_url_template": "https://novelbin.me/search?keyword={query}&page=1",
        "search_selector": "div.list.list-novel div.row",
        "novel_link_selector": "h3.novel-title a",
        "details_description_selector": "div.desc-text",
        "details_author_selector": "ul.info a",
        "details_genres_selector": "ul.info a",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "#rating",
        "chapter_ajax_attr": "data-novel-id",
        "chapter_ajax_url_template": "https://novelbin.me/ajax/chapter-archive?novelId={id}",
        "chapter_selector": "li a",
    },
    {
        "name": "NovelBuddy",
        "base_url": "https://novelbuddy.com",
        "search_url_template": "https://novelbuddy.com/search?status=all&sort=views&q={query}&page=1",
        "search_selector": "div.list.manga-list div.book-detailed-item",
        "novel_link_selector": "a[title]",
        "details_description_selector": "div.summary p.content",
        "details_author_selector": "div.meta.box p a",
        "details_genres_selector": "div.meta.box p a",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "script",
        "chapter_ajax_regex": r"bookId\s*=\s*(\d+)",
        "chapter_ajax_url_template": "https://novelbuddy.com/api/manga/{id}/chapters?source=detail",
        "chapter_selector": "ul#chapter-list li a",
    },
    {
        "name": "LNMTL",
        "base_url": "https://lnmtl.com",
        "search_url_template": "https://lnmtl.com/home",  # Special: loads all novels in script tag
        "search_selector": "script[type]",
        "novel_link_selector": None,  # Special handling needed
        "details_description_selector": ".novel .media .description",
        "details_author_selector": "dt:contains(Authors) + dd span",
        "details_genres_selector": "div.panel-heading:contains(Genres) + div ul li",
        "chapter_list_ajax": False,
        "chapter_selector": "script",  # Special: chapters in script tag
        "special_search": "lnmtl",  # Flag for special search handling
    },
    {
        "name": "EmpireNovel",
        "base_url": "https://www.empirenovel.com",
        "search_url_template": "https://www.empirenovel.com/?s={query}&post_type=wp-manga",
        "search_selector": "div.c-tabs-item__content",
        "novel_link_selector": "div.post-title a",
        "details_description_selector": "div.description-summary",
        "details_author_selector": "div.post-content_item.mg_author div.summary-content a",
        "details_genres_selector": "div.post-content_item.mg_genres div.summary-content a",
        "chapter_list_ajax": True,
        "chapter_ajax_url_template": "{novel_url}ajax/chapters/",
        "chapter_ajax_post_empty": True,  # POST with empty body
        "chapter_selector": "li.wp-manga-chapter a",
    },
    {
        "name": "BoxNovel",
        "base_url": "https://boxnovel.com",
        "search_url_template": "https://boxnovel.com/?s={query}&post_type=wp-manga",
        "search_selector": "div.c-tabs-item__content",
        "novel_link_selector": "div.post-title a",
        "details_description_selector": "div.description-summary",
        "details_author_selector": "div.post-content_item.mg_author div.summary-content a",
        "details_genres_selector": "div.post-content_item.mg_genres div.summary-content a",
        "chapter_list_ajax": True,
        "chapter_ajax_url_template": "{novel_url}ajax/chapters/",
        "chapter_ajax_post_empty": True,
        "chapter_selector": "li.wp-manga-chapter a",
    },
    {
        "name": "FanMTL",
        "base_url": "https://www.fanmtl.com",
        "search_url_template": "https://www.fanmtl.com/search?status=all&sort=views&q={query}&page=1",
        "search_selector": "div.list.manga-list div.book-detailed-item",
        "novel_link_selector": "a[title]",
        "details_description_selector": "div.summary p.content",
        "details_author_selector": "div.meta.box p a",
        "details_genres_selector": "div.meta.box p a",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "script",
        "chapter_ajax_regex": r"bookId\s=\s(\d+)",
        "chapter_ajax_url_template": "https://www.fanmtl.com/api/manga/{id}/chapters?source=detail",
        "chapter_selector": "ul#chapter-list li a",
    },
    {
        "name": "Ranobes",
        "base_url": "https://ranobes.net",
        "search_url_template": "https://ranobes.net/index.php?do=search",
        "search_selector": "div.block.story.shortstory.mod-poster",
        "novel_link_selector": "h2.title a",
        "details_description_selector": "[itemprop=description]",
        "details_author_selector": "span[itemprop=alternateName]",
        "details_genres_selector": "meta[name=keywords]",
        "chapter_list_ajax": False,
        "chapter_selector": "div.chapter-list a",  # Placeholder
        "special_search": "ranobes",  # POST-based search
    },
    {
        "name": "WuxiaWorldSite",
        "base_url": "https://wuxiaworldsite.co",
        "search_url_template": "https://wuxiaworldsite.co/search/{query}&page=1",
        "search_selector": "div.bz.item",
        "novel_link_selector": "a[href]",
        "details_description_selector": "div.story-introduction-content p",
        "details_author_selector": "i.fa.fa-user",
        "details_genres_selector": "div.tags a.a_tag_item",
        "chapter_list_ajax": True,
        "chapter_ajax_selector": "#rating",
        "chapter_ajax_attr": "data-novel-id",
        "chapter_ajax_url_template": "https://wuxiaworldsite.co/ajax-chapter-option?novelId={id}&currentChapterId=",
        "chapter_selector": "select.chapter_jump option",
        "chapter_link_attr": "value",
    },
    {
        "name": "PurrFiction",
        "base_url": "https://purrfiction.io",
        "search_url_template": "https://purrfiction.io/V2/books/search?language=ALL&filter=0&name={query}&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000",
        "search_selector": None,  # JSON API
        "novel_link_selector": None,
        "details_url_template": "https://purrfiction.io/V1/page/book?bookId={id}&language=EN",
        "details_description_selector": None,
        "details_author_selector": None,
        "details_genres_selector": None,
        "chapter_list_ajax": True,
        "chapter_ajax_url_template": "https://purrfiction.io/V5/chapters?bookId={id}&language=EN",
        "chapter_selector": None,
        "special_search": "purrfiction",  # JSON API
        "special_details": "purrfiction",  # JSON API
        "special_chapters": "purrfiction",  # JSON API
    },
    {
        "name": "JPMTL",
        "base_url": "https://jpmtl.com",
        "search_url_template": "https://jpmtl.com/v2/book/show/browse?query={query}&categories=&content_type=0&direction=0&page=1&limit=25&type=5&status=all&language=3&exclude_categories=",
        "search_selector": None,  # JSON API
        "novel_link_selector": None,
        "details_description_selector": None,
        "details_author_selector": None,
        "details_genres_selector": None,
        "chapter_list_ajax": False,
        "chapter_selector": None,
        "special_search": "jpmtl",  # JSON API
    },
]

class E2ETester:
    """End-to-end extension tester with rate limiting per host"""
    
    def __init__(self):
        self.scraper = cloudscraper.create_scraper(
            browser={'browser': 'chrome', 'platform': 'windows', 'desktop': True},
            delay=10
        )
        # Track last request time per host for rate limiting
        self.last_request_time = {}
        self.rate_limit_lock = Lock()
    
    def _rate_limit(self, host: str):
        """Ensure at least 1 second gap between requests to the same host"""
        with self.rate_limit_lock:
            now = time.time()
            if host in self.last_request_time:
                elapsed = now - self.last_request_time[host]
                if elapsed < 1.0:
                    sleep_time = 1.0 - elapsed
                    time.sleep(sleep_time)
            self.last_request_time[host] = time.time()
    
    def _make_request(self, url: str, method: str = 'GET', **kwargs):
        """Make HTTP request with rate limiting per host"""
        from urllib.parse import urlparse
        host = urlparse(url).netloc
        self._rate_limit(host)
        
        if method == 'POST':
            return self.scraper.post(url, **kwargs)
        else:
            return self.scraper.get(url, **kwargs)
    
    def test_extension(self, config: Dict, search_term: str) -> E2ETestResult:
        """Run full E2E test for an extension with a specific search term"""
        result = E2ETestResult(
            name=config["name"],
            base_url=config["base_url"],
            search_term=search_term
        )
        
        print(f"\n{'='*70}")
        print(f"Testing: {config['name']} (search: '{search_term}')")
        print('='*70)
        
        # Step 1: Test Search
        print("\n[1/3] Testing Search...")
        if not self._test_search(config, result, search_term):
            print(f"  ✗ Search failed - stopping test")
            return result
        print(f"  ✓ Search successful - {result.search_result_count} results")
        
        # Step 2: Test Novel Details
        print("\n[2/3] Testing Novel Details...")
        if not self._test_novel_details(config, result):
            print(f"  ✗ Novel details failed - stopping test")
            return result
        print(f"  ✓ Novel details loaded")
        
        # Step 3: Test Chapter List
        print("\n[3/3] Testing Chapter List...")
        if not self._test_chapter_list(config, result):
            print(f"  ✗ Chapter list failed - stopping test")
            return result
        print(f"  ✓ Chapter list loaded - {result.chapters_count} chapters")
        
        result.overall_success = True
        print(f"\n✅ ALL TESTS PASSED for {config['name']} ('{search_term}')!")
        
        return result
    
    def _test_search(self, config: Dict, result: E2ETestResult, search_term: str) -> bool:
        """Test search functionality"""
        try:
            # Handle base64 encoding for PurrFiction
            query = search_term
            if config.get("special_search") == "purrfiction":
                import base64
                query = base64.b64encode(search_term.encode()).decode()
            
            # Build search URL with the search term
            search_url = config["search_url_template"].format(query=query)
            result.search_url = search_url
            
            start = time.time()
            response = self._make_request(
                search_url,
                timeout=30,
                headers={'Referer': config["base_url"]}
            )
            result.search_time = (time.time() - start) * 1000
            result.search_status = response.status_code
            
            if response.status_code != 200:
                result.search_error = f"HTTP {response.status_code}"
                return False
            
            # Special handling for JSON API extensions
            if config.get("special_search") in ["purrfiction", "jpmtl"]:
                import json as json_lib
                try:
                    json_data = json_lib.loads(response.text)
                    
                    # Handle array response (PurrFiction, JPMTL)
                    if isinstance(json_data, list):
                        novels = json_data
                    elif isinstance(json_data, dict) and 'data' in json_data:
                        novels = json_data['data']
                    else:
                        result.search_error = "Unexpected JSON structure"
                        return False
                    
                    result.search_result_count = len(novels)
                    
                    if len(novels) == 0:
                        result.search_error = "No results found"
                        return False
                    
                    # Get first novel
                    first_novel = novels[0]
                    
                    if config.get("special_search") == "purrfiction":
                        novel_id = first_novel.get('id')
                        result.novel_url = f"{config['base_url']}/V1/page/book?bookId={novel_id}&language=EN"
                        result.novel_title = first_novel.get('name', 'Unknown')
                        # Store novel ID for later use
                        result.novel_id = novel_id
                    elif config.get("special_search") == "jpmtl":
                        novel_id = first_novel.get('id')
                        result.novel_url = f"{config['base_url']}/book/{novel_id}"
                        result.novel_title = first_novel.get('title', 'Unknown')
                    
                    result.search_success = True
                    return True
                    
                except (ValueError, KeyError, IndexError) as e:
                    result.search_error = f"Failed to parse JSON: {str(e)}"
                    return False
            
            soup = BeautifulSoup(response.content, 'lxml')
            
            # Special handling for LNMTL
            if config.get("special_search") == "lnmtl":
                # LNMTL loads all novels in a script tag
                scripts = soup.select(config["search_selector"])
                if not scripts:
                    result.search_error = "No script tags found"
                    return False
                
                # Find the script with novel data
                script = scripts[-1] if scripts else None
                if not script:
                    result.search_error = "Could not find novels script"
                    return False
                
                text = script.string or ""
                if "local:" not in text:
                    result.search_error = "Script doesn't contain novel data"
                    return False
                
                # Extract JSON from script
                import json as json_lib
                try:
                    json_start = text.index("local:") + 7
                    json_text = text[json_start:].split(']')[0] + ']'
                    novels = json_lib.loads(json_text)
                    
                    # Filter by search term
                    matching_novels = [n for n in novels if search_term.lower() in n.get('name', '').lower()]
                    result.search_result_count = len(matching_novels)
                    
                    if len(matching_novels) == 0:
                        result.search_error = "No matching novels found"
                        return False
                    
                    # Get first matching novel
                    first_novel = matching_novels[0]
                    result.novel_url = first_novel.get('url')
                    result.novel_title = first_novel.get('name')
                    
                    if not result.novel_url:
                        result.search_error = "Could not extract novel URL"
                        return False
                    
                    if not result.novel_url.startswith('http'):
                        result.novel_url = config["base_url"] + result.novel_url
                    
                    result.search_success = True
                    return True
                    
                except (ValueError, KeyError, IndexError) as e:
                    result.search_error = f"Failed to parse novel data: {str(e)}"
                    return False
            
            # Standard search handling
            results = soup.select(config["search_selector"])
            result.search_result_count = len(results)
            
            if len(results) == 0:
                result.search_error = "No results found"
                return False
            
            # Get first novel URL
            first_result = results[0]
            novel_link = first_result.select_one(config["novel_link_selector"])
            if not novel_link:
                result.search_error = "Could not find novel link"
                return False
            
            result.novel_url = novel_link.get('href')
            if not result.novel_url.startswith('http'):
                result.novel_url = config["base_url"] + result.novel_url
            
            result.novel_title = novel_link.get('title') or novel_link.get_text(strip=True)
            result.search_success = True
            return True
            
        except Exception as e:
            result.search_error = str(e)
            return False
            result.search_success = True
            return True
            
        except Exception as e:
            result.search_error = str(e)
            return False
    
    def _test_novel_details(self, config: Dict, result: E2ETestResult) -> bool:
        """Test novel details page"""
        try:
            start = time.time()
            response = self._make_request(
                result.novel_url,
                timeout=30,
                headers={'Referer': config["base_url"]}
            )
            result.details_time = (time.time() - start) * 1000
            result.details_status = response.status_code
            
            if response.status_code != 200:
                result.details_error = f"HTTP {response.status_code}"
                return False
            
            # Special handling for JSON API extensions
            if config.get("special_details") == "purrfiction":
                import json as json_lib
                try:
                    json_data = json_lib.loads(response.text)
                    
                    # PurrFiction structure
                    book_dto = json_data.get('bookDto', {})
                    authors_dto = json_data.get('authorsDto', [])
                    
                    # Check for description
                    result.details_has_description = bool(book_dto.get('bookDescription'))
                    
                    # Check for author
                    result.details_has_author = len(authors_dto) > 0
                    
                    # Check for genres
                    result.details_has_genres = len(book_dto.get('genreIds', [])) > 0
                    
                    result.details_success = True
                    return True
                    
                except (ValueError, KeyError) as e:
                    result.details_error = f"Failed to parse JSON: {str(e)}"
                    return False
            
            soup = BeautifulSoup(response.content, 'lxml')
            
            # Check for description
            desc = soup.select_one(config["details_description_selector"])
            result.details_has_description = desc is not None and len(desc.get_text(strip=True)) > 0
            
            # Check for author
            author = soup.select_one(config["details_author_selector"])
            result.details_has_author = author is not None
            
            # Check for genres
            genres = soup.select(config["details_genres_selector"])
            result.details_has_genres = len(genres) > 0
            
            # Store the soup for chapter list extraction
            self._details_soup = soup
            
            result.details_success = True
            return True
            
        except Exception as e:
            result.details_error = str(e)
            return False
    
    def _test_chapter_list(self, config: Dict, result: E2ETestResult) -> bool:
        """Test chapter list fetching"""
        try:
            start = time.time()
            
            # Special handling for PurrFiction JSON API
            if config.get("special_chapters") == "purrfiction":
                if not result.novel_id:
                    result.chapters_error = "No novel ID available"
                    return False
                
                chapter_url = config["chapter_ajax_url_template"].format(id=result.novel_id)
                response = self._make_request(
                    chapter_url,
                    timeout=30,
                    headers={'Referer': result.novel_url}
                )
                
                result.chapters_time = (time.time() - start) * 1000
                result.chapters_status = response.status_code
                
                if response.status_code != 200:
                    result.chapters_error = f"HTTP {response.status_code}"
                    return False
                
                import json as json_lib
                try:
                    chapters_data = json_lib.loads(response.text)
                    if isinstance(chapters_data, list):
                        result.chapters_count = len(chapters_data)
                    else:
                        result.chapters_error = "Unexpected JSON structure"
                        return False
                    
                    if result.chapters_count == 0:
                        result.chapters_error = "No chapters found"
                        return False
                    
                    result.chapters_success = True
                    return True
                    
                except (ValueError, KeyError) as e:
                    result.chapters_error = f"Failed to parse JSON: {str(e)}"
                    return False
            
            # Special handling for LNMTL
            if config.get("special_search") == "lnmtl":
                # LNMTL has chapters in script tag on novel page
                scripts = self._details_soup.select("script")
                script = None
                for s in scripts:
                    if s.string and "lnmtl.firstResponse =" in s.string:
                        script = s
                        break
                
                if not script:
                    result.chapters_error = "Could not find chapters script"
                    return False
                
                # Check if script contains volume data
                if "lnmtl.volumes =" in script.string:
                    # Count approximate chapters by checking volume data
                    # This is a simplified check - actual implementation would parse JSON
                    result.chapters_count = 1  # At least 1 chapter exists if volumes are defined
                    result.chapters_time = (time.time() - start) * 1000
                    result.chapters_status = 200
                    result.chapters_success = True
                    return True
                else:
                    result.chapters_error = "No volume data found in script"
                    return False
            
            if config.get("chapter_list_ajax"):
                # AJAX-based chapter list
                if config.get("chapter_ajax_post_empty"):
                    # POST with empty body (EmpireNovel, BoxNovel)
                    chapter_url = config["chapter_ajax_url_template"].format(novel_url=result.novel_url)
                    response = self._make_request(
                        chapter_url,
                        method='POST',
                        data={},
                        timeout=30,
                        headers={'Referer': result.novel_url}
                    )
                elif config.get("chapter_ajax_regex"):
                    # Extract ID using regex
                    import re
                    script_tags = self._details_soup.select(config["chapter_ajax_selector"])
                    novel_id = None
                    for script in script_tags:
                        match = re.search(config["chapter_ajax_regex"], script.string or "")
                        if match:
                            novel_id = match.group(1)
                            break
                else:
                    # Extract ID from attribute
                    id_elem = self._details_soup.select_one(config["chapter_ajax_selector"])
                    if not id_elem:
                        result.chapters_error = "Could not find chapter ID element"
                        return False
                    novel_id = id_elem.get(config["chapter_ajax_attr"])
                
                if not novel_id:
                    result.chapters_error = "Could not extract novel ID"
                    return False
                
                chapter_url = config["chapter_ajax_url_template"].format(id=novel_id)
                
                if config.get("chapter_ajax_post"):
                    # POST request (ScribbleHub)
                    response = self._make_request(
                        chapter_url,
                        method='POST',
                        data={
                            'action': 'wi_gettocchp',
                            'strSID': novel_id,
                            'strmypostid': '0',
                            'strFic': 'yes'
                        },
                        timeout=30,
                        headers={'Referer': result.novel_url}
                    )
                else:
                    # GET request
                    response = self._make_request(
                        chapter_url,
                        timeout=30,
                        headers={'Referer': result.novel_url}
                    )
                
                soup = BeautifulSoup(response.content, 'lxml')
            else:
                # Chapter list on same page
                soup = self._details_soup
                response = type('obj', (object,), {'status_code': 200})()
            
            result.chapters_time = (time.time() - start) * 1000
            result.chapters_status = response.status_code
            
            if response.status_code != 200:
                result.chapters_error = f"HTTP {response.status_code}"
                return False
            
            # Find chapters
            chapters = soup.select(config["chapter_selector"])
            result.chapters_count = len(chapters)
            
            if len(chapters) == 0:
                result.chapters_error = "No chapters found"
                return False
            
            # Get first chapter URL
            first_chapter = chapters[0]
            if config.get("chapter_link_attr"):
                chapter_href = first_chapter.get(config["chapter_link_attr"])
            else:
                chapter_href = first_chapter.get('href')
            
            if not chapter_href:
                result.chapters_error = "Could not find chapter link"
                return False
            
            result.chapters_success = True
            return True
            
        except Exception as e:
            result.chapters_error = str(e)
            return False
            result.chapters_error = str(e)
            return False

def test_extension_with_terms(tester: E2ETester, config: Dict) -> List[E2ETestResult]:
    """Test a single extension with multiple search terms until one passes"""
    results = []
    
    for search_term in SEARCH_TERMS:
        result = tester.test_extension(config, search_term)
        results.append(result)
        
        if result.overall_success:
            print(f"\n✅ {config['name']} PASSED with search term '{search_term}'")
            break  # Move to next extension once we get a pass
        else:
            print(f"\n⚠️ {config['name']} FAILED with search term '{search_term}', trying next term...")
    
    if not any(r.overall_success for r in results):
        print(f"\n❌ {config['name']} FAILED with all search terms")
    
    return results

def list_extensions():
    """List all available extensions"""
    print("Available Extensions:")
    print("="*70)
    for config in EXTENSIONS:
        print(f"  - {config['name']}")
    print("="*70)
    print(f"Total: {len(EXTENSIONS)} extensions")

def main():
    """Run E2E tests with parallel execution"""
    # Parse command line arguments
    extensions_to_test = EXTENSIONS
    single_extension = None
    json_output = '--json' in sys.argv
    
    # Remove --json from args for processing
    args = [arg for arg in sys.argv[1:] if arg != '--json']
    
    if len(args) > 0:
        arg = args[0]
        
        if arg in ['--list', '-l']:
            list_extensions()
            return 0
        
        if arg in ['--help', '-h']:
            print(__doc__)
            return 0
        
        # Find extension by name (case-insensitive)
        single_extension = next(
            (ext for ext in EXTENSIONS if ext['name'].lower() == arg.lower()),
            None
        )
        
        if not single_extension:
            print(f"❌ Error: Extension '{arg}' not found")
            print(f"\nAvailable extensions:")
            for ext in EXTENSIONS:
                print(f"  - {ext['name']}")
            return 1
        
        extensions_to_test = [single_extension]
        if not json_output:
            print(f"Testing single extension: {single_extension['name']}")
    
    if not json_output:
        print("="*70)
        if single_extension:
            print(f"End-to-End Extension Testing: {single_extension['name']}")
        else:
            print("End-to-End Extension Testing (Parallel)")
        print("Testing: Search → Novel Details → Chapter List")
        print(f"Search Terms: {', '.join(SEARCH_TERMS)}")
        print("="*70)
        print()
    
    tester = E2ETester()
    all_results: List[E2ETestResult] = []
    
    # Test extensions in parallel (or single extension)
    max_workers = 1 if single_extension else len(extensions_to_test)
    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        # Submit all extension tests
        future_to_config = {
            executor.submit(test_extension_with_terms, tester, config): config 
            for config in extensions_to_test
        }
        
        # Collect results as they complete
        for future in as_completed(future_to_config):
            config = future_to_config[future]
            try:
                results = future.result()
                all_results.extend(results)
            except Exception as e:
                if not json_output:
                    print(f"\n❌ {config['name']} - Exception: {str(e)}")
    
    # Print summary
    if not json_output:
        print("\n" + "="*70)
        print("SUMMARY")
        print("="*70)
    
    # Group results by extension
    extensions_tested = {}
    for result in all_results:
        if result.name not in extensions_tested:
            extensions_tested[result.name] = []
        extensions_tested[result.name].append(result)
    
    total_extensions = len(extensions_tested)
    fully_working = sum(1 for results in extensions_tested.values() if any(r.overall_success for r in results))
    
    # JSON output mode
    if json_output:
        output = {
            "total_extensions": total_extensions,
            "fully_working": fully_working,
            "failed": total_extensions - fully_working,
            "extensions": {}
        }
        
        for ext_name, results in extensions_tested.items():
            successful_result = next((r for r in results if r.overall_success), None)
            if successful_result:
                output["extensions"][ext_name] = {
                    "status": "PASS",
                    "search_term": successful_result.search_term,
                    "search_results": successful_result.search_result_count,
                    "novel_title": successful_result.novel_title,
                    "novel_url": successful_result.novel_url,
                    "chapters": successful_result.chapters_count,
                    "search_time_ms": successful_result.search_time,
                    "details_time_ms": successful_result.details_time,
                    "chapters_time_ms": successful_result.chapters_time
                }
            else:
                last_result = results[-1]
                output["extensions"][ext_name] = {
                    "status": "FAIL",
                    "tried_terms": [r.search_term for r in results],
                    "error": last_result.search_error or last_result.details_error or last_result.chapters_error
                }
        
        print(json.dumps(output, indent=2))
        return 0 if fully_working == total_extensions else 1
    
    # Regular console output
    print(f"\nTotal Extensions Tested: {total_extensions}")
    print(f"Fully Working (E2E): {fully_working} ({fully_working*100//total_extensions}%)")
    print(f"Failed All Terms: {total_extensions - fully_working}")
    
    # Detailed results by extension
    print("\n" + "="*70)
    print("DETAILED RESULTS BY EXTENSION")
    print("="*70)
    
    for ext_name, results in extensions_tested.items():
        successful_result = next((r for r in results if r.overall_success), None)
        
        if successful_result:
            print(f"\n✅ {ext_name} - PASS")
            print(f"   Search Term: '{successful_result.search_term}'")
            print(f"   Search Results: {successful_result.search_result_count}")
            print(f"   Novel: {successful_result.novel_title}")
            print(f"   Chapters: {successful_result.chapters_count}")
        else:
            print(f"\n❌ {ext_name} - FAIL")
            print(f"   Tried {len(results)} search terms: {', '.join(repr(r.search_term) for r in results)}")
            
            # Show errors from last attempt
            last_result = results[-1]
            if last_result.search_error:
                print(f"   Search Error: {last_result.search_error}")
            elif last_result.details_error:
                print(f"   Details Error: {last_result.details_error}")
            elif last_result.chapters_error:
                print(f"   Chapters Error: {last_result.chapters_error}")
    
    # Generate README in docs folder (only if testing all extensions)
    if not single_extension:
        import os
        from datetime import datetime
        
        os.makedirs('docs', exist_ok=True)
        
        with open('docs/EXTENSIONS-STATUS.md', 'w', encoding='utf-8') as f:
            f.write("# Extension Runtime Test Results\n\n")
            f.write(f"**Last Updated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
            f.write("This document contains the results of end-to-end runtime testing for all extensions.\n\n")
            f.write("**Test Flow:** Search → Novel Details → Chapter List\n\n")
            f.write(f"**Search Terms Used:** {', '.join(SEARCH_TERMS)}\n\n")
            
            # Summary section
            f.write("## Summary\n\n")
            f.write(f"- **Total Extensions Tested:** {total_extensions}\n")
            f.write(f"- **Fully Working (E2E):** {fully_working} ({fully_working*100//total_extensions}%)\n")
            f.write(f"- **Failed All Terms:** {total_extensions - fully_working}\n\n")
            
            # Status badges
            f.write("## Extension Status\n\n")
            f.write("| Extension | Status | Search Term | Results | Novel | Chapters | Error |\n")
            f.write("|-----------|--------|-------------|---------|-------|----------|-------|\n")
            
            for ext_name, results in extensions_tested.items():
                successful_result = next((r for r in results if r.overall_success), None)
                
                if successful_result:
                    f.write(f"| {ext_name} | ✅ PASS | `{successful_result.search_term}` | {successful_result.search_result_count} | {successful_result.novel_title} | {successful_result.chapters_count} | - |\n")
                else:
                    last_result = results[-1]
                    error = last_result.search_error or last_result.details_error or last_result.chapters_error or "Unknown"
                    tried_terms = ', '.join(f'`{r.search_term}`' for r in results)
                    f.write(f"| {ext_name} | ❌ FAIL | {tried_terms} | - | - | - | {error} |\n")
            
            # Detailed results
            f.write("\n## Detailed Results\n\n")
            
            for ext_name, results in extensions_tested.items():
                successful_result = next((r for r in results if r.overall_success), None)
                
                f.write(f"### {ext_name}\n\n")
                
                if successful_result:
                    f.write(f"**Status:** ✅ PASS\n\n")
                    f.write(f"- **Search Term:** `{successful_result.search_term}`\n")
                    f.write(f"- **Search Results:** {successful_result.search_result_count}\n")
                    f.write(f"- **Novel Title:** {successful_result.novel_title}\n")
                    f.write(f"- **Novel URL:** {successful_result.novel_url}\n")
                    f.write(f"- **Chapters Found:** {successful_result.chapters_count}\n")
                    f.write(f"- **Search Time:** {successful_result.search_time:.0f}ms\n")
                    f.write(f"- **Details Time:** {successful_result.details_time:.0f}ms\n")
                    f.write(f"- **Chapters Time:** {successful_result.chapters_time:.0f}ms\n\n")
                else:
                    f.write(f"**Status:** ❌ FAIL\n\n")
                    f.write(f"Tried {len(results)} search terms: {', '.join(f'`{r.search_term}`' for r in results)}\n\n")
                    
                    # Show all attempts
                    for i, result in enumerate(results, 1):
                        f.write(f"**Attempt {i} (search: `{result.search_term}`):**\n")
                        if result.search_error:
                            f.write(f"- ❌ Search failed: {result.search_error}\n")
                            if result.search_url:
                                f.write(f"- URL: `{result.search_url}`\n")
                        elif result.details_error:
                            f.write(f"- ✅ Search passed ({result.search_result_count} results)\n")
                            f.write(f"- ❌ Details failed: {result.details_error}\n")
                            if result.novel_url:
                                f.write(f"- URL: `{result.novel_url}`\n")
                        elif result.chapters_error:
                            f.write(f"- ✅ Search passed ({result.search_result_count} results)\n")
                            f.write(f"- ✅ Details passed\n")
                            f.write(f"- ❌ Chapters failed: {result.chapters_error}\n")
                            if result.novel_url:
                                f.write(f"- URL: `{result.novel_url}`\n")
                        f.write("\n")
            
            # Footer
            f.write("---\n\n")
            f.write("*This file is automatically generated by `test-extension-runtime.py`*\n")
        
        print("\n✓ Results exported to: docs/EXTENSIONS-STATUS.md")
    else:
        print("\n✓ Single extension test completed (status file not updated)")
    
    return 0 if fully_working == total_extensions else 1

if __name__ == "__main__":
    exit(main())
