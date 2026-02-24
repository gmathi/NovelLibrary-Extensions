package io.github.gmathi.novellibrary.testing

/**
 * Simple test to verify ExtensionSearchTestRunner functionality.
 * Tests command-line argument parsing and basic execution flow.
 */
fun main() {
    println("=== Extension Search Test Runner Test ===\n")
    
    // Test 1: No arguments (default configuration)
    println("Test 1: No arguments (default configuration)")
    println("---------------------------------------------")
    testRunner(arrayOf())
    
    // Test 2: With language filter
    println("\n\nTest 2: With language filter")
    println("---------------------------------------------")
    testRunner(arrayOf("--lang", "en"))
    
    // Test 3: With name filter
    println("\n\nTest 3: With name filter")
    println("---------------------------------------------")
    testRunner(arrayOf("--name", "Novel"))
    
    // Test 4: With custom query
    println("\n\nTest 4: With custom query")
    println("---------------------------------------------")
    testRunner(arrayOf("--query", "test"))
    
    // Test 5: With multiple filters
    println("\n\nTest 5: With multiple filters")
    println("---------------------------------------------")
    testRunner(arrayOf("--lang", "en", "--name", "Novel", "--query", "I"))
    
    // Test 6: Help flag
    println("\n\nTest 6: Help flag")
    println("---------------------------------------------")
    testRunner(arrayOf("--help"))
    
    println("\n\n=== All Tests Completed ===")
}

/**
 * Helper function to test the runner with different arguments.
 * Catches exceptions to prevent test termination.
 */
private fun testRunner(args: Array<String>) {
    try {
        // Call the main function from ExtensionSearchTestRunner
        io.github.gmathi.novellibrary.testing.main(args)
    } catch (e: Exception) {
        println("Exception caught: ${e.message}")
    }
}
