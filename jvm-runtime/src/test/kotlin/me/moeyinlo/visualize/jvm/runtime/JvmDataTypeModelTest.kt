package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JvmDataTypeModelTest {
    @Test
    fun `runtime data type model partitions JVM values into primitive reference and returnAddress values`() {
        val values = listOf(
            JvmBooleanValue(true),
            JvmByteValue(1),
            JvmCharValue('A'.code),
            JvmShortValue(2),
            JvmIntValue(3),
            JvmLongValue(4L),
            JvmFloatValue(5.0f),
            JvmDoubleValue(6.0),
            JvmNullValue,
            JvmObjectReferenceValue(JvmReferenceId(1)),
            JvmReturnAddressValue(7),
        )

        values.take(8).forEach { value -> assertIs<JvmPrimitiveValue>(value) }
        values.drop(8).take(2).forEach { value -> assertIs<JvmReferenceValue>(value) }
        assertIs<JvmReturnAddressValue>(values.last())
    }

    @Test
    fun `runtime data type model assigns JVMS category slot widths`() {
        val categoryOneValues = listOf<JvmValue>(
            JvmBooleanValue(false),
            JvmByteValue(Byte.MIN_VALUE.toInt()),
            JvmCharValue(Char.MAX_VALUE.code),
            JvmShortValue(Short.MAX_VALUE.toInt()),
            JvmIntValue(Int.MAX_VALUE),
            JvmFloatValue(Float.NaN),
            JvmNullValue,
            JvmObjectReferenceValue(JvmReferenceId(1)),
            JvmReturnAddressValue(0),
        )
        val categoryTwoValues = listOf<JvmValue>(
            JvmLongValue(Long.MIN_VALUE),
            JvmDoubleValue(Double.NaN),
        )

        categoryOneValues.forEach { value ->
            assertEquals(JvmValueCategory.Category1, value.category)
            assertEquals(1, value.category.slotWidth)
        }
        categoryTwoValues.forEach { value ->
            assertEquals(JvmValueCategory.Category2, value.category)
            assertEquals(2, value.category.slotWidth)
        }
    }
}
