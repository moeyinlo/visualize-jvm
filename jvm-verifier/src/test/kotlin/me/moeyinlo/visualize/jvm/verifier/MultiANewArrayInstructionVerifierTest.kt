package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MultiANewArrayInstructionVerifierTest {
    @Test
    fun `multianewarray replaces int counts with referenced array type`() {
        val arrayType = VerificationType.ArrayOf(
            VerificationType.ArrayOf(VerificationType.ClassType("pkg/Item")),
        )
        val frame = frame(
            stack = listOf(
                VerificationType.Float,
                VerificationType.Integer,
                VerificationType.Integer,
            ),
        )

        val nextFrame = MultiANewArrayInstructionVerifier.verify(
            frame = frame,
            arrayType = arrayType,
            dimensions = 2,
            maxStack = 3,
        )

        assertEquals(
            frame(stack = listOf(VerificationType.Float, arrayType)),
            nextFrame,
        )
    }

    @Test
    fun `multianewarray preserves full array type when creating fewer dimensions`() {
        val arrayType = VerificationType.ArrayOf(
            VerificationType.ArrayOf(
                VerificationType.ArrayOf(VerificationType.Integer),
            ),
        )

        val nextFrame = MultiANewArrayInstructionVerifier.verify(
            frame = frame(stack = listOf(VerificationType.Integer)),
            arrayType = arrayType,
            dimensions = 1,
            maxStack = 1,
        )

        assertEquals(
            frame(stack = listOf(arrayType)),
            nextFrame,
        )
    }

    @Test
    fun `multianewarray rejects zero dimensions`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MultiANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                arrayType = VerificationType.ArrayOf(VerificationType.Integer),
                dimensions = 0,
                maxStack = 1,
            )
        }

        assertEquals(
            "multianewarray dimensions 0 must be at least 1",
            exception.message,
        )
    }

    @Test
    fun `multianewarray rejects non array target type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MultiANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                arrayType = VerificationType.ClassType("pkg/Item"),
                dimensions = 1,
                maxStack = 1,
            )
        }

        assertEquals(
            "multianewarray target ClassType(internalName=pkg/Item, loader=bootstrap) is not an array type",
            exception.message,
        )
    }

    @Test
    fun `multianewarray rejects dimensions greater than target array dimensionality`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MultiANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Integer)),
                arrayType = VerificationType.ArrayOf(VerificationType.Integer),
                dimensions = 2,
                maxStack = 2,
            )
        }

        assertEquals(
            "multianewarray dimensions 2 exceed target array dimensionality 1",
            exception.message,
        )
    }

    @Test
    fun `multianewarray rejects non int count`() {
        val exception = assertFailsWith<MethodVerificationException> {
            MultiANewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                arrayType = VerificationType.ArrayOf(VerificationType.ArrayOf(VerificationType.Integer)),
                dimensions = 2,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 197, locals = emptyList(), stack = stack)
}
