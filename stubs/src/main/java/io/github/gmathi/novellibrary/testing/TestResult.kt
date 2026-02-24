package io.github.gmathi.novellibrary.testing

import io.github.gmathi.novellibrary.model.other.NovelsPage

/**
 * Result of testing a single extension.
 * Sealed class with Success and Failure subclasses.
 */
sealed class TestResult {
    abstract val extensionMetadata: ExtensionMetadata
    abstract val query: String
    abstract val executionTimeMs: Long
    
    data class Success(
        override val extensionMetadata: ExtensionMetadata,
        override val query: String,
        override val executionTimeMs: Long,
        val novelsPage: NovelsPage,
        val novelCount: Int,
        val hasNextPage: Boolean
    ) : TestResult()
    
    data class Failure(
        override val extensionMetadata: ExtensionMetadata,
        override val query: String,
        override val executionTimeMs: Long,
        val failureType: FailureType,
        val message: String,
        val exception: Throwable? = null
    ) : TestResult()
}
