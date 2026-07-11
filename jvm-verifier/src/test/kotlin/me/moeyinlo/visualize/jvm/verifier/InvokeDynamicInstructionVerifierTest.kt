package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeDynamicInstructionVerifierTest {
    @Test
    fun `invokedynamic pops descriptor arguments and pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 186,
            locals = emptyList(),
            stack = listOf(
                VerificationType.Float,
                VerificationType.ClassType("java/lang/String"),
                VerificationType.Long,
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeDynamicInstructionVerifier.verify(
            frame = frame,
            callSiteName = "bootstrapCall",
            descriptor = "(Ljava/lang/String;JI)D",
            maxStack = 5,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `invokedynamic with void return only pops descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 186,
            locals = emptyList(),
            stack = listOf(VerificationType.Float, VerificationType.Integer),
        )

        val nextFrame = InvokeDynamicInstructionVerifier.verify(
            frame = frame,
            callSiteName = "drop",
            descriptor = "(I)V",
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `invokedynamic rejects descriptor argument mismatch`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeDynamicInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 186,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("java/lang/String"),
                        VerificationType.Float,
                    ),
                ),
                callSiteName = "bootstrapCall",
                descriptor = "(Ljava/lang/String;I)D",
                maxStack = 2,
            )
        }

        assertEquals("Operand stack top contains Float, expected Integer", exception.message)
    }

    @Test
    fun `invokedynamic rejects max stack overflow caused by return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeDynamicInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 186,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                callSiteName = "wide",
                descriptor = "()J",
                maxStack = 2,
            )
        }

        assertEquals("Operand stack depth 3 exceeds max_stack=2", exception.message)
    }

    @Test
    fun `invokedynamic rejects instance initialization call site names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeDynamicInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 186,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                callSiteName = "<init>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokedynamic call site name must not be <init>", exception.message)
    }

    @Test
    fun `invokedynamic rejects class initialization call site names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeDynamicInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 186,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                callSiteName = "<clinit>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokedynamic call site name must not be <clinit>", exception.message)
    }
}
