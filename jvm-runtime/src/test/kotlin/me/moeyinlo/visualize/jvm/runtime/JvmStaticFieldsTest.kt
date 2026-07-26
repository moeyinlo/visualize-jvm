package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JvmStaticFieldsTest {
    @Test
    fun `static fields store values by owner name and descriptor`() {
        val fields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
        )

        fields.put(field, JvmIntValue(7))

        assertEquals(JvmIntValue(7), fields.get(field))
    }

    @Test
    fun `preparation assigns ConstantValue attributes to static fields`() {
        val fields = JvmStaticFields()

        fields.prepare(
            JvmClassDefinition(
                internalName = "Example",
                fields = listOf(
                    JvmFieldDefinition(
                        name = "answer",
                        descriptor = "I",
                        isStatic = true,
                        constantValue = JvmFieldConstantValue.Numeric(JvmIntValue(42)),
                    ),
                    JvmFieldDefinition(
                        name = "defaulted",
                        descriptor = "I",
                        isStatic = true,
                    ),
                    JvmFieldDefinition(
                        name = "instanceConstant",
                        descriptor = "I",
                        isStatic = false,
                        constantValue = JvmFieldConstantValue.Numeric(JvmIntValue(99)),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmIntValue(42),
            fields.get(
                JvmFieldReference(
                    ownerClassName = "Example",
                    name = "answer",
                    descriptor = "I",
                ),
            ),
        )
        assertEquals(
            JvmIntValue(0),
            fields.get(
                JvmFieldReference(
                    ownerClassName = "Example",
                    name = "defaulted",
                    descriptor = "I",
                ),
            ),
        )
        assertEquals(
            JvmIntValue(0),
            fields.get(
                JvmFieldReference(
                    ownerClassName = "Example",
                    name = "instanceConstant",
                    descriptor = "I",
                ),
            ),
        )
    }

    @Test
    fun `heap-aware preparation assigns String ConstantValue attributes to guest strings`() {
        val fields = JvmStaticFields()
        val heap = JvmHeap()

        fields.prepare(
            JvmClassDefinition(
                internalName = "Example",
                fields = listOf(
                    JvmFieldDefinition(
                        name = "literal",
                        descriptor = "Ljava/lang/String;",
                        isStatic = true,
                        constantValue = JvmFieldConstantValue.StringLiteral("hello"),
                    ),
                ),
            ),
            heap,
        )

        val value = fields.get(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "literal",
                descriptor = "Ljava/lang/String;",
            ),
        )
        val reference = assertIs<JvmObjectReferenceValue>(value)
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("hello"),
            ),
            heap.get(reference),
        )
    }
}
