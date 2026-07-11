package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class UninitializedThisRulesTest {
    @Test
    fun `uses uninitializedThis as initial this type for constructors with a superclass`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))

        assertEquals(
            VerificationType.UninitializedThis,
            UninitializedThisRules.initialThisType(
                isInstanceConstructor = true,
                hasSuperclass = true,
                thisType = thisType,
            ),
        )
    }

    @Test
    fun `uses current class type as initial this type outside superclass constructors`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))

        assertEquals(
            thisType,
            UninitializedThisRules.initialThisType(
                isInstanceConstructor = false,
                hasSuperclass = true,
                thisType = thisType,
            ),
        )
        assertEquals(
            thisType,
            UninitializedThisRules.initialThisType(
                isInstanceConstructor = true,
                hasSuperclass = false,
                thisType = thisType,
            ),
        )
    }

    @Test
    fun `constructor invocation replaces uninitializedThis in normal locals and stack`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val frameAfterPop = VerificationFrameState(
            bytecodeOffset = 10,
            locals = listOf(VerificationType.UninitializedThis, VerificationType.Integer),
            stack = listOf(VerificationType.UninitializedThis, VerificationType.Null),
        )

        val transition = UninitializedThisRules.completeConstructorInvocation(
            frameAfterPop = frameAfterPop,
            thisType = thisType,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 10,
                locals = listOf(thisType, VerificationType.Integer),
                stack = listOf(thisType, VerificationType.Null),
            ),
            transition.normalFrame,
        )
    }

    @Test
    fun `constructor invocation replaces uninitializedThis with top in exception locals and clears stack`() {
        val thisType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val frameAfterPop = VerificationFrameState(
            bytecodeOffset = 20,
            locals = listOf(VerificationType.UninitializedThis, VerificationType.Integer),
            stack = listOf(VerificationType.UninitializedThis),
        )

        val transition = UninitializedThisRules.completeConstructorInvocation(
            frameAfterPop = frameAfterPop,
            thisType = thisType,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 20,
                locals = listOf(VerificationType.Top, VerificationType.Integer),
                stack = emptyList(),
            ),
            transition.exceptionFrame,
        )
    }

    @Test
    fun `return is rejected while uninitializedThis remains in locals`() {
        val exception = assertFailsWith<MethodVerificationException> {
            UninitializedThisRules.requireInitializedThisForReturn(
                frame = VerificationFrameState(
                    bytecodeOffset = 30,
                    locals = listOf(VerificationType.UninitializedThis),
                    stack = emptyList(),
                ),
            )
        }

        assertEquals(
            "Cannot return from constructor while uninitializedThis is still present at bytecode offset 30",
            exception.message,
        )
    }
}
