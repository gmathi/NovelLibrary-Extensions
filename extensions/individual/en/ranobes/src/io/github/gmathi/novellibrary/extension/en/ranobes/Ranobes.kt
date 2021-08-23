package io.github.gmathi.novellibrary.extension.en.ranobes

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Exceptions
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_EXTERNAL_ID
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.lang.asJsonNullFree
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import okhttp3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Ranobes : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://ranobes.net"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Ranobes"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", DEFAULT_USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/index.php?do=search"
        val requestBody = FormBody.Builder()
            .add("do", "search")
            .add("subaction", "search")
            .add("search_start", "$page")
            .add("full_search", "0")
            .add("result_from", "${((page - 1) * 10) + 1}")
            .add("story", query)
        return POST(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val titleElement = element.selectFirst("h2.title a") ?: throw Exception(Exceptions.INVALID_NOVEL)
        val novel = Novel(titleElement.text(), titleElement.absUrl("href"), id)
        val ratingElement = element.selectFirst("li.current-rating")
        ratingElement?.let {
            val rating = it.text().toInt() // 0-100
            val newRating = rating / 20F // Will be converted to 0.0 - 5.0
            novel.rating = newRating.toString()
        }
        return novel
    }

    override fun searchNovelsSelector() = "div.block.story.shortstory.mod-poster"
    override fun searchNovelsNextPageSelector() = "a#nextlink"
    //endregion

    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val body = document.body()
        novel.longDescription = body.select("[itemprop=description]").first().text()

        val authors = body.select("span[itemprop=alternateName]").map { it.text().replace("by ", "") }
        novel.metadata["Author(s)"] = body.select("span.tag_list[itemprop=creator]").html()
        novel.authors = authors

        novel.imageUrl = document.head().selectFirst("meta[property=\"og:image\"]").attr("content")
        novel.genres = document.head().selectFirst("meta[name=keywords]").attr("content").split(", ")

        return novel
    }

    //endregion

    //region Chapters

    override fun chapterListSelector() = throw Exception(MISSING_IMPLEMENTATION)
    override fun chapterFromElement(element: Element) = throw Exception(MISSING_IMPLEMENTATION)

    override fun chapterListRequest(novel: Novel): Request {
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$baseUrl/v2/chapter/$id/list?state=published&structured=true&direction=false"
        return GET(url, headers)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val chapters = ArrayList<WebPage>()
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val jsonString = response.body?.string() ?: throw Exception(Exceptions.NETWORK_ERROR)
        var chapterIndex = 0
        val volumesArray = JsonParser.parseString(jsonString)?.asJsonArray ?: throw Exception(Exceptions.PARSING_ERROR)
        volumesArray.forEach { volumeElement ->
            val volumeJson = volumeElement?.asJsonNullFree?.asJsonObject ?: throw Exception(Exceptions.PARSING_ERROR)
            volumeJson["chapters"].asJsonNullFree?.asJsonArray?.forEach { chapterElement ->
                val chapterJson = chapterElement?.asJsonNullFree?.asJsonObject ?: throw Exception(Exceptions.PARSING_ERROR)
                val chapterId = chapterJson["id"].asString
                val title = chapterJson["title"].asJsonNullFreeString
                val chapterUrl = "$baseUrl/books/$id/$chapterId/mobile"
                val index = "${chapterJson["volume_index"]}.${chapterJson["index"]}"
                val webPage = WebPage(chapterUrl, "$index $title")
                webPage.orderId = chapterIndex.toLong()
                chapterIndex++
                chapters.add(webPage)
            }
        }
        return chapters
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
}
