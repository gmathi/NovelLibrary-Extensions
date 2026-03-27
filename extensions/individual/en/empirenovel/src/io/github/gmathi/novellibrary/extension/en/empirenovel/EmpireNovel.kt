package io.github.gmathi.novellibrary.extension.en.empirenovel

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class EmpireNovel : ParsedHttpSource() {
    override val id: Long
        get() = 13L
    override val baseUrl: String
        get() = "https://www.empirenovel.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Empire Novel"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder =
        Headers
            .Builder()
            .add("User-Agent", defaultUserAgent)
            .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search-live?q=$encodedQuery"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val jsonArray = JSONArray(response.body!!.string())
        val novels = mutableListOf<Novel>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val title = item.getString("name")
            val slug = item.getString("slug")
            val novelUrl = "$baseUrl/novel/$slug"

            val novel = Novel(title, novelUrl, id)
            // Build cover URL from slug if cover flag is set
            if (item.optInt("cover", 0) == 1) {
                // www.empirenovel.com/uploads/novel/gods-impact-online/cover/cover_250x350.jpg
                novel.imageUrl = "$baseUrl/uploads/novel/$slug/cover/cover_250x350.jpg"
            }
            novel.metadata["slug"] = slug

            // Extract summary from nested object
            val summaryObj = item.optJSONObject("summary")
            if (summaryObj != null) {
                novel.longDescription = summaryObj.optString("en").ifEmpty { null }
            }

            novels.add(novel)
        }

        return NovelsPage(novels, hasNextPage = false)
    }

    override fun searchNovelsFromElement(element: Element): Novel = throw Exception(NOT_USED)

    override fun searchNovelsSelector() = throw Exception(NOT_USED)

    override fun searchNovelsNextPageSelector() = "a:contains(Last)"
    //endregion

    //region Novel Details

    /** Fast path: parse raw HTML string with regex, skip Jsoup entirely. */
    override fun novelDetailsParse(
        novel: Novel,
        response: Response,
    ): Novel {
        val html = response.body!!.string()

        // Image — data-src on img.show_image
        RE_IMAGE.find(html)?.groupValues?.get(1)?.let { src ->
            novel.imageUrl = if (src.startsWith("http")) src else "$baseUrl$src"
        }

        // Title — first <h1> with itemprop containing "name"
        RE_TITLE
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.let { novel.name = decodeHtml(it).trim() }

        // Description — content inside <span id="more">...</span>, fall back to dd[itemprop=description]
        novel.longDescription = RE_DESC_MORE
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.let { decodeHtml(it).trim() }
            ?: RE_DESC_DD
                .find(html)
                ?.groupValues
                ?.get(1)
                ?.let { decodeHtml(it.replace(RE_HTML_TAG, "")).trim() }

        // Status & Author from show_details div.d-flex pairs
        RE_DETAIL_PAIR.findAll(html).forEach { match ->
            val label = match.groupValues[1].trim().lowercase()
            val value = decodeHtml(match.groupValues[2]).trim()
            when (label) {
                "status" -> novel.metadata["Status"] = value
                "author" -> {
                    novel.authors = listOf(value)
                    novel.metadata["Author"] = value
                }
            }
        }

        // Author fallback from itemprop=author
        if (novel.authors.isNullOrEmpty()) {
            RE_AUTHOR.find(html)?.groupValues?.get(1)?.let {
                val author = decodeHtml(it).trim()
                novel.authors = listOf(author)
                novel.metadata["Author"] = author
            }
        }

        // Genres from itemprop="genre"
        val genres = RE_GENRE.findAll(html).map { decodeHtml(it.groupValues[1]).trim() }.toList()
        if (genres.isNotEmpty()) novel.genres = genres

        return novel
    }

    /** Not used — we override the Response version directly. */
    override fun novelDetailsParse(
        novel: Novel,
        document: Document,
    ): Novel = novel
    //endregion

    //region Chapters
    override fun chapterListRequest(novel: Novel): Request = GET(novel.url, headers)

    override fun chapterListSelector() = "a.chapter_link"

    override fun chapterFromElement(element: Element): WebPage = throw Exception(NOT_USED)

    override fun chapterListParse(
        novel: Novel,
        response: Response,
    ): List<WebPage> {
        val firstHtml = response.body!!.string()
        val allChapters = mutableListOf<WebPage>()

        // Parse chapters from first page
        parseChaptersFromHtml(firstHtml, allChapters)

        // Determine last page from pagination links (?page=N)
        val lastPage =
            RE_PAGE_NUM
                .findAll(firstHtml)
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .maxOrNull() ?: 1

        // Fetch remaining pages
        for (page in 2..lastPage) {
            val html =
                client
                    .newCall(GET("${novel.url}?page=$page", headers))
                    .execute()
                    .body!!
                    .string()
            parseChaptersFromHtml(html, allChapters)
        }

        return allChapters.reversed().mapIndexed { index, chapter ->
            chapter.orderId = index.toLong()
            chapter
        }
    }

    /** Extract chapter links from raw HTML using regex — no Jsoup needed. */
    private fun parseChaptersFromHtml(
        html: String,
        out: MutableList<WebPage>,
    ) {
        RE_CHAPTER_LINK.findAll(html).forEach { match ->
            val url = match.groupValues[1].let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val title = decodeHtml(match.groupValues[2]).replace(Regex("""\s+"""), " ").trim()
            out.add(WebPage(url, title))
        }
    }
    //endregion

    //region stubs
    override fun latestUpdatesRequest(page: Int): Request = throw Exception(NOT_USED)

    override fun latestUpdatesSelector(): String = throw Exception(NOT_USED)

    override fun latestUpdatesFromElement(element: Element): Novel = throw Exception(NOT_USED)

    override fun latestUpdatesNextPageSelector(): String = throw Exception(NOT_USED)

    override fun popularNovelsRequest(page: Int): Request = throw Exception(NOT_USED)

    override fun popularNovelsSelector(): String = throw Exception(NOT_USED)

    override fun popularNovelsFromElement(element: Element): Novel = throw Exception(NOT_USED)

    override fun popularNovelNextPageSelector(): String = throw Exception(NOT_USED)
    //endregion

    companion object {
        // Novel details regex patterns
        private val RE_IMAGE = Regex("""img\s+[^>]*class="show_image[^"]*"[^>]*data-src="([^"]+)"""")
        private val RE_TITLE = Regex("""<h1[^>]*itemprop="[^"]*name[^"]*"[^>]*>([^<]+)</h1>""")
        private val RE_DESC_MORE = Regex("""<span\s+id="more">([^<]+)</span>""")
        private val RE_DESC_DD = Regex("""<dd[^>]*itemprop="description"[^>]*>(.*?)</dd>""", RegexOption.DOT_MATCHES_ALL)
        private val RE_DETAIL_PAIR = Regex("""<div\s+class="d-flex\s+justify-content-between">\s*(\w+)\s*<span>([^<]*)</span>""")
        private val RE_AUTHOR = Regex("""itemprop="author"[^>]*><a[^>]*>([^<]+)</a>""")
        private val RE_GENRE = Regex("""itemprop="genre"[^>]*>([^<]+)</a>""")
        private val RE_HTML_TAG = Regex("""<[^>]+>""")

        // Chapter parsing regex patterns
        private val RE_CHAPTER_LINK =
            Regex(
                """<a\s+class="chapter_link"\s+href="([^"]+)"[^>]*>.*?<div[^>]*class="[^"]*chapter[^"]*"[^>]*>\s*(?:<div[^>]*>)?\s*(Chapter(?:\s|&nbsp;)+[^\s<]+)""",
                RegexOption.DOT_MATCHES_ALL,
            )
        private val RE_PAGE_NUM = Regex("""\?page=(\d+)""")

        private fun decodeHtml(s: String): String =
            s
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
    }
}
