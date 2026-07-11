package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeSpecialInstructionVerifierTest {
    @Test
    fun `invokespecial non-initializer pops current receiver and descriptor arguments then pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 183,
            locals = listOf(VerificationType.ClassType("pkg/Sub")),
            stack = listOf(
                VerificationType.Float,
                VerificationType.ClassType("pkg/Sub"),
                VerificationType.ClassType("java/lang/String"),
                VerificationType.Long,
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "(Ljava/lang/String;JI)D",
            maxStack = 6,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `invokespecial non-initializer with void return only pops receiver and descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 183,
            locals = listOf(VerificationType.ClassType("pkg/Sub")),
            stack = listOf(
                VerificationType.ClassType("pkg/Sub"),
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "(I)V",
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokespecial non-initializer rejects receiver not assignable to current class`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = listOf(VerificationType.ClassType("other/Helper")),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains ClassType(internalName=other/Helper, loader=bootstrap), " +
                "expected ClassType(internalName=pkg/Sub, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `invokespecial non-initializer rejects descriptor argument mismatch before receiver pop`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("pkg/Sub"),
                        VerificationType.Float,
                    ),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "(I)V",
                maxStack = 2,
            )
        }

        assertEquals("Operand stack top contains Float, expected Integer", exception.message)
    }

    @Test
    fun `invokespecial non-initializer rejects instance initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "<init>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokespecial non-initializer target method must not be <init>", exception.message)
    }

    @Test
    fun `invokespecial non-initializer rejects class initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "<clinit>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokespecial non-initializer target method must not be <clinit>", exception.message)
    }
}
