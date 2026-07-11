package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class VerifierOperandStackTest {
    @Test
    fun `counts category two values toward operand stack depth`() {
        val stack = VerifierOperandStack.fromFrame(
            stack = listOf(VerificationType.Integer, VerificationType.Double),
            maxStack = 3,
        )

        assertEquals(3, stack.depth)
        assertEquals(listOf(VerificationType.Integer, VerificationType.Double), stack.values)
    }

    @Test
    fun `push rejects max stack overflow`() {
        val stack = VerifierOperandStack.empty(maxStack = 1)

        val exception = assertFailsWith<MethodVerificationException> {
            stack.push(VerificationType.Long)
        }

        assertEquals(
            "Operand stack depth 2 exceeds max_stack=1",
            exception.message,
        )
    }

    @Test
    fun `pop returns top value and remaining stack`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val stack = VerifierOperandStack.fromFrame(
            stack = listOf(VerificationType.Integer, objectType),
            maxStack = 2,
        )

        val result = stack.pop(expected = VerificationType.Reference)

        assertEquals(objectType, result.value)
        assertEquals(listOf(VerificationType.Integer), result.stack.values)
        assertEquals(1, result.stack.depth)
    }

    @Test
    fun `pop rejects wrong top type`() {
        val stack = VerifierOperandStack.fromFrame(
            stack = listOf(VerificationType.Float),
            maxStack = 1,
        )

        val exception = assertFailsWith<MethodVerificationException> {
            stack.pop(expected = VerificationType.Integer)
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `pop rejects empty stacks`() {
        val stack = VerifierOperandStack.empty(maxStack = 1)

        val exception = assertFailsWith<MethodVerificationException> {
            stack.pop(expected = VerificationType.Integer)
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `from frame rejects stack states that exceed max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            VerifierOperandStack.fromFrame(
                stack = listOf(VerificationType.Null, VerificationType.Double),
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }
}
