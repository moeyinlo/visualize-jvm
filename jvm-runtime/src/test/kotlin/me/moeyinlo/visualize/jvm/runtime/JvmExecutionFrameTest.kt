package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmExecutionFrameTest {
    @Test
    fun `execution frame creates local variables operand stack runtime constant pool and initial pc`() {
        val method = resolvedMethod(maxLocals = 2, maxStack = 2)
        val constantPool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(JvmRuntimeLiteralConstant(JvmIntValue(5))),
        )

        val frame = JvmExecutionFrame.create(method = method, runtimeConstantPool = constantPool)
        frame.localVariables.store(0, JvmIntValue(7))
        frame.operandStack.push(JvmIntValue(9))

        assertEquals(method, frame.method)
        assertSame(constantPool, frame.runtimeConstantPool)
        assertEquals(JvmProgramCounter.BytecodeOffset(0), frame.programCounter)
        assertEquals(JvmIntValue(7), frame.localVariables.load(0))
        assertEquals(JvmIntValue(9), frame.operandStack.peek())
        assertEquals(JvmRuntimeLiteralConstant(JvmIntValue(5)), frame.runtimeConstantPool[JvmRuntimeConstantPoolIndex(1)])
    }

    @Test
    fun `execution frame moves bytecode pc to non negative instruction offsets`() {
        val frame = JvmExecutionFrame.create(
            method = resolvedMethod(),
            runtimeConstantPool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList()),
        )

        frame.moveToBytecodeOffset(12)

        assertEquals(JvmProgramCounter.BytecodeOffset(12), frame.programCounter)
        assertFailsWith<IllegalArgumentException> { frame.moveToBytecodeOffset(-1) }
    }

    @Test
    fun `execution frame derives local and operand capacities from the resolved method`() {
        val frame = JvmExecutionFrame.create(
            method = resolvedMethod(maxLocals = 1, maxStack = 1),
            runtimeConstantPool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList()),
        )

        assertFailsWith<JvmLocalVariablesIndexException> { frame.localVariables.store(1, JvmIntValue(1)) }
        frame.operandStack.push(JvmIntValue(1))
        assertFailsWith<JvmOperandStackOverflowException> { frame.operandStack.push(JvmIntValue(2)) }
    }

    @Test
    fun `execution frame completes normally through the resolved method descriptor`() {
        val method = resolvedMethod(descriptor = "()I")
        val frame = JvmExecutionFrame.create(
            method = method,
            runtimeConstantPool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList()),
        )

        assertEquals(
            JvmMethodCompletion.Normal(method = method, returnValue = JvmIntValue(42)),
            frame.completeNormally(JvmIntValue(42)),
        )
    }

    @Test
    fun `execution frame completes abruptly with a guest throwable reference`() {
        val method = resolvedMethod()
        val frame = JvmExecutionFrame.create(
            method = method,
            runtimeConstantPool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList()),
        )
        val throwable = JvmObjectReferenceValue(JvmReferenceId(11))

        assertEquals(
            JvmMethodCompletion.Abrupt(method = method, throwable = throwable),
            frame.completeAbruptly(throwable),
        )
    }

    @Test
    fun `native execution frame has undefined pc and rejects bytecode pc moves`() {
        val frame = JvmExecutionFrame.create(
            method = resolvedMethod(isNative = true),
            runtimeConstantPool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList()),
        )

        val exception = assertFailsWith<JvmFrameStateException> {
            frame.moveToBytecodeOffset(1)
        }

        assertSame(JvmProgramCounter.UndefinedForNativeMethod, frame.programCounter)
        assertEquals("Native method Example.run()V has an undefined pc register", exception.message)
    }

    private fun resolvedMethod(
        descriptor: String = "()V",
        maxLocals: Int = 4,
        maxStack: Int = 4,
        isNative: Boolean = false,
    ): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "Example",
        name = "run",
        descriptor = descriptor,
        isStatic = true,
        isNative = isNative,
        code = if (isNative) null else byteArrayOf(0xB1.toByte()),
        maxLocals = maxLocals,
        maxStack = maxStack,
    )
}
