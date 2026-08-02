package com.gooludou.shadowplanner.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureBoundaryTest {
    @Test
    fun kotlinPackages_matchTheirSourceDirectories() {
        kotlinFiles(sourceRoot).forEach { file ->
            val relativeParent = sourceRoot.relativize(file.parent)
                .joinToString(".") { it.toString() }
            val expectedPackage = relativeParent
            val declaredPackage = file.readText()
                .lineSequence()
                .firstOrNull { it.startsWith("package ") }
                ?.removePrefix("package ")

            assertEquals("Package mismatch in $file", expectedPackage, declaredPackage)
        }
    }

    @Test
    fun core_doesNotDependOnOuterLayers() {
        assertNoImports(
            sourceRoot.resolve("com/gooludou/shadowplanner/core"),
            ".app.",
            ".feature.",
            ".renderer.",
            ".location.",
            ".project."
        )
    }

    @Test
    fun renderers_doNotDependOnFeaturesOrEachOther() {
        val rendererRoot = sourceRoot.resolve("com/gooludou/shadowplanner/renderer")
        assertNoImports(rendererRoot, ".app.", ".feature.", ".location.", ".project.")
        assertNoImports(rendererRoot.resolve("mapbox"), ".renderer.filament.")
        assertNoImports(rendererRoot.resolve("filament"), ".renderer.mapbox.")
    }

    @Test
    fun removedLegacyPackages_stayEmpty() {
        val packageRoot = sourceRoot.resolve("com/gooludou/shadowplanner")
        val legacyPackages = listOf(
            "domain",
            "map",
            "navigation",
            "presentation",
            "scene",
            "ui/theme"
        )

        legacyPackages.forEach { legacyPackage ->
            val files = kotlinFiles(packageRoot.resolve(legacyPackage))
            assertTrue("Legacy package $legacyPackage contains $files", files.isEmpty())
        }
    }

    private fun assertNoImports(root: Path, vararg forbiddenFragments: String) {
        kotlinFiles(root).forEach { file ->
            val imports = file.readText().lineSequence().filter { it.startsWith("import ") }
            imports.forEach { importLine ->
                forbiddenFragments.forEach { fragment ->
                    assertTrue(
                        "$file must not import across architecture boundary: $importLine",
                        fragment !in importLine
                    )
                }
            }
        }
    }

    private fun kotlinFiles(root: Path): List<Path> {
        if (!Files.exists(root)) return emptyList()
        return Files.walk(root).use { paths ->
            paths.filter { it.isRegularFile() && it.extension == "kt" }.toList()
        }
    }

    private val sourceRoot: Path by lazy {
        listOf(Paths.get("src/main/java"), Paths.get("app/src/main/java"))
            .firstOrNull(Files::exists)
            ?: error("Unable to locate the main Kotlin source directory")
    }
}
