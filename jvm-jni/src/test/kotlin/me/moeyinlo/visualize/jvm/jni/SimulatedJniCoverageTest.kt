package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SimulatedJniCoverageTest {
    @Test
    fun `simulated JNI coverage enumerates guest scoped JNIEnv helper families`() {
        val rulesByName = SimulatedJniCoverage.entries.associateBy(SimulatedJniCoverageEntry::rule)

        assertEquals(SimulatedJniCoverage.entries.size, rulesByName.size)
        assertTrue(SimulatedJniCoverage.entries.all { entry -> entry.specSection.startsWith("JNI") || entry.specSection.startsWith("JVMS") })
        assertTrue(SimulatedJniCoverage.entries.all { entry -> entry.rule.isNotBlank() })
        assertTrue(SimulatedJniCoverage.entries.all { entry -> entry.currentComponent.isNotBlank() })

        assertNotNull(rulesByName["handle table local references"])
        assertNotNull(rulesByName["FindClass and class handles"])
        assertNotNull(rulesByName["method and field ID lookup"])
        assertNotNull(rulesByName["guest method upcalls"])
        assertNotNull(rulesByName["string helpers"])
        assertNotNull(rulesByName["primitive and reference arrays"])
        assertNotNull(rulesByName["monitor helpers"])
        assertNotNull(rulesByName["exception helpers"])
        assertNotNull(rulesByName["pending exception native-return boundary"])
        assertNotNull(rulesByName["JNIEnv function table upcalls"])
    }

    @Test
    fun `implemented simulated JNI coverage binds to existing focused tests`() {
        val covered = SimulatedJniCoverage.entries.filter { entry ->
            entry.status == SimulatedJniCoverageStatus.Implemented ||
                entry.status == SimulatedJniCoverageStatus.PartiallyImplemented
        }

        assertTrue(covered.isNotEmpty())
        assertTrue(covered.all { entry -> entry.coveringTestClass != null })
        assertTrue(covered.all { entry -> testClassExists(entry.coveringTestClass!!) })
    }

    @Test
    fun `exception helper coverage is fully implemented`() {
        val exceptionHelpers = SimulatedJniCoverage.entries.single { entry ->
            entry.rule == "exception helpers"
        }

        assertEquals(SimulatedJniCoverageStatus.Implemented, exceptionHelpers.status)
    }

    @Test
    fun `current unsupported simulated JNI work is named explicitly`() {
        val unsupportedRules = SimulatedJniCoverage.entries
            .filter { entry -> entry.status == SimulatedJniCoverageStatus.NotYetImplemented }
            .map(SimulatedJniCoverageEntry::rule)

        assertEquals(
            emptyList(),
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

    private val testSearchRoots: List<Path> = listOf("jvm-jni", "jvm-interpreter", "jvm-runtime").map { module ->
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
