package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatReturnInstructionVerifierTest {
    @Test
    fun `freturn accepts a float return value for float methods`() {
        FloatReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 174,
                locals = listOf(VerificationType.Float),
                stack = listOf(VerificationType.Integer, VerificationType.Float),
            ),
            declaredReturnType = VerificationReturnType.Value(VerificationType.Float),
            maxStack = 2,
        )
    }

    @Test
    fun `freturn rejects a non float method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 174,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
                maxStack = 1,
            )
        }

        assertEquals(
            "Method return type is Long, expected Float",
            exception.message,
        )
    }

    @Test
    fun `freturn rejects a non float return value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 174,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Float),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Float",
            exception.message,
        )
    }

    @Test
    fun `freturn rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 174, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Float),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Float",
            exception.message,
        )
    }

    @Test
    fun `freturn rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            FloatReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 174,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Double, VerificationType.Float),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Float),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
