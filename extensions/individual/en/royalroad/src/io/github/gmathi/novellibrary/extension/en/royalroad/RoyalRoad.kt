package io.github.gmathi.novellibrary.extension.en.royalroad

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Exceptions.NOT_USED
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder

class RoyalRoad : ParsedHttpSource() {
    override val id: Long
        get() = 5L
    override val baseUrl: String
        get() = "https://www.royalroad.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Royal Road"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder().add("User-Agent", USER_AGENT).add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/fictions/search?title=${encodedQuery.replace(" ", "+")}&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val titleElement = element.selectFirst(".fiction-title > a[href]")
        val novel = Novel(titleElement.text(), titleElement.attr("abs:href"), id)
        novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")

        // Author fallback: try span.author, then derive from CDN image path
        val authorText = element.selectFirst("span.author")?.text()
        novel.metadata["Author"] = if (authorText != null && authorText.length > 3) authorText.substring(3) else null
        if (novel.metadata["Author"] == null && novel.imageUrl?.startsWith("https://www.royalroadcdn.com/") == true) {
            novel.metadata["Author"] = novel.imageUrl?.substring(29, novel.imageUrl?.indexOf('/', 29) ?: 0)
        }

        novel.rating = element.selectFirst("span.star[title]")?.attr("title")

        // Description
        novel.longDescription = element.selectFirst("div.fiction-description")?.text() ?: element.selectFirst("div.margin-top-10.col-xs-12")?.text()
        novel.shortDescription = novel.longDescription?.split("\n")?.firstOrNull()

        // Parse genre tags
        novel.genres = element.select("a.fiction-tag").map { it.text() }

        // Origin marker from labels (e.g. "Original", "Fan Fiction")
        val labels = element.select("div.margin-bottom-10 > span.label.bg-blue-hoki")
        // novel.metadata["OriginMarker"] = labels.firstOrNull()?.ownText()
        novel.metadata["Status"] = labels.getOrNull(1)?.ownText()?.trim()

        // Parse stats from the search result row
        val statElements = element.select("div.row.stats div.col-sm-6")
        for (stat in statElements) {
            val text = stat.text().trim()
            when {
                text.contains("Followers", ignoreCase = true) -> novel.metadata["Readers"] = text

                text.contains("Pages", ignoreCase = true) -> novel.metadata["Pages"] = text

                text.contains("Views", ignoreCase = true) -> novel.metadata["Views"] = text

                text.contains("Chapters", ignoreCase = true) -> {
                    novel.metadata["Chapters"] = text
                    val chapterCount = text.replace(",", "").filter { it.isDigit() }.toLongOrNull()
                    if (chapterCount != null) novel.chaptersCount = chapterCount
                }
            }
            // Last updated date
            val timeElement = stat.selectFirst("time[datetime]")
            if (timeElement != null) {
                novel.metadata["LastUpdated"] = timeElement.text()
            }
        }

        return novel
    }

    override fun searchNovelsSelector() = "div.fiction-list-item"

    override fun searchNovelsNextPageSelector() = "a:contains(Last)"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(
        novel: Novel,
        document: Document,
    ): Novel {
        val body = document.body()

        // Title
        val title = body.selectFirst("div.fic-title h1")?.text()?.trim()
        if (title != null) novel.name = title

        // Cover image
        novel.imageUrl = body.selectFirst("div.cover-art-container img[src]")?.attr("abs:src")

        // Author
        val authorElement = body.selectFirst("div.fic-title h4 a[href*=/profile/]")
        val authorName = authorElement?.text()?.trim()
        if (authorName != null) {
            novel.authors = listOf(authorName)
            novel.metadata["Author"] = authorName
        }

        // Description
        val descriptionDiv = body.selectFirst("div.description div.hidden-content")
        if (descriptionDiv != null) {
            novel.longDescription = descriptionDiv.text().trim()
            novel.shortDescription = descriptionDiv.selectFirst("p")?.text()?.trim()
        }

        // Genres / tags
        val tags = body.select("a.fiction-tag").map { it.text().trim() }
        if (tags.isNotEmpty()) {
            novel.genres = tags
        }

        // Status (e.g. "ONGOING", "COMPLETED") from the label badges
        val labels = body.select("div.fiction-info span.label.bg-blue-hoki")
        novel.metadata["OriginMarker"] = labels.getOrNull(0)?.text()?.trim()
        novel.metadata["Status"] = labels.getOrNull(1)?.text()?.trim()

        // Warnings
        val warnings = body.select("div.fiction-info ul.list-inline li").map { it.text().trim() }
        if (warnings.isNotEmpty()) {
            novel.metadata["ContentWarnings"] = warnings.joinToString(", ")
        }

        // Rating - overall score from the star element's data-content attribute
        val overallStar = body.selectFirst("span.popovers[data-original-title=Overall Score]")
        if (overallStar != null) {
            val ratingText = overallStar.attr("data-content").replace(Regex("[^0-9.]"), "")
            if (ratingText.isNotEmpty()) novel.rating = ratingText
        }

        // Sub-ratings (style, story, grammar, character)
        body.select("div.stats-content span.popovers[data-original-title]").forEach { span ->
            val label = span.attr("data-original-title").trim()
            val value = span.attr("data-content").trim()
            when (label) {
                "Style Score" -> novel.metadata["StyleScore"] = value
                "Story Score" -> novel.metadata["StoryScore"] = value
                "Grammar Score" -> novel.metadata["GrammarScore"] = value
                "Character Score" -> novel.metadata["CharacterScore"] = value
            }
        }

        // Statistics - parse the right-column stats (views, followers, etc.)
        val statsUl = body.select("div.stats-content div.col-sm-6 ul.list-unstyled")
        if (statsUl.size >= 2) {
            val rightStats = statsUl[1]
            val items = rightStats.select("li")
            var i = 0
            while (i < items.size - 1) {
                val key = items[i].text().replace(":", "").trim().uppercase()
                val value = items[i + 1].text().trim()
                when {
                    key.contains("TOTAL VIEWS") -> novel.metadata["TotalViews"] = value
                    key.contains("AVERAGE VIEWS") -> novel.metadata["AverageViews"] = value
                    key.contains("FOLLOWERS") -> novel.metadata["Followers"] = value
                    key.contains("FAVORITES") -> novel.metadata["Favorites"] = value
                    key.contains("RATINGS") -> novel.metadata["Ratings"] = value
                    key.contains("PAGES") -> novel.metadata["Pages"] = value
                }
                i += 2
            }
        }

        // Chapter count from the table of contents header
        val chapterCountText = body.selectFirst("div.portlet-title span.label.pull-right")?.text()?.trim()
        if (chapterCountText != null) {
            val count = chapterCountText.replace(Regex("[^0-9]"), "").toLongOrNull()
            if (count != null) novel.chaptersCount = count
        }

        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListSelector() = "table#chapters a[href]"

    override fun chapterFromElement(element: Element) = WebPage(element.absUrl("href"), element.text())

    override fun chapterListRequest(novel: Novel): Request = GET(novel.url, headers = headers)

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
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) " + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36"
    }
}
