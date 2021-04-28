package io.github.gmathi.novellibrary.model.source.online

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.CatalogueSource
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.network.NetworkHelper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.net.URI
import java.net.URISyntaxException

/**
 * A simple implementation for sources from a website.
 */
@Suppress("unused", "unused_parameteNetworkHelperr", "ThrowableNotThrown")
abstract class HttpSource : CatalogueSource {

    /**
     * Network service.
     */
    protected val network: NetworkHelper = throw Exception("Stub!")

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    abstract val baseUrl: String

    /**
     * Version id used to generate the source id. If the site completely changes and urls are
     * incompatible, you may increase this value and it'll be considered as a new source.
     */
    open val versionId: Int = throw Exception("Stub!")

    /**
     * Id of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string: sourcename/language/versionId
     * Note the generated id sets the sign bit to 0.
     */
    override val id: Long = throw Exception("Stub!")

    /**
     * Headers used for requests.
     */
    val headers: Headers = throw Exception("Stub!")

    /**
     * Default network client for doing requests.
     */
    open val client: OkHttpClient = throw Exception("Stub!")

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    protected open fun headersBuilder(): Headers.Builder {
        throw Exception("Stub!")
    }

    /**
     * Visible name of the source.
     */
    override fun toString(): String {
        throw Exception("Stub!")
    }


    /**
     * Returns an observable containing a page with a list of novels. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     */
    override fun fetchPopularNovels(page: Int): Observable<NovelsPage> {
        throw Exception("Stub!")
    }

    /**
     * Returns the request for the popular novels given the page.
     *
     * @param page the page number to retrieve.
     */
    protected abstract fun popularNovelsRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun popularNovelsParse(response: Response): NovelsPage


    /**
     * Returns an observable containing a page with a list of novels. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override fun fetchSearchNovels(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<NovelsPage> {
        throw Exception("Stub!")
    }

    /**
     * Returns the request for the search novel query given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    protected abstract fun searchNovelsRequest(
        page: Int,
        query: String,
        filters: FilterList
    ): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun searchNovelsParse(response: Response): NovelsPage


    /**
     * Returns an observable containing a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    override fun fetchLatestUpdates(page: Int): Observable<NovelsPage> {
        throw Exception("Stub!")
    }

    /**
     * Returns the request for latest novel given the page.
     *
     * @param page the page number to retrieve.
     */
    protected abstract fun latestUpdatesRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun latestUpdatesParse(response: Response): NovelsPage


    /**
     * Returns an observable with the updated details for a novel. Normally it's not needed to
     * override this method.
     *
     * @param novel the novel to be updated.
     */
    override fun fetchNovelDetails(novel: Novel): Observable<Novel> {
        throw Exception("Stub!")
    }

    /**
     * Returns the request for the details of a novel. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param novel the novel to be updated.
     */
    open fun novelDetailsRequest(novel: Novel): Request {
        throw Exception("Stub!")
    }

    /**
     * Parses the response from the site and returns the details of a novel.
     *
     * @param response the response from the site.
     */
    protected abstract fun novelDetailsParse(novel: Novel, response: Response): Novel


    /**
     * Returns an observable with the updated chapter list for a novel. Normally it's not needed to
     * override this method.
     *
     * @param novel the novel to look for chapters.
     */
    override fun fetchChapterList(novel: Novel): Observable<List<WebPage>> {
        throw Exception("Stub!")
    }

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param novel the novel to look for chapters.
     */
    protected open fun chapterListRequest(novel: Novel): Request {
        throw Exception("Stub!")
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    protected abstract fun chapterListParse(novel: Novel, response: Response): List<WebPage>


    /**
     * Assigns the url of the novel without the scheme and domain. It saves some redundancy from
     * database and the urls could still work after a domain change.
     *
     * @param url the full url to the novel.
     */
    fun Novel.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Returns the url of the given string without the scheme and domain.
     *
     * @param orig the full url.
     */
    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList(): FilterList {
        throw Exception("Stub!")
    }

}
