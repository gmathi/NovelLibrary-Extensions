package io.github.gmathi.novellibrary.testing

/**
 * Main entry point for the Extension Search Testing framework.
 * Parses command-line arguments, executes tests, and outputs results.
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 7.1, 7.2, 7.3**
 * 
 * Usage:
 *   kotlin ExtensionSearchTestRunner.kt [options]
 * 
 * Options:
 *   --query <query>        Search query to use (default: "I")
 *   --lang <language>      Filter by language code (e.g., "en", "es")
 *   --name <name>          Filter by extension name (partial match)
 *   --id <extension-id>    Filter by extension ID
 *   --timeout <ms>         Timeout in milliseconds (default: 30000)
 *   --help                 Display this help message
 * 
 * Examples:
 *   # Test all extensions with default query "I"
 *   kotlin ExtensionSearchTestRunner.kt
 * 
 *   # Test only English extensions
 *   kotlin ExtensionSearchTestRunner.kt --lang en
 * 
 *   # Test extensions with "Novel" in the name
 *   kotlin ExtensionSearchTestRunner.kt --name Novel
 * 
 *   # Test with custom query and timeout
 *   kotlin ExtensionSearchTestRunner.kt --query "test" --timeout 60000
 */
fun main(args: Array<String>) {
    try {
        // Parse command-line arguments
        val config = parseArguments(args)
        
        // Display help if requested
        if (args.contains("--help") || args.contains("-h")) {
            displayHelp()
            return
        }
        
        // Display configuration
        displayConfiguration(config)
        
        // Instantiate components
        val extensionDiscovery = FileSystemExtensionDiscovery()
        val searchExecutor = SearchExecutorImpl()
        val testOrchestrator = TestOrchestratorImpl(extensionDiscovery, searchExecutor)
        val reportGenerator = ReportGeneratorImpl()
        
        // Execute tests
        println("Starting test execution...\n")
        val results = testOrchestrator.executeTests(config)
        
        // Generate and output report
        println()
        val report = reportGenerator.generateReport(results)
        println(report)
        
        // Exit with appropriate status code
        val hasFailures = results.any { it is TestResult.Failure }
        if (hasFailures) {
            System.exit(1)
        }
        
    } catch (e: IllegalArgumentException) {
        System.err.println("Error: ${e.message}")
        System.err.println()
        displayHelp()
        System.exit(1)
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}

/**
 * Parses command-line arguments into a TestConfig object.
 * 
 * @param args Command-line arguments
 * @return TestConfig with parsed values
 * @throws IllegalArgumentException if arguments are invalid
 */
private fun parseArguments(args: Array<String>): TestConfig {
    var query = "I"
    var languageFilter: String? = null
    var nameFilter: String? = null
    var extensionIdFilter: Long? = null
    var timeout = 30_000L
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--query" -> {
                if (i + 1 >= args.size) {
                    throw IllegalArgumentException("--query requires a value")
                }
                query = args[i + 1]
                i += 2
            }
            "--lang", "--language" -> {
                if (i + 1 >= args.size) {
                    throw IllegalArgumentException("--lang requires a value")
                }
                languageFilter = args[i + 1]
                i += 2
            }
            "--name" -> {
                if (i + 1 >= args.size) {
                    throw IllegalArgumentException("--name requires a value")
                }
                nameFilter = args[i + 1]
                i += 2
            }
            "--id" -> {
                if (i + 1 >= args.size) {
                    throw IllegalArgumentException("--id requires a value")
                }
                try {
                    extensionIdFilter = args[i + 1].toLong()
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("--id must be a valid number: ${args[i + 1]}")
                }
                i += 2
            }
            "--timeout" -> {
                if (i + 1 >= args.size) {
                    throw IllegalArgumentException("--timeout requires a value")
                }
                try {
                    timeout = args[i + 1].toLong()
                    if (timeout <= 0) {
                        throw IllegalArgumentException("--timeout must be positive")
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("--timeout must be a valid number: ${args[i + 1]}")
                }
                i += 2
            }
            "--help", "-h" -> {
                // Help flag is handled in main()
                i++
            }
            else -> {
                throw IllegalArgumentException("Unknown argument: ${args[i]}")
            }
        }
    }
    
    return TestConfig(
        query = query,
        languageFilter = languageFilter,
        nameFilter = nameFilter,
        extensionIdFilter = extensionIdFilter,
        timeout = timeout
    )
}

/**
 * Displays the help message with usage information.
 */
private fun displayHelp() {
    println("""
        Extension Search Testing Framework
        ===================================
        
        Usage:
          kotlin ExtensionSearchTestRunner.kt [options]
        
        Options:
          --query <query>        Search query to use (default: "I")
          --lang <language>      Filter by language code (e.g., "en", "es")
          --name <name>          Filter by extension name (partial match)
          --id <extension-id>    Filter by extension ID
          --timeout <ms>         Timeout in milliseconds (default: 30000)
          --help, -h             Display this help message
        
        Examples:
          # Test all extensions with default query "I"
          kotlin ExtensionSearchTestRunner.kt
        
          # Test only English extensions
          kotlin ExtensionSearchTestRunner.kt --lang en
        
          # Test extensions with "Novel" in the name
          kotlin ExtensionSearchTestRunner.kt --name Novel
        
          # Test with custom query and timeout
          kotlin ExtensionSearchTestRunner.kt --query "test" --timeout 60000
        
          # Test a specific extension by ID
          kotlin ExtensionSearchTestRunner.kt --id 1234567890
    """.trimIndent())
}

/**
 * Displays the current test configuration.
 */
private fun displayConfiguration(config: TestConfig) {
    println("Extension Search Test Configuration")
    println("===================================")
    println("Query: \"${config.query}\"")
    println("Timeout: ${config.timeout}ms")
    
    if (config.languageFilter != null) {
        println("Language Filter: ${config.languageFilter}")
    }
    
    if (config.nameFilter != null) {
        println("Name Filter: ${config.nameFilter}")
    }
    
    if (config.extensionIdFilter != null) {
        println("Extension ID Filter: ${config.extensionIdFilter}")
    }
    
    if (config.languageFilter == null && config.nameFilter == null && config.extensionIdFilter == null) {
        println("Filters: None (testing all extensions)")
    }
    
    println()
}
