package me.moeyinlo.visualize.jvm.oracle

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JavapOracleTest {
    @Test
    fun `runs javap verbose against compiled fixture class`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-javap-test")
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

            val output = JavapOracle.verbose(fixture.outputDirectory, "sample/Hello")

            assertTrue(output.stdout.contains("public class sample.Hello"), output.stdout)
            assertTrue(output.stdout.contains("public int answer();"), output.stdout)
            assertTrue(output.stdout.contains("descriptor: ()I"), output.stdout)
            assertTrue(output.stdout.contains("Code:"), output.stdout)
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reports javap failures with command diagnostics`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-javap-test")
        try {
            val failure = assertFailsWith<JavapOracleException> {
                JavapOracle.verbose(outputDirectory, "sample/Missing")
            }

            assertTrue(failure.message.orEmpty().contains("javap -v failed"), failure.message)
            assertTrue(failure.message.orEmpty().contains("sample.Missing"), failure.message)
            assertTrue(failure.message.orEmpty().contains(outputDirectory.toAbsolutePath().toString()), failure.message)
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `rejects invalid class file paths with useful diagnostics`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-javap-test")
        try {
            val missingClassFile = outputDirectory.resolve("Missing.class")

            val failure = assertFailsWith<JavapOracleException> {
                JavapOracle.verbose(missingClassFile)
            }

            assertTrue(failure.message.orEmpty().contains("javap input class file does not exist"), failure.message)
            assertTrue(failure.message.orEmpty().contains(missingClassFile.toAbsolutePath().toString()), failure.message)
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }
}
