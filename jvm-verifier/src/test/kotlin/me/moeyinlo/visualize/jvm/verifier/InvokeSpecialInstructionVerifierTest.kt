package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeSpecialInstructionVerifierTest {
    @Test
    fun `invokespecial non-initializer pops current receiver and descriptor arguments then pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 183,
            locals = listOf(VerificationType.ClassType("pkg/Sub")),
            stack = listOf(
                VerificationType.Float,
                VerificationType.ClassType("pkg/Sub"),
                VerificationType.ClassType("java/lang/String"),
                VerificationType.Long,
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "(Ljava/lang/String;JI)D",
            maxStack = 6,
        )

        assertEquals(
            frame.copy(stack = listOf(VerificationType.Float, VerificationType.Double)),
            nextFrame,
        )
    }

    @Test
    fun `invokespecial non-initializer with void return only pops receiver and descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 183,
            locals = listOf(VerificationType.ClassType("pkg/Sub")),
            stack = listOf(
                VerificationType.ClassType("pkg/Sub"),
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "(I)V",
            maxStack = 2,
        )

        assertEquals(frame.copy(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokespecial non-initializer rejects receiver not assignable to current class`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = listOf(VerificationType.ClassType("other/Helper")),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains ClassType(internalName=other/Helper, loader=bootstrap), " +
                "expected ClassType(internalName=pkg/Sub, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `invokespecial non-initializer rejects descriptor argument mismatch before receiver pop`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = listOf(
                        VerificationType.ClassType("pkg/Sub"),
                        VerificationType.Float,
                    ),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "(I)V",
                maxStack = 2,
            )
        }

        assertEquals("Operand stack top contains Float, expected Integer", exception.message)
    }

    @Test
    fun `invokespecial non-initializer accepts method owner as current class`() {
        val frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub")))

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "()V",
            maxStack = 1,
            methodOwnerType = VerificationType.ClassType("pkg/Sub"),
            ownerEnvironment = ownerEnvironment(),
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokespecial non-initializer accepts method owner as superclass`() {
        val frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub")))

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "()V",
            maxStack = 1,
            methodOwnerType = VerificationType.ClassType("lib/Base"),
            ownerEnvironment = ownerEnvironment(),
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokespecial non-initializer accepts method owner as direct superinterface`() {
        val frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub")))

        val nextFrame = InvokeSpecialInstructionVerifier.verifyNonInitializer(
            frame = frame,
            thisType = VerificationType.ClassType("pkg/Sub"),
            methodName = "m",
            descriptor = "()V",
            maxStack = 1,
            methodOwnerType = VerificationType.ClassType("api/Iface"),
            ownerEnvironment = ownerEnvironment(),
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokespecial non-initializer rejects unrelated method owner`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub"))),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("other/Helper"),
                ownerEnvironment = ownerEnvironment(),
            )
        }

        assertEquals(
            "invokespecial non-initializer owner other/Helper is not current class pkg/Sub, " +
                "a superclass, or a direct superinterface",
            exception.message,
        )
    }

    @Test
    fun `invokespecial non-initializer rejects superclass owner from another loader`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub"))),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("lib/Base", loader = "child"),
                ownerEnvironment = ownerEnvironment(),
            )
        }

        assertEquals(
            "invokespecial non-initializer owner lib/Base is not current class pkg/Sub, " +
                "a superclass, or a direct superinterface",
            exception.message,
        )
    }

    @Test
    fun `invokespecial non-initializer owner verification requires owner and environment together`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub"))),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("pkg/Sub"),
            )
        }

        assertEquals(
            "invokespecial non-initializer owner verification requires both owner and environment",
            exception.message,
        )
    }

    @Test
    fun `invokespecial non-initializer rejects instance initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "<init>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokespecial non-initializer target method must not be <init>", exception.message)
    }

    @Test
    fun `invokespecial non-initializer rejects class initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyNonInitializer(
                frame = VerificationFrameState(
                    bytecodeOffset = 183,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                thisType = VerificationType.ClassType("pkg/Sub"),
                methodName = "<clinit>",
                descriptor = "()V",
                maxStack = 0,
            )
        }

        assertEquals("invokespecial non-initializer target method must not be <clinit>", exception.message)
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 183, locals = emptyList(), stack = stack)

    private fun ownerEnvironment(): InvokeSpecialOwnerEnvironment =
        InvokeSpecialOwnerEnvironment(
            currentClass = ProtectedVerifierClass("pkg/Sub"),
            superclasses = listOf(ProtectedVerifierClass("lib/Base")),
            directSuperinterfaceNames = listOf("api/Iface"),
        )
}
