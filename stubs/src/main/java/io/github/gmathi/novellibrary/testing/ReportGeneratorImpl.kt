package io.github.gmathi.novellibrary.testing

/**
 * Default implementation of ReportGenerator.
 * Formats test results into a comprehensive human-readable report.
 * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 */
class ReportGeneratorImpl : ReportGenerator {
    
    override fun generateReport(results: List<TestResult>): String {
        if (results.isEmpty()) {
            return "Extension Search Test Report\n" +
                   "============================\n" +
                   "No extensions tested."
        }
        
        val query = results.firstOrNull()?.query ?: "I"
        val passed = results.count { it is TestResult.Success }
        val failed = results.count { it is TestResult.Failure }
        val total = results.size
        
        val successRate = if (total > 0) (passed.toDouble() / total * 100) else 0.0
        
        val totalNovels = results.filterIsInstance<TestResult.Success>()
            .sumOf { it.novelCount }
        
        val avgNovelsPerExtension = if (passed > 0) {
            totalNovels.toDouble() / passed
        } else {
            0.0
        }
        
        return buildString {
            appendLine("Extension Search Test Report")
            appendLine("============================")
            appendLine("Query: \"$query\"")
            appendLine("Total Extensions: $total")
            appendLine("Passed: $passed")
            appendLine("Failed: $failed")
            appendLine()
            appendLine("Results:")
            appendLine("--------")
            
            results.forEach { result ->
                appendLine(formatTestResult(result))
            }
            
            appendLine()
            appendLine("Summary Statistics:")
            appendLine("- Success Rate: ${"%.1f".format(successRate)}%")
            appendLine("- Total Novels Retrieved: $totalNovels")
            appendLine("- Average Novels per Extension: ${"%.1f".format(avgNovelsPerExtension)}")
        }
    }
    
    private fun formatTestResult(result: TestResult): String {
        return when (result) {
            is TestResult.Success -> formatSuccess(result)
            is TestResult.Failure -> formatFailure(result)
        }
    }
    
    private fun formatSuccess(result: TestResult.Success): String {
        val metadata = result.extensionMetadata
        val paginationStatus = if (result.hasNextPage) "has next page" else "no next page"
        return "[PASS] ${metadata.name} (${metadata.lang}) - ${result.novelCount} novels, $paginationStatus"
    }
    
    private fun formatFailure(result: TestResult.Failure): String {
        val metadata = result.extensionMetadata
        val failureDetails = buildString {
            append(result.message)
            result.exception?.let { ex ->
                append(" (${ex.javaClass.simpleName}")
                ex.message?.let { msg ->
                    append(": $msg")
                }
                append(")")
            }
        }
        return "[FAIL] ${metadata.name} (${metadata.lang}) - $failureDetails"
    }
}
