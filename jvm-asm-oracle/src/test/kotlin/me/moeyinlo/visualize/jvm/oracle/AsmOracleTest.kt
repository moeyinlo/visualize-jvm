package me.moeyinlo.visualize.jvm.oracle

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AsmOracleTest {
    @Test
    fun `parses class metadata fields and methods from fixture bytes`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-asm-test")
        try {
            val fixture = JavaFixtureCompiler.compile(
                sourceName = "sample/Hello.java",
                source = """
                    package sample;

                    public class Hello implements Runnable {
                        private final int value = 42;

                        public int answer() {
                            return value;
                        }

                        @Override
                        public void run() {
                            answer();
                        }
                    }
                """.trimIndent(),
                outputDirectory = outputDirectory,
            )

            val facts = AsmOracle.parse(fixture.readClassBytes("sample/Hello"))

            assertEquals("sample/Hello", facts.name)
            assertEquals("java/lang/Object", facts.superName)
            assertTrue(facts.interfaces.contains("java/lang/Runnable"), facts.interfaces.toString())
            assertTrue(facts.majorVersion >= 45, "majorVersion=${facts.majorVersion}")
            assertTrue(facts.constantPoolEntryCount > 0, "constantPoolEntryCount=${facts.constantPoolEntryCount}")
            assertTrue(facts.fields.any { it.name == "value" && it.descriptor == "I" }, facts.fields.toString())
            assertTrue(facts.methods.any { it.name == "answer" && it.descriptor == "()I" }, facts.methods.toString())
            assertTrue(facts.methods.any { it.name == "run" && it.descriptor == "()V" }, facts.methods.toString())
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reports invalid class bytes with useful diagnostics`() {
        val failure = assertFailsWith<AsmOracleException> {
            AsmOracle.parse(byteArrayOf(0, 1, 2))
        }

        assertTrue(failure.message.orEmpty().contains("ASM failed to parse class bytes"), failure.message)
        assertTrue(failure.message.orEmpty().contains("length=3"), failure.message)
    }
}
