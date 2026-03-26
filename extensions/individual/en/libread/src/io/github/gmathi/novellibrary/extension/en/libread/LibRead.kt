package io.github.gmathi.novellibrary.extension.en.libread

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

class LibRead : ParsedHttpSource() {
    override val baseUrl: String
        get() = "https://libread.com"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = false
    override val name: String
        get() = "LibRead"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder =
        Headers
            .Builder()
            .add("User-Agent", defaultUserAgent)
            .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList,
    ): Request {
        val url = "$baseUrl/search"
        val formBody: RequestBody =
            FormBody
                .Builder()
                .add("searchkey", query)
                .build()
        return POST(url, headers, formBody)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels =
            document.select(searchNovelsSelector()).map { element ->
                searchNovelsFromElement(element)
            }

        // LibRead search doesn't paginate — single page of results
        return NovelsPage(novels, false)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val aElement = element.selectFirst("h3.tit a[href]")
        val novel =
            Novel(
                aElement.attr("title").ifBlank { aElement.text() },
                aElement.attr("abs:href"),
                this.id,
            )
        novel.imageUrl = element.selectFirst("div.pic img")?.attr("abs:src")
        novel.rating = element.selectFirst("div.core span")?.text()?.trim()
        novel.genres =
            element
                .select("div.desc div.item div.right a.novel")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
        val chapterText = element.selectFirst("span.s1")?.text()?.trim()
        if (chapterText != null) {
            novel.chaptersCount = chapterText.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            novel.metadata["Chapters"] = chapterText
        }
        val latestChapter = element.selectFirst("a.chapter")
        if (latestChapter != null) {
            novel.metadata["Latest Chapter"] = latestChapter.attr("title").trim()
        }
        return novel
    }

    override fun searchNovelsSelector() = "div.li-row"

    override fun searchNovelsNextPageSelector() = "li.next"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(
        novel: Novel,
        document: Document,
    ): Novel {
        // Extract bookId from script (needed for chapter list API)
        val script =
            document
                .body()
                .select("script")
                .firstOrNull { it.outerHtml().contains("bookId") }
                ?.childNode(0)
                ?.outerHtml()
        val p =
            Pattern.compile(
                "bookId\\s=\\s(.*?);",
                Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE,
            )
        val m = p.matcher(script ?: "")
        if (m.find()) {
            novel.externalNovelId = m.group(1)
        }

        novel.imageUrl = document.selectFirst("div.m-imgtxt div.pic img")?.attr("abs:src")

        novel.longDescription =
            document
                .select("div.m-desc div.txt div.inner p")
                .joinToString(separator = "\n") { it.text() }

        val ratingText = document.selectFirst("div.score p.vote")?.text()?.trim()
        if (ratingText != null) {
            val ratingMatch = Regex("([\\d.]+)\\s*/\\s*5").find(ratingText)
            if (ratingMatch != null) {
                novel.rating = ratingMatch.groupValues[1]
            }
        }

        val items = document.select("div.m-imgtxt div.item")
        for (item in items) {
            val label =
                item
                    .selectFirst("span.glyphicon")
                    ?.attr("title")
                    ?.trim()
                    ?.lowercase() ?: continue
            val right = item.selectFirst("div.right") ?: continue
            when (label) {
                "author" -> {
                    novel.metadata["Author(s)"] =
                        right
                            .select("a")
                            .joinToString(", ") {
                                "<a href=\"${it.attr("abs:href")}\">${it.attr("title")}</a>"
                            }
                }
                "genre" -> {
                    novel.genres =
                        right
                            .select("a")
                            .map { it.text().replace(",", "").trim() }
                            .filter { it.isNotEmpty() }
                    novel.metadata["Genre(s)"] =
                        right
                            .select("a")
                            .joinToString(", ") {
                                "<a href=\"${it.attr("abs:href")}\">${it.text().replace(",", "").trim()}</a>"
                            }
                }
                "status" -> {
                    novel.metadata["Status"] =
                        right.selectFirst("span a")?.text()?.trim()
                            ?: right.selectFirst("a")?.text()?.trim()
                            ?: right.text().trim()
                }
                "original language" -> {
                    novel.metadata["Original Language"] =
                        right.selectFirst("a")?.text()?.trim()
                }
            }
        }

        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListSelector() = "#idData > li, ul.chapter-list > li"

    override fun chapterFromElement(element: Element): WebPage {
        val aElement = element.selectFirst("a")
        val url = aElement.attr("abs:href")
        val name = aElement.attr("title").ifBlank { aElement.text() }
        return WebPage(url, name)
    }

    override fun chapterListParse(
        novel: Novel,
        response: Response,
    ): List<WebPage> {
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

    override fun latestUpdatesFromElement(element: Element): Novel = throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

    override fun popularNovelsRequest(page: Int): Request {
        val url = "$baseUrl/top/month?page=$page"
        return GET(url, headers)
    }

    override fun popularNovelsSelector(): String = "div.book-item div.book-detailed-item"

    override fun popularNovelsFromElement(element: Element): Novel {
        val aElement = element.selectFirst("div.title a[href]")
        val novel =
            Novel(
                aElement.attr("title"),
                aElement.attr("abs:href"),
                this.id,
            )
        novel.imageUrl = element.selectFirst("img")?.attr("abs:data-src")
        novel.rating = element.selectFirst("div.rating span.score")?.text()
        novel.genres = element.select("div.genres span").map { it.text() }
        novel.shortDescription = element.selectFirst("div.summary p")?.text()?.trim()
        novel.metadata["Readers"] =
            element
                .selectFirst("div.views span")
                ?.text()
                ?.replace("\u00A0", "")
                ?.trim()
        novel.metadata["Chapters"] = element.selectFirst("span.latest-chapter")?.attr("title")?.trim()
        novel.metadata["Reviews"] =
            element
                .selectFirst("span.rate-volumes")
                ?.text()
                ?.replace("(", "")
                ?.replace(")", "")
                ?.trim()
        return novel
    }

    override fun popularNovelNextPageSelector(): String = "div.paginator a.btn.link:not(.active)"

    //endregion
}
