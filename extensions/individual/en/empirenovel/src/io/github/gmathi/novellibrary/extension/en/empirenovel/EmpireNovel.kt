package io.github.gmathi.novellibrary.extension.en.empirenovel

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.*
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

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search-live?q=$encodedQuery"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val jsonArray = JSONArray(response.body!!.string())
        val novels = mutableListOf<Novel>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val title = item.getString("title")
            val slug = item.getString("slug")
            val novelUrl = "$baseUrl/novel/$slug"

            val novel = Novel(title, novelUrl, id)
            novel.imageUrl = item.optString("image", null)
            novel.metadata["slug"] = slug

            novels.add(novel)
        }

        return NovelsPage(novels, hasNextPage = false)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        throw Exception(NOT_USED)
    }

    override fun searchNovelsSelector() = throw Exception(NOT_USED)
    override fun searchNovelsNextPageSelector() = "a:contains(Last)"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        // Extract image from the book cover (try data-src first for lazy-loaded, then src)
        val imageElement = document.selectFirst("img.show_image")
        novel.imageUrl = imageElement?.attr("abs:data-src")?.takeIf { it.isNotEmpty() }
            ?: imageElement?.attr("abs:src")
            ?: document.selectFirst("div.book img")?.attr("abs:src")

        // Extract title
        val title = document.selectFirst("h1[itemprop=name]")?.text()?.trim()
        if (title != null) {
            novel.name = title
        }

        // Extract description
        val descriptionElement = document.selectFirst("dd[itemprop=description]")
        if (descriptionElement != null) {
            val fullDescription = descriptionElement.selectFirst("span#more")?.text()?.trim()
                ?: descriptionElement.text().trim()
            novel.longDescription = fullDescription
        }

        // Extract metadata from show_details section
        document.select("div.show_details div.d-flex").forEach {
            val parts = it.text().split(Regex("\\s{2,}"))
            if (parts.size >= 2) {
                val key = parts[0].trim()
                val value = parts.last().trim()
                novel.metadata[key] = value

                // Set specific fields
                when (key.lowercase()) {
                    "status" -> novel.metadata["Status"] = value
                    "author" -> {
                        novel.authors = listOf(value)
                        novel.metadata["Author"] = value
                    }
                }
            }
        }

        // Extract author from itemprop
        val author = document.selectFirst("span[itemprop=author] a")?.text()?.trim()
        if (author != null && novel.authors.isNullOrEmpty()) {
            novel.authors = listOf(author)
            novel.metadata["Author"] = author
        }

        // Extract genres/categories
        val genres = document.select("a.category[itemprop=genre]").map { it.text().trim() }
        if (genres.isNotEmpty()) {
            novel.genres = genres
            novel.metadata["Genres"] = genres.joinToString(", ")
        }

        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListRequest(novel: Novel): Request {
        return GET(novel.url, headers)
    }

    override fun chapterListSelector() = "a.chapter_link"

    override fun chapterFromElement(element: Element): WebPage {
        val url = element.attr("abs:href")
        val title = element.selectFirst("div.chapter")?.text()?.trim() ?: element.text().trim()
        return WebPage(url, title)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val allChapters = mutableListOf<WebPage>()
        var currentPage = 1
        var hasMoreChapters = true

        while (hasMoreChapters) {
            val pageUrl = if (currentPage == 1) {
                novel.url
            } else {
                "${novel.url}?page=$currentPage"
            }

            val pageResponse = client.newCall(GET(pageUrl, headers)).execute()
            val document = pageResponse.asJsoup()
            val chapters = document.select(chapterListSelector())

            if (chapters.isEmpty()) {
                hasMoreChapters = false
            } else {
                chapters.forEach { element ->
                    allChapters.add(chapterFromElement(element))
                }
                currentPage++
            }
        }

        // Reverse to get correct order (oldest to newest) and set order IDs
        return allChapters.reversed().mapIndexed { index, chapter ->
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
