package io.github.gmathi.novellibrary.testing

/**
 * Metadata about a discovered extension.
 * Contains information needed to identify and load an extension.
 */
data class ExtensionMetadata(
    val id: Long,
    val name: String,
    val lang: String,
    val baseUrl: String,
    val className: String,
    val packageName: String
)
