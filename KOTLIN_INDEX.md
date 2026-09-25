# Kotlin Index — NovelLibrary-Extensions

Machine-oriented index of every Kotlin source file in this repository: path, package,
declared types, and public member signatures. Generated to help AI models and tools
navigate the codebase without re-scanning every file.

See `CODE_GRAPH.md` for module/class dependency and inheritance relationships.

Repo root: `c:\Users\Govani\Documents\GitHub\NovelLibrary-Extensions`

---

## Module map

| Module | Path | Role |
|---|---|---|
| `buildSrc` | `buildSrc/` | Gradle build-logic module (Kotlin). Defines shared build constants. |
| `core` | `core/` | Android library, **no Kotlin source**. Supplies `AndroidManifest.xml` + app icon resources shared by all extension APKs. |
| `stubs` | `stubs/` | Android library, Kotlin. Compile-time API surface (models, base source classes, network/util helpers) implemented at runtime by the host NovelLibrary app. Extensions depend on it as `compileOnly`. |
| `extensions/individual/en/*` | `extensions/individual/en/<name>/` | 14 sibling Android application modules, one per novel site, each implementing a single `HttpSource`/`ParsedHttpSource` subclass. |

---

## buildSrc

### `buildSrc/src/main/kotlin/Config.kt`
```kotlin
object Config {
    compileSdk: Int = 35
    minSdk: Int = 21
    targetSdk: Int = 35
    buildTools: String = "35.0.0"
    namespace: String = "io.github.gmathi.novellibrary.extension"
}
```

### `buildSrc/src/main/kotlin/Deps.kt`
```kotlin
object Deps {
    object kotlin { version = "1.4.32"; stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version" }
    jsoup = "org.jsoup:jsoup:1.13.1"
    okhttp = "com.squareup.okhttp3:okhttp:3.10.0"
}
```
Note: appears unused by the Groovy build scripts, which hardcode different versions
inline (e.g. okhttp `5.0.0-alpha.2` in `common-dependencies.gradle`). Legacy/dead code.

---

## stubs module

Source root: `stubs/src/main/java/io/github/gmathi/novellibrary/`

### model/database/Novel.kt
Package: `io.github.gmathi.novellibrary.model.database`
```kotlin
data class Novel(url: String, sourceId: Long) {
    constructor(name: String?, url: String, sourceId: Long)
    var name: String
    var externalNovelId: String?
    var imageUrl: String?
    var rating: String?
    var shortDescription: String?
    var longDescription: String?
    var genres: List<String>?
    var authors: List<String>?
    var illustrator: List<String>?
    var chaptersCount: Long
    var metadata: HashMap<String, String?>
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
```

### model/database/WebPage.kt
Package: `io.github.gmathi.novellibrary.model.database`
```kotlin
data class WebPage(url: String, chapterName: String) {
    var orderId: Long
    var translatorSourceName: String?
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}
```

### model/other/NovelsPage.kt
Package: `io.github.gmathi.novellibrary.model.other`
```kotlin
data class NovelsPage(novels: List<Novel>, hasNextPage: Boolean)
```

### model/source/Source.kt
Package: `io.github.gmathi.novellibrary.model.source`
```kotlin
interface Source {
    val id: Long
    val name: String
    fun fetchNovelDetails(novel: Novel): Observable<Novel>
    fun fetchChapterList(novel: Novel): Observable<List<WebPage>>
}
```

### model/source/CatalogueSource.kt
Package: `io.github.gmathi.novellibrary.model.source`
```kotlin
interface CatalogueSource : Source {
    val lang: String
    val supportsLatest: Boolean
    fun fetchPopularNovels(page: Int): Observable<NovelsPage>
    fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage>
    fun fetchLatestUpdates(page: Int): Observable<NovelsPage>
    fun getFilterList(): FilterList
}
```

### model/source/filter/Filter.kt
Package: `io.github.gmathi.novellibrary.model.source.filter`
```kotlin
sealed class Filter<T>(name: String, state: T) {
    class Header(name: String) : Filter<Boolean>
    class Separator(name: String = "") : Filter<Boolean>
    open class Select<V>(name: String, values: Array<V>, state: Int = 0) : Filter<Int>
    open class Text(name: String, state: String = "") : Filter<String>
    open class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>
    open class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int> {
        fun isIgnored(): Boolean
        fun isIncluded(): Boolean
        fun isExcluded(): Boolean
        companion object { const val STATE_IGNORE; const val STATE_INCLUDE; const val STATE_EXCLUDE }
    }
    open class Group<V>(name: String, val state: List<V>) : Filter<List<V>>
    class Sort(name: String, values: Array<String> = emptyArray(), state: Selection? = null) : Filter<Sort.Selection?> {
        data class Selection(index: Int, ascending: Boolean = true)
    }
}
```

