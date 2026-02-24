package io.github.gmathi.novellibrary.testing

/**
 * Types of failures that can occur during extension testing.
 */
enum class FailureType {
    INITIALIZATION_ERROR,
    NETWORK_ERROR,
    TIMEOUT,
    HTTP_ERROR,
    PARSE_ERROR,
    VALIDATION_ERROR
}
