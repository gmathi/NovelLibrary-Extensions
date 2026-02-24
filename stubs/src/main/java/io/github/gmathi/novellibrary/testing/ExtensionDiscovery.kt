package io.github.gmathi.novellibrary.testing

import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import java.io.File

/**
 * Interface for discovering and loading extensions from the file system.
 */
interface ExtensionDiscovery {
    /**
     * Discovers all extensions in the extensions/individual directory structure.
     * @return List of discovered extension metadata
     */
    fun discoverExtensions(): List<ExtensionMetadata>
    
    /**
     * Loads an extension class and instantiates it.
     * @param metadata Extension metadata from discovery
     * @return Instantiated ParsedHttpSource or null if loading fails
     */
    fun loadExtension(metadata: ExtensionMetadata): ParsedHttpSource?
}

/**
 * Default implementation of ExtensionDiscovery that scans the file system
 * and uses reflection to load extension classes.
 */
class FileSystemExtensionDiscovery(
    private val extensionsRootPath: String = "extensions/individual"
) : ExtensionDiscovery {
    
    override fun discoverExtensions(): List<ExtensionMetadata> {
        val extensionsRoot = File(extensionsRootPath)
        if (!extensionsRoot.exists() || !extensionsRoot.isDirectory) {
            println("Warning: Extensions directory not found at $extensionsRootPath")
            return emptyList()
        }
        
        val metadataList = mutableListOf<ExtensionMetadata>()
        
        // Iterate through language directories (e.g., "en", "es")
        extensionsRoot.listFiles()?.forEach { langDir ->
            if (!langDir.isDirectory) return@forEach
            val lang = langDir.name
            
            // Iterate through source directories (e.g., "novelfull", "boxnovel")
            langDir.listFiles()?.forEach { sourceDir ->
                if (!sourceDir.isDirectory) return@forEach
                val sourceName = sourceDir.name
                
                try {
                    val metadata = extractMetadata(lang, sourceName, sourceDir)
                    if (metadata != null) {
                        metadataList.add(metadata)
                    }
                } catch (e: Exception) {
                    println("Warning: Failed to extract metadata for $lang/$sourceName: ${e.message}")
                }
            }
        }
        
        return metadataList
    }
    
    override fun loadExtension(metadata: ExtensionMetadata): ParsedHttpSource? {
        return try {
            val fullClassName = "${metadata.packageName}.${metadata.className}"
            val clazz = Class.forName(fullClassName)
            clazz.getDeclaredConstructor().newInstance() as ParsedHttpSource
        } catch (e: ClassNotFoundException) {
            println("Error: Class not found for ${metadata.name}: ${e.message}")
            null
        } catch (e: InstantiationException) {
            println("Error: Failed to instantiate ${metadata.name}: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error: Failed to load extension ${metadata.name}: ${e.message}")
            null
        }
    }
    
    /**
     * Extracts metadata from an extension directory by loading and instantiating the extension class.
     */
    private fun extractMetadata(lang: String, sourceName: String, sourceDir: File): ExtensionMetadata? {
        // Convert source name from kebab-case to PascalCase
        // e.g., "novelfull" -> "NovelFull", "boxnovel" -> "BoxNovel"
        val className = sourceName.split("-")
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        
        val packageName = "io.github.gmathi.novellibrary.extension.$lang.$sourceName"
        
        // Check if the source file exists
        val expectedSourcePath = "src/io/github/gmathi/novellibrary/extension/$lang/$sourceName/$className.kt"
        val sourceFile = File(sourceDir, expectedSourcePath)
        
        if (!sourceFile.exists()) {
            println("Warning: Source file not found for $lang/$sourceName at $expectedSourcePath")
            return null
        }
        
        // Try to load and instantiate the extension to extract metadata
        return try {
            val fullClassName = "$packageName.$className"
            val clazz = Class.forName(fullClassName)
            val instance = clazz.getDeclaredConstructor().newInstance() as ParsedHttpSource
            
            ExtensionMetadata(
                id = instance.id,
                name = instance.name,
                lang = instance.lang,
                baseUrl = instance.baseUrl,
                className = className,
                packageName = packageName
            )
        } catch (e: ClassNotFoundException) {
            println("Warning: Class not found for $lang/$sourceName: ${e.message}")
            null
        } catch (e: InstantiationException) {
            println("Warning: Failed to instantiate $lang/$sourceName: ${e.message}")
            null
        } catch (e: Exception) {
            println("Warning: Failed to extract metadata for $lang/$sourceName: ${e.message}")
            null
        }
    }
}
