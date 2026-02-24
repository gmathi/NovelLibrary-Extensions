package io.github.gmathi.novellibrary.testing

/**
 * Configuration for test execution.
 * Controls which extensions are tested and how tests are executed.
 */
data class TestConfig(
    val query: String = "I",
    val languageFilter: String? = null,
    val nameFilter: String? = null,
    val extensionIdFilter: Long? = null,
    val timeout: Long = 30_000L // milliseconds
)
