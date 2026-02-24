package io.github.gmathi.novellibrary.testing

/**
 * Generates formatted reports from test results.
 * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 */
interface ReportGenerator {
    /**
     * Generates a formatted report from test results
     * @param results List of test results
     * @return Formatted report string
     */
    fun generateReport(results: List<TestResult>): String
}
