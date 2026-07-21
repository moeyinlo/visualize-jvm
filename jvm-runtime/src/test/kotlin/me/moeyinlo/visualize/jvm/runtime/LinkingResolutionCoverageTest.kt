package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LinkingResolutionCoverageTest {
    @Test
    fun `linking resolution coverage enumerates JVMS symbolic reference families`() {
        val rulesByName = LinkingResolutionCoverage.entries.associateBy(LinkingResolutionCoverageEntry::rule)

        assertEquals(LinkingResolutionCoverage.entries.size, rulesByName.size)
        assertTrue(LinkingResolutionCoverage.entries.all { entry -> entry.specSection.startsWith("JVMS 5.4") })
        assertTrue(LinkingResolutionCoverage.entries.all { entry -> entry.rule.isNotBlank() })
        assertTrue(LinkingResolutionCoverage.entries.all { entry -> entry.currentComponent.isNotBlank() })

        assertNotNull(rulesByName["class and interface resolution"])
        assertNotNull(rulesByName["field resolution"])
        assertNotNull(rulesByName["method resolution"])
        assertNotNull(rulesByName["interface method resolution"])
        assertNotNull(rulesByName["method type and method handle resolution"])
        assertNotNull(rulesByName["dynamically-computed constants and call sites"])
    }

    @Test
    fun `implemented linking resolution coverage binds to existing focused tests`() {
        val covered = LinkingResolutionCoverage.entries.filter { entry ->
            entry.status == LinkingResolutionCoverageStatus.Implemented ||
                entry.status == LinkingResolutionCoverageStatus.PartiallyImplemented
        }

        assertTrue(covered.isNotEmpty())
        assertTrue(covered.all { entry -> entry.coveringTestClass != null })
        assertTrue(covered.all { entry -> testClassExists(entry.coveringTestClass!!) })
    }

    @Test
    fun `current unsupported linking resolution work is named explicitly`() {
        val unsupportedRules = LinkingResolutionCoverage.entries
            .filter { entry -> entry.status == LinkingResolutionCoverageStatus.NotYetImplemented }
            .map(LinkingResolutionCoverageEntry::rule)

        assertEquals(
            listOf(
                "class and interface resolution",
                "method type and method handle resolution",
                "dynamically-computed constants and call sites",
                "access control during resolution",
                "resolution error memoization",
            ),
            unsupportedRules,
        )
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

    private val testSearchRoots: List<Path> = listOf("jvm-runtime", "jvm-interpreter", "jvm-classfile").map { module ->
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
