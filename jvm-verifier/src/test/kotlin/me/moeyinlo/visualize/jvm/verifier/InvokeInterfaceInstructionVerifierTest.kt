package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeInterfaceInstructionVerifierTest {
    @Test
    fun `invokeinterface pops receiver and descriptor arguments then pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 185,
            locals = emptyList(),
            stack = listOf(
                VerificationType.Float,
                VerificationType.ClassType("pkg/Impl"),
                VerificationType.ClassType("java/lang/String"),
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeInterfaceInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = VerificationType.Reference,
            methodName = "m",
            descriptor = "(Ljava/lang/String;I)J",
            count = 3,
            maxStack = 4,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `invokeinterface with void return only pops receiver and descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 185,
            locals = emptyList(),
            stack = listOf(
                VerificationType.ClassType("pkg/Impl"),
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeInterfaceInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = VerificationType.Reference,
            methodName = "m",
            descriptor = "(I)V",
            count = 2,
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokeinterface rejects invalid count after popping receiver and descriptor arguments`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeInterfaceInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 185,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("pkg/Impl"),
                        VerificationType.Integer,
                    ),
                ),
                methodOwnerType = VerificationType.Reference,
                methodName = "m",
                descriptor = "(I)V",
                count = 1,
                maxStack = 2,
            )
        }

        assertEquals("invokeinterface count operand 1 does not match popped operand count 2", exception.message)
    }

    @Test
    fun `invokeinterface count includes category two parameter slots`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 185,
            locals = emptyList(),
            stack = listOf(
                VerificationType.ClassType("pkg/Impl"),
                VerificationType.Long,
            ),
        )

        val nextFrame = InvokeInterfaceInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = VerificationType.Reference,
            methodName = "m",
            descriptor = "(J)V",
            count = 3,
            maxStack = 3,
        )

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokeinterface rejects descriptor argument mismatch before count validation`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeInterfaceInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 185,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("pkg/Impl"),
                        VerificationType.Float,
                    ),
                ),
                methodOwnerType = VerificationType.Reference,
                methodName = "m",
                descriptor = "(I)V",
                count = 2,
                maxStack = 2,
            )
        }

        assertEquals("Operand stack top contains Float, expected Integer", exception.message)
    }

    @Test
    fun `invokeinterface rejects instance initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeInterfaceInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 185,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                methodOwnerType = VerificationType.Reference,
                methodName = "<init>",
                descriptor = "()V",
                count = 1,
                maxStack = 0,
            )
        }

        assertEquals("invokeinterface target method must not be <init>", exception.message)
    }

    @Test
    fun `invokeinterface rejects class initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeInterfaceInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 185,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                methodOwnerType = VerificationType.Reference,
                methodName = "<clinit>",
                descriptor = "()V",
                count = 1,
                maxStack = 0,
            )
        }

        assertEquals("invokeinterface target method must not be <clinit>", exception.message)
    }
}
