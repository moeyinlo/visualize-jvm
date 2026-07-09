package me.moeyinlo.visualize.jvm.classfile

import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import me.moeyinlo.visualize.jvm.oracle.JavaFixtureCompiler
import me.moeyinlo.visualize.jvm.oracle.JavapOracle

class ClassFileJavapDifferentialTest {
    @Test
    fun `matches javap verbose core class field and method facts`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-classfile-javap-test")
        try {
            val fixture = JavaFixtureCompiler.compile(
                sourceName = "sample/JavapSubject.java",
                source = """
                    package sample;

                    public class JavapSubject implements Runnable {
                        public final int value = 42;

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

            val classFile = ClassFileParser.parse(
                bytes = fixture.readClassBytes("sample/JavapSubject"),
                source = "sample/JavapSubject.class",
                attributeParsers = knownAttributeParsers(),
            )
            val javap = JavapOracle.verbose(fixture.outputDirectory, "sample/JavapSubject").stdout

            assertTrue(javap.contains("public class ${classFile.thisBinaryName()}"), javap)
            assertTrue(javap.contains("minor version: ${classFile.version.minor}"), javap)
            assertTrue(javap.contains("major version: ${classFile.version.major}"), javap)
            assertTrue(javap.contains("interfaces: ${classFile.identity.interfaceIndexes.size}"), javap)

            classFile.fields.forEach { field ->
                assertTrue(javap.contains("${classFile.utf8(field.nameIndex)};"), javap)
                assertTrue(javap.contains("descriptor: ${classFile.utf8(field.descriptorIndex)}"), javap)
            }

            classFile.methods
                .filterNot { classFile.utf8(it.nameIndex) == "<init>" }
                .forEach { method ->
                    assertTrue(javap.contains(methodJavapSignature(classFile, method)), javap)
                    assertTrue(javap.contains("descriptor: ${classFile.utf8(method.descriptorIndex)}"), javap)
                }
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    private fun methodJavapSignature(
        classFile: ClassFile,
        method: MethodInfo,
    ): String =
        when (classFile.utf8(method.nameIndex)) {
            "answer" -> "public int answer();"
            "run" -> "public void run();"
            else -> error("Unexpected javap fixture method: ${classFile.utf8(method.nameIndex)}")
        }

    private fun ClassFile.thisBinaryName(): String =
        className(identity.thisClassIndex).replace('/', '.')

    private fun ClassFile.className(index: ConstantPoolIndex): String {
        val classEntry = assertIs<ConstantClassEntry>(constantPool[index])
        return utf8(classEntry.nameIndex)
    }

    private fun ClassFile.utf8(index: ConstantPoolIndex): String =
        assertIs<ConstantUtf8Entry>(constantPool[index]).value

    private fun knownAttributeParsers(): AttributeParserRegistry =
        AttributeParserRegistry.of(
            "Code" to CodeAttributeParser,
            "ConstantValue" to ConstantValueAttributeParser,
            "LineNumberTable" to LineNumberTableAttributeParser,
            "SourceFile" to SourceFileAttributeParser,
            "StackMapTable" to StackMapTableAttributeParser,
        )
}
