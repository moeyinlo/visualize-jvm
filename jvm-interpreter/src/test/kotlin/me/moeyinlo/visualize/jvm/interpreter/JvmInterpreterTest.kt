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
            maxStack = 2,
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
                code = byteArrayOf(0x60.toByte()),
                maxStack = 1,
            )
        }

        assertEquals("Unsupported instruction iadd (0x60) at offset 0", exception.message)
    }
}
