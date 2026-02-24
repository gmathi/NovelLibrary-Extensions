package io.github.gmathi.novellibrary.testing

/**
 * Simple test program to verify TestOrchestrator functionality.
 * Run this to test the orchestration mechanism.
 */
fun main() {
    println("=== Test Orchestrator Test ===\n")
    
    // Create dependencies
    val discovery = FileSystemExtensionDiscovery()
    val executor = SearchExecutorImpl()
    val orchestrator = TestOrchestratorImpl(discovery, executor)
    
    // Test 1: Execute tests with no filters
    println("Test 1: Execute tests with no filters")
    println("---------------------------------------")
    val config1 = TestConfig(query = "I")
    val results1 = orchestrator.executeTests(config1)
    printResults(results1)
    
    // Test 2: Execute tests with language filter
    println("\nTest 2: Execute tests with language filter (en)")
    println("---------------------------------------")
    val config2 = TestConfig(query = "I", languageFilter = "en")
    val results2 = orchestrator.executeTests(config2)
    printResults(results2)
    
    // Test 3: Execute tests with name filter
    println("\nTest 3: Execute tests with name filter (Novel)")
    println("---------------------------------------")
    val config3 = TestConfig(query = "I", nameFilter = "Novel")
    val results3 = orchestrator.executeTests(config3)
    printResults(results3)
    
    // Test 4: Execute tests with multiple filters
    println("\nTest 4: Execute tests with multiple filters (en + Novel)")
    println("---------------------------------------")
    val config4 = TestConfig(query = "I", languageFilter = "en", nameFilter = "Novel")
    val results4 = orchestrator.executeTests(config4)
    printResults(results4)
}

/**
 * Prints test results in a readable format.
 */
private fun printResults(results: List<TestResult>) {
    if (results.isEmpty()) {
        println("No results")
        return
    }
    
    val successCount = results.count { it is TestResult.Success }
    val failureCount = results.count { it is TestResult.Failure }
    
    println("Total: ${results.size}, Success: $successCount, Failure: $failureCount\n")
    
    results.forEach { result ->
        when (result) {
            is TestResult.Success -> {
                println("[PASS] ${result.extensionMetadata.name} (${result.extensionMetadata.lang})")
                println("  Query: ${result.query}")
                println("  Novels: ${result.novelCount}, Has Next: ${result.hasNextPage}")
                println("  Execution Time: ${result.executionTimeMs}ms")
            }
            is TestResult.Failure -> {
                println("[FAIL] ${result.extensionMetadata.name} (${result.extensionMetadata.lang})")
                println("  Query: ${result.query}")
                println("  Failure Type: ${result.failureType}")
                println("  Message: ${result.message}")
                println("  Execution Time: ${result.executionTimeMs}ms")
            }
        }
        println()
    }
}
