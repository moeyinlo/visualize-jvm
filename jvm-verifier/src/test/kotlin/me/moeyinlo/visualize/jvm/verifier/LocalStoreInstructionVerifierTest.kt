package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class LocalStoreInstructionVerifierTest {
    private val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))

    @Test
    fun `istore pops an int from the operand stack into a local`() {
        val frame = frame(locals = listOf(VerificationType.Top), stack = listOf(VerificationType.Float, VerificationType.Integer))

        val nextFrame = LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalStoreKind.Int,
            maxLocals = 1,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(locals = listOf(VerificationType.Integer), stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `astore stores the actual reference subtype popped from the operand stack`() {
        val frame = frame(locals = listOf(VerificationType.Top), stack = listOf(objectType))

        val nextFrame = LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalStoreKind.Reference,
            maxLocals = 1,
            maxStack = 1,
        )

        assertEquals(frame.copy(locals = listOf(objectType), stack = emptyList()), nextFrame)
    }

    @Test
    fun `astore stores a returnAddress popped from the operand stack`() {
        val frame = frame(locals = listOf(VerificationType.Top), stack = listOf(VerificationType.ReturnAddress))

        val nextFrame = LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 0,
            kind = LocalStoreKind.Reference,
            maxLocals = 1,
            maxStack = 1,
        )

        assertEquals(frame.copy(locals = listOf(VerificationType.ReturnAddress), stack = emptyList()), nextFrame)
    }

    @Test
    fun `dstore writes a category two value with a trailing top local slot`() {
        val frame = frame(locals = emptyList(), stack = listOf(VerificationType.Double))

        val nextFrame = LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 1,
            kind = LocalStoreKind.Double,
            maxLocals = 3,
            maxStack = 2,
        )

        assertEquals(
            frame.copy(
                locals = listOf(VerificationType.Top, VerificationType.Double, VerificationType.Top),
                stack = emptyList(),
            ),
            nextFrame,
        )
    }

    @Test
    fun `storing into the second slot of an old category two local invalidates the pair`() {
        val frame = frame(locals = listOf(VerificationType.Long), stack = listOf(VerificationType.Integer))

        val nextFrame = LocalStoreInstructionVerifier.verify(
            frame = frame,
            index = 1,
            kind = LocalStoreKind.Int,
            maxLocals = 3,
            maxStack = 1,
        )

        assertEquals(
            frame.copy(
                locals = listOf(VerificationType.Top, VerificationType.Integer, VerificationType.Top),
                stack = emptyList(),
            ),
            nextFrame,
        )
    }

    @Test
    fun `fstore rejects operand stack tops that are not assignable to float`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalStoreInstructionVerifier.verify(
                frame = frame(locals = listOf(VerificationType.Top), stack = listOf(VerificationType.Integer)),
                index = 0,
                kind = LocalStoreKind.Float,
                maxLocals = 1,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `store rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalStoreInstructionVerifier.verify(
                frame = frame(locals = emptyList(), stack = emptyList()),
                index = 0,
                kind = LocalStoreKind.Long,
                maxLocals = 2,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lstore rejects category two stores that exceed max locals`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LocalStoreInstructionVerifier.verify(
                frame = frame(locals = emptyList(), stack = listOf(VerificationType.Long)),
                index = 1,
                kind = LocalStoreKind.Long,
                maxLocals = 2,
                maxStack = 2,
            )
        }

        assertEquals(
            "Local variable index 1 with width 2 exceeds max_locals=2",
            exception.message,
        )
    }

    private fun frame(
        locals: List<VerificationType>,
        stack: List<VerificationType>,
    ): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 15, locals = locals, stack = stack)
}
