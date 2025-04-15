package io.github.gmathi.novellibrary.extension.en.neovel

import com.github.salomonbrys.kotson.get
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.Exceptions.NETWORK_ERROR
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.encodeBase64ToString
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

class Neovel : HttpSource() {

    override val id: Long
        get() = 3L
    override val baseUrl: String
        get() = "https://neoread.neovel.io/"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Neovel"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val url =
            baseUrl + "V2/books/search?language=ALL&filter=0&name=${query.encodeBase64ToString()}&sort=6&page=${page - 1}&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000"
        return GET(url, headers = headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val searchResults: ArrayList<Novel> = ArrayList()
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray

        jsonArray.forEach { result ->
            val resultObject = result.asJsonObject
            val id = resultObject["id"].asInt.toString()
            val novelUrl = baseUrl + "V1/book/details?bookId=$id&language=EN"
            val novel = Novel(novelUrl, this.id)
            resultObject["name"].asJsonNullFreeString?.let { novel.name = it }
            novel.imageUrl = baseUrl + "V2/book/image?bookId=$id&oldApp=false"
            novel.metadata["id"] = id
            novel.externalNovelId = id
            novel.rating = resultObject["rating"].asFloat.toString()
            searchResults.add(novel)
        }

        return NovelsPage(searchResults, false)
    }

    //endregion

    //region Novel Details
    override fun novelDetailsRequest(novel: Novel): Request {
        val url = baseUrl + "V1/page/book?bookId=${novel.externalNovelId}&language=EN"
        return GET(url, headers = headers)
    }

    override fun novelDetailsParse(novel: Novel, response: Response): Novel {
        val jsonString = response.body?.string() ?: return novel
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject ?: return novel

        val authorsDto = rootJsonObject["authorsDto"]?.asJsonArray
        val booksDto = rootJsonObject["bookDto"]?.asJsonObject

        val authorsList = authorsDto?.mapNotNull { it["name"].asJsonNullFreeString }
        novel.metadata["Author(s)"] = authorsList?.joinToString(", ")
        novel.authors = authorsList
        novel.longDescription = booksDto?.get("bookDescription")?.asString
        novel.chaptersCount = booksDto?.get("nbrReleases")?.asLong ?: 0L

        // If local fetch copy is empty, then get it from network
        if (neovelGenres == null) {
            getNeovelGenres()
        }
        neovelGenres?.let { map ->
            novel.genres =
                booksDto?.getAsJsonArray("genreIds")?.filter { map[it.asInt] != null }
                    ?.map { map[it.asInt]!! }
            novel.metadata["Genre(s)"] =
                booksDto?.getAsJsonArray("tagIds")?.filter { map[it.asInt] != null }
                    ?.joinToString(", ") { map[it.asInt]!! }
        }

        // If local fetch copy is empty, then get it from network
        if (neovelTags == null) {
            getNeovelTags()
        }
        neovelTags?.let { map ->
            novel.metadata["Tag(s)"] =
                booksDto?.getAsJsonArray("tagIds")?.filter { map[it.asInt] != null }
                    ?.joinToString(", ") { map[it.asInt]!! }
        }

        novel.metadata["Release Frequency"] = booksDto?.get("releaseFrequency")?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Chapter Read Count"] = booksDto?.get("chapterReadCount")?.asInt.toString()
        novel.metadata["Followers"] = booksDto?.get("followers")?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Mature Content"] = booksDto?.get("matureContent")?.asBoolean.toString()
        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListRequest(novel: Novel): Request {
        val novelId = novel.externalNovelId ?: novel.metadata["id"]
        val url = baseUrl + "V5/chapters?bookId=$novelId&language=EN"
        return GET(url, headers)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {

        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val releasesArray = JsonParser.parseString(jsonString)?.asJsonArray
            ?: throw Exception(NETWORK_ERROR)

        var orderId = 0L
        val chapters = ArrayList<WebPage>()
        val neovelChaptersArray =
            Gson().fromJson(releasesArray.toString(), Array<NeovelChapter>::class.java)

        neovelChaptersArray.sortedWith(
            Comparator<NeovelChapter> { o1, o2 ->
                val volumeDifference = (o1.chapterVolume * 100).toInt() - (o2.chapterVolume * 100).toInt()
                if (volumeDifference != 0) return@Comparator volumeDifference // returns the volume difference
                // else returns the chapter difference
                return@Comparator (o1.chapterNumber * 100).toInt() - (o2.chapterNumber * 100).toInt()
            }
        ).forEach {
            val url = "${baseUrl}read/${it.bookId}/${it.language}/${it.chapterId}"
            val chapterName = arrayListOf(
                it.chapterVolume,
                it.chapterNumber,
                it.chapterName ?: ""
            ).filter { name -> name.toString().isNotBlank() }.joinToString(" - ")
            val chapter = WebPage(url, chapterName)
            chapter.orderId = orderId++
            chapter.translatorSourceName = it.websiteName
            chapters.add(chapter)
        }

        return chapters
    }
    //endregion

    //region Genres & Tags
    private fun getNeovelGenres() {
        try {

            val request = GET(baseUrl + "V1/genres", headers)
            val response = client.newCall(request).safeExecute()
            val jsonString = response.body?.string() ?: return

            val jsonArray = JsonParser.parseString(jsonString)?.asJsonArray
            neovelGenres = HashMap()
            jsonArray?.forEach {
                val genreObject = it.asJsonObject
                neovelGenres!![genreObject["id"].asInt] = genreObject["en"].asString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNeovelTags() {
        try {

            val request = GET(baseUrl + "V1/tags", headers)
            val response = client.newCall(request).safeExecute()
            val jsonString = response.body?.string() ?: return

            val jsonArray = JsonParser.parseString(jsonString)?.asJsonArray
            neovelTags = HashMap()
            jsonArray?.forEach {
                val tagObject = it.asJsonObject
                neovelTags!![tagObject["id"].asInt] = tagObject["en"].asString
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    //endregion

    //region stubs
    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsParse(response: Response): NovelsPage =
        throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesParse(response: Response): NovelsPage =
        throw Exception(MISSING_IMPLEMENTATION)
    //endregion

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"

        // Below is to cache Neovel Genres & Tags
        private var neovelGenres: HashMap<Int, String>? = null
        private var neovelTags: HashMap<Int, String>? = null
    }

    private class NeovelChapter(
        @SerializedName("chapterId") val chapterId: Long,
        @SerializedName("bookId") val bookId: Long,
        @SerializedName("chapterName") val chapterName: String?,
        @SerializedName("chapterVolume") val chapterVolume: Double,
        @SerializedName("chapterNumber") val chapterNumber: Double,
        @SerializedName("chapterUrl") val chapterUrl: String?,
        @SerializedName("trueUrl") val trueUrl: String?,
        @SerializedName("websiteName") val websiteName: String?,
        @SerializedName("postDate") val postDate: String?,
        @SerializedName("downloadable") val downloadable: Boolean,
        @SerializedName("language") val language: String?,
        @SerializedName("alreadyRead") val alreadyRead: Boolean,
        @SerializedName("state") val state: Long,
        @SerializedName("premiumAccess") val premiumAccess: Boolean
    )
}
