package io.github.gmathi.novellibrary.extension.en.novelbin

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

class NovelBin : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://novelbin.org"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Bin"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?keyword=${encodedQuery.replace(" ", "+")}&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select("div.list.list-novel").firstOrNull()?.select("div.row")
            ?.map { element ->
                searchNovelsFromElement(element)
            }

        val hasNextPage =
            document.select("li.next").firstOrNull() != null && document.select("li.next.disabled")
                .firstOrNull() == null

        return NovelsPage(novels ?: emptyList(), hasNextPage)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val novel = Novel(
            element.select("h3.novel-title").text(),
            element.select("h3.novel-title").select("a[href]").attr("abs:href"),
            this.id
        )
        novel.imageUrl = element.select("img.cover").attr("abs:src")
        return novel
    }

    override fun searchNovelsSelector() = "div.list.list-novel div.row"
    override fun searchNovelsNextPageSelector() = "li.next"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val booksElement = document.body().select("div.books")
        val infoElements = document.body().select("ul.info").first().children()
        novel.imageUrl = booksElement.select("div.book > img").attr("abs:src")

        infoElements.forEach { element ->
            val title = element.select("h3").text()
            val valueList = element.select("a[href]").map { it.text() }
            val rawValueList = element.select("a[href]")
                .joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }

            if (title.contains("Author")) {
                novel.authors = valueList
                novel.metadata["Author(s)"] = rawValueList
            } else if (title.contains("Genre")) {
                novel.genres = valueList
                novel.metadata["Genre(s)"] = rawValueList
            } else {
                novel.metadata[title] = rawValueList
            }
        }

        novel.longDescription = document.body().select("div.desc-text").joinToString(separator = "\n") { it.text() }
        novel.rating = (document.body().select("[itemprop=ratingValue]").first().text().toDouble() / 2).toString()
        novel.externalNovelId = document.selectFirst("#rating")?.attr("data-novel-id")

        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListSelector() = "li a"
    override fun chapterFromElement(element: Element): WebPage {
        val url = element.attr("href")
        val name = element.text()
        return WebPage(url, name)
    }

    override fun chapterListRequest(novel: Novel): Request {
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$baseUrl/ajax/chapter-archive?novelId=$id"
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
