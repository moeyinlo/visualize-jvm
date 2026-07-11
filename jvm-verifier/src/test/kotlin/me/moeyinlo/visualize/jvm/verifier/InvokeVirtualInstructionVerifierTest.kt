package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeVirtualInstructionVerifierTest {
    private val targetType = VerificationType.ClassType("example/Target")

    @Test
    fun `invokevirtual pops receiver and descriptor arguments then pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 182,
            locals = emptyList(),
            stack = listOf(
                VerificationType.Float,
                targetType,
                VerificationType.Integer,
                VerificationType.Long,
            ),
        )

        val nextFrame = InvokeVirtualInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = targetType,
            descriptor = "(IJ)Ljava/lang/String;",
            maxStack = 5,
        )

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Float,
                    VerificationType.ClassType("java/lang/String"),
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `invokevirtual with void return only pops receiver and descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 182,
            locals = emptyList(),
            stack = listOf(targetType),
        )

        val nextFrame = InvokeVirtualInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = targetType,
            descriptor = "()V",
            maxStack = 1,
        )

        assertEquals(
            frame.copy(stack = emptyList()),
            nextFrame,
        )
    }

    @Test
    fun `invokevirtual rejects a mismatched descriptor argument`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(targetType, VerificationType.Float),
                ),
                methodOwnerType = targetType,
                descriptor = "(I)V",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects a mismatched receiver`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                methodOwnerType = targetType,
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected $targetType",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects max stack overflow caused by return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, targetType),
                ),
                methodOwnerType = targetType,
                descriptor = "()J",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
