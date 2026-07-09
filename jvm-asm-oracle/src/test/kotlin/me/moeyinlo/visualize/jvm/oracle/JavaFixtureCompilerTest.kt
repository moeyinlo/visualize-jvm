package me.moeyinlo.visualize.jvm.oracle

import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavaFixtureCompilerTest {
    @Test
    fun `compiles Java source into class bytes`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-fixture-test")
        try {
            val fixture = JavaFixtureCompiler.compile(
                sourceName = "sample/Hello.java",
                source = """
                    package sample;

                    public class Hello {
                        public int answer() {
                            return 42;
                        }
                    }
                """.trimIndent(),
                outputDirectory = outputDirectory,
            )

            val classBytes = fixture.readClassBytes("sample/Hello")

            assertContentEquals(byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte()), classBytes.take(4).toByteArray())
            assertTrue(fixture.classFiles.any { it.fileName.toString() == "Hello.class" })
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reports compiler failures with diagnostics`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-fixture-test")
        try {
            val failure = assertFailsWith<JavaFixtureCompilationException> {
                JavaFixtureCompiler.compile(
                    sourceName = "broken/Broken.java",
                    source = """
                        package broken;

                        public class Broken {
                            public void nope() {
                                doesNotExist();
                            }
                        }
                    """.trimIndent(),
                    outputDirectory = outputDirectory,
                )
            }

            assertTrue(failure.message.orEmpty().contains("doesNotExist"), failure.message)
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }
}
