package me.moeyinlo.visualize.jvm.oracle

import java.net.URLClassLoader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotSpotRuntimeDifferentialCorpusTest {
    @Test
    fun `HotSpot runtime differential corpus enumerates executable oracle cases`() {
        val casesByName = HotSpotRuntimeDifferentialCorpus.cases.associateBy(HotSpotRuntimeDifferentialCase::name)

        assertEquals(HotSpotRuntimeDifferentialCorpus.cases.size, casesByName.size)
        assertTrue(HotSpotRuntimeDifferentialCorpus.cases.all { case -> case.sourceName.endsWith(".java") })
        assertTrue(HotSpotRuntimeDifferentialCorpus.cases.all { case -> case.internalName.isNotBlank() })
        assertTrue(HotSpotRuntimeDifferentialCorpus.cases.all { case -> case.entryMethodName.isNotBlank() })
        assertTrue(HotSpotRuntimeDifferentialCorpus.cases.all { case -> case.expectedResult != Unit })

        assertTrue(casesByName.containsKey("integer loop arithmetic"))
        assertTrue(casesByName.containsKey("string switch dispatch"))
        assertTrue(casesByName.containsKey("array allocation and stores"))
        assertTrue(casesByName.containsKey("exception handler result"))
        assertTrue(casesByName.containsKey("synchronized static method"))
    }

    @Test
    fun `HotSpot runtime differential corpus executes against the host JVM oracle`() {
        HotSpotRuntimeDifferentialCorpus.cases.forEach { case ->
            val outputDirectory = Files.createTempDirectory("visualize-jvm-hotspot-differential")
            try {
                JavaFixtureCompiler.compile(
                    sourceName = case.sourceName,
                    source = case.source,
                    outputDirectory = outputDirectory,
                )

                URLClassLoader(arrayOf(outputDirectory.toUri().toURL())).use { loader ->
                    val result = loader
                        .loadClass(case.internalName.replace('/', '.'))
                        .getDeclaredMethod(case.entryMethodName)
                        .invoke(null)

                    assertEquals(case.expectedResult, result, case.name)
                }
            } finally {
                outputDirectory.toFile().deleteRecursively()
            }
        }
    }
}
