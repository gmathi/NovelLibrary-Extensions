package io.github.gmathi.novellibrary.testing

/**
 * Represents a single validation rule violation.
 * Contains the rule that was violated, a descriptive message, and optionally the index of the novel that violated the rule.
 */
data class ValidationViolation(
    val rule: String,
    val message: String,
    val novelIndex: Int? = null
)
