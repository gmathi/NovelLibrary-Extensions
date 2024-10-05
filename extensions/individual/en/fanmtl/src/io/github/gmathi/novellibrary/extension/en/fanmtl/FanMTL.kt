package io.github.gmathi.novellibrary.extension.en.fanmtl

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_EXTERNAL_ID
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.regex.Pattern

class FanMTL : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://www.fanmtl.com/"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "FanMTL"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?status=all&sort=views&q=${encodedQuery.replace(" ", "+")}&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select(searchNovelsSelector()).map { element ->
            searchNovelsFromElement(element)
        }

        val isLastPage = document.select("div.paginator a.btn.link").lastOrNull()?.hasClass("active") ?: true
        return NovelsPage(novels, !isLastPage)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val aElement = element.selectFirst("a[href]")
        val novel = Novel(
            aElement.attr("title"),
            aElement.attr("abs:href"),
            this.id
        )
        novel.imageUrl = element.selectFirst("img").attr("abs:data-src")
        novel.rating = element.selectFirst("div.rating span.score").text()
        return novel
    }

    override fun searchNovelsSelector() = "div.list.manga-list div.book-detailed-item"
    override fun searchNovelsNextPageSelector() = "li.next"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val booksElement = document.body().select("div.book-info")
        val metaDataElement = booksElement.select("div.meta.box > p")

        novel.genres = metaDataElement[1].select("a").map { it.text().replace(" ,", "") }
        novel.longDescription =
            document.body().select("div.summary > p.content").joinToString(separator = "\n") { it.text() }

        val script = document.body().select("script").firstOrNull { it.outerHtml().contains("bookId") }?.childNode(0)?.outerHtml()
        val p = Pattern.compile("bookId\\s=\\s(.*?);", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
        val m = p.matcher(script ?: "")
        if (m.find()) {
            novel.externalNovelId = m.group(1)
        }

        novel.metadata["Author(s)"] = metaDataElement[0].select("a")
            .joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.attr("title")}</a>" }
        novel.metadata["Genre(s)"] = metaDataElement[2].select("a")
            .joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text().replace(" ,", "")}</a>" }
        novel.metadata["Status"] = metaDataElement[1].select("span").text()
        novel.chaptersCount = metaDataElement[3].select("span").text().toLong()

        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListSelector() = "ul#chapter-list > li"
    override fun chapterFromElement(element: Element): WebPage {
        val aElement = element.selectFirst("a")
        val url = aElement.attr("abs:href")
        val name = element.selectFirst(".chapter-title").text()
        return WebPage(url, name)
    }

    override fun chapterListRequest(novel: Novel): Request {
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$baseUrl/api/manga/$id/chapters?source=detail"
        return GET(url, headers)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).reversed().mapIndexed { index, element ->
            val chapter = chapterFromElement(element)
            chapter.orderId = index.toLong()
            chapter
        }
    }

    //endregion

    //region stubs

    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesSelector(): String = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesFromElement(element: Element): Novel =
        throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsSelector(): String = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsFromElement(element: Element): Novel =
        throw Exception(MISSING_IMPLEMENTATION)

    override fun popularNovelNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

//endregion

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}
