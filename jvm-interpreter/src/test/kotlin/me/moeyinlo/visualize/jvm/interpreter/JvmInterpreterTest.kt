package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInterfaceMethodRefEntry
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
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmExceptionHandler
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmHeapObject
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLocalVariables
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandlePayload
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmMethodTypePayload
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStackOverflowException
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReturnAddressValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `idiv by zero transfers control to matching ArithmeticException handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x07.toByte(),
                0x03.toByte(),
                0x6C.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/ArithmeticException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/ArithmeticException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `idiv by zero transfers control to RuntimeException handler through standard throwable hierarchy`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x07.toByte(),
                0x03.toByte(),
                0x6C.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy.Empty,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/RuntimeException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/ArithmeticException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
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
    fun `ineg negates the top int operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x07.toByte(),
                0x74.toByte(),
                0x10.toByte(),
                0xF9.toByte(),
                0x74.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(-7), JvmIntValue(7)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ineg leaves minimum int unchanged on overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x74.toByte(),
            ),
            maxStack = 1,
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
    fun `lneg negates the top long operand stack value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x75.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x75.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(7L),
                    ConstantLongEntry(-7L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-7L), JvmLongValue(7L)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `lneg leaves minimum long unchanged on overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x75.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MIN_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(Long.MIN_VALUE)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ishl shifts int values left by the low five shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x78.toByte(),
                0x04.toByte(),
                0x10.toByte(),
                0x20.toByte(),
                0x78.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(4), JvmIntValue(1)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ishl wraps shifted int overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x78.toByte(),
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
    fun `lshl shifts long values left by the low six shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0x05.toByte(),
                0x79.toByte(),
                0x0A.toByte(),
                0x10.toByte(),
                0x40.toByte(),
                0x79.toByte(),
            ),
            maxStack = 5,
        )

        assertEquals(listOf(JvmLongValue(4L), JvmLongValue(1L)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `lshl wraps shifted long overflow without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x79.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(Long.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-2L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ishr arithmetically shifts int values right by the low five shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xF8.toByte(),
                0x04.toByte(),
                0x7A.toByte(),
                0x10.toByte(),
                0x10.toByte(),
                0x10.toByte(),
                0x20.toByte(),
                0x7A.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(-4), JvmIntValue(16)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lshr arithmetically shifts long values right by the low six shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x7B.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x10.toByte(),
                0x40.toByte(),
                0x7B.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-8L),
                    ConstantLongEntry(16L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(-4L), JvmLongValue(16L)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `iushr logically shifts int values right by the low five shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xF8.toByte(),
                0x04.toByte(),
                0x7C.toByte(),
                0x10.toByte(),
                0x10.toByte(),
                0x10.toByte(),
                0x20.toByte(),
                0x7C.toByte(),
            ),
            maxStack = 3,
        )

        assertEquals(listOf(JvmIntValue(2_147_483_644), JvmIntValue(16)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `lushr logically shifts long values right by the low six shift bits`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x04.toByte(),
                0x7D.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x10.toByte(),
                0x40.toByte(),
                0x7D.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-8L),
                    ConstantLongEntry(16L),
                ),
            ),
        )

        assertEquals(
            listOf(JvmLongValue(9_223_372_036_854_775_804L), JvmLongValue(16L)),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `iand computes the bitwise and of the top two int values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xAA.toByte(),
                0x10.toByte(),
                0x0F.toByte(),
                0x7E.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(0x0A)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `land computes the bitwise and of the top two long values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x7F.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(0x0F0FL),
                    ConstantLongEntry(0x00FFL),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(0x000FL)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ior computes the bitwise or of the top two int values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x50.toByte(),
                0x10.toByte(),
                0x0F.toByte(),
                0x80.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(0x5F)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `lor computes the bitwise or of the top two long values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x81.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(0x0F00L),
                    ConstantLongEntry(0x00F0L),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(0x0FF0L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `ixor computes the bitwise exclusive or of the top two int values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x5A.toByte(),
                0x10.toByte(),
                0x3C.toByte(),
                0x82.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(0x66)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `lxor computes the bitwise exclusive or of the top two long values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x83.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(0x0FF0L),
                    ConstantLongEntry(0x00FFL),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(0x0F0FL)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `i2l sign extends the top int operand stack value into a long`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xFF.toByte(),
                0x85.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x85.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MAX_VALUE),
                ),
            ),
        )

        assertEquals(
            listOf(JvmLongValue(-1L), JvmLongValue(Int.MAX_VALUE.toLong())),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `i2f converts the top int operand stack value into a float`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xFF.toByte(),
                0x86.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x86.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MAX_VALUE),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(-1.0f, values[0])
        assertEquals(0x4F00_0000, values[1].toRawBits())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `i2d converts the top int operand stack value into a double`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xFF.toByte(),
                0x87.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x87.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(Int.MAX_VALUE),
                ),
            ),
        )

        assertEquals(
            listOf(JvmDoubleValue(-1.0), JvmDoubleValue(Int.MAX_VALUE.toDouble())),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `l2i truncates the top long operand stack value into an int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x88.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x88.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(0x1_0000_0000L),
                    ConstantLongEntry(Long.MAX_VALUE),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(0), JvmIntValue(-1)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `l2f converts the top long operand stack value into a float`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x89.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x89.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-1L),
                    ConstantLongEntry(Long.MAX_VALUE),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(-1.0f, values[0])
        assertEquals(0x5F00_0000, values[1].toRawBits())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `l2d converts the top long operand stack value into a double`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x8A.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x8A.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(-1L),
                    ConstantLongEntry(Long.MAX_VALUE),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(-1.0, values[0])
        assertEquals(0x43E0_0000_0000_0000L, values[1].toRawBits())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `f2i converts the top float operand stack value into an int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x8B.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x8B.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x8B.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x8B.toByte(),
                0x12.toByte(),
                0x05.toByte(),
                0x8B.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                    ConstantFloatEntry(3.75f),
                    ConstantFloatEntry(-3.75f),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(0),
                JvmIntValue(Int.MAX_VALUE),
                JvmIntValue(Int.MIN_VALUE),
                JvmIntValue(3),
                JvmIntValue(-3),
            ),
            result.operandStack.toList(),
        )
        assertEquals(5, result.operandStack.slotDepth)
    }

    @Test
    fun `f2l converts the top float operand stack value into a long`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x8C.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x8C.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x8C.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x8C.toByte(),
                0x12.toByte(),
                0x05.toByte(),
                0x8C.toByte(),
            ),
            maxStack = 10,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                    ConstantFloatEntry(3.75f),
                    ConstantFloatEntry(-3.75f),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmLongValue(0L),
                JvmLongValue(Long.MAX_VALUE),
                JvmLongValue(Long.MIN_VALUE),
                JvmLongValue(3L),
                JvmLongValue(-3L),
            ),
            result.operandStack.toList(),
        )
        assertEquals(10, result.operandStack.slotDepth)
    }

    @Test
    fun `f2d converts the top float operand stack value into a double`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x8D.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x8D.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x8D.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-1.5f),
                    ConstantFloatEntry(Float.MAX_VALUE),
                    ConstantFloatEntry(Float.NaN),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(-1.5, values[0])
        assertEquals(Float.MAX_VALUE.toDouble(), values[1])
        assertEquals(true, values[2].isNaN())
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `d2i converts the top double operand stack value into an int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x8E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x8E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x8E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x8E.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x8E.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                    ConstantDoubleEntry(3.75),
                    ConstantDoubleEntry(-3.75),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(0),
                JvmIntValue(Int.MAX_VALUE),
                JvmIntValue(Int.MIN_VALUE),
                JvmIntValue(3),
                JvmIntValue(-3),
            ),
            result.operandStack.toList(),
        )
        assertEquals(5, result.operandStack.slotDepth)
    }

    @Test
    fun `d2l converts the top double operand stack value into a long`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x8F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x8F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x8F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x8F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x8F.toByte(),
            ),
            maxStack = 10,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                    ConstantDoubleEntry(3.75),
                    ConstantDoubleEntry(-3.75),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmLongValue(0L),
                JvmLongValue(Long.MAX_VALUE),
                JvmLongValue(Long.MIN_VALUE),
                JvmLongValue(3L),
                JvmLongValue(-3L),
            ),
            result.operandStack.toList(),
        )
        assertEquals(10, result.operandStack.slotDepth)
    }

    @Test
    fun `d2f converts the top double operand stack value into a float`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x90.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x90.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x90.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-1.5),
                    ConstantDoubleEntry(Double.MAX_VALUE),
                    ConstantDoubleEntry(Double.NaN),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(-1.5f, values[0])
        assertEquals(Float.POSITIVE_INFINITY, values[1])
        assertEquals(true, values[2].isNaN())
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `i2b converts the top int operand stack value into a sign-extended byte int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x11.toByte(),
                0x00.toByte(),
                0x7F.toByte(),
                0x91.toByte(),
                0x11.toByte(),
                0x00.toByte(),
                0x80.toByte(),
                0x91.toByte(),
                0x11.toByte(),
                0x00.toByte(),
                0xFF.toByte(),
                0x91.toByte(),
                0x11.toByte(),
                0xFF.toByte(),
                0x7F.toByte(),
                0x91.toByte(),
            ),
            maxStack = 4,
        )

        assertEquals(
            listOf(
                JvmIntValue(127),
                JvmIntValue(-128),
                JvmIntValue(-1),
                JvmIntValue(127),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `i2c converts the top int operand stack value into a zero-extended char int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x92.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x92.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x92.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(-1),
                    ConstantIntegerEntry(0x1_0000),
                    ConstantIntegerEntry(65),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(0xFFFF),
                JvmIntValue(0),
                JvmIntValue(65),
            ),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `i2s converts the top int operand stack value into a sign-extended short int`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x93.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x93.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x93.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x93.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantIntegerEntry(32767),
                    ConstantIntegerEntry(32768),
                    ConstantIntegerEntry(65535),
                    ConstantIntegerEntry(-32769),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(32767),
                JvmIntValue(-32768),
                JvmIntValue(-1),
                JvmIntValue(32767),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `lcmp compares the top two long operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x94.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x94.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0B.toByte(),
                0x94.toByte(),
            ),
            maxStack = 6,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantLongEntry(5L),
                    ConstantLongEntry(3L),
                    ConstantLongEntry(3L),
                    ConstantLongEntry(5L),
                    ConstantLongEntry(Long.MIN_VALUE),
                    ConstantLongEntry(Long.MIN_VALUE),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(1),
                JvmIntValue(-1),
                JvmIntValue(0),
            ),
            result.operandStack.toList(),
        )
        assertEquals(3, result.operandStack.slotDepth)
    }

    @Test
    fun `fcmpl compares float operand stack values and returns minus one for NaN`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0x0C.toByte(),
                0x95.toByte(),
                0x0C.toByte(),
                0x0C.toByte(),
                0x95.toByte(),
                0x0C.toByte(),
                0x0D.toByte(),
                0x95.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x95.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(1),
                JvmIntValue(0),
                JvmIntValue(-1),
                JvmIntValue(-1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `fcmpg compares float operand stack values and returns one for NaN`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0x0C.toByte(),
                0x96.toByte(),
                0x0C.toByte(),
                0x0C.toByte(),
                0x96.toByte(),
                0x0C.toByte(),
                0x0D.toByte(),
                0x96.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x0C.toByte(),
                0x96.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(1),
                JvmIntValue(0),
                JvmIntValue(-1),
                JvmIntValue(1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dcmpl compares double operand stack values and returns minus one for NaN`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x97.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x97.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0B.toByte(),
                0x97.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0D.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0F.toByte(),
                0x97.toByte(),
            ),
            maxStack = 7,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(2.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(2.0),
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(1.0),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(1),
                JvmIntValue(0),
                JvmIntValue(-1),
                JvmIntValue(-1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dcmpg compares double operand stack values and returns one for NaN`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x98.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x98.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0B.toByte(),
                0x98.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0D.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x0F.toByte(),
                0x98.toByte(),
            ),
            maxStack = 7,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(2.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(1.0),
                    ConstantDoubleEntry(2.0),
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(1.0),
                ),
            ),
        )

        assertEquals(
            listOf(
                JvmIntValue(1),
                JvmIntValue(0),
                JvmIntValue(-1),
                JvmIntValue(1),
            ),
            result.operandStack.toList(),
        )
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `ifeq branches when int operand is zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x99.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x99.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifne branches when int operand is not zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x9A.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x9A.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `iflt branches when int operand is less than zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xFF.toByte(),
                0x9B.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x9B.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifge branches when int operand is greater than or equal to zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x9C.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0xFF.toByte(),
                0x9C.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifgt branches when int operand is greater than zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x9D.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x9D.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifle branches when int operand is less than or equal to zero and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0x9E.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x9E.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmpeq branches when two int operands are equal and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x04.toByte(),
                0x9F.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0x9F.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmpne branches when two int operands are not equal and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0xA0.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x04.toByte(),
                0xA0.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmplt branches when the next int operand is less than the top int operand and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0xA1.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x04.toByte(),
                0xA1.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmpge branches when the next int operand is greater than or equal to the top int operand and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x04.toByte(),
                0xA2.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x05.toByte(),
                0xA2.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmpgt branches when the next int operand is greater than the top int operand and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x04.toByte(),
                0xA3.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x04.toByte(),
                0xA3.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_icmple branches when the next int operand is less than or equal to the top int operand and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x04.toByte(),
                0xA4.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
        )
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x05.toByte(),
                0x04.toByte(),
                0xA4.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_acmpeq branches when two reference operands are equal and falls through otherwise`() {
        val sameReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val takenLocals = JvmLocalVariables(maxLocals = 2)
        takenLocals.store(0, sameReference)
        takenLocals.store(1, sameReference)
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xA5.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
            localVariables = takenLocals,
        )

        val notTakenLocals = JvmLocalVariables(maxLocals = 2)
        notTakenLocals.store(0, JvmObjectReferenceValue(JvmReferenceId(1)))
        notTakenLocals.store(1, JvmObjectReferenceValue(JvmReferenceId(2)))
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xA5.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
            localVariables = notTakenLocals,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `if_acmpne branches when two reference operands are not equal and falls through otherwise`() {
        val takenLocals = JvmLocalVariables(maxLocals = 2)
        takenLocals.store(0, JvmObjectReferenceValue(JvmReferenceId(1)))
        takenLocals.store(1, JvmObjectReferenceValue(JvmReferenceId(2)))
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xA6.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 2,
            localVariables = takenLocals,
        )

        val sameReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val notTakenLocals = JvmLocalVariables(maxLocals = 2)
        notTakenLocals.store(0, sameReference)
        notTakenLocals.store(1, sameReference)
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xA6.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 2,
            localVariables = notTakenLocals,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifnull branches when the reference operand is null and falls through otherwise`() {
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xC6.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )

        val notTakenLocals = JvmLocalVariables(maxLocals = 1)
        notTakenLocals.store(0, JvmObjectReferenceValue(JvmReferenceId(1)))
        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC6.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
            localVariables = notTakenLocals,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `ifnonnull branches when the reference operand is not null and falls through otherwise`() {
        val takenLocals = JvmLocalVariables(maxLocals = 1)
        takenLocals.store(0, JvmObjectReferenceValue(JvmReferenceId(1)))
        val taken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC7.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
            localVariables = takenLocals,
        )

        val notTaken = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xC7.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x05.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), taken.operandStack.toList())
        assertEquals(1, taken.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(2)), notTaken.operandStack.toList())
        assertEquals(1, notTaken.operandStack.slotDepth)
    }

    @Test
    fun `goto branches unconditionally using a signed short offset`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xA7.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `goto_w branches unconditionally using a signed int offset`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC8.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `tableswitch branches by key and uses the default offset for out of range values`() {
        val matchingCase = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0xAA.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x17.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x05.toByte(),
                0xA7.toByte(),
                0x00.toByte(),
                0x04.toByte(),
                0x06.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )
        val defaultCase = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0xAA.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x17.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x05.toByte(),
                0xA7.toByte(),
                0x00.toByte(),
                0x04.toByte(),
                0x06.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(2)), matchingCase.operandStack.toList())
        assertEquals(1, matchingCase.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(3)), defaultCase.operandStack.toList())
        assertEquals(1, defaultCase.operandStack.slotDepth)
    }

    @Test
    fun `lookupswitch branches by matching key and uses the default offset for missing keys`() {
        val matchingCase = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0xAB.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1F.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1F.toByte(),
                0x05.toByte(),
                0xA7.toByte(),
                0x00.toByte(),
                0x04.toByte(),
                0x06.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )
        val defaultCase = JvmInterpreter.execute(
            code = byteArrayOf(
                0x03.toByte(),
                0xAB.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1F.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1B.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x1F.toByte(),
                0x05.toByte(),
                0xA7.toByte(),
                0x00.toByte(),
                0x04.toByte(),
                0x06.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(2)), matchingCase.operandStack.toList())
        assertEquals(1, matchingCase.operandStack.slotDepth)
        assertEquals(listOf(JvmIntValue(3)), defaultCase.operandStack.toList())
        assertEquals(1, defaultCase.operandStack.slotDepth)
    }

    @Test
    fun `jsr pushes the return address and branches using a signed short offset`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xA8.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmReturnAddressValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `jsr_w pushes the return address and branches using a signed int offset`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC9.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmReturnAddressValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `ret branches to the returnAddress stored in a local variable`() {
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, JvmReturnAddressValue(4))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xA9.toByte(),
                0x00.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x10.toByte(),
                0x07.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
            localVariables = localVariables,
        )

        assertEquals(listOf(JvmIntValue(7)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `wide ret branches using an unsigned short local variable index`() {
        val localVariables = JvmLocalVariables(maxLocals = 259)
        localVariables.store(258, JvmReturnAddressValue(6))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xC4.toByte(),
                0xA9.toByte(),
                0x01.toByte(),
                0x02.toByte(),
                0x10.toByte(),
                0x63.toByte(),
                0x10.toByte(),
                0x07.toByte(),
                0x00.toByte(),
            ),
            maxStack = 1,
            localVariables = localVariables,
        )

        assertEquals(listOf(JvmIntValue(7)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `new allocates an object for a CONSTANT_Class reference`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xBB.toByte(),
                0x00.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("example/Foo", "example/Foo".encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("example/Foo", heap.get(reference).className)
        assertFalse(heap.isInitialized(reference))
    }

    @Test
    fun `invokespecial initializes an uninitialized object after constructor returns`() {
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xBB.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0x59.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x03.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "<init>",
                                descriptor = "()V",
                                isStatic = false,
                                code = byteArrayOf(0xB1.toByte()),
                                maxStack = 0,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Owner",
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals("Owner", heap.get(reference).className)
        assertEquals(1, result.operandStack.slotDepth)
        assertTrue(heap.isInitialized(reference))
    }

    @Test
    fun `invokespecial allows current constructor to initialize receiver through direct superclass constructor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateUninitializedObject("pkg/Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Super", "pkg/Super".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Sub",
                        superclassName = "pkg/Super",
                    ),
                    JvmClassDefinition(
                        internalName = "pkg/Super",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "<init>",
                                descriptor = "()V",
                                isStatic = false,
                                code = byteArrayOf(0xB1.toByte()),
                                maxStack = 0,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "pkg/Sub",
        )

        assertTrue(heap.isInitialized(receiver))
    }

    @Test
    fun `invokespecial rejects superclass constructor outside receiver constructor context`() {
        val heap = JvmHeap()
        val receiver = heap.allocateUninitializedObject("pkg/Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Super", "pkg/Super".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Sub",
                            superclassName = "pkg/Super",
                        ),
                        JvmClassDefinition(
                            internalName = "pkg/Super",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "<init>",
                                    descriptor = "()V",
                                    isStatic = false,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "pkg/Helper",
            )
        }

        assertEquals(
            "Constructor pkg/Super.<init>:()V cannot initialize receiver pkg/Sub " +
                "outside constructor context for pkg/Sub",
            exception.message,
        )
        assertFalse(heap.isInitialized(receiver))
    }

    @Test
    fun `invokespecial rejects constructor descriptors that do not return void`() {
        val heap = JvmHeap()
        val receiver = heap.allocateUninitializedObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "<init>",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x04.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Owner",
            )
        }

        assertEquals("Constructor Owner.<init>:()I must have a void descriptor for invokespecial", exception.message)
        assertFalse(heap.isInitialized(receiver))
    }

    @Test
    fun `invokespecial rejects constructor calls on initialized receivers`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "<init>",
                                    descriptor = "()V",
                                    isStatic = false,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Owner",
            )
        }

        assertEquals("Constructor Owner.<init>:()V receiver is already initialized", exception.message)
        assertTrue(heap.isInitialized(receiver))
    }

    @Test
    fun `invokespecial rejects non constructor calls on uninitialized receivers`() {
        val heap = JvmHeap()
        val receiver = heap.allocateUninitializedObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x04.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Owner",
            )
        }

        assertEquals("Cannot invoke special method Owner.value:()I on uninitialized receiver", exception.message)
        assertFalse(heap.isInitialized(receiver))
    }

    @Test
    fun `anewarray allocates a reference array with default null values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBD.toByte(),
                0x00.toByte(),
                0x02.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("example/Foo", "example/Foo".encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                ),
            ),
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[Lexample/Foo;", array.className)
        val payload = array.payload as JvmReferenceArrayPayload
        assertEquals(listOf<JvmReferenceValue>(JvmNullValue, JvmNullValue, JvmNullValue), payload.elements)
    }

    @Test
    fun `newarray allocates an int array with default zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x0A.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[I", array.className)
        val payload = array.payload as JvmIntArrayPayload
        assertEquals(listOf(0, 0, 0), payload.elements)
    }

    @Test
    fun `arraylength returns the length of an int array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x0A.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a reference array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBD.toByte(),
                0x00.toByte(),
                0x02.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantUtf8Entry("example/Foo", "example/Foo".encodeToByteArray()),
                    ConstantClassEntry(ConstantPoolIndex(1)),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a boolean array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x04.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a byte array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x08.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a char array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x05.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a short array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x09.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a float array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x06.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a double array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x07.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength returns the length of a long array`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x0B.toByte(),
                0xBE.toByte(),
            ),
            maxStack = 1,
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `arraylength throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xBE.toByte(),
                ),
                maxStack = 1,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("arraylength on null arrayref", exception.message)
    }

    @Test
    fun `iaload loads an int from an int array`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val payload = heap.get(reference).payload as JvmIntArrayPayload
        payload.elements[1] = 42
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x2E.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(42)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `iaload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x2E.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("iaload on null arrayref", exception.message)
    }

    @Test
    fun `iaload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x2E.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("iaload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `iaload out of range index transfers control to matching ArrayIndexOutOfBoundsException handler`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x06.toByte(),
                0x2E.toByte(),
                0x4C.toByte(),
                0x08.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/ArrayIndexOutOfBoundsException",
                ),
            ),
        )

        val caught = locals.load(1) as JvmObjectReferenceValue
        assertEquals("java/lang/ArrayIndexOutOfBoundsException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `laload loads a long from a long array`() {
        val heap = JvmHeap()
        val reference = heap.allocateLongArray(3)
        val payload = heap.get(reference).payload as JvmLongArrayPayload
        payload.elements[1] = 9_876_543_210L
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x2F.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmLongValue(9_876_543_210L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `laload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x2F.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("laload on null arrayref", exception.message)
    }

    @Test
    fun `laload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateLongArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x2F.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("laload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `faload loads a float from a float array`() {
        val heap = JvmHeap()
        val reference = heap.allocateFloatArray(3)
        val payload = heap.get(reference).payload as JvmFloatArrayPayload
        payload.elements[1] = -1.25f
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x30.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmFloatValue(-1.25f)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `faload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x30.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("faload on null arrayref", exception.message)
    }

    @Test
    fun `faload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateFloatArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x30.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("faload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `daload loads a double from a double array`() {
        val heap = JvmHeap()
        val reference = heap.allocateDoubleArray(3)
        val payload = heap.get(reference).payload as JvmDoubleArrayPayload
        payload.elements[1] = -0.25
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x31.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmDoubleValue(-0.25)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `daload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x31.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("daload on null arrayref", exception.message)
    }

    @Test
    fun `daload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateDoubleArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x31.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("daload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `aaload loads a reference from a reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("example/Foo", 3)
        val objectReference = heap.allocateObject("example/Foo")
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        payload.elements[1] = objectReference
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, arrayReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x32.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(objectReference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `aaload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x32.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("aaload on null arrayref", exception.message)
    }

    @Test
    fun `aaload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("example/Foo", 3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, arrayReference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x32.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("aaload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `baload loads a byte from a byte array as an int`() {
        val heap = JvmHeap()
        val reference = heap.allocateByteArray(3)
        val payload = heap.get(reference).payload as JvmByteArrayPayload
        payload.elements[1] = (-2).toByte()
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x33.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(-2)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `baload loads a boolean from a boolean array as an int`() {
        val heap = JvmHeap()
        val reference = heap.allocateBooleanArray(3)
        val payload = heap.get(reference).payload as JvmBooleanArrayPayload
        payload.elements[1] = true
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x33.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `baload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x33.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("baload on null arrayref", exception.message)
    }

    @Test
    fun `baload throws guest ArrayIndexOutOfBoundsException for out of range byte array index`() {
        val heap = JvmHeap()
        val reference = heap.allocateByteArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x33.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("baload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `baload throws guest ArrayIndexOutOfBoundsException for out of range boolean array index`() {
        val heap = JvmHeap()
        val reference = heap.allocateBooleanArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x33.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("baload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `caload loads a char from a char array as an int`() {
        val heap = JvmHeap()
        val reference = heap.allocateCharArray(3)
        val payload = heap.get(reference).payload as JvmCharArrayPayload
        payload.elements[1] = '\u20AC'
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x34.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(0x20AC)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `caload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x34.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("caload on null arrayref", exception.message)
    }

    @Test
    fun `caload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateCharArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x34.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("caload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `saload loads a short from a short array as an int`() {
        val heap = JvmHeap()
        val reference = heap.allocateShortArray(3)
        val payload = heap.get(reference).payload as JvmShortArrayPayload
        payload.elements[1] = (-1234).toShort()
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x35.toByte(),
            ),
            maxStack = 2,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(-1234)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `saload throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x35.toByte(),
                ),
                maxStack = 2,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("saload on null arrayref", exception.message)
    }

    @Test
    fun `saload throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateShortArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x35.toByte(),
                ),
                maxStack = 2,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("saload index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `iastore stores an int into an int array`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val payload = heap.get(reference).payload as JvmIntArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x10.toByte(),
                0x2A.toByte(),
                0x4F.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(42, payload.elements[1])
    }

    @Test
    fun `iastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x03.toByte(),
                    0x4F.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("iastore on null arrayref", exception.message)
    }

    @Test
    fun `iastore throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x03.toByte(),
                    0x4F.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("iastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `lastore stores a long into a long array`() {
        val heap = JvmHeap()
        val reference = heap.allocateLongArray(3)
        val payload = heap.get(reference).payload as JvmLongArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x0A.toByte(),
                0x50.toByte(),
            ),
            maxStack = 4,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(1L, payload.elements[1])
    }

    @Test
    fun `lastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x0A.toByte(),
                    0x50.toByte(),
                ),
                maxStack = 4,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("lastore on null arrayref", exception.message)
    }

    @Test
    fun `lastore throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateLongArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x0A.toByte(),
                    0x50.toByte(),
                ),
                maxStack = 4,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("lastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `fastore stores a float into a float array`() {
        val heap = JvmHeap()
        val reference = heap.allocateFloatArray(3)
        val payload = heap.get(reference).payload as JvmFloatArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x0D.toByte(),
                0x51.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(2.0f, payload.elements[1])
    }

    @Test
    fun `fastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x0D.toByte(),
                    0x51.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("fastore on null arrayref", exception.message)
    }

    @Test
    fun `fastore throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateFloatArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x0D.toByte(),
                    0x51.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("fastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `dastore stores a double into a double array`() {
        val heap = JvmHeap()
        val reference = heap.allocateDoubleArray(3)
        val payload = heap.get(reference).payload as JvmDoubleArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x0F.toByte(),
                0x52.toByte(),
            ),
            maxStack = 4,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(1.0, payload.elements[1])
    }

    @Test
    fun `dastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x0F.toByte(),
                    0x52.toByte(),
                ),
                maxStack = 4,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("dastore on null arrayref", exception.message)
    }

    @Test
    fun `dastore throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val reference = heap.allocateDoubleArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x0F.toByte(),
                    0x52.toByte(),
                ),
                maxStack = 4,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("dastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `aastore stores a reference into a reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Object", 3)
        val valueReference = heap.allocateObject("java/lang/Object")
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(valueReference, payload.elements[1])
    }

    @Test
    fun `aastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x01.toByte(),
                    0x53.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("aastore on null arrayref", exception.message)
    }

    @Test
    fun `aastore throws guest ArrayIndexOutOfBoundsException for out of range index`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Object", 3)
        val valueReference = heap.allocateObject("java/lang/Object")
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x2B.toByte(),
                    0x53.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("aastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `aastore throws guest ArrayStoreException for incompatible object element`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Integer", 1)
        val valueReference = heap.allocateObject("java/lang/String")
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val exception = assertFailsWith<JvmArrayStoreException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x03.toByte(),
                    0x2B.toByte(),
                    0x53.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayStoreException", exception.guestClassName)
        assertEquals(
            "aastore cannot store java/lang/String into [Ljava/lang/Integer;",
            exception.message,
        )
    }

    @Test
    fun `aastore incompatible element transfers control to matching ArrayStoreException handler`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Integer", 1)
        val valueReference = heap.allocateObject("java/lang/String")
        val locals = JvmLocalVariables(maxLocals = 3)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x03.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
                0x4D.toByte(),
                0x06.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 4,
                    handlerPc = 4,
                    catchClassName = "java/lang/ArrayStoreException",
                ),
            ),
        )

        val caught = locals.load(2) as JvmObjectReferenceValue
        assertEquals("java/lang/ArrayStoreException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
    }

    @Test
    fun `aastore stores subclass object into superclass reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Number", 1)
        val valueReference = heap.allocateObject("java/lang/Integer")
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x03.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "java/lang/Integer",
                        superclassName = "java/lang/Number",
                    ),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(valueReference, payload.elements[0])
    }

    @Test
    fun `aastore stores covariant reference array into reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("[Ljava/lang/Object;", 1)
        val valueReference = heap.allocateReferenceArray("java/lang/String", 1)
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x03.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(valueReference, payload.elements[0])
    }

    @Test
    fun `aastore stores reference array into Cloneable reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/lang/Cloneable", 1)
        val valueReference = heap.allocateReferenceArray("java/lang/String", 1)
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x03.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(valueReference, payload.elements[0])
    }

    @Test
    fun `aastore stores reference array into Serializable reference array`() {
        val heap = JvmHeap()
        val arrayReference = heap.allocateReferenceArray("java/io/Serializable", 1)
        val valueReference = heap.allocateReferenceArray("java/lang/String", 1)
        val payload = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, arrayReference)
        locals.store(1, valueReference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x03.toByte(),
                0x2B.toByte(),
                0x53.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(valueReference, payload.elements[0])
    }

    @Test
    fun `checkcast leaves null reference on operand stack`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xC0.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
        )

        assertEquals(listOf(JvmNullValue), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `checkcast leaves assignable object reference on operand stack`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/String")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC0.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `checkcast throws guest ClassCastException for incompatible object reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/Integer")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmClassCastException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xC0.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantClassEntry(ConstantPoolIndex(2)),
                        ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ClassCastException", exception.guestClassName)
        assertEquals("java/lang/Integer cannot be cast to java/lang/String", exception.message)
    }

    @Test
    fun `checkcast incompatible reference transfers control to matching ClassCastException handler`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/Integer")
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC0.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4C.toByte(),
                0x07.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 4,
                    handlerPc = 4,
                    catchClassName = "java/lang/ClassCastException",
                ),
            ),
        )

        val caught = locals.load(1) as JvmObjectReferenceValue
        assertEquals("java/lang/ClassCastException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
    }

    @Test
    fun `instanceof pushes zero for null reference`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xC1.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `instanceof pushes one for assignable object reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/String")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC1.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `instanceof pushes zero for incompatible object reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/Integer")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC1.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantClassEntry(ConstantPoolIndex(2)),
                    ConstantUtf8Entry("java/lang/String", "java/lang/String".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes prepared int static field value`() {
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
            ),
            JvmIntValue(7),
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
        )

        assertEquals(listOf(JvmIntValue(7)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic reads superclass field after resolving symbolic field reference`() {
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Parent",
                name = "counter",
                descriptor = "I",
            ),
            JvmIntValue(11),
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Example", superclassName = "Parent"),
                    JvmClassDefinition(
                        internalName = "Parent",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true)),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(11)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic throws guest IncompatibleClassChangeError for instance fields`() {
        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false)),
                        ),
                    ),
                ),
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected static field Example.counter:I for getstatic", exception.message)
    }

    @Test
    fun `getstatic instance field error transfers control to matching IncompatibleClassChangeError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false)),
                    ),
                ),
            ),
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/IncompatibleClassChangeError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/IncompatibleClassChangeError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `getstatic throws guest IllegalAccessError for private fields from another class`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = true,
                                    isPrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private field Owner.secret:I", exception.message)
    }

    @Test
    fun `getstatic private field error transfers control to matching IllegalAccessError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "secret",
                                descriptor = "I",
                                isStatic = true,
                                isPrivate = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("Caller"),
                ),
            ),
            currentClassName = "Caller",
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/IllegalAccessError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/IllegalAccessError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `getstatic throws guest IllegalAccessError for package private fields from another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = true,
                                    isPackagePrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access package-private field pkg/Owner.secret:I",
            exception.message,
        )
    }

    @Test
    fun `getstatic allows package private fields from the same package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "shared",
                                descriptor = "I",
                                isStatic = true,
                                isPackagePrivate = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic throws guest IllegalAccessError for protected fields from non subclass in another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = true,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access protected field pkg/Owner.guarded:I",
            exception.message,
        )
    }

    @Test
    fun `getstatic allows protected fields from the same package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = true,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic allows protected superclass fields from subclasses in another package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "lib/Base",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = true,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic throws guest NoClassDefFoundError when field owner class is missing`() {
        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Missing", "Missing".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("Other"),
                    ),
                    strictClassResolution = true,
                ),
            )
        }

        assertEquals("Missing", exception.message)
        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
    }

    @Test
    fun `getstatic missing field owner transfers control to matching NoClassDefFoundError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Missing", "Missing".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Other"),
                ),
                strictClassResolution = true,
            ),
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/NoClassDefFoundError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NoClassDefFoundError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `getstatic throws guest NoSuchFieldError when resolved field is missing`() {
        val exception = assertFailsWith<JvmNoSuchFieldError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("Owner"),
                    ),
                    strictClassResolution = true,
                ),
            )
        }

        assertEquals("java/lang/NoSuchFieldError", exception.guestClassName)
        assertEquals("Owner.missing:I", exception.message)
    }

    @Test
    fun `getstatic missing resolved field transfers control to matching NoSuchFieldError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Owner"),
                ),
                strictClassResolution = true,
            ),
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/NoSuchFieldError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NoSuchFieldError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `getstatic pushes prepared long static field value as category two`() {
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "wide",
                descriptor = "J",
            ),
            JvmLongValue(7L),
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("wide", "wide".encodeToByteArray()),
                    ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
        )

        assertEquals(listOf(JvmLongValue(7L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes default zero for unwritten int static field`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes default null for unwritten reference static field`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
        )

        assertEquals(listOf(JvmNullValue), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic rejects prepared value that does not match field descriptor`() {
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
            ),
            JvmLongValue(7L),
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                staticFields = staticFields,
            )
        }

        assertEquals(
            "Invalid getstatic value for Example.counter:I at offset 0: expected I but was JvmLongValue",
            exception.message,
        )
    }

    @Test
    fun `getstatic pushes object reference assignable to declared field class`() {
        val heap = JvmHeap()
        val value = heap.allocateObject("example/StringChild")
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/String;",
            ),
            value,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes array reference assignable to declared array field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("example/StringChild", 1)
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "values",
                descriptor = "[Ljava/lang/String;",
            ),
            value,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("values", "values".encodeToByteArray()),
                    ConstantUtf8Entry("[Ljava/lang/String;", "[Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes array reference assignable to declared object field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Object;",
            ),
            value,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes array reference assignable to declared Cloneable field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Cloneable;",
            ),
            value,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Cloneable;", "Ljava/lang/Cloneable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic pushes array reference assignable to declared Serializable field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/io/Serializable;",
            ),
            value,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB2.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/io/Serializable;", "Ljava/io/Serializable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getstatic rejects object reference that is not assignable to declared field class`() {
        val heap = JvmHeap()
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/String;",
            ),
            incompatibleValue,
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                staticFields = staticFields,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid getstatic value for Example.value:Ljava/lang/String; at offset 0: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `putstatic stores int value into prepared static fields`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x09.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(9), staticFields.get(field))
    }

    @Test
    fun `putstatic writes superclass field after resolving symbolic field reference`() {
        val staticFields = JvmStaticFields()
        val resolvedField = JvmFieldReference(
            ownerClassName = "Parent",
            name = "counter",
            descriptor = "I",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x10.toByte(),
                0x0C.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Example", superclassName = "Parent"),
                    JvmClassDefinition(
                        internalName = "Parent",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true)),
                    ),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(12), staticFields.get(resolvedField))
    }

    @Test
    fun `putstatic throws guest IncompatibleClassChangeError for instance fields`() {
        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false)),
                        ),
                    ),
                ),
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected static field Example.counter:I for putstatic", exception.message)
    }

    @Test
    fun `putstatic throws guest IllegalAccessError for private fields from another class`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = true,
                                    isPrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private field Owner.secret:I", exception.message)
    }

    @Test
    fun `putstatic throws guest IllegalAccessError for package private fields from another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = true,
                                    isPackagePrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access package-private field pkg/Owner.secret:I",
            exception.message,
        )
    }

    @Test
    fun `putstatic allows package private fields from the same package`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "pkg/Owner",
            name = "shared",
            descriptor = "I",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "shared",
                                descriptor = "I",
                                isStatic = true,
                                isPackagePrivate = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), staticFields.get(field))
    }

    @Test
    fun `putstatic throws guest IllegalAccessError for protected fields from non subclass in another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x04.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = true,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access protected field pkg/Owner.guarded:I",
            exception.message,
        )
    }

    @Test
    fun `putstatic allows protected fields from the same package`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "pkg/Owner",
            name = "guarded",
            descriptor = "I",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = true,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), staticFields.get(field))
    }

    @Test
    fun `putstatic allows protected superclass fields from subclasses in another package`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "lib/Base",
            name = "guarded",
            descriptor = "I",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "lib/Base",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = true,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), staticFields.get(field))
    }

    @Test
    fun `putstatic stores category two long value into prepared static fields`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "wide",
            descriptor = "J",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("wide", "wide".encodeToByteArray()),
                    ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmLongValue(1L), staticFields.get(field))
    }

    @Test
    fun `putstatic rejects value that does not match field descriptor`() {
        val staticFields = JvmStaticFields()

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x0A.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                staticFields = staticFields,
            )
        }

        assertEquals(
            "Invalid putstatic value for Example.counter:I at offset 1: expected I but was JvmLongValue",
            exception.message,
        )
    }

    @Test
    fun `putstatic stores null into reference static field`() {
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmNullValue, staticFields.get(field))
    }

    @Test
    fun `putstatic stores object reference assignable to declared field class`() {
        val heap = JvmHeap()
        val value = heap.allocateObject("example/StringChild")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, value)
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, staticFields.get(field))
    }

    @Test
    fun `putstatic stores array reference assignable to declared array field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("example/StringChild", 1)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, value)
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "values",
            descriptor = "[Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("values", "values".encodeToByteArray()),
                    ConstantUtf8Entry("[Ljava/lang/String;", "[Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, staticFields.get(field))
    }

    @Test
    fun `putstatic stores array reference assignable to declared object field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, value)
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/Object;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, staticFields.get(field))
    }

    @Test
    fun `putstatic stores array reference assignable to declared Cloneable field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, value)
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/Cloneable;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Cloneable;", "Ljava/lang/Cloneable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, staticFields.get(field))
    }

    @Test
    fun `putstatic stores array reference assignable to declared Serializable field descriptor`() {
        val heap = JvmHeap()
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, value)
        val staticFields = JvmStaticFields()
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/io/Serializable;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB3.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/io/Serializable;", "Ljava/io/Serializable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            staticFields = staticFields,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, staticFields.get(field))
    }

    @Test
    fun `putstatic rejects object reference that is not assignable to declared field class`() {
        val heap = JvmHeap()
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, incompatibleValue)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB3.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid putstatic value for Example.value:Ljava/lang/String; at offset 1: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `getfield pushes int instance field value from object reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
            ),
            JvmIntValue(11),
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(11)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield reads superclass field after resolving symbolic field reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = "Parent",
                name = "counter",
                descriptor = "I",
            ),
            JvmIntValue(13),
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Example", superclassName = "Parent"),
                    JvmClassDefinition(
                        internalName = "Parent",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false)),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(13)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield throws guest IncompatibleClassChangeError for static fields`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true)),
                        ),
                    ),
                ),
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected instance field Example.counter:I for getfield", exception.message)
    }

    @Test
    fun `getfield throws guest IllegalAccessError for private fields from another class`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = false,
                                    isPrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private field Owner.secret:I", exception.message)
    }

    @Test
    fun `getfield throws guest IllegalAccessError for package private fields from another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = false,
                                    isPackagePrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access package-private field pkg/Owner.secret:I",
            exception.message,
        )
    }

    @Test
    fun `getfield allows package private fields from the same package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "shared",
                                descriptor = "I",
                                isStatic = false,
                                isPackagePrivate = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield throws guest IllegalAccessError for protected fields from non subclass in another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = false,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access protected field pkg/Owner.guarded:I",
            exception.message,
        )
    }

    @Test
    fun `getfield allows protected fields from the same package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = false,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield throws guest IllegalAccessError for protected superclass fields on unrelated receivers`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("lib/Base")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "lib/Base",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = false,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                    ),
                ),
                currentClassName = "other/Sub",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Sub cannot access protected field lib/Base.guarded:I on receiver lib/Base",
            exception.message,
        )
    }

    @Test
    fun `getfield allows protected superclass fields on subclass receivers from another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("other/Sub")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "lib/Base",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = false,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes category two long instance field value from object reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "wide",
                descriptor = "J",
            ),
            JvmLongValue(11L),
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("wide", "wide".encodeToByteArray()),
                    ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmLongValue(11L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes default zero for unwritten int instance field`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmIntValue(0)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes default null for unwritten reference instance field`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(JvmNullValue), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes object reference assignable to declared field class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateObject("example/StringChild")
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/String;",
            ),
            value,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes array reference assignable to declared array field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("example/StringChild", 1)
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "values",
                descriptor = "[Ljava/lang/String;",
            ),
            value,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("values", "values".encodeToByteArray()),
                    ConstantUtf8Entry("[Ljava/lang/String;", "[Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes array reference assignable to declared object field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Object;",
            ),
            value,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes array reference assignable to declared Cloneable field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Cloneable;",
            ),
            value,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Cloneable;", "Ljava/lang/Cloneable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield pushes array reference assignable to declared Serializable field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/io/Serializable;",
            ),
            value,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB4.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/io/Serializable;", "Ljava/io/Serializable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(listOf(value), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `getfield throws guest NullPointerException for null objectref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                    ),
                ),
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("getfield on null objectref", exception.message)
    }

    @Test
    fun `getfield rejects object reference that is not assignable to declared field class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        heap.putInstanceField(
            receiver,
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/String;",
            ),
            incompatibleValue,
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB4.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid getfield value for Example.value:Ljava/lang/String; at offset 1: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `putfield stores int value into object instance field`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x10.toByte(),
                0x0D.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(13), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield writes superclass field after resolving symbolic field reference`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val field = JvmFieldReference(
            ownerClassName = "Parent",
            name = "counter",
            descriptor = "I",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x10.toByte(),
                0x0E.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("Example", superclassName = "Parent"),
                    JvmClassDefinition(
                        internalName = "Parent",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false)),
                    ),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(14), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield throws guest IncompatibleClassChangeError for static fields`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("counter", "counter".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true)),
                        ),
                    ),
                ),
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected instance field Example.counter:I for putfield", exception.message)
    }

    @Test
    fun `putfield throws guest IllegalAccessError for private fields from another class`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = false,
                                    isPrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private field Owner.secret:I", exception.message)
    }

    @Test
    fun `putfield throws guest IllegalAccessError for package private fields from another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "secret",
                                    descriptor = "I",
                                    isStatic = false,
                                    isPackagePrivate = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access package-private field pkg/Owner.secret:I",
            exception.message,
        )
    }

    @Test
    fun `putfield allows package private fields from the same package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val field = JvmFieldReference(
            ownerClassName = "pkg/Owner",
            name = "shared",
            descriptor = "I",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "shared",
                                descriptor = "I",
                                isStatic = false,
                                isPackagePrivate = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield throws guest IllegalAccessError for protected fields from non subclass in another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = false,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access protected field pkg/Owner.guarded:I",
            exception.message,
        )
    }

    @Test
    fun `putfield allows protected fields from the same package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("pkg/Owner")
        val field = JvmFieldReference(
            ownerClassName = "pkg/Owner",
            name = "guarded",
            descriptor = "I",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = false,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield throws guest IllegalAccessError for protected superclass fields on unrelated receivers`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("lib/Base")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "lib/Base",
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "guarded",
                                    descriptor = "I",
                                    isStatic = false,
                                    isProtected = true,
                                ),
                            ),
                        ),
                        JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                    ),
                ),
                currentClassName = "other/Sub",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Sub cannot access protected field lib/Base.guarded:I on receiver lib/Base",
            exception.message,
        )
    }

    @Test
    fun `putfield allows protected superclass fields on subclass receivers from another package`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("other/Sub")
        val field = JvmFieldReference(
            ownerClassName = "lib/Base",
            name = "guarded",
            descriptor = "I",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("lib/Base", "lib/Base".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "lib/Base",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "guarded",
                                descriptor = "I",
                                isStatic = false,
                                isProtected = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition("other/Sub", superclassName = "lib/Base"),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmIntValue(1), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield stores category two long value into object instance field`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("Example")
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "wide",
            descriptor = "J",
        )
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x0A.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("wide", "wide".encodeToByteArray()),
                    ConstantUtf8Entry("J", "J".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmLongValue(1L), heap.getInstanceField(reference, field))
    }

    @Test
    fun `putfield stores null into reference instance field`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, receiver)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x01.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(JvmNullValue, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield stores object reference assignable to declared field class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateObject("example/StringChild")
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, value)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield stores array reference assignable to declared array field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("example/StringChild", 1)
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, value)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "values",
            descriptor = "[Ljava/lang/String;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("values", "values".encodeToByteArray()),
                    ConstantUtf8Entry("[Ljava/lang/String;", "[Ljava/lang/String;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    JvmClassDefinition("example/StringChild", superclassName = "java/lang/String"),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield stores array reference assignable to declared object field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, value)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/Object;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield stores array reference assignable to declared Cloneable field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, value)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/lang/Cloneable;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/lang/Cloneable;", "Ljava/lang/Cloneable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield stores array reference assignable to declared Serializable field descriptor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val value = heap.allocateReferenceArray("java/lang/String", 1)
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, value)
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "value",
            descriptor = "Ljava/io/Serializable;",
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x2B.toByte(),
                0xB5.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("Ljava/io/Serializable;", "Ljava/io/Serializable;".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(value, heap.getInstanceField(receiver, field))
    }

    @Test
    fun `putfield throws guest NullPointerException for null objectref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x04.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("I", "I".encodeToByteArray()),
                    ),
                ),
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("putfield on null objectref", exception.message)
    }

    @Test
    fun `putfield rejects object reference that is not assignable to declared field class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        val locals = JvmLocalVariables(maxLocals = 2)
        locals.store(0, receiver)
        locals.store(1, incompatibleValue)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x2B.toByte(),
                    0xB5.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/String;", "Ljava/lang/String;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = locals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition("java/lang/String", superclassName = "java/lang/Object"),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid putfield value for Example.value:Ljava/lang/String; at offset 2: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `bastore stores an int value into a byte array as a byte`() {
        val heap = JvmHeap()
        val reference = heap.allocateByteArray(3)
        val payload = heap.get(reference).payload as JvmByteArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x08.toByte(),
                0x54.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(5.toByte(), payload.elements[1])
    }

    @Test
    fun `bastore stores a nonzero int value into a boolean array as true`() {
        val heap = JvmHeap()
        val reference = heap.allocateBooleanArray(3)
        val payload = heap.get(reference).payload as JvmBooleanArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x04.toByte(),
                0x54.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(true, payload.elements[1])
    }

    @Test
    fun `bastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x03.toByte(),
                    0x54.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("bastore on null arrayref", exception.message)
    }

    @Test
    fun `bastore throws guest ArrayIndexOutOfBoundsException for out of range byte array index`() {
        val heap = JvmHeap()
        val reference = heap.allocateByteArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x03.toByte(),
                    0x54.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("bastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `bastore throws guest ArrayIndexOutOfBoundsException for out of range boolean array index`() {
        val heap = JvmHeap()
        val reference = heap.allocateBooleanArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x03.toByte(),
                    0x54.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("bastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `castore stores an int value into a char array as a char`() {
        val heap = JvmHeap()
        val reference = heap.allocateCharArray(3)
        val payload = heap.get(reference).payload as JvmCharArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x10.toByte(),
                0x41.toByte(),
                0x55.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals('A', payload.elements[1])
    }

    @Test
    fun `castore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x03.toByte(),
                    0x55.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("castore on null arrayref", exception.message)
    }

    @Test
    fun `castore throws guest ArrayIndexOutOfBoundsException for out of range int index`() {
        val heap = JvmHeap()
        val reference = heap.allocateCharArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x03.toByte(),
                    0x55.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("castore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `sastore stores an int value into a short array as a short`() {
        val heap = JvmHeap()
        val reference = heap.allocateShortArray(3)
        val payload = heap.get(reference).payload as JvmShortArrayPayload
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0x11.toByte(),
                0x12.toByte(),
                0x34.toByte(),
                0x56.toByte(),
            ),
            maxStack = 3,
            heap = heap,
            localVariables = locals,
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
        assertEquals(0x1234.toShort(), payload.elements[1])
    }

    @Test
    fun `sastore throws guest NullPointerException for null arrayref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0x03.toByte(),
                    0x03.toByte(),
                    0x56.toByte(),
                ),
                maxStack = 3,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("sastore on null arrayref", exception.message)
    }

    @Test
    fun `sastore throws guest ArrayIndexOutOfBoundsException for out of range int index`() {
        val heap = JvmHeap()
        val reference = heap.allocateShortArray(3)
        val locals = JvmLocalVariables(maxLocals = 1)
        locals.store(0, reference)

        val exception = assertFailsWith<JvmArrayIndexOutOfBoundsException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x06.toByte(),
                    0x03.toByte(),
                    0x56.toByte(),
                ),
                maxStack = 3,
                heap = heap,
                localVariables = locals,
            )
        }

        assertEquals("java/lang/ArrayIndexOutOfBoundsException", exception.guestClassName)
        assertEquals("sastore index 3 out of bounds for length 3", exception.message)
    }

    @Test
    fun `newarray allocates a boolean array with default false values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x04.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[Z", array.className)
        val payload = array.payload as JvmBooleanArrayPayload
        assertEquals(listOf(false, false, false), payload.elements)
    }

    @Test
    fun `newarray allocates a byte array with default zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[B", array.className)
        val payload = array.payload as JvmByteArrayPayload
        assertEquals(listOf(0.toByte(), 0.toByte(), 0.toByte()), payload.elements)
    }

    @Test
    fun `newarray allocates a char array with default null characters`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x05.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[C", array.className)
        val payload = array.payload as JvmCharArrayPayload
        assertEquals(listOf('\u0000', '\u0000', '\u0000'), payload.elements)
    }

    @Test
    fun `newarray allocates a short array with default zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x09.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[S", array.className)
        val payload = array.payload as JvmShortArrayPayload
        assertEquals(listOf(0.toShort(), 0.toShort(), 0.toShort()), payload.elements)
    }

    @Test
    fun `newarray allocates a float array with default positive zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x06.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[F", array.className)
        val payload = array.payload as JvmFloatArrayPayload
        assertEquals(listOf(0.0f, 0.0f, 0.0f), payload.elements)
    }

    @Test
    fun `newarray allocates a double array with default positive zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x07.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[D", array.className)
        val payload = array.payload as JvmDoubleArrayPayload
        assertEquals(listOf(0.0, 0.0, 0.0), payload.elements)
    }

    @Test
    fun `newarray allocates a long array with default zero values`() {
        val heap = JvmHeap()
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x06.toByte(),
                0xBC.toByte(),
                0x0B.toByte(),
            ),
            maxStack = 1,
            heap = heap,
        )

        val reference = JvmObjectReferenceValue(JvmReferenceId(1))
        val array = heap.get(reference)
        assertEquals(listOf(reference), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
        assertEquals("[J", array.className)
        val payload = array.payload as JvmLongArrayPayload
        assertEquals(listOf(0L, 0L, 0L), payload.elements)
    }

    @Test
    fun `newarray throws guest NegativeArraySizeException for a negative count`() {
        val exception = assertFailsWith<JvmNegativeArraySizeException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x02.toByte(),
                    0xBC.toByte(),
                    0x0A.toByte(),
                ),
                maxStack = 1,
            )
        }

        assertEquals("java/lang/NegativeArraySizeException", exception.guestClassName)
        assertEquals("-1", exception.message)
    }

    @Test
    fun `newarray negative count transfers control to matching NegativeArraySizeException handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x02.toByte(),
                0xBC.toByte(),
                0x0A.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/NegativeArraySizeException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NegativeArraySizeException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
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
    fun `fneg negates finite float operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0D.toByte(),
                0x76.toByte(),
                0x12.toByte(),
                0x01.toByte(),
                0x76.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(-3.5f),
                ),
            ),
        )

        assertEquals(listOf(JvmFloatValue(-2.0f), JvmFloatValue(3.5f)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `fneg handles NaN infinity and signed zero without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x12.toByte(),
                0x01.toByte(),
                0x76.toByte(),
                0x12.toByte(),
                0x02.toByte(),
                0x76.toByte(),
                0x12.toByte(),
                0x03.toByte(),
                0x76.toByte(),
                0x0B.toByte(),
                0x76.toByte(),
                0x12.toByte(),
                0x04.toByte(),
                0x76.toByte(),
            ),
            maxStack = 5,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantFloatEntry(Float.NaN),
                    ConstantFloatEntry(Float.POSITIVE_INFINITY),
                    ConstantFloatEntry(Float.NEGATIVE_INFINITY),
                    ConstantFloatEntry(-0.0f),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmFloatValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(Float.NEGATIVE_INFINITY, values[1])
        assertEquals(Float.POSITIVE_INFINITY, values[2])
        assertEquals(Int.MIN_VALUE, values[3].toRawBits())
        assertEquals(0x00000000, values[4].toRawBits())
        assertEquals(5, result.operandStack.slotDepth)
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
    fun `drem divides the next double operand stack value by the top value and pushes the remainder`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x73.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(7.0),
                    ConstantDoubleEntry(2.0),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(1.0)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `drem uses truncating fmod semantics and keeps the dividend sign`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x73.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x73.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x09.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x73.toByte(),
            ),
            maxStack = 8,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-7.0),
                    ConstantDoubleEntry(2.0),
                    ConstantDoubleEntry(7.0),
                    ConstantDoubleEntry(-2.0),
                    ConstantDoubleEntry(5.5),
                ),
            ),
        )

        assertEquals(
            listOf(JvmDoubleValue(-1.0), JvmDoubleValue(1.0), JvmDoubleValue(1.5)),
            result.operandStack.toList(),
        )
        assertEquals(6, result.operandStack.slotDepth)
    }

    @Test
    fun `drem follows NaN infinity zero divisor and signed zero rules without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x0F.toByte(),
                0x73.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x0F.toByte(),
                0x73.toByte(),
                0x0F.toByte(),
                0x0E.toByte(),
                0x73.toByte(),
                0x0F.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x73.toByte(),
                0x0E.toByte(),
                0x0F.toByte(),
                0x73.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x0F.toByte(),
                0x73.toByte(),
            ),
            maxStack = 14,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(-0.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(true, values[1].isNaN())
        assertEquals(true, values[2].isNaN())
        assertEquals(1.0, values[3])
        assertEquals(0x0000000000000000L, values[4].toRawBits())
        assertEquals(Long.MIN_VALUE, values[5].toRawBits())
        assertEquals(12, result.operandStack.slotDepth)
    }

    @Test
    fun `dneg negates finite double operand stack values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0F.toByte(),
                0x77.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x77.toByte(),
            ),
            maxStack = 4,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(-3.5),
                ),
            ),
        )

        assertEquals(listOf(JvmDoubleValue(-1.0), JvmDoubleValue(3.5)), result.operandStack.toList())
        assertEquals(4, result.operandStack.slotDepth)
    }

    @Test
    fun `dneg handles NaN infinity and signed zero without throwing`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x14.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x77.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x03.toByte(),
                0x77.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x05.toByte(),
                0x77.toByte(),
                0x0E.toByte(),
                0x77.toByte(),
                0x14.toByte(),
                0x00.toByte(),
                0x07.toByte(),
                0x77.toByte(),
            ),
            maxStack = 10,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantDoubleEntry(Double.NaN),
                    ConstantDoubleEntry(Double.POSITIVE_INFINITY),
                    ConstantDoubleEntry(Double.NEGATIVE_INFINITY),
                    ConstantDoubleEntry(-0.0),
                ),
            ),
        )

        val values = result.operandStack.toList().map { (it as JvmDoubleValue).value }
        assertEquals(true, values[0].isNaN())
        assertEquals(Double.NEGATIVE_INFINITY, values[1])
        assertEquals(Double.POSITIVE_INFINITY, values[2])
        assertEquals(Long.MIN_VALUE, values[3].toRawBits())
        assertEquals(0x0000000000000000L, values[4].toRawBits())
        assertEquals(10, result.operandStack.slotDepth)
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
    fun `invokestatic executes no argument int returning static method`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "()I",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x08.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic passes int arguments into callee locals`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x07.toByte(),
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("identity", "identity".encodeToByteArray()),
                    ConstantUtf8Entry("(I)I", "(I)I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "identity",
                                descriptor = "(I)I",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x1A.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic executes void static method without pushing a return value`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 0,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("touch", "touch".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "touch",
                                descriptor = "()V",
                                isStatic = true,
                                code = byteArrayOf(0xB1.toByte()),
                                maxStack = 0,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(0, result.operandStack.slotDepth)
        assertEquals(0, result.operandStack.valueCount)
    }

    @Test
    fun `invokestatic passes and returns category two long values`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x0A.toByte(),
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("identity", "identity".encodeToByteArray()),
                    ConstantUtf8Entry("(J)J", "(J)J".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "identity",
                                descriptor = "(J)J",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x1E.toByte(),
                                    0xAD.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 2,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmLongValue(1L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic throws guest IncompatibleClassChangeError for instance methods`() {
        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "answer",
                                    descriptor = "()I",
                                    isStatic = false,
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected static method Example.answer:()I for invokestatic", exception.message)
    }

    @Test
    fun `invokestatic rejects object return values not assignable to declared return class`() {
        val heap = JvmHeap()
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Object;",
            ),
            incompatibleValue,
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()Ljava/lang/String;", "()Ljava/lang/String;".encodeToByteArray()),
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(8)),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(9)),
                        ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                staticFields = staticFields,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "java/lang/String",
                            superclassName = "java/lang/Object",
                        ),
                        JvmClassDefinition(
                            internalName = "Example",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()Ljava/lang/String;",
                                    isStatic = true,
                                    code = byteArrayOf(
                                        0xB2.toByte(),
                                        0x00.toByte(),
                                        0x07.toByte(),
                                        0xB0.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 0,
                                ),
                            ),
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "value",
                                    descriptor = "Ljava/lang/Object;",
                                    isStatic = true,
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid invokestatic return for Example.value:()Ljava/lang/String; at offset 0: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `invokestatic rejects object arguments not assignable to declared parameter class`() {
        val heap = JvmHeap()
        val incompatibleValue = heap.allocateObject("java/lang/Object")
        val staticFields = JvmStaticFields()
        staticFields.put(
            JvmFieldReference(
                ownerClassName = "Example",
                name = "value",
                descriptor = "Ljava/lang/Object;",
            ),
            incompatibleValue,
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB2.toByte(),
                    0x00.toByte(),
                    0x07.toByte(),
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("consume", "consume".encodeToByteArray()),
                        ConstantUtf8Entry("(Ljava/lang/String;)V", "(Ljava/lang/String;)V".encodeToByteArray()),
                        ConstantFieldRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(8)),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(9), ConstantPoolIndex(10)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("Ljava/lang/Object;", "Ljava/lang/Object;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                staticFields = staticFields,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "java/lang/String",
                            superclassName = "java/lang/Object",
                        ),
                        JvmClassDefinition(
                            internalName = "Example",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "consume",
                                    descriptor = "(Ljava/lang/String;)V",
                                    isStatic = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                            fields = listOf(
                                JvmFieldDefinition(
                                    name = "value",
                                    descriptor = "Ljava/lang/Object;",
                                    isStatic = true,
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        assertEquals(
            "Invalid invokestatic argument for Example.consume:(Ljava/lang/String;)V at offset 3: " +
                "java/lang/Object is not assignable to java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `invokestatic throws guest NoSuchMethodError when method resolution misses`() {
        val exception = assertFailsWith<JvmNoSuchMethodError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(internalName = "Example"),
                    ),
                ),
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.missing:()V", exception.message)
    }

    @Test
    fun `invokestatic missing method transfers control to matching NoSuchMethodError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/NoSuchMethodError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NoSuchMethodError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `invokestatic throws guest NoClassDefFoundError when method owner class is missing`() {
        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Missing", "Missing".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("method", "method".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy.Empty,
            )
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("Missing", exception.message)
    }

    @Test
    fun `invokestatic throws guest UnsatisfiedLinkError for unbound native methods`() {
        val exception = assertFailsWith<JvmUnsatisfiedLinkError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Example",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "nativeValue",
                                    descriptor = "()I",
                                    isStatic = true,
                                    isNative = true,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/UnsatisfiedLinkError", exception.guestClassName)
        assertEquals("Native method Example.nativeValue:()I is not linked for invokestatic", exception.message)
    }

    @Test
    fun `invokestatic unbound native transfers control to matching UnsatisfiedLinkError handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 3,
                    handlerPc = 3,
                    catchClassName = "java/lang/UnsatisfiedLinkError",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/UnsatisfiedLinkError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `invokestatic executes bound native intrinsic methods`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.from(
                JvmNativeMethodKey(
                    ownerClassName = "Example",
                    name = "nativeValue",
                    descriptor = "()I",
                    isStatic = true,
                ) to JvmNativeMethodIntrinsic { _, invocation ->
                    assertEquals(null, invocation.receiver)
                    assertEquals(emptyList(), invocation.arguments)
                    JvmIntValue(9)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(9)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic falls back to simulated JNI when no native intrinsic is bound`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.fromSimulatedJni(
                JvmNativeMethodKey(
                    ownerClassName = "Example",
                    name = "nativeValue",
                    descriptor = "()I",
                    isStatic = true,
                ) to JvmNativeMethodIntrinsic { _, invocation ->
                    assertEquals(null, invocation.receiver)
                    assertEquals(emptyList(), invocation.arguments)
                    JvmIntValue(4)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `simulated JNI bindings can upcall interpreted static guest methods`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x07.toByte(),
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("NativeOwner", "NativeOwner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeTwice", "nativeTwice".encodeToByteArray()),
                    ConstantUtf8Entry("(I)I", "(I)I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "NativeOwner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeTwice",
                                descriptor = "(I)I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Helper",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "twice",
                                descriptor = "(I)I",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x1A.toByte(),
                                    0x1A.toByte(),
                                    0x60.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.fromSimulatedJni(
                JvmNativeMethodKey(
                    ownerClassName = "NativeOwner",
                    name = "nativeTwice",
                    descriptor = "(I)I",
                    isStatic = true,
                ) to JvmNativeMethodIntrinsic { context, invocation ->
                    context.callStaticMethod(
                        ownerClassName = "Helper",
                        name = "twice",
                        descriptor = "(I)I",
                        arguments = invocation.arguments,
                    )
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(8)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `simulated JNI bindings can upcall interpreted instance guest methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("NativeOwner")
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("NativeOwner", "NativeOwner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = localVariables,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "NativeOwner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = false,
                                isNative = true,
                            ),
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x08.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.fromSimulatedJni(
                JvmNativeMethodKey(
                    ownerClassName = "NativeOwner",
                    name = "nativeValue",
                    descriptor = "()I",
                    isStatic = false,
                ) to JvmNativeMethodIntrinsic { context, invocation ->
                    context.callInstanceMethod(
                        receiver = invocation.receiver!!,
                        ownerClassName = "NativeOwner",
                        name = "value",
                        descriptor = "()I",
                        arguments = emptyList(),
                    )
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic prefers native intrinsics over simulated JNI bindings`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "Example",
            name = "nativeValue",
            descriptor = "()I",
            isStatic = true,
        )

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry(
                intrinsics = mapOf(
                    key to JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(8) },
                ),
                simulatedJni = mapOf(
                    key to JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(2) },
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(8)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic executes native intrinsics with callee owner context`() {
        val heap = JvmHeap()
        val staticFields = JvmStaticFields()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Example", "Example".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "()I",
                                isStatic = true,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.from(
                JvmNativeMethodKey(
                    ownerClassName = "Example",
                    name = "nativeValue",
                    descriptor = "()I",
                    isStatic = true,
                ) to JvmNativeMethodIntrinsic { context, _ ->
                    assertEquals("Example", context.currentClassName)
                    assertTrue(context.heap === heap)
                    assertTrue(context.staticFields === staticFields)
                    JvmIntValue(5)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic resolves static interface methods from interface method references`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantInterfaceMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("ExampleInterface", "ExampleInterface".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ExampleInterface",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "()I",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x05.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(listOf(JvmIntValue(2)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic throws guest IllegalAccessError for private methods from another class`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "secret",
                                    descriptor = "()V",
                                    isStatic = true,
                                    isPrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private method Owner.secret:()V", exception.message)
    }

    @Test
    fun `invokestatic allows private methods from the same class`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "secret",
                                descriptor = "()I",
                                isStatic = true,
                                isPrivate = true,
                                code = byteArrayOf(
                                    0x06.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Owner",
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic throws guest IllegalAccessError for package private methods from another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "shared",
                                    descriptor = "()V",
                                    isStatic = true,
                                    isPackagePrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access package-private method pkg/Owner.shared:()V",
            exception.message,
        )
    }

    @Test
    fun `invokestatic allows package private methods from the same package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "shared",
                                descriptor = "()I",
                                isStatic = true,
                                isPackagePrivate = true,
                                code = byteArrayOf(
                                    0x07.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic throws guest IllegalAccessError for protected methods from non subclass in another package`() {
        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0xB8.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 0,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "shared",
                                    descriptor = "()V",
                                    isStatic = true,
                                    isProtected = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Caller cannot access protected method pkg/Owner.shared:()V",
            exception.message,
        )
    }

    @Test
    fun `invokestatic allows protected methods from the same package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "shared",
                                descriptor = "()I",
                                isStatic = true,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x08.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokestatic allows protected superclass methods from subclasses in another package`() {
        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("shared", "shared".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "shared",
                                descriptor = "()I",
                                isStatic = true,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x05.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 0,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "other/Sub",
                        superclassName = "pkg/Owner",
                    ),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(listOf(JvmIntValue(2)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual executes no argument int returning instance method`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x06.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual dispatches to receiver class override`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Base", "Base".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Sub",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x07.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x05.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual rejects instance initialization method names`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("<init>", "<init>".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "<init>",
                                    descriptor = "()V",
                                    isStatic = false,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Owner",
            )
        }

        assertEquals("Method Owner.<init>:()V cannot be invoked with invokevirtual", exception.message)
    }

    @Test
    fun `invokevirtual throws guest IncompatibleClassChangeError for static methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = true,
                                    code = byteArrayOf(
                                        0x06.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected instance method Owner.value:()I for invokevirtual", exception.message)
    }

    @Test
    fun `invokevirtual throws guest IncompatibleClassChangeError when selected method is static`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x04.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(
                            internalName = "Sub",
                            superclassName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = true,
                                    code = byteArrayOf(
                                        0x05.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected instance method Sub.value:()I for invokevirtual", exception.message)
    }

    @Test
    fun `invokevirtual throws guest NullPointerException for null objectref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x06.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("Cannot invoke virtual method Owner.value:()I on null object reference", exception.message)
    }

    @Test
    fun `invokevirtual rejects receivers that are not assignable to the resolved method owner`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Other")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x06.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Other"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokevirtual receiver for Owner.value:()I at offset 1: Other is not assignable to Owner",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual throws guest NoClassDefFoundError when method owner class is missing`() {
        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("MissingOwner", "MissingOwner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(emptyList(), strictClassResolution = true),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("MissingOwner", exception.message)
    }

    @Test
    fun `invokevirtual throws guest NoSuchMethodError when method resolution misses`() {
        val exception = assertFailsWith<JvmNoSuchMethodError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(internalName = "Owner"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Owner.missing:()I", exception.message)
    }

    @Test
    fun `invokevirtual throws guest AbstractMethodError when selected method is abstract`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmAbstractMethodError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    isAbstract = true,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/AbstractMethodError", exception.guestClassName)
        assertEquals("Owner.value:()I", exception.message)
    }

    @Test
    fun `invokevirtual abstract selected method transfers control to matching AbstractMethodError handler`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                isAbstract = true,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 4,
                    handlerPc = 4,
                    catchClassName = "java/lang/AbstractMethodError",
                ),
            ),
        )

        val caught = callerLocals.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/AbstractMethodError", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `invokevirtual throws guest UnsatisfiedLinkError for unbound native methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsatisfiedLinkError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "nativeValue",
                                    descriptor = "()I",
                                    isStatic = false,
                                    isNative = true,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/UnsatisfiedLinkError", exception.guestClassName)
        assertEquals("Native method Owner.nativeValue:()I is not linked for invokevirtual", exception.message)
    }

    @Test
    fun `athrow throws a non null guest throwable reference`() {
        val heap = JvmHeap()
        val throwable = heap.allocateObject("java/lang/RuntimeException")
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, throwable)

        val exception = assertFailsWith<JvmThrownException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xBF.toByte(),
                ),
                maxStack = 1,
                heap = heap,
                localVariables = localVariables,
            )
        }

        assertEquals(throwable, exception.throwable)
        assertEquals(
            "Unhandled guest exception java/lang/RuntimeException thrown by athrow at offset 1",
            exception.message,
        )
    }

    @Test
    fun `athrow of null throws guest NullPointerException`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xBF.toByte(),
                ),
                maxStack = 1,
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("athrow of null objectref at offset 1", exception.message)
    }

    @Test
    fun `athrow of null transfers control to matching NullPointerException handler`() {
        val heap = JvmHeap()
        val localVariables = JvmLocalVariables(maxLocals = 1)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x01.toByte(),
                0xBF.toByte(),
                0x4B.toByte(),
                0x06.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchClassName = "java/lang/NullPointerException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NullPointerException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
    }

    @Test
    fun `athrow rejects non reference operand stack values`() {
        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x03.toByte(),
                    0xBF.toByte(),
                ),
                maxStack = 1,
            )
        }

        assertEquals(
            "Invalid athrow objectref at offset 1: expected JvmReferenceValue but was JvmIntValue",
            exception.message,
        )
    }

    @Test
    fun `athrow transfers control to a matching exception handler`() {
        val heap = JvmHeap()
        val throwable = heap.allocateObject("java/lang/RuntimeException")
        val localVariables = JvmLocalVariables(maxLocals = 2)
        localVariables.store(0, throwable)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xBF.toByte(),
                0x4C.toByte(),
                0x05.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchClassName = "java/lang/RuntimeException",
                ),
            ),
        )

        assertEquals(throwable, localVariables.load(1))
        assertEquals(listOf(JvmIntValue(2)), result.operandStack.toList())
    }

    @Test
    fun `caller exception handler catches guest exception thrown by invoked method`() {
        val heap = JvmHeap()
        val throwable = heap.allocateObject("java/lang/RuntimeException")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, throwable)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB8.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x03.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("thrower", "thrower".encodeToByteArray()),
                    ConstantUtf8Entry("(Ljava/lang/Throwable;)V", "(Ljava/lang/Throwable;)V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Throwable", superclassName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "java/lang/RuntimeException",
                        superclassName = "java/lang/Throwable",
                    ),
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "thrower",
                                descriptor = "(Ljava/lang/Throwable;)V",
                                isStatic = true,
                                code = byteArrayOf(
                                    0x2A.toByte(),
                                    0xBF.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 4,
                    handlerPc = 5,
                    catchClassName = "java/lang/RuntimeException",
                ),
            ),
        )

        assertEquals(throwable, callerLocals.load(0))
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `invokevirtual executes bound native intrinsic methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x06.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                    ConstantUtf8Entry("(I)I", "(I)I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeValue",
                                descriptor = "(I)I",
                                isStatic = false,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.from(
                JvmNativeMethodKey(
                    ownerClassName = "Owner",
                    name = "nativeValue",
                    descriptor = "(I)I",
                    isStatic = false,
                ) to JvmNativeMethodIntrinsic { _, invocation ->
                    assertEquals(receiver, invocation.receiver)
                    assertEquals(listOf(JvmIntValue(3)), invocation.arguments)
                    JvmIntValue(7)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(7)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual executes signature polymorphic method handle native call sites`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/invoke/MethodHandle")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x06.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry(
                        "java/lang/invoke/MethodHandle",
                        "java/lang/invoke/MethodHandle".encodeToByteArray(),
                    ),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("invokeExact", "invokeExact".encodeToByteArray()),
                    ConstantUtf8Entry("(I)J", "(I)J".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "java/lang/invoke/MethodHandle",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "invokeExact",
                                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                                isStatic = false,
                                isNative = true,
                                isVarargs = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.from(
                JvmNativeMethodKey(
                    ownerClassName = "java/lang/invoke/MethodHandle",
                    name = "invokeExact",
                    descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                    isStatic = false,
                ) to JvmNativeMethodIntrinsic { _, invocation ->
                    assertEquals(receiver, invocation.receiver)
                    assertEquals(listOf(JvmIntValue(3)), invocation.arguments)
                    JvmLongValue(11L)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmLongValue(11L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual passes int arguments into callee locals after receiver`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("sum", "sum".encodeToByteArray()),
                    ConstantUtf8Entry("(II)I", "(II)I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "sum",
                                descriptor = "(II)I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x1B.toByte(),
                                    0x1C.toByte(),
                                    0x60.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 3,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual executes void instance method without pushing a return value`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x04.toByte(),
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("touch", "touch".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "touch",
                                descriptor = "()V",
                                isStatic = false,
                                code = byteArrayOf(0xB1.toByte()),
                                maxStack = 0,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(1)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual passes and returns category two long values`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x0A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("echo", "echo".encodeToByteArray()),
                    ConstantUtf8Entry("(J)J", "(J)J".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "echo",
                                descriptor = "(J)J",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x1F.toByte(),
                                    0xAD.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 3,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmLongValue(1L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual rejects object arguments that are not assignable to reference descriptors`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val argument = heap.allocateObject("Other")
        val callerLocals = JvmLocalVariables(maxLocals = 2)
        callerLocals.store(0, receiver)
        callerLocals.store(1, argument)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x2B.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("take", "take".encodeToByteArray()),
                        ConstantUtf8Entry("(LExpected;)V", "(LExpected;)V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "take",
                                    descriptor = "(LExpected;)V",
                                    isStatic = false,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 2,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Expected"),
                        JvmClassDefinition(internalName = "Other"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokevirtual argument for Owner.take:(LExpected;)V at offset 2: " +
                "Other is not assignable to Expected",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects object returns that are not assignable to reference descriptors`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("make", "make".encodeToByteArray()),
                        ConstantUtf8Entry("()LExpected;", "()LExpected;".encodeToByteArray()),
                        ConstantUtf8Entry("Other", "Other".encodeToByteArray()),
                        ConstantClassEntry(ConstantPoolIndex(7)),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "make",
                                    descriptor = "()LExpected;",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0xBB.toByte(),
                                        0x00.toByte(),
                                        0x08.toByte(),
                                        0xB0.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Expected"),
                        JvmClassDefinition(internalName = "Other"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokevirtual return for Owner.make:()LExpected; at offset 1: " +
                "Other is not assignable to Expected",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual throws guest IllegalAccessError for private methods from another class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "secret",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isPrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private method Owner.secret:()V", exception.message)
    }

    @Test
    fun `invokevirtual allows private methods from the same class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "secret",
                                descriptor = "()I",
                                isStatic = false,
                                isPrivate = true,
                                code = byteArrayOf(
                                    0x08.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Owner",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual throws guest IllegalAccessError for package private methods from another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("hidden", "hidden".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "hidden",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isPackagePrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class other/Caller cannot access package-private method pkg/Owner.hidden:()V", exception.message)
    }

    @Test
    fun `invokevirtual allows package private methods from the same package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("hidden", "hidden".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "hidden",
                                descriptor = "()I",
                                isStatic = false,
                                isPackagePrivate = true,
                                code = byteArrayOf(
                                    0x07.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual throws guest IllegalAccessError for protected methods from non subclass in another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "guarded",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isProtected = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class other/Caller cannot access protected method pkg/Owner.guarded:()V", exception.message)
    }

    @Test
    fun `invokevirtual allows protected methods from the same package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "guarded",
                                descriptor = "()I",
                                isStatic = false,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x06.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual allows protected superclass methods from subclasses in another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("other/Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB6.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "guarded",
                                descriptor = "()I",
                                isStatic = false,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x05.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "other/Sub",
                        superclassName = "pkg/Owner",
                    ),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(listOf(JvmIntValue(2)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokevirtual throws guest IllegalAccessError for protected superclass methods on non subclass receivers`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB6.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "guarded",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isProtected = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(
                            internalName = "other/Sub",
                            superclassName = "pkg/Owner",
                        ),
                    ),
                ),
                currentClassName = "other/Sub",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Sub cannot access protected method pkg/Owner.guarded:()V on receiver pkg/Owner",
            exception.message,
        )
    }

    @Test
    fun `invokespecial executes no argument int returning instance method`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("value", "value".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x06.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokespecial passes int arguments into callee locals after receiver`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x05.toByte(),
                0x06.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("sum", "sum".encodeToByteArray()),
                    ConstantUtf8Entry("(II)I", "(II)I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "sum",
                                descriptor = "(II)I",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x1B.toByte(),
                                    0x1C.toByte(),
                                    0x60.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 3,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokespecial executes void instance method without pushing a return value`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x07.toByte(),
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("touch", "touch".encodeToByteArray()),
                    ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "touch",
                                descriptor = "()V",
                                isStatic = false,
                                code = byteArrayOf(0xB1.toByte()),
                                maxStack = 0,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokespecial throws guest NullPointerException for null objectref`() {
        val exception = assertFailsWith<JvmNullPointerException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x01.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x06.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NullPointerException", exception.guestClassName)
        assertEquals("Cannot invoke special method Owner.value:()I on null object reference", exception.message)
    }

    @Test
    fun `invokespecial throws guest IncompatibleClassChangeError for static methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = true,
                                    code = byteArrayOf(
                                        0x06.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 0,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Expected instance method Owner.value:()I for invokespecial", exception.message)
    }

    @Test
    fun `invokespecial throws guest UnsatisfiedLinkError for unbound native methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsatisfiedLinkError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("nativeValue", "nativeValue".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "nativeValue",
                                    descriptor = "()I",
                                    isStatic = false,
                                    isNative = true,
                                ),
                            ),
                        ),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/UnsatisfiedLinkError", exception.guestClassName)
        assertEquals("Native method Owner.nativeValue:()I is not linked for invokespecial", exception.message)
    }

    @Test
    fun `invokespecial executes bound native intrinsic methods`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x04.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 2,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("nativeSpecial", "nativeSpecial".encodeToByteArray()),
                    ConstantUtf8Entry("(I)I", "(I)I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "nativeSpecial",
                                descriptor = "(I)I",
                                isStatic = false,
                                isNative = true,
                            ),
                        ),
                    ),
                ),
            ),
            nativeMethods = JvmNativeMethodRegistry.from(
                JvmNativeMethodKey(
                    ownerClassName = "Owner",
                    name = "nativeSpecial",
                    descriptor = "(I)I",
                    isStatic = false,
                ) to JvmNativeMethodIntrinsic { _, invocation ->
                    assertEquals(receiver, invocation.receiver)
                    assertEquals(listOf(JvmIntValue(1)), invocation.arguments)
                    JvmIntValue(6)
                },
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmIntValue(6)), result.operandStack.toList())
        assertEquals(1, result.operandStack.slotDepth)
    }

    @Test
    fun `invokespecial throws guest NoSuchMethodError when method resolution misses`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("missing", "missing".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(internalName = "Owner"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Owner.missing:()I", exception.message)
    }

    @Test
    fun `invokespecial throws guest NoClassDefFoundError when method owner class is missing`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("MissingOwner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("MissingOwner", "MissingOwner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(internalName = "Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("MissingOwner", exception.message)
    }

    @Test
    fun `invokespecial throws guest IllegalAccessError for private methods from another class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "secret",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isPrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "Caller"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class Caller cannot access private method Owner.secret:()V", exception.message)
    }

    @Test
    fun `invokespecial allows private methods from the same class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("secret", "secret".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "secret",
                                descriptor = "()I",
                                isStatic = false,
                                isPrivate = true,
                                code = byteArrayOf(
                                    0x08.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Owner",
        )

        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `invokespecial throws guest IllegalAccessError for package private methods from another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("hidden", "hidden".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "hidden",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isPackagePrivate = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class other/Caller cannot access package-private method pkg/Owner.hidden:()V", exception.message)
    }

    @Test
    fun `invokespecial allows package private methods from the same package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("hidden", "hidden".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "hidden",
                                descriptor = "()I",
                                isStatic = false,
                                isPackagePrivate = true,
                                code = byteArrayOf(
                                    0x07.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(4)), result.operandStack.toList())
    }

    @Test
    fun `invokespecial throws guest IllegalAccessError for protected methods from non subclass in another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "guarded",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isProtected = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/Caller"),
                    ),
                ),
                currentClassName = "other/Caller",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals("Class other/Caller cannot access protected method pkg/Owner.guarded:()V", exception.message)
    }

    @Test
    fun `invokespecial allows protected methods from the same package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "guarded",
                                descriptor = "()I",
                                isStatic = false,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x06.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "pkg/Caller"),
                ),
            ),
            currentClassName = "pkg/Caller",
        )

        assertEquals(listOf(JvmIntValue(3)), result.operandStack.toList())
    }

    @Test
    fun `invokespecial throws guest IllegalAccessError for protected superclass methods on non subclass receivers`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmIllegalAccessError> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                        ConstantUtf8Entry("()V", "()V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "guarded",
                                    descriptor = "()V",
                                    isStatic = false,
                                    isProtected = true,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(
                            internalName = "other/Sub",
                            superclassName = "pkg/Owner",
                        ),
                    ),
                ),
                currentClassName = "other/Sub",
            )
        }

        assertEquals("java/lang/IllegalAccessError", exception.guestClassName)
        assertEquals(
            "Class other/Sub cannot access protected method pkg/Owner.guarded:()V on receiver pkg/Owner",
            exception.message,
        )
    }

    @Test
    fun `invokespecial allows protected superclass methods from subclasses in another package`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("other/Sub")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 1,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("guarded", "guarded".encodeToByteArray()),
                    ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "guarded",
                                descriptor = "()I",
                                isStatic = false,
                                isProtected = true,
                                code = byteArrayOf(
                                    0x05.toByte(),
                                    0xAC.toByte(),
                                ),
                                maxStack = 1,
                                maxLocals = 1,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "other/Sub",
                        superclassName = "pkg/Owner",
                    ),
                ),
            ),
            currentClassName = "other/Sub",
        )

        assertEquals(listOf(JvmIntValue(2)), result.operandStack.toList())
    }

    @Test
    fun `invokespecial rejects object arguments that are not assignable to reference descriptors`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val argument = heap.allocateObject("other/Arg")
        val callerLocals = JvmLocalVariables(maxLocals = 2)
        callerLocals.store(0, receiver)
        callerLocals.store(1, argument)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0x2B.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 2,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("accept", "accept".encodeToByteArray()),
                        ConstantUtf8Entry("(Lpkg/Param;)V", "(Lpkg/Param;)V".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "accept",
                                    descriptor = "(Lpkg/Param;)V",
                                    isStatic = false,
                                    code = byteArrayOf(0xB1.toByte()),
                                    maxStack = 0,
                                    maxLocals = 2,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "pkg/Param"),
                        JvmClassDefinition(internalName = "other/Arg"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokespecial argument for Owner.accept:(Lpkg/Param;)V at offset 2: " +
                "other/Arg is not assignable to pkg/Param",
            exception.message,
        )
    }

    @Test
    fun `invokespecial rejects object returns that are not assignable to reference descriptors`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("self", "self".encodeToByteArray()),
                        ConstantUtf8Entry("()Lpkg/Param;", "()Lpkg/Param;".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "self",
                                    descriptor = "()Lpkg/Param;",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x2A.toByte(),
                                        0xB0.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "pkg/Param"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokespecial return for Owner.self:()Lpkg/Param; at offset 1: " +
                "Owner is not assignable to pkg/Param",
            exception.message,
        )
    }

    @Test
    fun `invokespecial passes and returns category two long values`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Owner")
        val callerLocals = JvmLocalVariables(maxLocals = 3)
        callerLocals.store(0, receiver)
        callerLocals.store(1, JvmLongValue(42L))

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0x1F.toByte(),
                0xB7.toByte(),
                0x00.toByte(),
                0x01.toByte(),
            ),
            maxStack = 3,
            constantPool = ConstantPool.fromEntries(
                listOf(
                    ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                    ConstantClassEntry(ConstantPoolIndex(3)),
                    ConstantUtf8Entry("Owner", "Owner".encodeToByteArray()),
                    ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                    ConstantUtf8Entry("echo", "echo".encodeToByteArray()),
                    ConstantUtf8Entry("(J)J", "(J)J".encodeToByteArray()),
                ),
            ),
            heap = heap,
            localVariables = callerLocals,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Owner",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "echo",
                                descriptor = "(J)J",
                                isStatic = false,
                                code = byteArrayOf(
                                    0x1F.toByte(),
                                    0xAD.toByte(),
                                ),
                                maxStack = 2,
                                maxLocals = 3,
                            ),
                        ),
                    ),
                ),
            ),
            currentClassName = "Caller",
        )

        assertEquals(listOf(JvmLongValue(42L)), result.operandStack.toList())
        assertEquals(2, result.operandStack.slotDepth)
    }

    @Test
    fun `invokespecial rejects receivers that are not assignable to the resolved method owner`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("other/NotOwner")
        val callerLocals = JvmLocalVariables(maxLocals = 1)
        callerLocals.store(0, receiver)

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(
                    0x2A.toByte(),
                    0xB7.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                ),
                maxStack = 1,
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantMethodRefEntry(ConstantPoolIndex(2), ConstantPoolIndex(4)),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/Owner", "pkg/Owner".encodeToByteArray()),
                        ConstantNameAndTypeEntry(ConstantPoolIndex(5), ConstantPoolIndex(6)),
                        ConstantUtf8Entry("value", "value".encodeToByteArray()),
                        ConstantUtf8Entry("()I", "()I".encodeToByteArray()),
                    ),
                ),
                heap = heap,
                localVariables = callerLocals,
                classHierarchy = JvmClassHierarchy(
                    listOf(
                        JvmClassDefinition(
                            internalName = "pkg/Owner",
                            methods = listOf(
                                JvmMethodDefinition(
                                    name = "value",
                                    descriptor = "()I",
                                    isStatic = false,
                                    code = byteArrayOf(
                                        0x04.toByte(),
                                        0xAC.toByte(),
                                    ),
                                    maxStack = 1,
                                    maxLocals = 1,
                                ),
                            ),
                        ),
                        JvmClassDefinition(internalName = "other/NotOwner"),
                    ),
                ),
                currentClassName = "Caller",
            )
        }

        assertEquals(
            "Invalid invokespecial receiver for pkg/Owner.value:()I at offset 1: " +
                "other/NotOwner is not assignable to pkg/Owner",
            exception.message,
        )
    }

    @Test
    fun `monitorenter acquires the object monitor for the current thread`() {
        val heap = JvmHeap()
        val monitor = JvmMonitorState()
        val receiver = heap.allocateObject("pkg/Lock")
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC2.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            monitors = monitor,
            currentThreadId = "worker-1",
        )

        assertEquals(emptyList(), result.operandStack.toList())
        assertEquals(1, monitor.holdCount(receiver, "worker-1"))
    }

    @Test
    fun `monitorenter throws guest NullPointerException for null object references`() {
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, JvmNullValue)
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC2.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchClassName = "java/lang/NullPointerException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NullPointerException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `monitorexit releases the object monitor for the current thread`() {
        val heap = JvmHeap()
        val monitor = JvmMonitorState()
        val receiver = heap.allocateObject("pkg/Lock")
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, receiver)
        monitor.enter(receiver, "worker-1")

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC3.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            monitors = monitor,
            currentThreadId = "worker-1",
        )

        assertEquals(emptyList(), result.operandStack.toList())
        assertEquals(0, monitor.holdCount(receiver, "worker-1"))
    }

    @Test
    fun `monitorexit throws guest NullPointerException for null object references`() {
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, JvmNullValue)
        val heap = JvmHeap()

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC3.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchClassName = "java/lang/NullPointerException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/NullPointerException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `monitorexit throws guest IllegalMonitorStateException when current thread does not own the monitor`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("pkg/Lock")
        val localVariables = JvmLocalVariables(maxLocals = 1)
        localVariables.store(0, receiver)

        val result = JvmInterpreter.execute(
            code = byteArrayOf(
                0x2A.toByte(),
                0xC3.toByte(),
                0x4B.toByte(),
                0x08.toByte(),
            ),
            maxStack = 1,
            heap = heap,
            localVariables = localVariables,
            exceptionHandlers = listOf(
                JvmExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchClassName = "java/lang/IllegalMonitorStateException",
                ),
            ),
        )

        val caught = localVariables.load(0) as JvmObjectReferenceValue
        assertEquals("java/lang/IllegalMonitorStateException", heap.get(caught).className)
        assertEquals(listOf(JvmIntValue(5)), result.operandStack.toList())
    }

    @Test
    fun `unsupported instructions fail explicitly`() {
        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            JvmInterpreter.execute(
                code = byteArrayOf(0xAC.toByte()),
                maxStack = 0,
            )
        }

        assertEquals("Unsupported instruction ireturn (0xac) at offset 0", exception.message)
    }
}
