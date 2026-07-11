package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class ObjectInitializationRulesTest {
    @Test
    fun `new pushes an uninitialized object and clears matching locals`() {
        val staleNewItem = VerificationType.Uninitialized(offset = 4)
        val frame = VerificationFrameState(
            bytecodeOffset = 4,
            locals = listOf(staleNewItem, VerificationType.Integer),
            stack = listOf(VerificationType.Null),
        )

        val nextFrame = ObjectInitializationRules.beginNewObject(
            frame = frame,
            newOffset = 4,
            maxStack = 2,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 4,
                locals = listOf(VerificationType.Top, VerificationType.Integer),
                stack = listOf(VerificationType.Null, staleNewItem),
            ),
            nextFrame,
        )
    }

    @Test
    fun `new rejects an incoming stack that already contains the same uninitialized object`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ObjectInitializationRules.beginNewObject(
                frame = VerificationFrameState(
                    bytecodeOffset = 4,
                    locals = emptyList(),
                    stack = listOf(VerificationType.Uninitialized(offset = 4)),
                ),
                newOffset = 4,
                maxStack = 2,
            )
        }

        assertEquals(
            "Operand stack already contains uninitialized object created at bytecode offset 4",
            exception.message,
        )
    }

    @Test
    fun `new enforces max stack while pushing the uninitialized object`() {
        val exception = assertFailsWith<MethodVerificationException> {
            ObjectInitializationRules.beginNewObject(
                frame = VerificationFrameState(
                    bytecodeOffset = 4,
                    locals = emptyList(),
                    stack = emptyList(),
                ),
                newOffset = 4,
                maxStack = 0,
            )
        }

        assertEquals("Operand stack depth 1 exceeds max_stack=0", exception.message)
    }

    @Test
    fun `constructor invocation replaces uninitialized object in normal locals and stack`() {
        val newItem = VerificationType.Uninitialized(offset = 4)
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val frameAfterPop = VerificationFrameState(
            bytecodeOffset = 8,
            locals = listOf(newItem, VerificationType.Integer),
            stack = listOf(newItem, VerificationType.Null),
        )

        val transition = ObjectInitializationRules.completeObjectConstructorInvocation(
            frameAfterPop = frameAfterPop,
            newOffset = 4,
            objectType = objectType,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 8,
                locals = listOf(objectType, VerificationType.Integer),
                stack = listOf(objectType, VerificationType.Null),
            ),
            transition.normalFrame,
        )
    }

    @Test
    fun `constructor invocation replaces uninitialized object with top in exception locals and clears stack`() {
        val newItem = VerificationType.Uninitialized(offset = 4)
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(7))
        val frameAfterPop = VerificationFrameState(
            bytecodeOffset = 8,
            locals = listOf(newItem, VerificationType.Integer),
            stack = listOf(newItem),
        )

        val transition = ObjectInitializationRules.completeObjectConstructorInvocation(
            frameAfterPop = frameAfterPop,
            newOffset = 4,
            objectType = objectType,
        )

        assertEquals(
            VerificationFrameState(
                bytecodeOffset = 8,
                locals = listOf(VerificationType.Top, VerificationType.Integer),
                stack = emptyList(),
            ),
            transition.exceptionFrame,
        )
    }
}
