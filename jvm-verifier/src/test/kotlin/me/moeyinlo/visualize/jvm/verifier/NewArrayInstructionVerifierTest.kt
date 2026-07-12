package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NewArrayInstructionVerifierTest {
    @Test
    fun `newarray replaces int count with primitive array reference`() {
        listOf(
            4 to VerificationType.Boolean,
            5 to VerificationType.Char,
            6 to VerificationType.Float,
            7 to VerificationType.Double,
            8 to VerificationType.Byte,
            9 to VerificationType.Short,
            10 to VerificationType.Integer,
            11 to VerificationType.Long,
        ).forEach { (atype, componentType) ->
            val frame = frame(
                stack = listOf(VerificationType.Float, VerificationType.Integer),
            )

            val nextFrame = NewArrayInstructionVerifier.verify(
                frame = frame,
                atype = atype,
                maxStack = 2,
            )

            assertEquals(
                frame(stack = listOf(VerificationType.Float, VerificationType.ArrayOf(componentType))),
                nextFrame,
            )
        }
    }

    @Test
    fun `newarray rejects non int count`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Float)),
                atype = 10,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `newarray rejects invalid primitive array type code`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                atype = 3,
                maxStack = 1,
            )
        }

        assertEquals(
            "newarray atype 3 is not a valid primitive array type code",
            exception.message,
        )
    }

    @Test
    fun `newarray rejects incoming stack exceeding max stack`() {
        val exception = assertFailsWith<MethodVerificationException> {
            NewArrayInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Long, VerificationType.Integer)),
                atype = 10,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 188, locals = emptyList(), stack = stack)
}
