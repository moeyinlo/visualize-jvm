package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongReturnInstructionVerifierTest {
    @Test
    fun `lreturn accepts a long return value for long methods`() {
        LongReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 173,
                locals = listOf(VerificationType.Long),
                stack = listOf(VerificationType.Integer, VerificationType.Long),
            ),
            declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
            maxStack = 3,
        )
    }

    @Test
    fun `lreturn rejects a non long method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 173,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 2,
            )
        }

        assertEquals(
            "Method return type is Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lreturn rejects a non long return value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 173,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lreturn rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 173, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Long",
            exception.message,
        )
    }

    @Test
    fun `lreturn rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            LongReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 173,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Long),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }
}
