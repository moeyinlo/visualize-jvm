package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntReturnInstructionVerifierTest {
    @Test
    fun `ireturn accepts an int return value for int methods`() {
        IntReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 172,
                locals = listOf(VerificationType.Integer),
                stack = listOf(VerificationType.Float, VerificationType.Integer),
            ),
            declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
            maxStack = 2,
        )
    }

    @Test
    fun `ireturn rejects a non int method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 172,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Long),
                maxStack = 1,
            )
        }

        assertEquals(
            "Method return type is Long, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `ireturn rejects a non int return value`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 172,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `ireturn rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 172, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `ireturn rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            IntReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 172,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
