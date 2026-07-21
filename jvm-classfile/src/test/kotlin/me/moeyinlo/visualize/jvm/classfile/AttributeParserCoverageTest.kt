package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttributeParserCoverageTest {
    @Test
    fun `attribute parser coverage classifies every standard attribute name`() {
        val coverageByName = AttributeParserCoverage.entries.associateBy(AttributeParserCoverageEntry::attributeName)

        assertEquals(standardAttributeNames, AttributeParserCoverage.entries.map(AttributeParserCoverageEntry::attributeName))
        assertEquals(standardAttributeNames.toSet(), coverageByName.keys)
        assertTrue(AttributeParserCoverage.entries.all { entry -> entry.ownerScopes.isNotEmpty() })
    }

    @Test
    fun `attribute parser coverage binds implemented parsers to parser tests`() {
        val parsed = AttributeParserCoverage.entries.filter { entry -> entry.status == AttributeParserCoverageStatus.Parsed }

        assertTrue(parsed.isNotEmpty())
        assertTrue(parsed.all { entry -> entry.parserObjectName != null })
        assertTrue(parsed.all { entry -> entry.coveringTestClass != null })
    }

    @Test
    fun `attribute parser coverage names standard raw-preserved gaps explicitly`() {
        val rawPreserved = AttributeParserCoverage.entries
            .filter { entry -> entry.status == AttributeParserCoverageStatus.RawPreserved }
            .map(AttributeParserCoverageEntry::attributeName)

        assertEquals(emptyList(), rawPreserved)
    }

    private companion object {
        private val standardAttributeNames = listOf(
            "ConstantValue",
            "Code",
            "StackMapTable",
            "Exceptions",
            "InnerClasses",
            "EnclosingMethod",
            "Synthetic",
            "Signature",
            "SourceFile",
            "SourceDebugExtension",
            "LineNumberTable",
            "LocalVariableTable",
            "LocalVariableTypeTable",
            "Deprecated",
            "RuntimeVisibleAnnotations",
            "RuntimeInvisibleAnnotations",
            "RuntimeVisibleParameterAnnotations",
            "RuntimeInvisibleParameterAnnotations",
            "RuntimeVisibleTypeAnnotations",
            "RuntimeInvisibleTypeAnnotations",
            "AnnotationDefault",
            "BootstrapMethods",
            "MethodParameters",
            "Module",
            "ModulePackages",
            "ModuleMainClass",
            "NestHost",
            "NestMembers",
            "Record",
            "PermittedSubclasses",
        )
    }
}
