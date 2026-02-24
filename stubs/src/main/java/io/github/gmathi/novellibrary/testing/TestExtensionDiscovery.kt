package io.github.gmathi.novellibrary.testing

/**
 * Simple test program to verify ExtensionDiscovery functionality.
 * Run this to test the discovery mechanism.
 */
fun main() {
    println("=== Extension Discovery Test ===\n")
    
    val discovery = FileSystemExtensionDiscovery()
    
    println("Discovering extensions...")
    val extensions = discovery.discoverExtensions()
    
    println("Found ${extensions.size} extensions:\n")
    
    extensions.forEach { metadata ->
        println("Extension: ${metadata.name}")
        println("  ID: ${metadata.id}")
        println("  Language: ${metadata.lang}")
        println("  Base URL: ${metadata.baseUrl}")
        println("  Class: ${metadata.packageName}.${metadata.className}")
        println()
    }
    
    // Test loading an extension
    if (extensions.isNotEmpty()) {
        val firstExtension = extensions.first()
        println("Testing extension loading for: ${firstExtension.name}")
        
        val loaded = discovery.loadExtension(firstExtension)
        if (loaded != null) {
            println("✓ Successfully loaded extension")
            println("  Loaded ID: ${loaded.id}")
            println("  Loaded Name: ${loaded.name}")
            println("  Loaded Lang: ${loaded.lang}")
            println("  Loaded Base URL: ${loaded.baseUrl}")
        } else {
            println("✗ Failed to load extension")
        }
    }
}
