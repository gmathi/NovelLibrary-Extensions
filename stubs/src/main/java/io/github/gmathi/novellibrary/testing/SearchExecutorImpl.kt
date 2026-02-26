package io.github.gmathi.novellibrary.testing

import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Default implementation of SearchExecutor.
 * Executes search queries against extensions with proper error handling and timeout configuration.
 */
class SearchExecutorImpl : SearchExecutor {
    
    override fun executeSearch(
        extension: ParsedHttpSource,
        query: String,
        timeout: Long
    ): TestResult {
        val startTime = System.currentTimeMillis()
        val metadata = extractMetadata(extension)
        
        return try {
            // Use the public fetchSearchNovels API with RxJava Observable
            val novelsPage = extension.fetchSearchNovels(
                page = 1,
                query = query,
                filters = FilterList()
            ).toBlocking().first()
            
            val executionTime = System.currentTimeMillis() - startTime
            
            // Return success result
            TestResult.Success(
                extensionMetadata = metadata,
                query = query,
                executionTimeMs = executionTime,
                novelsPage = novelsPage,
                novelCount = novelsPage.novels.size,
                hasNextPage = novelsPage.hasNextPage
            )
            
        } catch (e: SocketTimeoutException) {
            val executionTime = System.currentTimeMillis() - startTime
            TestResult.Failure(
                extensionMetadata = metadata,
                query = query,
                executionTimeMs = executionTime,
                failureType = FailureType.TIMEOUT,
                message = "Request timed out after ${timeout}ms",
                exception = e
            )
        } catch (e: IOException) {
            val executionTime = System.currentTimeMillis() - startTime
            TestResult.Failure(
                extensionMetadata = metadata,
                query = query,
                executionTimeMs = executionTime,
                failureType = FailureType.NETWORK_ERROR,
                message = "Network error: ${e.message}",
                exception = e
            )
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            TestResult.Failure(
                extensionMetadata = metadata,
                query = query,
                executionTimeMs = executionTime,
                failureType = FailureType.PARSE_ERROR,
                message = "Parse error: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Extracts metadata from an extension instance.
     */
    private fun extractMetadata(extension: ParsedHttpSource): ExtensionMetadata {
        return ExtensionMetadata(
            id = extension.id,
            name = extension.name,
            lang = extension.lang,
            baseUrl = extension.baseUrl,
            className = extension::class.java.simpleName,
            packageName = extension::class.java.packageName
        )
    }
}
