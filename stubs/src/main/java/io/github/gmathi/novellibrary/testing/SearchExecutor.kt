package io.github.gmathi.novellibrary.testing

import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource

/**
 * Executes search queries against individual extensions.
 * Handles HTTP request execution, timeout configuration, and result parsing.
 */
interface SearchExecutor {
    /**
     * Executes a search query against an extension.
     * 
     * @param extension The extension to test
     * @param query The search query string
     * @param timeout Timeout in milliseconds
     * @return TestResult containing success/failure information
     */
    fun executeSearch(
        extension: ParsedHttpSource,
        query: String,
        timeout: Long
    ): TestResult
}
