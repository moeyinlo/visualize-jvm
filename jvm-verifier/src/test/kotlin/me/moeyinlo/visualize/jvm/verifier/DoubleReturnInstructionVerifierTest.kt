package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DoubleReturnInstructionVerifierTest {
    @Test
    fun `dreturn accepts a double return value for double methods`() {
        DoubleReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 175,
                locals = listOf(VerificationType.Double),
                stack = listOf(VerificationType.Integer, VerificationType.Double),
            ),
            declaredReturnType = VerificationReturnType.Value(VerificationType.Double),
            maxStack = 3,
        )
    }

    @Test
    fun `dreturn rejects a non double method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 175,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Float),
                maxStack = 2,
            )
        }

        assertEquals(
            "Method return type is Float, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dreturn rejects a non double return value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 175,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Double),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Long, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dreturn rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 175, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Double),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Double",
            exception.message,
        )
    }

    @Test
    fun `dreturn rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            DoubleReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 175,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Double),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Double),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }
}
