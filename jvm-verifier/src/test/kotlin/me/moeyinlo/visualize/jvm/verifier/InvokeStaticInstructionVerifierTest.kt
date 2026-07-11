package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeStaticInstructionVerifierTest {
    @Test
    fun `invokestatic pops descriptor arguments and pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 184,
            locals = listOf(VerificationType.Integer),
            stack = listOf(
                VerificationType.Float,
                VerificationType.ClassType("java/lang/String"),
                VerificationType.Long,
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeStaticInstructionVerifier.verify(
            frame = frame,
            methodName = "mix",
            descriptor = "(Ljava/lang/String;JI)D",
            maxStack = 5,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `invokestatic with void return only pops descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 184,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, VerificationType.Integer),
        )

        val nextFrame = InvokeStaticInstructionVerifier.verify(
            frame = frame,
            methodName = "drop",
            descriptor = "(I)V",
            maxStack = 2,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float)),
            nextFrame,
        )
    }

    @Test
    fun `invokestatic rejects a mismatched descriptor argument`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeStaticInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 184,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("java/lang/String"),
                        VerificationType.Float,
                    ),
                ),
                methodName = "mix",
                descriptor = "(Ljava/lang/String;I)D",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `invokestatic rejects max stack overflow caused by return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeStaticInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 184,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                methodName = "wide",
                descriptor = "()J",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `invokestatic rejects instance initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeStaticInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 184,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                methodName = "<init>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals(
            "invokestatic target method must not be <init>",
            exception.message,
        )
    }

    @Test
    fun `invokestatic rejects class initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeStaticInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 184,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                methodName = "<clinit>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals(
            "invokestatic target method must not be <clinit>",
            exception.message,
        )
    }
}