### model/source/filter/FilterList.kt
Package: `io.github.gmathi.novellibrary.model.source.filter`
```kotlin
data class FilterList(list: List<Filter<*>>) : List<Filter<*>> by list {
    constructor(vararg fs: Filter<*>)
}
```

### model/source/online/HttpSource.kt
Package: `io.github.gmathi.novellibrary.model.source.online`
```kotlin
abstract class HttpSource : CatalogueSource {
    protected val network: NetworkHelper
    abstract val baseUrl: String
    open val versionId: Int
    override val id: Long
    val headers: Headers
    open val client: OkHttpClient
    open val defaultUserAgent: String

    protected open fun headersBuilder(): Headers.Builder
    override fun toString(): String

    override fun fetchPopularNovels(page: Int): Observable<NovelsPage>
    protected abstract fun popularNovelsRequest(page: Int): Request
    protected abstract fun popularNovelsParse(response: Response): NovelsPage

    override fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage>
    protected abstract fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request
    protected abstract fun searchNovelsParse(response: Response): NovelsPage

    override fun fetchLatestUpdates(page: Int): Observable<NovelsPage>
    protected abstract fun latestUpdatesRequest(page: Int): Request
    protected abstract fun latestUpdatesParse(response: Response): NovelsPage

    override fun fetchNovelDetails(novel: Novel): Observable<Novel>
    open fun novelDetailsRequest(novel: Novel): Request
    protected abstract fun novelDetailsParse(novel: Novel, response: Response): Novel

    override fun fetchChapterList(novel: Novel): Observable<List<WebPage>>
    protected open fun chapterListRequest(novel: Novel): Request
    protected abstract fun chapterListParse(novel: Novel, response: Response): List<WebPage>

    fun Novel.setUrlWithoutDomain(url: String)
    private fun getUrlWithoutDomain(orig: String): String
    override fun getFilterList(): FilterList

    companion object { const val DEFAULT_USER_AGENT: String }
}
```

### model/source/online/ParsedHttpSource.kt
Package: `io.github.gmathi.novellibrary.model.source.online`
```kotlin
abstract class ParsedHttpSource : HttpSource() {
    // Overrides the Response-based *Parse methods from HttpSource to throw Exception("Stub!")
    // and instead exposes Jsoup Document/Element based contracts:
    protected abstract fun popularNovelsSelector(): String
    protected abstract fun popularNovelsFromElement(element: Element): Novel
    protected abstract fun popularNovelNextPageSelector(): String?

    protected abstract fun searchNovelsSelector(): String
    protected abstract fun searchNovelsFromElement(element: Element): Novel
    protected abstract fun searchNovelsNextPageSelector(): String?

    protected abstract fun latestUpdatesSelector(): String
    protected abstract fun latestUpdatesFromElement(element: Element): Novel
    protected abstract fun latestUpdatesNextPageSelector(): String?

    protected abstract fun novelDetailsParse(novel: Novel, document: Document): Novel

    protected abstract fun chapterListSelector(): String
    protected abstract fun chapterFromElement(element: Element): WebPage
}
```

### network/Requests.kt
Package: `io.github.gmathi.novellibrary.network`
```kotlin
fun GET(url: String, headers: Headers = DEFAULT_HEADERS, cache: CacheControl = DEFAULT_CACHE_CONTROL): Request
fun POST(url: String, headers: Headers = DEFAULT_HEADERS, body: RequestBody = DEFAULT_BODY, cache: CacheControl = DEFAULT_CACHE_CONTROL): Request
```
Fully implemented (not a stub).

### network/NetworkHelper.kt
Package: `io.github.gmathi.novellibrary.network`
```kotlin
class NetworkHelper(context: Context) {
    val client: OkHttpClient      // throws Exception("Stub!")
    val cloudflareClient: OkHttpClient  // throws Exception("Stub!")
    fun isConnectedToNetwork(): Boolean // throws Exception("Stub!")
}
```

### util/Exceptions.kt
Package: `io.github.gmathi.novellibrary.util`
```kotlin
object Exceptions {
    const val INVALID_NOVEL: String
    const val MISSING_IMPLEMENTATION: String
    const val INVALID_PARAMETERS: String
    const val MISSING_EXTERNAL_ID: String
    const val NETWORK_ERROR: String
    const val PARSING_ERROR: String
    const val MISSING_SOURCE_ID: String
    const val NOT_USED: String
}
```

