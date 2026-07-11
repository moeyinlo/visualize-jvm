package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ReferenceReturnInstructionVerifierTest {
    private val returnType = VerificationType.ObjectType(ConstantPoolIndex(11))
    private val otherType = VerificationType.ObjectType(ConstantPoolIndex(12))

    @Test
    fun `areturn accepts an assignable object return value for reference methods`() {
        ReferenceReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 176,
                locals = listOf(returnType),
                stack = listOf(VerificationType.Integer, returnType),
            ),
            declaredReturnType = VerificationReturnType.Value(returnType),
            maxStack = 2,
        )
    }

    @Test
    fun `areturn accepts null for reference methods`() {
        ReferenceReturnInstructionVerifier.verify(
            frame = VerificationFrameState(
                bytecodeOffset = 176,
                locals = emptyList(),
                stack = listOf(VerificationType.Null),
            ),
            declaredReturnType = VerificationReturnType.Value(returnType),
            maxStack = 1,
        )
    }

    @Test
    fun `areturn rejects a primitive method return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 176,
                    locals = emptyList(),
                    stack = listOf(returnType),
                ),
                declaredReturnType = VerificationReturnType.Value(VerificationType.Integer),
                maxStack = 1,
            )
        }

        assertEquals(
            "Method return type is Integer, expected Reference",
            exception.message,
        )
    }

    @Test
    fun `areturn rejects a return value not assignable to the declared reference return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 176,
                    locals = emptyList(),
                    stack = listOf(otherType),
                ),
                declaredReturnType = VerificationReturnType.Value(returnType),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains ObjectType(constantPoolIndex=#12), expected ObjectType(constantPoolIndex=#11)",
            exception.message,
        )
    }

    @Test
    fun `areturn rejects an empty operand stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceReturnInstructionVerifier.verify(
                frame = VerificationFrameState(bytecodeOffset = 176, locals = emptyList(), stack = emptyList()),
                declaredReturnType = VerificationReturnType.Value(returnType),
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected ObjectType(constantPoolIndex=#11)",
            exception.message,
        )
    }

    @Test
    fun `areturn rejects an incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ReferenceReturnInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 176,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Long, returnType),
                ),
                declaredReturnType = VerificationReturnType.Value(returnType),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
