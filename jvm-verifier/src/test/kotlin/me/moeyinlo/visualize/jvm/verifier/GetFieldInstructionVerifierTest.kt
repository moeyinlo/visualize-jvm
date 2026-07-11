package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetFieldInstructionVerifierTest {
    @Test
    fun `getfield replaces an object receiver with the declared field type`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")
        val frame = frame(stack = listOf(VerificationType.Float, ownerType))

        val nextFrame = GetFieldInstructionVerifier.verify(
            frame = frame,
            fieldOwnerType = ownerType,
            fieldType = VerificationType.Long,
            maxStack = 3,
        )

        assertEquals(
            frame(stack = listOf(VerificationType.Float, VerificationType.Long)),
            nextFrame,
        )
    }

    @Test
    fun `getfield accepts a null receiver for verifier type transition`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")
        val frame = frame(stack = listOf(VerificationType.Null))

        val nextFrame = GetFieldInstructionVerifier.verify(
            frame = frame,
            fieldOwnerType = ownerType,
            fieldType = VerificationType.Integer,
            maxStack = 1,
        )

        assertEquals(frame(stack = listOf(VerificationType.Integer)), nextFrame)
    }

    @Test
    fun `getfield rejects a non assignable receiver`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            GetFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Float,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected ClassType(internalName=pkg/Owner, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `getfield rejects an empty operand stack`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            GetFieldInstructionVerifier.verify(
                frame = frame(stack = emptyList()),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Integer,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected ClassType(internalName=pkg/Owner, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `getfield rejects operand stack overflow from the field type`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            GetFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, ownerType)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Double,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `getfield rejects an incoming stack exceeding max stack`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            GetFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, ownerType)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Integer,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 180, locals = emptyList(), stack = stack)
}