### util/lang/JavaLangExt.kt
Package: `io.github.gmathi.novellibrary.util.lang`
```kotlin
fun String?.containsCaseInsensitive(other: String?): Boolean
val JsonElement.asJsonNullFreeString: String?
val JsonElement.asJsonNullFree: JsonElement?
val String?.urlEncoded: String
val String.fixMalformed: String
fun String.fixMalformedWithHost(host: String?, protocol: String?): String
val String?.getUrlDomain: String?
```

### util/network/JsoupExtensions.kt
Package: `io.github.gmathi.novellibrary.util.network`
```kotlin
fun Response.asJsoup(html: String? = null): Document  // stub, throws Exception("Stub!")
```

### util/network/OkHttpExt.kt
Package: `io.github.gmathi.novellibrary.util.network`
```kotlin
fun Call.safeExecute(): Response  // stub, throws Exception("Stub!")
```

### util/system/Base64Ext.kt
Package: `io.github.gmathi.novellibrary.util.system`
```kotlin
fun String.encodeBase64ToString(): String
fun String.encodeBase64ToByteArray(): ByteArray
fun ByteArray.encodeBase64ToString(): String
fun String.decodeBase64(): String
fun String.decodeBase64ToByteArray(): ByteArray
fun ByteArray.decodeBase64ToString(): String
fun ByteArray.encodeBase64(): ByteArray   // stub
fun ByteArray.decodeBase64(): ByteArray   // stub
```

### testing/ package
Package: `io.github.gmathi.novellibrary.testing`
Self-test/validation harness for exercising extensions (not consumed by extension code).
15 files: `ExtensionMetadata.kt`, `ExtensionDiscovery.kt`, `TestExtensionDiscovery.kt`,
`TestResult.kt`, `ValidationResult.kt`, `ValidationViolation.kt`, `FailureType.kt`,
`ExtensionSearchTestRunner.kt`, `TestExtensionSearchTestRunner.kt`, `ReportGenerator.kt`,
`ReportGeneratorImpl.kt`, `ResultValidator.kt`, `SearchExecutor.kt`, `SearchExecutorImpl.kt`,
`TestConfig.kt`, `TestOrchestration.kt`, `TestOrchestrator.kt`.
Not part of the extension-authoring API graph — used by repo-internal validation tooling.

---

## Extensions (extensions/individual/en/*)

Each row is one Android application module. `id`/`baseUrl` are literal values read from
source where the extension overrides them explicitly (blank = inherited default / not
overridden as a simple literal).

| Extension | Class | Superclass | id | baseUrl | Main file |
|---|---|---|---|---|---|
| boxnovel | `BoxNovel` | `ParsedHttpSource` | — | `https://boxnovel.com` | `extensions/individual/en/boxnovel/src/io/github/gmathi/novellibrary/extension/en/boxnovel/BoxNovel.kt` |
| empirenovel | `EmpireNovel` | `ParsedHttpSource` | `13L` | `https://www.empirenovel.com` | `extensions/individual/en/empirenovel/src/io/github/gmathi/novellibrary/extension/en/empirenovel/EmpireNovel.kt` |
| fanmtl | `FanMTL` | `ParsedHttpSource` | — | `https://www.fanmtl.com/` | `extensions/individual/en/fanmtl/src/io/github/gmathi/novellibrary/extension/en/fanmtl/FanMTL.kt` |
| jpmtl | `JPMTL` | `ParsedHttpSource` | — | `https://jpmtl.com` | `extensions/individual/en/jpmtl/src/io/github/gmathi/novellibrary/extension/en/jpmtl/JPMTL.kt` |
| libread | `LibRead` | `ParsedHttpSource` | — | `https://libread.com` | `extensions/individual/en/libread/src/io/github/gmathi/novellibrary/extension/en/libread/LibRead.kt` |
| lnmtl | `LNMTL` | `ParsedHttpSource` | `4L` | `https://lnmtl.com/` | `extensions/individual/en/lnmtl/src/io/github/gmathi/novellibrary/extension/en/lnmtl/LNMTL.kt` |
| novelbin | `NovelBin` | `ParsedHttpSource` | — | `https://novelbin.me` | `extensions/individual/en/novelbin/src/io/github/gmathi/novellibrary/extension/en/novelbin/NovelBin.kt` |
| novelbuddy | `NovelBuddy` | **`HttpSource`** | — | `https://novelbuddy.me` | `extensions/individual/en/novelbuddy/src/io/github/gmathi/novellibrary/extension/en/novelbuddy/NovelBuddy.kt` |
| novelfull | `NovelFull` | `ParsedHttpSource` | `7L` | `https://novelfull.com` | `extensions/individual/en/novelfull/src/io/github/gmathi/novellibrary/extension/en/novelfull/NovelFull.kt` |
| purrfiction | `PurrFiction` | **`HttpSource`** | `3L` | `https://purrfiction.io/` | `extensions/individual/en/purrfiction/src/io/github/gmathi/novellibrary/extension/en/purrfiction/PurrFiction.kt` |
| ranobes | `Ranobes` | `ParsedHttpSource` | — | `https://ranobes.net` | `extensions/individual/en/ranobes/src/io/github/gmathi/novellibrary/extension/en/ranobes/Ranobes.kt` |
| royalroad | `RoyalRoad` | `ParsedHttpSource` | `5L` | `https://www.royalroad.com` | `extensions/individual/en/royalroad/src/io/github/gmathi/novellibrary/extension/en/royalroad/RoyalRoad.kt` |
| scribblehub | `ScribbleHub` | `ParsedHttpSource` | `6L` | `https://www.scribblehub.com` | `extensions/individual/en/scribblehub/src/io/github/gmathi/novellibrary/extension/en/scribblehub/ScribbleHub.kt` |
| wuxiaworldsite | `WuxiaWorldSite` | `ParsedHttpSource` | — | `https://wuxiaworldsite.co` | `extensions/individual/en/wuxiaworldsite/src/io/github/gmathi/novellibrary/extension/en/wuxiaworldsite/WuxiaWorldSite.kt` |

