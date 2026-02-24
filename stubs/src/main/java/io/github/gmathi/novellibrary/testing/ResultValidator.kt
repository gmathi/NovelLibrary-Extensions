package io.github.gmathi.novellibrary.testing

import io.github.gmathi.novellibrary.model.other.NovelsPage

/**
 * Validates search results to ensure they meet structural requirements.
 * Checks that novels have required fields populated and match the expected extension.
 */
interface ResultValidator {
    /**
     * Validates a NovelsPage result from a search operation.
     * 
     * @param result The search result to validate
     * @param extensionId The ID of the extension that produced the result
     * @return ValidationResult with pass/fail status and list of violations
     */
    fun validate(result: NovelsPage, extensionId: Long): ValidationResult
}

/**
 * Default implementation of ResultValidator.
 * Validates that novels have non-empty names and URLs, and correct sourceId.
 */
class DefaultResultValidator : ResultValidator {
    override fun validate(result: NovelsPage, extensionId: Long): ValidationResult {
        val violations = mutableListOf<ValidationViolation>()
        
        // Validate each novel in the list
        result.novels.forEachIndexed { index, novel ->
            // Check if name is not empty (not blank)
            if (novel.name.isBlank()) {
                violations.add(
                    ValidationViolation(
                        rule = "novel.name.notEmpty",
                        message = "Novel at index $index has empty or blank name",
                        novelIndex = index
                    )
                )
            }
            
            // Check if url is not empty (not blank)
            if (novel.url.isBlank()) {
                violations.add(
                    ValidationViolation(
                        rule = "novel.url.notEmpty",
                        message = "Novel at index $index has empty or blank url",
                        novelIndex = index
                    )
                )
            }
            
            // Check if sourceId matches the extension ID
            if (novel.sourceId != extensionId) {
                violations.add(
                    ValidationViolation(
                        rule = "novel.sourceId.matches",
                        message = "Novel at index $index has sourceId ${novel.sourceId} but expected $extensionId",
                        novelIndex = index
                    )
                )
            }
        }
        
        return ValidationResult(
            passed = violations.isEmpty(),
            violations = violations
        )
    }
}
