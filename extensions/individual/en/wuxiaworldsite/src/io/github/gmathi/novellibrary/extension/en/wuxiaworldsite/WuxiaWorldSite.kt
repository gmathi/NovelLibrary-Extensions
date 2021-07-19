package io.github.gmathi.novellibrary.extension.en.wuxiaworldsite

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

class WuxiaWorldSite : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://wuxiaworldsite.co"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Full"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val reformattedQuery = query.replace(Regex("[^a-zA-Z0-9]"), "-")
        val url = "$baseUrl/search/$reformattedQuery&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()
        val novels = document.select(searchNovelsSelector())?.map { element ->
            searchNovelsFromElement(element)
        }

        val hasNextPage =
            document.select("li.next").firstOrNull() != null && document.select("li.next.disabled")
                .firstOrNull() == null

        return NovelsPage(novels ?: emptyList(), hasNextPage)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val aElement = element.selectFirst("a[href]")
        val novel = Novel(
            aElement.text(),
            aElement.attr("abs:href"),
            this.id
        )
        novel.imageUrl = aElement.selectFirst("img").attr("abs:src")
        return novel
    }

    override fun searchNovelsSelector() = "div.bz.item"
    override fun searchNovelsNextPageSelector() = "span:contains(>|)"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val booksElement = document.body().selectFirst("div.read-book")
        val genres = booksElement.select("div.tags a.a_tag_item")
        val infoElements = document.body().select("div.info").first().children()

        novel.imageUrl = booksElement.selectFirst("div.img-read img").attr("abs:src")
        novel.genres = genres.map { it.text() }

        novel.longDescription = booksElement.select("div.story-introduction-content p").joinToString(separator = "\n") { it.text() }
        novel.rating = (
            document.body().select("div.small > em > strong > span").first().text()
                .toDouble() / 2
            ).toString()
        novel.externalNovelId = document.selectFirst("#rating")?.attr("data-novel-id")

        novel.metadata["Author(s)"] = booksElement.select("i.fa.fa-user").joinToString(", ") { it.text() }
        novel.metadata["Genre(s)"] = genres.joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metadata["Source"] = infoElements[2].text()
        novel.metadata["Status"] = infoElements[3].text()

        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListSelector() = "select.chapter_jump option"
    override fun chapterFromElement(element: Element): WebPage {
        val url = "$baseUrl${element.attr("value")}"
        val name = element.text()
        return WebPage(url, name)
    }

    override fun chapterListRequest(novel: Novel): Request {
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$baseUrl/ajax-chapter-option?novelId=$id&currentChapterId="
        return GET(url, headers)
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
