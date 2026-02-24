package io.github.gmathi.novellibrary.testing

/**
 * Result of validating a search result.
 * Contains pass/fail status and list of validation violations.
 */
data class ValidationResult(
    val passed: Boolean,
    val violations: List<ValidationViolation>
)
