package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmHeapObject
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLocalVariables
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandlePayload
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmMethodTypePayload
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
            maxStack = 5,
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
    fun `iload instructions push int local variables onto the operand stack`() {
        val locals = JvmLocalVariables(maxLocals = 3)
        locals.store(0, JvmIntValue(-1))
        locals.store(2, JvmIntValue(42))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x15.toByte(),
                0x02.toByte(),
                0x1A.toByte(),
            ),
            maxStack = 3,
            localVariables = locals,
        )

        assertEquals(
            listOf(
                JvmIntValue(42),
                JvmIntValue(-1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `wide iload pushes int local variables from a two byte index`() {
        val locals = JvmLocalVariables(maxLocals = 260)
        locals.store(258, JvmIntValue(1234))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x15.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(1234)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `lload instructions push long local variables onto the operand stack`() {
        val locals = JvmLocalVariables(maxLocals = 4)
        locals.store(0, JvmLongValue(4L))
        locals.store(2, JvmLongValue(9L))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x16.toByte(),
                0x02.toByte(),
                0x1E.toByte(),
            ),
            maxStack = 4,
            localVariables = locals,
        )

        assertEquals(
            listOf(
                JvmLongValue(9L),
                JvmLongValue(4L),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `wide lload pushes long local variables from a two byte index`() {
        val locals = JvmLocalVariables(maxLocals = 260)
        locals.store(258, JvmLongValue(0x0102_0304_0506_0708L))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x16.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(listOf(JvmLongValue(0x0102_0304_0506_0708L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `fload instructions push float local variables onto the operand stack`() {
        val locals = JvmLocalVariables(maxLocals = 3)
        locals.store(1, JvmFloatValue(1.25f))
        locals.store(2, JvmFloatValue(-2.5f))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x17.toByte(),
                0x02.toByte(),
                0x23.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(
            listOf(
                JvmFloatValue(-2.5f),
                JvmFloatValue(1.25f),
            ),
            result.operandStack.toList(),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `wide fload pushes float local variables from a two byte index`() {
        val locals = JvmLocalVariables(maxLocals = 259)
        locals.store(258, JvmFloatValue(-6.25f))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x17.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(listOf(JvmFloatValue(-6.25f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `dload instructions push double local variables onto the operand stack`() {
        val locals = JvmLocalVariables(maxLocals = 4)
        locals.store(0, JvmDoubleValue(3.0))
        locals.store(2, JvmDoubleValue(-7.5))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x18.toByte(),
                0x02.toByte(),
                0x26.toByte(),
            ),
            maxStack = 4,
            localVariables = locals,
        )

        assertEquals(
            listOf(
                JvmDoubleValue(-7.5),
                JvmDoubleValue(3.0),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `wide dload pushes double local variables from a two byte index`() {
        val locals = JvmLocalVariables(maxLocals = 260)
        locals.store(258, JvmDoubleValue(13.5))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x18.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(listOf(JvmDoubleValue(13.5)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `aload instructions push reference local variables onto the operand stack`() {
        val locals = JvmLocalVariables(maxLocals = 3)
        val reference = JvmObjectReferenceValue(JvmReferenceId(3))
        locals.store(0, reference)
        locals.store(2, JvmNullValue)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x19.toByte(),
                0x02.toByte(),
                0x2A.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(
            listOf(
                JvmNullValue,
                reference,
            ),
            result.operandStack.toList(),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `wide aload pushes reference local variables from a two byte index`() {
        val locals = JvmLocalVariables(maxLocals = 259)
        val reference = JvmObjectReferenceValue(JvmReferenceId(258))
        locals.store(258, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x19.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `istore instructions pop int values into local variables`() {
        val locals = JvmLocalVariables(maxLocals = 3)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x08.toByte(),
                0x36.toByte(),
                0x02.toByte(),
                0x10.toByte(),
                0x7F.toByte(),
                0x3B.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(5), locals.load(2))
        assertEquals(JvmIntValue(127), locals.load(0))
    }

    @Test
    fun `wide istore pops int values into a two byte local variable index`() {
        val locals = JvmLocalVariables(maxLocals = 259)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x08.toByte(),
                0xC4.toByte(),
                0x36.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(5), locals.load(258))
    }

    @Test
    fun `lstore instructions pop long values into local variables`() {
        val locals = JvmLocalVariables(maxLocals = 4)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0x37.toByte(),
                0x02.toByte(),
                0x09.toByte(),
                0x3F.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmLongValue(1L), locals.load(2))
        assertEquals(JvmLongValue(0L), locals.load(0))
    }

    @Test
    fun `wide lstore pops long values into a two byte local variable index`() {
        val locals = JvmLocalVariables(maxLocals = 260)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0xC4.toByte(),
                0x37.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmLongValue(1L), locals.load(258))
    }

    @Test
    fun `fstore instructions pop float values into local variables`() {
        val locals = JvmLocalVariables(maxLocals = 3)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0x38.toByte(),
                0x02.toByte(),
                0x0C.toByte(),
                0x43.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmFloatValue(2.0f), locals.load(2))
        assertEquals(JvmFloatValue(1.0f), locals.load(0))
    }

    @Test
    fun `wide fstore pops float values into a two byte local variable index`() {
        val locals = JvmLocalVariables(maxLocals = 259)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0xC4.toByte(),
                0x38.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmFloatValue(2.0f), locals.load(258))
    }

    @Test
    fun `dstore instructions pop double values into local variables`() {
        val locals = JvmLocalVariables(maxLocals = 4)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0F.toByte(),
                0x39.toByte(),
                0x02.toByte(),
                0x0E.toByte(),
                0x47.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmDoubleValue(1.0), locals.load(2))
        assertEquals(JvmDoubleValue(0.0), locals.load(0))
    }

    @Test
    fun `wide dstore pops double values into a two byte local variable index`() {
        val locals = JvmLocalVariables(maxLocals = 260)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0F.toByte(),
                0xC4.toByte(),
                0x39.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 2,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmDoubleValue(1.0), locals.load(258))
    }

    @Test
    fun `astore instructions pop reference values into local variables`() {
        val heap = JvmHeap()
        val locals = JvmLocalVariables(maxLocals = 3)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x01.toByte(),
                0x3A.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantStringEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("stored", "stored".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(heap.internString("stored"), locals.load(0))
        assertEquals(JvmNullValue, locals.load(2))
    }

    @Test
    fun `wide astore pops reference values into a two byte local variable index`() {
        val locals = JvmLocalVariables(maxLocals = 260)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xC4.toByte(),
                0x3A.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmNullValue, locals.load(258))
    }

    @Test
    fun `iinc increments int local variables by a signed byte constant`() {
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(1, JvmIntValue(40))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x84.toByte(),
                0x01.toByte(),
                0xFE.toByte(),
            ),
            maxStack = 0,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(38), locals.load(1))
    }

    @Test
    fun `wide iinc increments int local variables by a two byte index and signed short constant`() {
        val locals = JvmLocalVariables(maxLocals = 260)
        locals.store(258, JvmIntValue(1_000))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0x84.toByte(),
                0x01.toByte(),
                0x02.toByte(),
                0xFF.toByte(),
                0x38.toByte(),
            ),
            maxStack = 0,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(800), locals.load(258))
    }

    @Test
    fun `pop removes the top category one operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x57.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `pop2 removes the top two category one operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x58.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `pop2 removes the top category two operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x0F.toByte(),
                0x58.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `dup duplicates the top category one operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x59.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1), JvmIntValue(1)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dup_x1 duplicates the top category one operand stack value two values down`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x5A.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(2), JvmIntValue(1), JvmIntValue(2)), result.operandStack.toList())
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `dup_x2 duplicates the top category one operand stack value three category one values down`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x5B.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(JvmIntValue(3), JvmIntValue(1), JvmIntValue(2), JvmIntValue(3)),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dup_x2 duplicates the top category one operand stack value two values down over category two`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x09.toByte(),
                0x04.toByte(),
                0x5B.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(JvmIntValue(1), JvmLongValue(0), JvmIntValue(1)),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2 duplicates the top two category one operand stack values in original order`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x5C.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(JvmIntValue(1), JvmIntValue(2), JvmIntValue(1), JvmIntValue(2)),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2 duplicates the top category two operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0x5C.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(listOf(JvmLongValue(1), JvmLongValue(1)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x1 duplicates the top two category one operand stack values three values down`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x5D.toByte(),
            ),
            maxStack = 5,
        )

        assertEquals(
            listOf(JvmIntValue(2), JvmIntValue(3), JvmIntValue(1), JvmIntValue(2), JvmIntValue(3)),
            result.operandStack.toList(),
        )
        assertEquals(5, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x1 duplicates the top category two operand stack value two values down`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x0A.toByte(),
                0x5D.toByte(),
            ),
            maxStack = 5,
        )

        assertEquals(
            listOf(JvmLongValue(1), JvmIntValue(1), JvmLongValue(1)),
            result.operandStack.toList(),
        )
        assertEquals(5, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x2 duplicates the top two category one operand stack values four values down`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0x07.toByte(),
                0x5E.toByte(),
            ),
            maxStack = 6,
        )

        assertEquals(
            listOf(
                JvmIntValue(3),
                JvmIntValue(4),
                JvmIntValue(1),
                JvmIntValue(2),
                JvmIntValue(3),
                JvmIntValue(4),
            ),
            result.operandStack.toList(),
        )
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x2 duplicates the top category two operand stack value over two category one values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x0A.toByte(),
                0x5E.toByte(),
            ),
            maxStack = 6,
        )

        assertEquals(
            listOf(JvmLongValue(1), JvmIntValue(1), JvmIntValue(2), JvmLongValue(1)),
            result.operandStack.toList(),
        )
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x2 duplicates the top two category one operand stack values over category two`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0x04.toByte(),
                0x05.toByte(),
                0x5E.toByte(),
            ),
            maxStack = 6,
        )

        assertEquals(
            listOf(JvmIntValue(1), JvmIntValue(2), JvmLongValue(1), JvmIntValue(1), JvmIntValue(2)),
            result.operandStack.toList(),
        )
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dup2_x2 duplicates the top category two operand stack value over category two`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x09.toByte(),
                0x0A.toByte(),
                0x5E.toByte(),
            ),
            maxStack = 6,
        )

        assertEquals(
            listOf(JvmLongValue(1), JvmLongValue(0), JvmLongValue(1)),
            result.operandStack.toList(),
        )
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `swap exchanges the top two category one operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x5F.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(2), JvmIntValue(1)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `iadd adds the top two int operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x06.toByte(),
                0x60.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `iadd wraps signed int overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x60.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(Int.MIN_VALUE)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `isub subtracts the top int operand stack value from the next value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x07.toByte(),
                0x05.toByte(),
                0x64.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `isub wraps signed int overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x64.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MIN_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(Int.MAX_VALUE)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `imul multiplies the top two int operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x06.toByte(),
                0x68.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(6)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `imul wraps signed int overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x05.toByte(),
                0x68.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(-2)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `idiv divides the next int operand stack value by the top value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x07.toByte(),
                0x05.toByte(),
                0x6C.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `idiv rounds integer quotients toward zero`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xF9.toByte(),
                0x05.toByte(),
                0x6C.toByte(),
                0x10.toByte(),
                0x07.toByte(),
                0x10.toByte(),
                0xFE.toByte(),
                0x6C.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(-3), JvmIntValue(-3)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `idiv returns the dividend for minimum int divided by negative one`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x02.toByte(),
                0x6C.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MIN_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(Int.MIN_VALUE)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `idiv throws ArithmeticException when the divisor is zero`() {
        val exception = assertFailsWith<ArithmeticException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0x03.toByte(),
                    0x6C.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("idiv at offset 2: division by zero", exception.message)
    }

    @Test
    fun `irem divides the next int operand stack value by the top value and pushes the remainder`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x07.toByte(),
                0x05.toByte(),
                0x70.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `irem keeps the remainder sign aligned with the dividend`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xF9.toByte(),
                0x05.toByte(),
                0x70.toByte(),
                0x10.toByte(),
                0x07.toByte(),
                0x10.toByte(),
                0xFE.toByte(),
                0x70.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(-1), JvmIntValue(1)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `irem returns zero for minimum int divided by negative one`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x02.toByte(),
                0x70.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MIN_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `irem throws ArithmeticException when the divisor is zero`() {
        val exception = assertFailsWith<ArithmeticException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0x03.toByte(),
                    0x70.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("irem at offset 2: division by zero", exception.message)
    }

    @Test
    fun `ladd adds the top two long operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0x0A.toByte(),
                0x61.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(listOf(JvmLongValue(2L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ladd wraps signed long overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0A.toByte(),
                0x61.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(Long.MIN_VALUE)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lsub subtracts the top long operand stack value from the next value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0A.toByte(),
                0x65.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(7L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(6L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lsub wraps signed long overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0A.toByte(),
                0x65.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MIN_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(Long.MAX_VALUE)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lmul multiplies the top two long operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x69.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(3L),
                    ConstantLongEntry(2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(6L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lmul wraps signed long overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x69.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MAX_VALUE),
                    ConstantLongEntry(2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-2L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldiv divides the next long operand stack value by the top value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6D.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(7L),
                    ConstantLongEntry(2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(3L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldiv rounds long quotients toward zero`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6D.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x6D.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-7L),
                    ConstantLongEntry(2L),
                    ConstantLongEntry(7L),
                    ConstantLongEntry(-2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-3L), JvmLongValue(-3L)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `ldiv returns the dividend for minimum long divided by negative one`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6D.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MIN_VALUE),
                    ConstantLongEntry(-1L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(Long.MIN_VALUE)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldiv throws ArithmeticException when the divisor is zero`() {
        val exception = assertFailsWith<ArithmeticException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x0A.toByte(),
                    0x09.toByte(),
                    0x6D.toByte(),
                ),
                maxStack = 4,
            )
        }

        assertEquals("ldiv at offset 2: division by zero", exception.message)
    }

    @Test
    fun `lrem divides the next long operand stack value by the top value and pushes the remainder`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x71.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(7L),
                    ConstantLongEntry(2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(1L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lrem keeps the remainder sign aligned with the dividend`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x71.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x71.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-7L),
                    ConstantLongEntry(2L),
                    ConstantLongEntry(7L),
                    ConstantLongEntry(-2L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-1L), JvmLongValue(1L)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `lrem returns zero for minimum long divided by negative one`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x71.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MIN_VALUE),
                    ConstantLongEntry(-1L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(0L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lrem throws ArithmeticException when the divisor is zero`() {
        val exception = assertFailsWith<ArithmeticException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x0A.toByte(),
                    0x09.toByte(),
                    0x71.toByte(),
                ),
                maxStack = 4,
            )
        }

        assertEquals("lrem at offset 2: division by zero", exception.message)
    }

    @Test
    fun `fadd adds the top two float operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0C.toByte(),
                0x0D.toByte(),
                0x62.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmFloatValue(3.0f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fadd follows NaN and infinity addition rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x62.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x62.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x62.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Float.POSITIVE_INFINITY, values[2])
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `fadd follows signed zero addition rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0B.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x62.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x62.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x62.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-0.0f),
                    ConstantFloatEntry(1.0f),
                    ConstantFloatEntry(-1.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(0x00000000, values[0].toRawBits())
        assertEquals(Int.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x00000000, values[2].toRawBits())
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `fadd overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x62.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(Float.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fsub subtracts the top float operand stack value from the next value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0x0C.toByte(),
                0x66.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmFloatValue(1.0f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fsub follows NaN and infinity subtraction rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x66.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x66.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x66.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Float.POSITIVE_INFINITY, values[2])
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `fsub follows signed zero subtraction rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0B.toByte(),
                0x0B.toByte(),
                0x66.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x0B.toByte(),
                0x66.toByte(),
                0x0B.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x66.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-0.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(0x00000000, values[0].toRawBits())
        assertEquals(Int.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x00000000, values[2].toRawBits())
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `fsub overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x66.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.MAX_VALUE),
                    ConstantFloatEntry(-Float.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(Float.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fmul multiplies the top two float operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0D.toByte(),
                0x6A.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(3.0f),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(6.0f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fmul follows NaN and infinity multiplication rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x6A.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x0B.toByte(),
                0x6A.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x6A.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x6A.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(-1.0f),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Float.NEGATIVE_INFINITY, values[2])
        assertEquals(Float.POSITIVE_INFINITY, values[3])
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `fmul follows signed zero multiplication rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0B.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x6A.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x6A.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-1.0f),
                    ConstantFloatEntry(-0.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(Int.MIN_VALUE, values[0].toRawBits())
        assertEquals(0x00000000, values[1].toRawBits())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `fmul overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0D.toByte(),
                0x6A.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(Float.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fdiv divides the next float operand stack value by the top value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0D.toByte(),
                0x6E.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(7.0f),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(3.5f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `fdiv follows NaN infinity and division by zero rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x6E.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x6E.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x6E.toByte(),
                0x0C.toByte(),
                0x0B.toByte(),
                0x6E.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x0B.toByte(),
                0x6E.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(-1.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Float.NEGATIVE_INFINITY, values[2])
        assertEquals(Float.POSITIVE_INFINITY, values[3])
        assertEquals(Float.NEGATIVE_INFINITY, values[4])
        assertEquals(5, result.operandStack.slotDepth)
    }

    @Test
    fun `fdiv follows signed zero division rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0C.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x6E.toByte(),
                0x0C.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x6E.toByte(),
                0x0B.toByte(),
                0x0C.toByte(),
                0x6E.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x0C.toByte(),
                0x6E.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                    ConstantFloatEntry(-0.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(0x00000000, values[0].toRawBits())
        assertEquals(Int.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x00000000, values[2].toRawBits())
        assertEquals(Int.MIN_VALUE, values[3].toRawBits())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `fdiv overflows and underflows to signed results without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x6E.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x0D.toByte(),
                0x6E.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.MAX_VALUE),
                    ConstantFloatEntry(Float.MIN_VALUE),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(Float.POSITIVE_INFINITY, values[0])
        assertEquals(0x00000000, values[1].toRawBits())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `frem divides the next float operand stack value by the top value and pushes the remainder`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0D.toByte(),
                0x72.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(7.0f),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(1.0f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `frem uses truncating fmod semantics and keeps the dividend sign`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0D.toByte(),
                0x72.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x0D.toByte(),
                0x72.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x72.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-7.0f),
                    ConstantFloatEntry(7.0f),
                    ConstantFloatEntry(5.5f),
                    ConstantFloatEntry(2.0f),
                ),
            ),
        )

        assertEquals(
            listOf(JvmFloatValue(-1.0f), JvmFloatValue(1.0f), JvmFloatValue(1.5f)),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `frem follows NaN infinity zero divisor and signed zero rules without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x72.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x0C.toByte(),
                0x72.toByte(),
                0x0C.toByte(),
                0x0B.toByte(),
                0x72.toByte(),
                0x0C.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x72.toByte(),
                0x0B.toByte(),
                0x0C.toByte(),
                0x72.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x0C.toByte(),
                0x72.toByte(),
            ),
            maxStack = 7,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(-0.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(true, values[2].isNaN())
        assertEquals(1.0f, values[3])
        assertEquals(0x00000000, values[4].toRawBits())
        assertEquals(Int.MIN_VALUE, values[5].toRawBits())
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dadd adds the top two double operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0F.toByte(),
                0x0F.toByte(),
                0x63.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(listOf(JvmDoubleValue(2.0)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dadd follows NaN and infinity addition rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x63.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x63.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x63.toByte(),
            ),
            maxStack = 8,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Double.POSITIVE_INFINITY, values[2])
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dadd follows signed zero addition rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x63.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x63.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x63.toByte(),
            ),
            maxStack = 8,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-0.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(-1.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(0x0000000000000000L, values[0].toRawBits())
        assertEquals(Long.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x0000000000000000L, values[2].toRawBits())
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dadd overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x63.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(Double.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dsub subtracts the top double operand stack value from the next value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x67.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(7.0),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(6.0)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dsub follows NaN and infinity subtraction rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x67.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x67.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x67.toByte(),
            ),
            maxStack = 8,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Double.POSITIVE_INFINITY, values[2])
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dsub follows signed zero subtraction rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0E.toByte(),
                0x0E.toByte(),
                0x67.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0E.toByte(),
                0x67.toByte(),
                0x0E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x67.toByte(),
            ),
            maxStack = 8,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-0.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(0x0000000000000000L, values[0].toRawBits())
        assertEquals(Long.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x0000000000000000L, values[2].toRawBits())
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `dsub overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x67.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.MAX_VALUE),
                    ConstantDoubleEntry(-Double.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(Double.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dmul multiplies the top two double operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6B.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(3.0),
                    ConstantDoubleEntry(2.0),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(6.0)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dmul follows NaN and infinity multiplication rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x6B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x0E.toByte(),
                0x6B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x6B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x6B.toByte(),
            ),
            maxStack = 10,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(-1.0),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Double.NEGATIVE_INFINITY, values[2])
        assertEquals(Double.POSITIVE_INFINITY, values[3])
        assertEquals(8, result.operandStack.slotDepth)
    }

    @Test
    fun `dmul follows signed zero multiplication rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x6B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x6B.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-1.0),
                    ConstantDoubleEntry(-0.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(Long.MIN_VALUE, values[0].toRawBits())
        assertEquals(0x0000000000000000L, values[1].toRawBits())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dmul overflows to signed infinity without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6B.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.MAX_VALUE),
                    ConstantDoubleEntry(2.0),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(Double.POSITIVE_INFINITY)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `dmul underflows to signed zero without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6B.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.MIN_VALUE),
                    ConstantDoubleEntry(0.5),
                    ConstantDoubleEntry(-Double.MIN_VALUE),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(0x0000000000000000L, values[0].toRawBits())
        assertEquals(Long.MIN_VALUE, values[1].toRawBits())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `ddiv divides the next double operand stack value by the top value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6F.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(7.0),
                    ConstantDoubleEntry(2.0),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(3.5)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ddiv follows NaN infinity and division by zero rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x6F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x6F.toByte(),
                0x0F.toByte(),
                0x0E.toByte(),
                0x6F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x0E.toByte(),
                0x6F.toByte(),
            ),
            maxStack = 12,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(-1.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(Double.NEGATIVE_INFINITY, values[2])
        assertEquals(Double.POSITIVE_INFINITY, values[3])
        assertEquals(Double.NEGATIVE_INFINITY, values[4])
        assertEquals(10, result.operandStack.slotDepth)
    }

    @Test
    fun `ddiv follows signed zero division rules`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6F.toByte(),
                0x0F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x6F.toByte(),
                0x0E.toByte(),
                0x0F.toByte(),
                0x6F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x6F.toByte(),
            ),
            maxStack = 10,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-0.0),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(0x0000000000000000L, values[0].toRawBits())
        assertEquals(Long.MIN_VALUE, values[1].toRawBits())
        assertEquals(0x0000000000000000L, values[2].toRawBits())
        assertEquals(Long.MIN_VALUE, values[3].toRawBits())
        assertEquals(8, result.operandStack.slotDepth)
    }

    @Test
    fun `ddiv overflows and underflows to signed results without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x6F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x6F.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.MAX_VALUE),
                    ConstantDoubleEntry(Double.MIN_VALUE),
                    ConstantDoubleEntry(2.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(Double.POSITIVE_INFINITY, values[0])
        assertEquals(0x0000000000000000L, values[1].toRawBits())
        assertEquals(4, result.operandStack.slotDepth)
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
    fun `ldc reuses interned guest string constants with identical code points`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("same literal", "same literal".encodeToByteArray()),
                    ConstantStringEntry(stringIndex = ConstantPoolIndex(1)),
                    ConstantStringEntry(stringIndex = ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference, reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("same literal"),
            ),
            heap.get(reference),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc reuses guest class mirror constants with identical represented names`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                    ConstantClassEntry(nameIndex = ConstantPoolIndex(1)),
                    ConstantClassEntry(nameIndex = ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference, reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("java/lang/String"),
            ),
            heap.get(reference),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc reuses guest method type constants with identical descriptors`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x02.toByte(),
                0x12.toByte(),
                0x03.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("(Ljava/lang/String;)I", "(Ljava/lang/String;)I".encodeToByteArray()),
                    ConstantMethodTypeEntry(descriptorIndex = ConstantPoolIndex(1)),
                    ConstantMethodTypeEntry(descriptorIndex = ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference, reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodType",
                payload = JvmMethodTypePayload("(Ljava/lang/String;)I"),
            ),
            heap.get(reference),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc reuses guest method handle constants with identical symbolic references`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x07.toByte(),
                0x12.toByte(),
                0x08.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                    ConstantClassEntry(nameIndex = ConstantPoolIndex(1)),
                    ConstantUtf8Entry("valueOf", "valueOf".encodeToByteArray()),
                    ConstantUtf8Entry("(I)Ljava/lang/String;", "(I)Ljava/lang/String;".encodeToByteArray()),
                    ConstantNameAndTypeEntry(nameIndex = ConstantPoolIndex(3), descriptorIndex = ConstantPoolIndex(4)),
                    ConstantMethodRefEntry(classIndex = ConstantPoolIndex(2), nameAndTypeIndex = ConstantPoolIndex(5)),
                    ConstantMethodHandleEntry(
                        referenceKind = MethodHandleReferenceKind.InvokeStatic,
                        referenceIndex = ConstantPoolIndex(6),
                    ),
                    ConstantMethodHandleEntry(
                        referenceKind = MethodHandleReferenceKind.InvokeStatic,
                        referenceIndex = ConstantPoolIndex(6),
                    ),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference, reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodHandle",
                payload = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = 6,
                ),
            ),
            heap.get(reference),
        )
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc_w pushes wide indexed reference constants from the runtime constant pool`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x13.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x13.toByte(),
                0x01.toByte(),
                0x01.toByte(),
                0x13.toByte(),
                0x01.toByte(),
                0x02.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("wide literal", "wide literal".encodeToByteArray()),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ) +
                    List(252) { value -> ConstantIntegerEntry(value) } +
                    listOf(
                        ConstantStringEntry(stringIndex = ConstantPoolIndex(1)),
                        ConstantClassEntry(nameIndex = ConstantPoolIndex(2)),
                        ConstantMethodTypeEntry(descriptorIndex = ConstantPoolIndex(3)),
                    ),
            ),
            heap = heap,
        )

        val stringReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val classReference = JvmObjectReferenceValue(JvmReferenceId(2))
        val methodTypeReference = JvmObjectReferenceValue(JvmReferenceId(3))
        assertEquals(listOf(stringReference, classReference, methodTypeReference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("wide literal"),
            ),
            heap.get(stringReference),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("java/lang/String"),
            ),
            heap.get(classReference),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodType",
                payload = JvmMethodTypePayload("()V"),
            ),
            heap.get(methodTypeReference),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `ldc_w pushes wide indexed method handle constants from the runtime constant pool`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x13.toByte(),
                0x01.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                    ConstantClassEntry(nameIndex = ConstantPoolIndex(1)),
                    ConstantUtf8Entry("valueOf", "valueOf".encodeToByteArray()),
                    ConstantUtf8Entry("(I)Ljava/lang/String;", "(I)Ljava/lang/String;".encodeToByteArray()),
                    ConstantNameAndTypeEntry(nameIndex = ConstantPoolIndex(3), descriptorIndex = ConstantPoolIndex(4)),
                    ConstantMethodRefEntry(classIndex = ConstantPoolIndex(2), nameAndTypeIndex = ConstantPoolIndex(5)),
                ) +
                    List(249) { value -> ConstantIntegerEntry(value) } +
                    ConstantMethodHandleEntry(
                        referenceKind = MethodHandleReferenceKind.InvokeStatic,
                        referenceIndex = ConstantPoolIndex(6),
                    ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodHandle",
                payload = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = 6,
                ),
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
                code = byteArrayOf(0x2E.toByte()),
                maxStack = 0,
            )
        }

        assertEquals("Unsupported instruction iaload (0x2e) at offset 0", exception.message)
    }
}
