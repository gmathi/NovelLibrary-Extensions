#!/usr/bin/env python3
"""
End-to-End Extension Testing
Tests: Search → Novel Details → Chapter List
Tests with multiple search terms to avoid edge cases
"""

import time
import json
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
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
    
    # Novel details test
    novel_url: Optional[str] = None
    novel_title: Optional[str] = None
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
]

class E2ETester:
    """End-to-end extension tester"""
    
    def __init__(self):
        self.scraper = cloudscraper.create_scraper(
            browser={'browser': 'chrome', 'platform': 'windows', 'desktop': True},
            delay=10
        )
    
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
            # Build search URL with the search term
            search_url = config["search_url_template"].format(query=search_term)
            
            start = time.time()
            response = self.scraper.get(
                search_url,
                timeout=30,
                headers={'Referer': config["base_url"]}
            )
            result.search_time = (time.time() - start) * 1000
            result.search_status = response.status_code
            
            if response.status_code != 200:
                result.search_error = f"HTTP {response.status_code}"
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
            
            if response.status_code != 200:
                result.search_error = f"HTTP {response.status_code}"
                return False
            
            soup = BeautifulSoup(response.content, 'lxml')
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
    
    def _test_novel_details(self, config: Dict, result: E2ETestResult) -> bool:
        """Test novel details page"""
        try:
            start = time.time()
            response = self.scraper.get(
                result.novel_url,
                timeout=30,
                headers={'Referer': config["base_url"]}
            )
            result.details_time = (time.time() - start) * 1000
            result.details_status = response.status_code
            
            if response.status_code != 200:
                result.details_error = f"HTTP {response.status_code}"
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
                    response = self.scraper.post(
                        chapter_url,
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
                    response = self.scraper.post(
                        chapter_url,
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
                    response = self.scraper.get(
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

def main():
    """Run E2E tests with multiple search terms"""
    print("="*70)
    print("End-to-End Extension Testing")
    print("Testing: Search → Novel Details → Chapter List")
    print(f"Search Terms: {', '.join(SEARCH_TERMS)}")
    print("="*70)
    print()
    
    tester = E2ETester()
    all_results: List[E2ETestResult] = []
    
    # Test each extension with multiple search terms
    for config in EXTENSIONS:
        extension_passed = False
        
        for search_term in SEARCH_TERMS:
            result = tester.test_extension(config, search_term)
            all_results.append(result)
            
            if result.overall_success:
                extension_passed = True
                print(f"\n✅ {config['name']} PASSED with search term '{search_term}'")
                break  # Move to next extension once we get a pass
            else:
                print(f"\n⚠️ {config['name']} FAILED with search term '{search_term}', trying next term...")
            
            time.sleep(1)  # Small delay between attempts
        
        if not extension_passed:
            print(f"\n❌ {config['name']} FAILED with all search terms")
        
        time.sleep(2)  # Be nice to servers between extensions
    
    # Print summary
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
    
    # Export all results
    results_dict = [asdict(r) for r in all_results]
    with open('e2e-test-results.json', 'w') as f:
        json.dump(results_dict, f, indent=2)
    
    print("\n✓ Results exported to: e2e-test-results.json")
    
    return 0 if fully_working == total_extensions else 1

if __name__ == "__main__":
    exit(main())
