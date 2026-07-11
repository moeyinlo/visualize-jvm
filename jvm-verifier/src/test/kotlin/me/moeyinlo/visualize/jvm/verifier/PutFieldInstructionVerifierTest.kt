package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PutFieldInstructionVerifierTest {
    @Test
    fun `putfield pops a receiver and value matching the declared field`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")
        val frame = frame(stack = listOf(VerificationType.Float, ownerType, VerificationType.Integer))

        val nextFrame = PutFieldInstructionVerifier.verify(
            frame = frame,
            fieldOwnerType = ownerType,
            fieldType = VerificationType.Integer,
            maxStack = 3,
        )

        assertEquals(frame(stack = listOf(VerificationType.Float)), nextFrame)
    }

    @Test
    fun `putfield accepts a null receiver for verifier type transition`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")
        val frame = frame(stack = listOf(VerificationType.Null, VerificationType.Integer))

        val nextFrame = PutFieldInstructionVerifier.verify(
            frame = frame,
            fieldOwnerType = ownerType,
            fieldType = VerificationType.Integer,
            maxStack = 2,
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `putfield accepts a reference subtype field value`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")
        val valueType = VerificationType.ClassType("pkg/Value")
        val frame = frame(stack = listOf(ownerType, valueType))

        val nextFrame = PutFieldInstructionVerifier.verify(
            frame = frame,
            fieldOwnerType = ownerType,
            fieldType = VerificationType.Reference,
            maxStack = 2,
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `putfield rejects a non assignable field value`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            PutFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(ownerType, VerificationType.Integer)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Long,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected Long",
            exception.message,
        )
    }

    @Test
    fun `putfield rejects a non assignable receiver after popping the value`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            PutFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer, VerificationType.Float)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Float,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Integer, expected ClassType(internalName=pkg/Owner, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `putfield rejects a missing receiver after popping the value`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            PutFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Integer)),
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
    fun `putfield rejects an empty operand stack`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            PutFieldInstructionVerifier.verify(
                frame = frame(stack = emptyList()),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Integer,
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack is empty, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `putfield rejects an incoming stack exceeding max stack`() {
        val ownerType = VerificationType.ClassType("pkg/Owner")

        val exception = assertFailsWith<MethodVerificationException> {
            PutFieldInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.Double, ownerType, VerificationType.Integer)),
                fieldOwnerType = ownerType,
                fieldType = VerificationType.Integer,
                maxStack = 3,
            )
        }

        assertEquals(
            "Operand stack depth 4 exceeds max_stack=3",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 181, locals = emptyList(), stack = stack)
}
