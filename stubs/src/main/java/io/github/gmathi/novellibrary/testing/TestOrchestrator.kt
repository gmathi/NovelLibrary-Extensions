package io.github.gmathi.novellibrary.testing

/**
 * Coordinates the overall test execution flow and applies filtering logic.
 * Orchestrates extension discovery, filtering, and test execution.
 */
interface TestOrchestrator {
    /**
     * Executes tests against all discovered extensions.
     * 
     * @param config Test configuration including filters and parameters
     * @return List of test results for all tested extensions
     */
    fun executeTests(config: TestConfig): List<TestResult>
}

/**
 * Default implementation of TestOrchestrator.
 * Coordinates extension discovery, filtering, and test execution.
 */
class TestOrchestratorImpl(
    private val extensionDiscovery: ExtensionDiscovery,
    private val searchExecutor: SearchExecutor
) : TestOrchestrator {
    
    override fun executeTests(config: TestConfig): List<TestResult> {
        // Discover all extensions
        val allExtensions = extensionDiscovery.discoverExtensions()
        
        if (allExtensions.isEmpty()) {
            println("No extensions discovered")
            return emptyList()
        }
        
        // Apply filters to extension list
        val filteredExtensions = applyFilters(allExtensions, config)
        
        if (filteredExtensions.isEmpty()) {
            println("No extensions match the specified filters")
            return emptyList()
        }
        
        println("Testing ${filteredExtensions.size} extension(s)...")
        
        // Execute tests for each filtered extension
        val results = mutableListOf<TestResult>()
        
        for (metadata in filteredExtensions) {
            try {
                val result = executeTestForExtension(metadata, config)
                results.add(result)
            } catch (e: Exception) {
                // Ensure failures in one extension don't stop testing others
                println("Unexpected error testing ${metadata.name}: ${e.message}")
                results.add(
                    TestResult.Failure(
                        extensionMetadata = metadata,
                        query = config.query,
                        executionTimeMs = 0,
                        failureType = FailureType.INITIALIZATION_ERROR,
                        message = "Unexpected error during test execution: ${e.message}",
                        exception = e
                    )
                )
            }
        }
        
        return results
    }
    
    /**
     * Applies language, name, and extension ID filters to the extension list.
     */
    private fun applyFilters(
        extensions: List<ExtensionMetadata>,
        config: TestConfig
    ): List<ExtensionMetadata> {
        var filtered = extensions
        
        // Apply language filter
        config.languageFilter?.let { langFilter ->
            filtered = filtered.filter { it.lang.equals(langFilter, ignoreCase = true) }
        }
        
        // Apply name filter
        config.nameFilter?.let { nameFilter ->
            filtered = filtered.filter { 
                it.name.contains(nameFilter, ignoreCase = true)
            }
        }
        
        // Apply extension ID filter
        config.extensionIdFilter?.let { idFilter ->
            filtered = filtered.filter { it.id == idFilter }
        }
        
        return filtered
    }
    
    /**
     * Executes a test for a single extension.
     * Handles extension loading and delegates search execution to SearchExecutor.
     */
    private fun executeTestForExtension(
        metadata: ExtensionMetadata,
        config: TestConfig
    ): TestResult {
        val startTime = System.currentTimeMillis()
        
        // Load the extension
        val extension = extensionDiscovery.loadExtension(metadata)
        
        if (extension == null) {
            val executionTime = System.currentTimeMillis() - startTime
            return TestResult.Failure(
                extensionMetadata = metadata,
                query = config.query,
                executionTimeMs = executionTime,
                failureType = FailureType.INITIALIZATION_ERROR,
                message = "Failed to load extension class"
            )
        }
        
        // Execute search using SearchExecutor
        return try {
            searchExecutor.executeSearch(extension, config.query, config.timeout)
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            TestResult.Failure(
                extensionMetadata = metadata,
                query = config.query,
                executionTimeMs = executionTime,
                failureType = FailureType.INITIALIZATION_ERROR,
                message = "Error during search execution: ${e.message}",
                exception = e
            )
        }
    }
}
