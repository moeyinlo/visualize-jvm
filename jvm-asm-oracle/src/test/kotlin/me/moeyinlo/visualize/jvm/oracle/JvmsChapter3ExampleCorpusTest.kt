package me.moeyinlo.visualize.jvm.oracle

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmsChapter3ExampleCorpusTest {
    @Test
    fun `chapter 3 example corpus enumerates compiling for the JVM topics`() {
        val examplesByName = JvmsChapter3ExampleCorpus.examples.associateBy(JvmsChapter3Example::name)

        assertEquals(JvmsChapter3ExampleCorpus.examples.size, examplesByName.size)
        assertTrue(JvmsChapter3ExampleCorpus.examples.all { example -> example.sourceName.endsWith(".java") })
        assertTrue(JvmsChapter3ExampleCorpus.examples.all { example -> example.internalName.isNotBlank() })
        assertTrue(JvmsChapter3ExampleCorpus.examples.all { example -> example.chapter3Topics.isNotEmpty() })

        assertTrue(examplesByName.containsKey("arithmetic and local variables"))
        assertTrue(examplesByName.containsKey("control flow and switch"))
        assertTrue(examplesByName.containsKey("method invocation and object construction"))
        assertTrue(examplesByName.containsKey("arrays"))
        assertTrue(examplesByName.containsKey("exceptions and finally"))
        assertTrue(examplesByName.containsKey("synchronization"))
    }

    @Test
    fun `chapter 3 example corpus compiles each Java source into a classfile`() {
        JvmsChapter3ExampleCorpus.examples.forEach { example ->
            val outputDirectory = Files.createTempDirectory("visualize-jvm-chapter3-corpus")
            try {
                val fixture = JavaFixtureCompiler.compile(
                    sourceName = example.sourceName,
                    source = example.source,
                    outputDirectory = outputDirectory,
                )

                val classBytes = fixture.readClassBytes(example.internalName)
                assertContentEquals(
                    byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()),
                    classBytes.take(4).toByteArray(),
                    example.name,
                )
            } finally {
                outputDirectory.toFile().deleteRecursively()
            }
        }
    }
}
