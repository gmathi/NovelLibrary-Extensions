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
            // Create search request with page=1, query, and empty FilterList
            val request = extension.searchNovelsRequest(
                page = 1,
                query = query,
                filters = FilterList()
            )
            
            // Configure OkHttpClient with timeout
            val client = extension.client.newBuilder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .build()
            
            // Execute HTTP request
            val response = client.newCall(request).execute()
            
            // Check HTTP status code
            if (!response.isSuccessful) {
                val executionTime = System.currentTimeMillis() - startTime
                return TestResult.Failure(
                    extensionMetadata = metadata,
                    query = query,
                    executionTimeMs = executionTime,
                    failureType = FailureType.HTTP_ERROR,
                    message = "HTTP error: ${response.code} ${response.message}",
                    exception = null
                )
            }
            
            // Parse response to get NovelsPage
            val novelsPage = extension.searchNovelsParse(response)
            
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
