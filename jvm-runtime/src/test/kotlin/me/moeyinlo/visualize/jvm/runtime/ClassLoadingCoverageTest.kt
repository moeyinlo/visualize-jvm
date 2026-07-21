package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClassLoadingCoverageTest {
    @Test
    fun `class loading coverage enumerates JVMS class and interface creation rules`() {
        val rulesByName = ClassLoadingCoverage.entries.associateBy(ClassLoadingCoverageEntry::rule)

        assertEquals(ClassLoadingCoverage.entries.size, rulesByName.size)
        assertTrue(ClassLoadingCoverage.entries.all { entry -> entry.specSection.startsWith("JVMS 5.") })
        assertTrue(ClassLoadingCoverage.entries.all { entry -> entry.rule.isNotBlank() })
        assertTrue(ClassLoadingCoverage.entries.all { entry -> entry.currentComponent.isNotBlank() })

        assertNotNull(rulesByName["binary name to class bytes"])
        assertNotNull(rulesByName["bootstrap and user-defined loader identity"])
        assertNotNull(rulesByName["array class creation"])
        assertNotNull(rulesByName["loading constraints"])
        assertNotNull(rulesByName["erroneous class state"])
    }

    @Test
    fun `implemented class loading coverage binds to focused tests`() {
        val covered = ClassLoadingCoverage.entries.filter { entry ->
            entry.status == ClassLoadingCoverageStatus.Implemented ||
                entry.status == ClassLoadingCoverageStatus.PartiallyImplemented ||
                entry.status == ClassLoadingCoverageStatus.HostDelegationBoundary
        }

        assertTrue(covered.isNotEmpty())
        assertTrue(covered.all { entry -> entry.coveringTestClass != null })
        assertTrue(covered.all { entry -> testClassExists(entry.coveringTestClass!!) })
    }

    @Test
    fun `current unsupported class loading work is named explicitly`() {
        val unsupportedRules = ClassLoadingCoverage.entries
            .filter { entry -> entry.status == ClassLoadingCoverageStatus.NotYetImplemented }
            .map(ClassLoadingCoverageEntry::rule)

        assertEquals(
            listOf(
                "binary name to class bytes",
                "bootstrap and user-defined loader identity",
                "array class creation",
                "loading constraints",
                "erroneous class state",
                "class loader consistency checks",
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

    private val testSearchRoots: List<Path> = listOf("jvm-runtime", "jvm-gui").map { module ->
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
