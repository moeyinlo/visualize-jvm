package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

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
    fun `invokespecial initializer with uninitializedThis initializes normal frame and poisons exception frame`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val frame = frame(
            locals = listOf(VerificationType.UninitializedThis, VerificationType.Integer),
            stack = listOf(
                VerificationType.UninitializedThis,
                VerificationType.UninitializedThis,
                VerificationType.Integer,
            ),
        )

        val transition = InvokeSpecialInstructionVerifier.verifyUninitializedThisInitializer(
            frame = frame,
            descriptor = "(I)V",
            maxStack = 3,
            methodOwnerType = VerificationType.ClassType("pkg/Sub"),
            ownerEnvironment = ownerEnvironment(),
            initializedThisType = thisType,
        )

        assertEquals(
            frame(
                locals = listOf(thisType, VerificationType.Integer),
                stack = listOf(thisType),
            ),
            transition.normalFrame,
        )
        assertEquals(
            frame(
                locals = listOf(VerificationType.Top, VerificationType.Integer),
                stack = emptyList(),
            ),
            transition.exceptionFrame,
        )
    }

    @Test
    fun `invokespecial initializer with uninitializedThis accepts direct superclass owner`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))

        val transition = InvokeSpecialInstructionVerifier.verifyUninitializedThisInitializer(
            frame = frame(
                locals = listOf(VerificationType.UninitializedThis),
                stack = listOf(VerificationType.UninitializedThis),
            ),
            descriptor = "()V",
            maxStack = 1,
            methodOwnerType = VerificationType.ClassType("lib/Base"),
            ownerEnvironment = ownerEnvironment(),
            initializedThisType = thisType,
        )

        assertEquals(
            frame(locals = listOf(thisType), stack = emptyList()),
            transition.normalFrame,
        )
    }

    @Test
    fun `invokespecial initializer with uninitializedThis rejects non-void descriptor`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyUninitializedThisInitializer(
                frame = frame(stack = listOf(VerificationType.UninitializedThis)),
                descriptor = "()Ljava/lang/Object;",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("pkg/Sub"),
                ownerEnvironment = ownerEnvironment(),
                initializedThisType = VerificationType.ObjectType(ConstantPoolIndex(1)),
            )
        }

        assertEquals("invokespecial initializer descriptor must return void", exception.message)
    }

    @Test
    fun `invokespecial initializer with uninitializedThis rejects unrelated owner`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyUninitializedThisInitializer(
                frame = frame(stack = listOf(VerificationType.UninitializedThis)),
                descriptor = "()V",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("other/Helper"),
                ownerEnvironment = ownerEnvironment(),
                initializedThisType = VerificationType.ObjectType(ConstantPoolIndex(1)),
            )
        }

        assertEquals(
            "invokespecial initializer owner other/Helper is not current class pkg/Sub or its direct superclass",
            exception.message,
        )
    }

    @Test
    fun `invokespecial initializer with uninitializedThis rejects indirect superclass owner`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyUninitializedThisInitializer(
                frame = frame(stack = listOf(VerificationType.UninitializedThis)),
                descriptor = "()V",
                maxStack = 1,
                methodOwnerType = VerificationType.ClassType("java/lang/Object"),
                ownerEnvironment = InvokeSpecialOwnerEnvironment(
                    currentClass = ProtectedVerifierClass("pkg/Sub"),
                    superclasses = listOf(
                        ProtectedVerifierClass("lib/Base"),
                        ProtectedVerifierClass("java/lang/Object"),
                    ),
                    directSuperinterfaceNames = emptyList(),
                    directSuperclassName = "lib/Base",
                ),
                initializedThisType = VerificationType.ObjectType(ConstantPoolIndex(1)),
            )
        }

        assertEquals(
            "invokespecial initializer owner java/lang/Object is not current class pkg/Sub or its direct superclass",
            exception.message,
        )
    }

    @Test
    fun `invokespecial initializer with uninitialized object initializes normal frame and poisons exception frame`() {
        val newItem = VerificationType.Uninitialized(offset = 4)
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val frame = frame(
            locals = listOf(newItem, VerificationType.Integer),
            stack = listOf(newItem, newItem, VerificationType.Integer),
        )

        val transition = InvokeSpecialInstructionVerifier.verifyUninitializedObjectInitializer(
            frame = frame,
            descriptor = "(I)V",
            maxStack = 3,
            newOffset = 4,
            methodOwnerType = objectType,
            newInstructionObjectType = objectType,
        )

        assertEquals(
            frame(
                locals = listOf(objectType, VerificationType.Integer),
                stack = listOf(objectType),
            ),
            transition.normalFrame,
        )
        assertEquals(
            frame(
                locals = listOf(VerificationType.Top, VerificationType.Integer),
                stack = emptyList(),
            ),
            transition.exceptionFrame,
        )
    }

    @Test
    fun `invokespecial initializer with uninitialized object rejects non-void descriptor`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyUninitializedObjectInitializer(
                frame = frame(stack = listOf(VerificationType.Uninitialized(offset = 4))),
                descriptor = "()Ljava/lang/Object;",
                maxStack = 1,
                newOffset = 4,
                methodOwnerType = objectType,
                newInstructionObjectType = objectType,
            )
        }

        assertEquals("invokespecial initializer descriptor must return void", exception.message)
    }

    @Test
    fun `invokespecial initializer with uninitialized object rejects owner not created by new instruction`() {
        val exception = assertFailsWith<MethodVerificationException> {
            InvokeSpecialInstructionVerifier.verifyUninitializedObjectInitializer(
                frame = frame(stack = listOf(VerificationType.Uninitialized(offset = 4))),
                descriptor = "()V",
                maxStack = 1,
                newOffset = 4,
                methodOwnerType = VerificationType.ObjectType(ConstantPoolIndex(7)),
                newInstructionObjectType = VerificationType.ObjectType(ConstantPoolIndex(8)),
            )
        }

        assertEquals(
            "invokespecial initializer owner ObjectType(constantPoolIndex=#7) does not match " +
                "new instruction type ObjectType(constantPoolIndex=#8) at bytecode offset 4",
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

    private fun frame(
        stack: List<VerificationType>,
        locals: List<VerificationType> = emptyList(),
    ): VerificationFrameState =
        VerificationFrameState(bytecodeOffset = 183, locals = locals, stack = stack)

    private fun ownerEnvironment(): InvokeSpecialOwnerEnvironment =
        InvokeSpecialOwnerEnvironment(
            currentClass = ProtectedVerifierClass("pkg/Sub"),
            superclasses = listOf(ProtectedVerifierClass("lib/Base")),
            directSuperinterfaceNames = listOf("api/Iface"),
        )
}
