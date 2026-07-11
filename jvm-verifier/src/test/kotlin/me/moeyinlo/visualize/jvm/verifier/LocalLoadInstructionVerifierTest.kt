package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class LocalLoadInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))

    @Test
    fun `iload pushes an int local onto the operand stack`() {
        val frame = frame(locals = listOf(VerificationType.Integer), stack = listOf(VerificationType.Float))

        val nextFrame = LocalLoadInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalLoadKind.Int,
            maxLocals = 1,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Integer)),
            nextFrame,
        )
    }

    @Test
    fun `aload pushes the actual reference subtype from a local`() {
        val frame = frame(locals = listOf(objectType), stack = emptyList())

        val nextFrame = LocalLoadInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalLoadKind.Reference,
            maxLocals = 1,
            maxStack = 1,
        )

        assertEquals(frame.copy(stack = listOf(objectType)), nextFrame)
    }

    @Test
    fun `dload accounts for category two operand stack depth`() {
        val frame = frame(locals = listOf(VerificationType.Double), stack = listOf(VerificationType.Integer))

        val nextFrame = LocalLoadInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalLoadKind.Double,
            maxLocals = 2,
            maxStack = 3,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Integer, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `lload rejects the second slot of a category two local`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalLoadInstructionVerifier.verify(
                frame = frame(locals = listOf(VerificationType.Long), stack = emptyList()),
                index = 1,
                kind = LocalLoadKind.Long,
                maxLocals = 3,
                maxStack = 2,
            )
        }

        assertEquals(
            "Local variable 1 contains Top, expected Long",
            exception.message,
        )
    }

    @Test
    fun `fload rejects locals that are not assignable to float`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalLoadInstructionVerifier.verify(
                frame = frame(locals = listOf(VerificationType.Integer), stack = emptyList()),
                index = 0,
                kind = LocalLoadKind.Float,
                maxLocals = 1,
                maxStack = 1,
            )
        }

        assertEquals(
            "Local variable 0 contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `load rejects operand stack overflow`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalLoadInstructionVerifier.verify(
                frame = frame(locals = listOf(VerificationType.Double), stack = listOf(VerificationType.Integer)),
                index = 0,
                kind = LocalLoadKind.Double,
                maxLocals = 2,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(
        locals: List<VerificationType>,
        stack: List<VerificationType>,
    ): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 12, locals = locals, stack = stack)
}
