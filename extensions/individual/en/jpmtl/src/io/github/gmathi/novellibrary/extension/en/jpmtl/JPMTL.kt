package io.github.gmathi.novellibrary.extension.en.jpmtl

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_EXTERNAL_ID
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.lang.asJsonNullFree
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class JPMTL : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://jpmtl.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "JPMTL"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", DEFAULT_USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/v2/book/show/browse?query=$encodedQuery&categories=&content_type=0&direction=0&page=$page&limit=25&type=5&status=all&language=3&exclude_categories="
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val searchResults: ArrayList<Novel> = ArrayList()
        val jsonString = response.body?.string() ?: throw Exception(Exceptions.NETWORK_ERROR)
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray

        jsonArray.forEach { result ->
            val resultObject = result.asJsonObject
            val id = resultObject["id"].asInt.toString()
            val novelUrl = "$baseUrl/v2/book/$id"
            val novel = Novel(novelUrl, this.id)
            resultObject["title"].asJsonNullFreeString?.let { novel.name = it }
            novel.imageUrl = resultObject["cover"].asJsonNullFreeString
            novel.metadata["id"] = id
            novel.externalNovelId = id
            novel.rating = resultObject["rating"]?.asJsonNullFree?.asFloat.toString()
            novel.genres = resultObject["genres"].asJsonNullFree?.asJsonArray?.map { it.asJsonObject.get("name").asString }
            searchResults.add(novel)
        }

        return NovelsPage(searchResults, hasNextPage = jsonArray.count() == 25) // 25 is the limit on the items per page.
    }

    override fun searchNovelsFromElement(element: Element) = throw Exception(MISSING_IMPLEMENTATION)
    override fun searchNovelsSelector() = throw Exception(MISSING_IMPLEMENTATION)
    override fun searchNovelsNextPageSelector() = throw Exception(MISSING_IMPLEMENTATION)
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document) = throw Exception(MISSING_IMPLEMENTATION)
    override fun novelDetailsParse(novel: Novel, response: Response): Novel {
        val jsonString = response.body?.string() ?: throw Exception(Exceptions.NETWORK_ERROR)
        val json = JsonParser.parseString(jsonString).asJsonObject
        json["author"]?.asJsonNullFreeString?.let { novel.authors = listOf(it) }
        json["synopsis"]?.asJsonNullFreeString?.let { novel.longDescription = it }
        json["chapter_count"]?.asJsonNullFree?.asLong?.let { novel.chaptersCount = it }

        // Metadata
        json["status"]?.asJsonNullFreeString?.let { novel.metadata["status"] = it }
        json["created_at"]?.asJsonNullFreeString?.let { novel.metadata["created_at"] = it }
        json["updated_at"]?.asJsonNullFreeString?.let { novel.metadata["updated_at"] = it }
        json["content_warnings"]?.asJsonNullFreeString?.let { novel.metadata["content_warnings"] = it }
        json["type"]?.asJsonNullFreeString?.let { novel.metadata["type"] = it }
        json["alias"]?.asJsonNullFree?.asJsonArray?.joinToString { it.asJsonObject.get("name").asString }?.let { novel.metadata["status"] = it }
        json["raw_link"]?.asJsonNullFreeString?.let { novel.metadata["raw_link"] = it }
        json["content_type"]?.asJsonNullFreeString?.let { novel.metadata["content_type"] = it }
        json["language"]?.asJsonNullFreeString?.let { novel.metadata["language"] = it }
        json["upcoming"]?.asJsonNullFreeString?.let { novel.metadata["upcoming"] = it }
        json["dmca"]?.asJsonNullFreeString?.let { novel.metadata["dmca"] = it }
        json["dmca_by"]?.asJsonNullFreeString?.let { novel.metadata["dmca_by"] = it }
        json["accepted"]?.asJsonNullFreeString?.let { novel.metadata["accepted"] = it }
        json["pen_name"]?.asJsonNullFreeString?.let { novel.metadata["pen_name"] = it }
        json["genre_name"]?.asJsonNullFreeString?.let {
            novel.metadata["genre_name"] = it
            // novel.genres = listOf(it)
        }

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
