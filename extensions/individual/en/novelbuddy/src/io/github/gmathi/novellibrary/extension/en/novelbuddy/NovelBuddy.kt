package io.github.gmathi.novellibrary.extension.en.novelbuddy

import com.github.salomonbrys.kotson.get
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_EXTERNAL_ID
import io.github.gmathi.novellibrary.util.Exceptions.NETWORK_ERROR
import io.github.gmathi.novellibrary.util.Exceptions.PARSING_ERROR
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URLEncoder

/**
 * NovelBuddy moved from novelbuddy.com to novelbuddy.me and was rebuilt as a Next.js SPA.
 * The site no longer server-renders novel/chapter data into static HTML (pages ship as
 * `animate-pulse` skeletons), so this extension talks directly to the site's own JSON API
 * at api.novelbuddy.me instead of scraping HTML with Jsoup selectors.
 *
 * The API's response shape is inconsistent across entries (some novels omit fields like
 * "genres" entirely, or return unexpected types), so every JSON access below uses safe
 * casting (`as?`) instead of Gson's throwing `.asJsonObject`/`.asJsonArray`/`.asXxx()`
 * accessors to avoid crashing on malformed or missing data.
 */
class NovelBuddy : HttpSource() {
    override val baseUrl: String
        get() = "https://novelbuddy.me"

    private val apiUrl: String
        get() = "https://api.novelbuddy.me"

    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Buddy"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder =
        Headers
            .Builder()
            .add("User-Agent", defaultUserAgent)
            .add("Referer", "$baseUrl/")

    //region Safe JSON helpers
    // Gson's asJsonObject/asJsonArray/asXxx() throw when the element isn't actually that
    // type. These helpers return null instead so a single malformed field can't crash parsing.
    private fun JsonElement?.safeObject(): JsonObject? = this as? JsonObject

    private fun JsonElement?.safeArray(): JsonArray? = this as? JsonArray

