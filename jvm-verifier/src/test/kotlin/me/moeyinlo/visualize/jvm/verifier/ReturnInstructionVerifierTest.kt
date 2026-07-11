package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReturnInstructionVerifierTest {
    @Test
    fun `return accepts void methods and discards any current operand stack values`() {
        ReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 177,
                locals = listOf(VerificationType.Integer),
                stack = listOf(VerificationType.Float, VerificationType.Integer),
            ),
            declaredReturnType = VerificationReturnType.Void,
            maxStack = 2,
        )
    }

    @Test
    fun `return rejects a non void method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 177, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 0,
            )
        }

        assertEquals(
            "Method return type is Integer, expected void",
            exception.message,
        )
    }

    @Test
    fun `return rejects a constructor before uninitializedThis is initialized`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 177,
                    locals = listOf(VerificationType.UninitializedThis),
                    stack = emptyList(),
                ),
                declaredReturnType = VerificationReturnType.Void,
                maxStack = 0,
            )
        }

        assertEquals(
            "Cannot return from constructor while uninitializedThis is still present at bytecode offset 177",
            exception.message,
        )
    }

    @Test
    fun `return rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 177,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, VerificationType.Integer),
                ),
                declaredReturnType = VerificationReturnType.Void,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
