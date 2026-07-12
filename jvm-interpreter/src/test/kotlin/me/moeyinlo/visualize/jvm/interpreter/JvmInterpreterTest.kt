package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmHeapObject
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStackOverflowException
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmInterpreterTest {
    @Test
    fun `nop completes without changing the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(0x00.toByte()),
            maxStack = 0,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
    }

    @Test
    fun `iconst instructions push int values onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x02.toByte(),
                0x03.toByte(),
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x07.toByte(),
                0x08.toByte(),
            ),
            maxStack = 7,
        )

        assertEquals(
            listOf(
                JvmIntValue(-1),
                JvmIntValue(0),
                JvmIntValue(1),
                JvmIntValue(2),
                JvmIntValue(3),
                JvmIntValue(4),
                JvmIntValue(5),
            ),
            result.operandStack.toList(),
        )
        assertEquals(7, result.operandStack.slotDepth)
    }

    @Test
    fun `aconst_null pushes the null reference onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(0x01.toByte()),
            maxStack = 1,
        )

        assertEquals(listOf(JvmNullValue), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `lconst instructions push long values onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x09.toByte(),
                0x0A.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(
                JvmLongValue(0L),
                JvmLongValue(1L),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `fconst instructions push float values onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0B.toByte(),
                0x0C.toByte(),
                0x0D.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(
            listOf(
                JvmFloatValue(0.0f),
                JvmFloatValue(1.0f),
                JvmFloatValue(2.0f),
            ),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `dconst instructions push double values onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0E.toByte(),
                0x0F.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(
                JvmDoubleValue(0.0),
                JvmDoubleValue(1.0),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `bipush sign extends the immediate byte onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x7F.toByte(),
                0x10.toByte(),
                0x80.toByte(),
                0x10.toByte(),
                0xFF.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(
            listOf(
                JvmIntValue(127),
                JvmIntValue(-128),
                JvmIntValue(-1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `sipush sign extends the immediate short onto the operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x11.toByte(),
                0x7F.toByte(),
                0xFF.toByte(),
                0x11.toByte(),
                0x80.toByte(),
                0x00.toByte(),
                0x11.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(
            listOf(
                JvmIntValue(32767),
                JvmIntValue(-32768),
                JvmIntValue(-1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc pushes integer constants from the runtime constant pool`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(123_456),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(123_456)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc pushes float constants from the runtime constant pool`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-3.5f),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(-3.5f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc pushes string constants as guest string references`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("guest string", "guest string".encodeToByteArray()),
                    ConstantStringEntry(stringIndex = ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("guest string"),
            ),
            heap.get(reference),
        )
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc_w pushes integer constants from a two byte runtime constant pool index`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x13.toByte(),
                0x01.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                (1..255).map { value -> ConstantIntegerEntry(value) } + ConstantIntegerEntry(65_536),
            ),
        )

        assertEquals(listOf(JvmIntValue(65_536)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc2_w pushes long constants from the runtime constant pool`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(9_876_543_210L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(9_876_543_210L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc2_w pushes double constants from the runtime constant pool`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-0.25),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(-0.25)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `constant execution respects max stack bounds`() {
        assertFailsWith<JvmOperandStackOverflowException> {
            JvmInterpreter.execute(
                code = byteArrayOf(0x03.toByte()),
                maxStack = 0,
            )
        }
    }

    @Test
    fun `unsupported instructions fail explicitly`() {
        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(0x60.toByte()),
                maxStack = 1,
            )
        }

        assertEquals("Unsupported instruction iadd (0x60) at offset 0", exception.message)
    }
}