    private fun JsonElement?.safeString(): String? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        if (!primitive.isString && !primitive.isNumber && !primitive.isBoolean) return null
        val text = primitive.asString
        return text.ifBlank { null }
    }

    private fun JsonElement?.safeLong(): Long? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        return if (primitive.isNumber) primitive.asLong else null
    }

    private fun JsonElement?.safeFloat(): Float? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        return if (primitive.isNumber) primitive.asFloat else null
    }

    private fun JsonElement?.safeBoolean(): Boolean? {
        val primitive = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return null
        return if (primitive.isBoolean) primitive.asBoolean else null
    }

    private fun jsonObjectOf(jsonString: String): JsonObject? =
        try {
            JsonParser.parseString(jsonString).safeObject()
        } catch (e: Exception) {
            null
        }

    /**
     * The API returns "summary" fields as raw HTML (e.g. "<p>...</p><br />..."). Strip the
     * markup down to readable plain text, using blank lines for block/line breaks.
     */
    private fun String.htmlToPlainText(): String {
        val body = Jsoup.parse(this).body()
        val sb = StringBuilder()

        fun traverse(node: Node) {
            when (node) {
                is TextNode -> sb.append(node.text())
                is Element -> {
                    when (node.tagName().lowercase()) {
                        "br" -> sb.append("\n")
                        "p", "div" -> {
                            if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append("\n")
                            node.childNodes().forEach(::traverse)
                            if (!sb.endsWith("\n")) sb.append("\n")
                        }
                        else -> node.childNodes().forEach(::traverse)
                    }
                }
                else -> Unit
            }
        }

        body.childNodes().forEach(::traverse)

        return sb
            .toString()
            .lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
    //endregion

    //region Search Novel
    override fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$apiUrl/titles/search?q=$encodedQuery&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage = parseNovelsPageResponse(response)
    //endregion

    //region Popular / Latest Novels
    override fun popularNovelsRequest(page: Int): Request {
        val url = "$apiUrl/titles/search?sort=popular&page=$page"
        return GET(url, headers)
    }

    override fun popularNovelsParse(response: Response): NovelsPage = parseNovelsPageResponse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiUrl/titles/search?sort=latest&page=$page"
        return GET(url, headers)
    }

    override fun latestUpdatesParse(response: Response): NovelsPage = parseNovelsPageResponse(response)

    private fun parseNovelsPageResponse(response: Response): NovelsPage {
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val data = jsonObjectOf(jsonString)?.get("data").safeObject() ?: throw Exception(PARSING_ERROR)
        val items = data["items"].safeArray() ?: throw Exception(PARSING_ERROR)

        val novels = items.mapNotNull { it.safeObject()?.let(::novelFromJsonObject) }
        val pagination = data["pagination"].safeObject()
        val hasNextPage = pagination?.get("has_next").safeBoolean() ?: false

        return NovelsPage(novels, hasNextPage)
    }

    private fun novelFromJsonObject(obj: JsonObject): Novel {
        val hsid = obj["id"].safeString() ?: return Novel("Unknown - Not Found!", baseUrl, id)
        val relativeUrl = obj["url"].safeString() ?: "/$hsid"
        val novel = Novel(obj["name"].safeString() ?: "Unknown - Not Found!", baseUrl + relativeUrl, id)
        novel.externalNovelId = hsid
        novel.imageUrl = obj["cover"].safeString()
        novel.rating = obj["rating"].safeFloat()?.toString()
        novel.shortDescription = obj["summary"].safeString()?.htmlToPlainText()
        novel.genres =
            obj["genres"].safeArray()?.mapNotNull { it.safeObject()?.get("name")?.safeString() }
        obj["stats"].safeObject()?.let { stats ->
            novel.chaptersCount = stats["chapters_count"].safeLong() ?: 0L
            novel.metadata["Views"] = stats["views"].safeLong()?.toString()
            novel.metadata["Bookmarks"] = stats["bookmarks_count"].safeLong()?.toString()
        }
        return novel
    }
    //endregion

    //region Novel Details
    override fun novelDetailsRequest(novel: Novel): Request {
        val hsid = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$apiUrl/titles/$hsid"
        return GET(url, headers)
    }

    override fun novelDetailsParse(
        novel: Novel,
        response: Response,
    ): Novel {
        val jsonString = response.body?.string() ?: return novel
        val title =
            jsonObjectOf(jsonString)
                ?.get("data")
                .safeObject()
                ?.get("title")
                .safeObject() ?: return novel

        title["name"].safeString()?.let { novel.name = it }
        novel.imageUrl = title["cover"].safeString() ?: novel.imageUrl
        novel.longDescription = title["summary"].safeString()?.htmlToPlainText()
        novel.shortDescription = novel.longDescription

        val authors =
            title["authors"]
                .safeArray()
                ?.mapNotNull { it.safeObject()?.get("name")?.safeString() }
                ?.distinct()
        if (!authors.isNullOrEmpty()) {
            novel.authors = authors
            novel.metadata["Author(s)"] = authors.joinToString(", ")
        }

        val genres = title["genres"].safeArray()?.mapNotNull { it.safeObject()?.get("name")?.safeString() }
        if (!genres.isNullOrEmpty()) {
            novel.genres = genres
            novel.metadata["Genre(s)"] = genres.joinToString(", ")
        }

        val tags = title["tags"].safeArray()?.mapNotNull { it.safeObject()?.get("name")?.safeString() }
        if (!tags.isNullOrEmpty()) {
            novel.metadata["Tags"] = tags.joinToString(", ")
        }

        novel.metadata["Status"] = title["status"].safeString()
        novel.metadata["Type"] = title["type"].safeObject()?.get("name")?.safeString()

        title["stats"].safeObject()?.let { stats ->
            novel.chaptersCount = stats["chapters_count"].safeLong() ?: novel.chaptersCount
            novel.metadata["Views"] = stats["views"].safeLong()?.toString()
            novel.metadata["Bookmarks"] = stats["bookmarks_count"].safeLong()?.toString()
            novel.metadata["Reviews"] = stats["reviews_count"].safeLong()?.toString()
        }

        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListRequest(novel: Novel): Request {
        val hsid = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$apiUrl/titles/$hsid/chapters"
        return GET(url, headers)
    }

    override fun chapterListParse(
        novel: Novel,
        response: Response,
    ): List<WebPage> {
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val data = jsonObjectOf(jsonString)?.get("data").safeObject() ?: throw Exception(PARSING_ERROR)
        val chaptersArray = data["chapters"].safeArray() ?: throw Exception(PARSING_ERROR)

        // The API returns chapters newest-first; reverse so orderId ascends with release order.
        return chaptersArray
            .mapNotNull { it.safeObject() }
            .reversed()
            .mapIndexed { index, chapterJson ->
                val relativeUrl = chapterJson["url"].safeString() ?: ""
                val chapterName = chapterJson["name"].safeString() ?: "Chapter ${index + 1}"
                val webPage = WebPage(baseUrl + relativeUrl, chapterName)
                webPage.orderId = index.toLong()
                webPage
            }
    }
    //endregion
}