All packages follow the pattern `io.github.gmathi.novellibrary.extension.en.<lowercasename>`.

### Common override set (ParsedHttpSource-based extensions)

Nearly all `ParsedHttpSource` extensions implement the same member set:
```
baseUrl, lang, supportsLatest, name, client, headersBuilder()
searchNovelsRequest(), searchNovelsParse()/searchNovelsFromElement()/searchNovelsSelector()/searchNovelsNextPageSelector()
novelDetailsParse(novel, document)
chapterListSelector(), chapterFromElement(), chapterListRequest(), chapterListParse()
popularNovels*()   -> stub: throws Exception(Exceptions.MISSING_IMPLEMENTATION) or NOT_USED
latestUpdates*()   -> stub: throws Exception(Exceptions.MISSING_IMPLEMENTATION) or NOT_USED
```
This means most extensions are search-driven only; popular/latest browsing throws for them.
**Exception: `NovelBuddy`** fully implements `popularNovels*`/`latestUpdates*` (and sets
`supportsLatest = true`) since its JSON API natively supports sort-by-popular/latest listing.

`novelbuddy` and `purrfiction` extend `HttpSource` directly (not `ParsedHttpSource`) because
their target sites expose a JSON API, so they parse Gson `JsonObject`/`JsonParser` from
`Response` bodies instead of Jsoup `Document`/`Element`.

---

## Shared utility usage across extensions

| Utility | Source | Used by |
|---|---|---|
| `Exceptions.MISSING_IMPLEMENTATION` / `NOT_USED` / etc. | `stubs` `util/Exceptions.kt` | virtually all extensions (stubbed popular/latest methods) |
| `safeExecute()` | `stubs` `util/network/OkHttpExt.kt` | lnmtl, purrfiction |
| `asJsonNullFreeString` / `asJsonNullFree` | `stubs` `util/lang/JavaLangExt.kt` | ranobes, purrfiction |
| `encodeBase64ToString()` | `stubs` `util/system/Base64Ext.kt` | purrfiction |
| `asJsoup()` | `stubs` `util/network/JsoupExtensions.kt` | all `ParsedHttpSource` extensions |
| `GET()` / `POST()` | `stubs` `network/Requests.kt` | all extensions |
| `Novel`, `WebPage`, `NovelsPage`, `FilterList` | `stubs` `model/**` | all extensions (return/param types of every overridden method) |

Third-party libraries used directly by extensions: `com.google.gson.*` (novelbuddy, lnmtl,
purrfiction, ranobes, jpmtl), `org.json.JSONArray` (empirenovel), `org.jsoup.nodes.{Document,Element}`
(all Jsoup-based extensions), `okhttp3.{Headers,OkHttpClient,Request,Response,FormBody}` (all).
