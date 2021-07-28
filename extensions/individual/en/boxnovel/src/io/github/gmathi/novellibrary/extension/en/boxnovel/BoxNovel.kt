package io.github.gmathi.novellibrary.extension.en.boxnovel

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class BoxNovel : ParsedHttpSource() {

    override val id: Long
        get() = 5L
    override val baseUrl: String
        get() = "https://boxnovel.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Box Novel"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/?s=${encodedQuery.replace(" ", "+")}&post_type=wp-manga"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val titleElement = element.selectFirst("div.post-title a")
        val novel = Novel(titleElement.text(), titleElement.attr("abs:href"), id)
        novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
        novel.authors = element.selectFirst("div.post-content_item.mg_author div.summary-content")?.children()?.map { it.text() }
        novel.genres = element.selectFirst("div.post-content_item.mg_genres div.summary-content")?.children()?.map { it.text() }
        novel.rating = element.selectFirst("span.score.font-meta.total_votes").text()
        return novel
    }

    override fun searchNovelsSelector() = "div.c-tabs-item__content"
    override fun searchNovelsNextPageSelector() = "a:contains(Last)"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        novel.imageUrl = document.body().selectFirst("div.summary_image img[src]")?.attr("abs:src")
        novel.longDescription = document.body().selectFirst("div.description-summary")?.text()
        document.body().select("div.post-content_item")?.forEach {
            val heading = it.select("div.summary-heading")?.first()?.text() ?: return@forEach
            val value = it.select("div.summary-content")?.first()?.children()?.first()?.html()
            novel.metadata[heading] = value
        }
        novel.chaptersCount = document.body().select("li.wp-manga-chapter a")?.count()?.toLong() ?: 0L
        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListRequest(novel: Novel): Request {
        return GET(novel.url, headers = headers)
    }
    override fun chapterListSelector() = "li.wp-manga-chapter a"
    override fun chapterFromElement(element: Element) = WebPage(element.absUrl("href"), element.text())
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
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}
