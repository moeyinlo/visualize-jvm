package me.moeyinlo.visualize.jvm.runtime

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitializationCoverageTest {
    @Test
    fun `initialization coverage enumerates JVMS class initialization obligations`() {
        val rulesByName = InitializationCoverage.entries.associateBy(InitializationCoverageEntry::rule)

        assertEquals(InitializationCoverage.entries.size, rulesByName.size)
        assertTrue(InitializationCoverage.entries.all { entry -> entry.specSection.startsWith("JVMS 5.5") })
        assertTrue(InitializationCoverage.entries.all { entry -> entry.rule.isNotBlank() })
        assertTrue(InitializationCoverage.entries.all { entry -> entry.currentComponent.isNotBlank() })

        assertNotNull(rulesByName["preparation default values"])
        assertNotNull(rulesByName["ConstantValue attribute assignment"])
        assertNotNull(rulesByName["active use triggers initialization"])
        assertNotNull(rulesByName["class initialization state machine"])
        assertNotNull(rulesByName["superclass and superinterface initialization"])
        assertNotNull(rulesByName["initialization error handling"])
    }

    @Test
    fun `implemented initialization coverage binds to existing focused tests`() {
        val covered = InitializationCoverage.entries.filter { entry ->
            entry.status == InitializationCoverageStatus.Implemented ||
                entry.status == InitializationCoverageStatus.PartiallyImplemented
        }

        assertTrue(covered.isNotEmpty())
        assertTrue(covered.all { entry -> entry.coveringTestClass != null })
        assertTrue(covered.all { entry -> testClassExists(entry.coveringTestClass!!) })
    }

    @Test
    fun `current unsupported initialization work is named explicitly`() {
        val unsupportedRules = InitializationCoverage.entries
            .filter { entry -> entry.status == InitializationCoverageStatus.NotYetImplemented }
            .map(InitializationCoverageEntry::rule)

        assertEquals(
            listOf(
                "superclass and superinterface initialization",
                "initialization error handling",
                "native class initialization boundary",
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
