package io.github.gmathi.novellibrary.extension.en.scribblehub

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Exceptions.INVALID_NOVEL
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class ScribbleHub : ParsedHttpSource() {
    override val id: Long
        get() = 6L
    override val baseUrl: String
        get() = "https://www.scribblehub.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Scribble Hub"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    // Matches the numeric series id in URLs like /series/123456/some-slug/
    private val seriesIdRegex = Regex("""/series/(\d+)""")

    override fun headersBuilder(): Headers.Builder =
        Headers
            .Builder()
            .add("User-Agent", defaultUserAgent)
            .add("Referer", baseUrl)
            .add("Origin", baseUrl)
            .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .add("Accept-Language", "en-US,en;q=0.5")

    //region Search Novel
    override fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/series-finder/?sf=1&sh=${encodedQuery.replace(" ", "+")}&sort=views&order=desc&pg=$page"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val urlElement = element.select("div.search_title > a[href]")
        val novel = Novel(urlElement.text(), urlElement.attr("abs:href"), id)
        novel.imageUrl = element.select("div.search_img > img[src]").attr("abs:src")
        novel.rating =
            element
                .select("div.search_img")
                .select("div.search_ratings")
                .text()
                .replace("(", "")
                .replace(")", "")
        return novel
    }

    override fun searchNovelsSelector() = "div.search_main_box"

    override fun searchNovelsNextPageSelector() = "a.page-link.next"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(
        novel: Novel,
        document: Document,
    ): Novel {
        val pageElement = document.body().select("div#page")

        novel.imageUrl = pageElement.select("div.fic_image > img").attr("abs:src")
        novel.metadata["Author(s)"] = pageElement.select("span[property='author'] a").outerHtml()

        val genresElements = pageElement.select("span.wi_fic_genre a.fic_genre")
        novel.genres = genresElements.map { it.text() }
        novel.metadata["Genre(s)"] = genresElements.joinToString(", ") { it.outerHtml() }

        novel.longDescription = pageElement.select("div.wi_fic_desc").text()
        novel.rating = pageElement.select("meta[property='ratingValue']").attr("content")

        novel.metadata["Tags"] =
            pageElement.select("#etagme")?.joinToString(", ") { it.outerHtml() }
        novel.externalNovelId = document.getElementById("mypostid").attr("value")
        novel.metadata["PostId"] = novel.externalNovelId
        novel.chaptersCount = document.getElementById("chpcounter").attr("value").toLong()

        return novel
    }
    //endregion

    //region Chapters

    // The wi_gettocchp AJAX endpoint returns an HTML fragment where each chapter is an
    // <a class="toc_a"> inside <li class="toc_w">. Targeting the class avoids picking up
    // unrelated links, and text() is the reliable chapter name (the title attr is often empty).
    override fun chapterListSelector() = "a.toc_a[href]"

    override fun chapterFromElement(element: Element): WebPage {
        val url = element.attr("abs:href").ifBlank { element.attr("href") }
        val name = element.attr("title").ifBlank { element.text() }.trim()
        return WebPage(url, name)
    }

    override fun chapterListParse(
        novel: Novel,
        response: Response,
    ): List<WebPage> {
        val document = response.asJsoup()
        var elements = document.select(chapterListSelector())
        // Fallback: if the fragment structure changes, fall back to any anchor with an href.
        if (elements.isEmpty()) {
            elements = document.select("a[href]")
        }
        // ScribbleHub returns chapters newest-first; reverse so orderId ascends with release order.
        return elements.reversed().mapIndexed { index, element ->
            val chapter = chapterFromElement(element)
            chapter.orderId = index.toLong()
            chapter
        }
    }

    override fun chapterListRequest(novel: Novel): Request {
        // The TOC AJAX needs the numeric series id. Prefer the id captured during details
        // parsing, but fall back to extracting it from the series URL (/series/{id}/{slug}/)
        // so chapters still load even if novelDetailsParse never ran or was blocked.
        val scribbleNovelId =
            novel.externalNovelId
                ?: novel.metadata["PostId"]
                ?: novel.url.let { seriesIdRegex.find(it)?.groupValues?.get(1) }
                ?: throw Exception(INVALID_NOVEL)
        val url = "$baseUrl/wp-admin/admin-ajax.php"
        val formBody: RequestBody =
            FormBody
                .Builder()
                .add("action", "wi_gettocchp")
                .add("strSID", scribbleNovelId)
                .add("strmypostid", "0")
                .add("strFic", "yes")
                .build()
        return POST(url, headers = headers, body = formBody)
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
}
