package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvokeVirtualInstructionVerifierTest {
    private val targetType = VerificationType.ClassType("example/Target")

    @Test
    fun `invokevirtual pops receiver and descriptor arguments then pushes return type`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 182,
            locals = emptyList(),
            stack = listOf(
                VerificationType.Float,
                targetType,
                VerificationType.Integer,
                VerificationType.Long,
            ),
        )

        val nextFrame = InvokeVirtualInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = targetType,
            methodName = "mix",
            descriptor = "(IJ)Ljava/lang/String;",
            maxStack = 5,
        )

        assertEquals(
            frame.copy(
                stack = listOf(
                    VerificationType.Float,
                    VerificationType.ClassType("java/lang/String"),
                ),
            ),
            nextFrame,
        )
    }

    @Test
    fun `invokevirtual with void return only pops receiver and descriptor arguments`() {
        val frame = VerificationFrameState(
            bytecodeOffset = 182,
            locals = emptyList(),
            stack = listOf(targetType),
        )

        val nextFrame = InvokeVirtualInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = targetType,
            methodName = "drop",
            descriptor = "()V",
            maxStack = 1,
        )

        assertEquals(
            frame.copy(stack = emptyList()),
            nextFrame,
        )
    }

    @Test
    fun `invokevirtual rejects a mismatched descriptor argument`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(targetType, VerificationType.Float),
                ),
                methodOwnerType = targetType,
                methodName = "mix",
                descriptor = "(I)V",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected Integer",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects a mismatched receiver`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Float),
                ),
                methodOwnerType = targetType,
                methodName = "drop",
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "Operand stack top contains Float, expected $targetType",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects max stack overflow caused by return type`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Integer, targetType),
                ),
                methodOwnerType = targetType,
                methodName = "wide",
                descriptor = "()J",
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack depth 3 exceeds max_stack=2",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects protected superclass receiver after popping descriptor arguments`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = frame(
                    stack = listOf(
                        VerificationType.ClassType("other/Helper"),
                        VerificationType.Integer,
                    ),
                ),
                methodOwnerType = VerificationType.Reference,
                methodName = "m",
                descriptor = "(I)V",
                maxStack = 2,
                protectedAccess = protectedMethodAccess(),
                protectedEnvironment = protectedEnvironment(),
            )
        }

        assertEquals(
            "Protected member lib/Base.m:(I)V requires receiver assignable to current class pkg/Sub " +
                "at bytecode offset 182, but found ClassType(internalName=other/Helper, loader=bootstrap)",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual applies protected superclass check to receiver below descriptor arguments`() {
        val frame = frame(
            stack = listOf(
                VerificationType.ClassType("pkg/Sub"),
                VerificationType.Integer,
            ),
        )

        val nextFrame = InvokeVirtualInstructionVerifier.verify(
            frame = frame,
            methodOwnerType = VerificationType.Reference,
            methodName = "m",
            descriptor = "(I)V",
            maxStack = 2,
            protectedAccess = protectedMethodAccess(),
            protectedEnvironment = protectedEnvironment(),
        )

        assertEquals(frame(stack = emptyList()), nextFrame)
    }

    @Test
    fun `invokevirtual protected access requires both access and environment`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = frame(stack = listOf(VerificationType.ClassType("pkg/Sub"))),
                methodOwnerType = VerificationType.Reference,
                methodName = "m",
                descriptor = "()V",
                maxStack = 1,
                protectedAccess = protectedMethodAccess(),
            )
        }

        assertEquals(
            "invokevirtual protected access verification requires both access and environment",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects instance initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(targetType),
                ),
                methodOwnerType = targetType,
                methodName = "<init>",
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "invokevirtual target method must not be <init>",
            exception.message,
        )
    }

    @Test
    fun `invokevirtual rejects class initialization method names`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeVirtualInstructionVerifier.verify(
                frame = VerificationFrameState(
                    bytecodeOffset = 182,
                    locals = emptyList(),
                    stack = listOf(targetType),
                ),
                methodOwnerType = targetType,
                methodName = "<clinit>",
                descriptor = "()V",
                maxStack = 1,
            )
        }

        assertEquals(
            "invokevirtual target method must not be <clinit>",
            exception.message,
        )
    }

    private fun frame(stack: List<VerificationType>): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 182, locals = emptyList(), stack = stack)

    private fun protectedEnvironment(): ProtectedMemberAccessEnvironment {
        val baseClass = ProtectedVerifierClass(
            internalName = "lib/Base",
            members = listOf(
                ProtectedClassMember(name = "m", descriptor = "(I)V", isProtected = true),
            ),
        )
        return ProtectedMemberAccessEnvironment(
            currentClass = ProtectedVerifierClass("pkg/Sub"),
            thisType = VerificationType.ClassType("pkg/Sub"),
            superclasses = listOf(baseClass),
            loadedClasses = mapOf(ProtectedClassKey("lib/Base") to baseClass),
        )
    }

    private fun protectedMethodAccess(): ProtectedMemberAccess =
        ProtectedMemberAccess(
            owner = ProtectedMemberOwner.ClassType("lib/Base"),
            name = "m",
            descriptor = "(I)V",
        )
}
