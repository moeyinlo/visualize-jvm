package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class AthrowInstructionVerifierTest {
    private val throwableType = VerificationType.ObjectType(ConstantPoolIndex(11))
    private val otherReferenceType = VerificationType.ObjectType(ConstantPoolIndex(12))

    @Test
    fun `athrow accepts a Throwable object reference`() {
        AthrowInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 191,
                locals = listOf(VerificationType.Integer),
                stack = listOf(VerificationType.Float, throwableType),
            ),
            throwableType = throwableType,
            maxStack = 2,
        )
    }

    @Test
    fun `athrow accepts null because it is assignable to Throwable`() {
        AthrowInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 191,
                locals = emptyList(),
                stack = listOf(VerificationType.Null),
            ),
            throwableType = throwableType,
            maxStack = 1,
        )
    }

    @Test
    fun `athrow rejects a non Throwable reference`() {
        val exception = assertFailsWith<MethodVerificationException> {
            AthrowInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 191,
                    locals = emptyList(),
                    stack = listOf(otherReferenceType),
                ),
                throwableType = throwableType,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains ObjectType(constantPoolIndex=#12), expected ObjectType(constantPoolIndex=#11)",
            exception.message,
        )
    }

    @Test
    fun `athrow rejects a primitive operand stack top`() {
        val exception = assertFailsWith<MethodVerificationException> {
            AthrowInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 191,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer),
                ),
                throwableType = throwableType,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected ObjectType(constantPoolIndex=#11)",
            exception.message,
        )
    }

    @Test
    fun `athrow rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            AthrowInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 191, locals = emptyList(), stack = emptyList()),
                throwableType = throwableType,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected ObjectType(constantPoolIndex=#11)",
            exception.message,
        )
    }

    @Test
    fun `athrow rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            AthrowInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 191,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, throwableType),
                ),
                throwableType = throwableType,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
