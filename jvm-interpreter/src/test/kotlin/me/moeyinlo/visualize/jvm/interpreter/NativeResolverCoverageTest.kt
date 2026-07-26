package me.moeyinlo.visualize.jvm.interpreter

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NativeResolverCoverageTest {
    @Test
    fun `native resolver coverage enumerates intrinsic then simulated JNI layers`() {
        val rulesByName = NativeResolverCoverage.entries.associateBy(NativeResolverCoverageEntry::rule)

        assertEquals(NativeResolverCoverage.entries.size, rulesByName.size)
        assertTrue(NativeResolverCoverage.entries.all { entry -> entry.specSection.startsWith("JVMS 5.") })
        assertTrue(NativeResolverCoverage.entries.all { entry -> entry.rule.isNotBlank() })
        assertTrue(NativeResolverCoverage.entries.all { entry -> entry.currentComponent.isNotBlank() })

        assertNotNull(rulesByName["native method key identity"])
        assertNotNull(rulesByName["VM intrinsic lookup before simulated JNI"])
        assertNotNull(rulesByName["simulated JNI fallback lookup"])
        assertNotNull(rulesByName["unresolved native method error"])
        assertNotNull(rulesByName["JNI short and long symbol candidates"])
        assertNotNull(rulesByName["native library export lookup"])
        assertNotNull(rulesByName["loaded native library export dispatch"])
    }

    @Test
    fun `implemented native resolver coverage binds to existing focused tests`() {
        val covered = NativeResolverCoverage.entries.filter { entry ->
            entry.status == NativeResolverCoverageStatus.Implemented ||
                entry.status == NativeResolverCoverageStatus.PartiallyImplemented
        }

        assertTrue(covered.isNotEmpty())
        assertTrue(covered.all { entry -> entry.coveringTestClass != null })
        assertTrue(covered.all { entry -> testClassExists(entry.coveringTestClass!!) })
    }

    @Test
    fun `current unsupported native resolver work is named explicitly`() {
        val unsupportedRules = NativeResolverCoverage.entries
            .filter { entry -> entry.status == NativeResolverCoverageStatus.NotYetImplemented }
            .map(NativeResolverCoverageEntry::rule)

        assertEquals(emptyList(), unsupportedRules)
    }

    private fun testClassExists(testClass: String): Boolean =
        testSearchRoots.any { root ->
            if (!Files.isDirectory(root)) {
                return@any false
            }
            Files.walk(root).use { paths ->
                paths.anyMatch { path -> path.fileName.toString() == "$testClass.kt" }
            }
        }

    private val testSearchRoots: List<Path> = listOf("jvm-interpreter", "jvm-jni", "jvm-gui").map { module ->
        repositoryRoot()
            .resolve(module)
            .resolve("src/test/kotlin")
    }

    private fun repositoryRoot(): Path {
        val userDir = Path.of(System.getProperty("user.dir"))
        return if (Files.exists(userDir.resolve("settings.gradle.kts"))) {
            userDir
        } else {
            userDir.parent
        }
    }
}
