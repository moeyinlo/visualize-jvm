package me.moeyinlo.visualize.jvm.classfile

import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import me.moeyinlo.visualize.jvm.oracle.JavaFixtureCompiler

class ClassFileRoundTripTest {
    @Test
    fun `round trips javac fixture bytes through parser and writer`() {
        val outputDirectory = Files.createTempDirectory("visualize-jvm-classfile-roundtrip-test")
        try {
            val fixture = JavaFixtureCompiler.compile(
                sourceName = "sample/RoundTrip.java",
                source = """
                    package sample;

                    public class RoundTrip {
                        public static final int ANSWER = 42;

                        public int abs(int value) {
                            if (value >= 0) {
                                return value;
                            }
                            return -value;
                        }
                    }
                """.trimIndent(),
                outputDirectory = outputDirectory,
            )
            val originalBytes = fixture.readClassBytes("sample/RoundTrip")
            val registry = knownAttributeParsers()

            val parsed = ClassFileParser.parse(
                bytes = originalBytes,
                source = "sample/RoundTrip.class",
                attributeParsers = registry,
            )
            val writtenBytes = ClassFileWriter.writeClassFile(parsed)
            val parsedAgain = ClassFileParser.parse(
                bytes = writtenBytes,
                source = "sample/RoundTrip-written.class",
                attributeParsers = registry,
            )

            assertContentEquals(originalBytes, writtenBytes)
            assertEquals(parsed.version, parsedAgain.version)
            assertEquals(parsed.constantPool.constantPoolCount, parsedAgain.constantPool.constantPoolCount)
            assertEquals(parsed.fields.size, parsedAgain.fields.size)
            assertEquals(parsed.methods.size, parsedAgain.methods.size)
            assertTrue(parsedAgain.methods.any { parsedAgain.utf8(it.nameIndex) == "abs" }, parsedAgain.methods.toString())

            val absMethod = parsedAgain.methods.single { parsedAgain.utf8(it.nameIndex) == "abs" }
            val code = assertIs<CodeAttribute>(absMethod.attributes.single { it is CodeAttribute })
            assertTrue(code.code.isNotEmpty(), "abs method Code attribute should contain bytecode")
        } finally {
            outputDirectory.toFile().deleteRecursively()
        }
    }

    private fun ClassFile.utf8(index: ConstantPoolIndex): String =
        assertIs<ConstantUtf8Entry>(constantPool[index]).value

    private fun knownAttributeParsers(): AttributeParserRegistry =
        AttributeParserRegistry.of(
            "AnnotationDefault" to AnnotationDefaultAttributeParser,
            "BootstrapMethods" to BootstrapMethodsAttributeParser,
            "Code" to CodeAttributeParser,
            "ConstantValue" to ConstantValueAttributeParser,
            "Deprecated" to DeprecatedAttributeParser,
            "EnclosingMethod" to EnclosingMethodAttributeParser,
            "Exceptions" to ExceptionsAttributeParser,
            "InnerClasses" to InnerClassesAttributeParser,
            "LineNumberTable" to LineNumberTableAttributeParser,
            "LocalVariableTable" to LocalVariableTableAttributeParser,
            "LocalVariableTypeTable" to LocalVariableTypeTableAttributeParser,
            "MethodParameters" to MethodParametersAttributeParser,
            "Module" to ModuleAttributeParser,
            "ModuleMainClass" to ModuleMainClassAttributeParser,
            "ModulePackages" to ModulePackagesAttributeParser,
            "NestHost" to NestHostAttributeParser,
            "NestMembers" to NestMembersAttributeParser,
            "PermittedSubclasses" to PermittedSubclassesAttributeParser,
            "Record" to RecordAttributeParser,
            "RuntimeInvisibleAnnotations" to RuntimeInvisibleAnnotationsAttributeParser,
            "RuntimeInvisibleParameterAnnotations" to RuntimeInvisibleParameterAnnotationsAttributeParser,
            "RuntimeInvisibleTypeAnnotations" to RuntimeInvisibleTypeAnnotationsAttributeParser,
            "RuntimeVisibleAnnotations" to RuntimeVisibleAnnotationsAttributeParser,
            "RuntimeVisibleParameterAnnotations" to RuntimeVisibleParameterAnnotationsAttributeParser,
            "RuntimeVisibleTypeAnnotations" to RuntimeVisibleTypeAnnotationsAttributeParser,
            "Signature" to SignatureAttributeParser,
            "SourceDebugExtension" to SourceDebugExtensionAttributeParser,
            "SourceFile" to SourceFileAttributeParser,
            "StackMapTable" to StackMapTableAttributeParser,
            "Synthetic" to SyntheticAttributeParser,
        )
}
