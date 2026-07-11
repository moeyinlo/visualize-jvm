package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class VerifierLocalVariablesTest {
    @Test
    fun `expands compact locals into local variable slots`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))

        val locals = VerifierLocalVariables.fromCompact(
            locals = listOf(VerificationType.Integer, VerificationType.Long, objectType),
            maxLocals = 5,
        )

        assertEquals(
            listOf(
                VerificationType.Integer,
                VerificationType.Long,
                VerificationType.Top,
                objectType,
                VerificationType.Top,
            ),
            locals.slots,
        )
    }

    @Test
    fun `loads locals when stored type is assignable to expected type`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val locals = VerifierLocalVariables.fromCompact(
            locals = listOf(objectType),
            maxLocals = 1,
        )

        assertEquals(objectType, locals.load(index = 0, expected = VerificationType.Reference))
    }

    @Test
    fun `rejects loads from the second slot of a category two local`() {
        val locals = VerifierLocalVariables.fromCompact(
            locals = listOf(VerificationType.Long),
            maxLocals = 3,
        )

        val exception = assertFailsWith<MethodVerificationException> {
            locals.load(index = 1, expected = VerificationType.Long)
        }

        assertEquals(
            "Local variable 1 contains Top, expected Long",
            exception.message,
        )
    }

    @Test
    fun `stores category two values with a trailing top slot`() {
        val locals = VerifierLocalVariables.fromCompact(
            locals = emptyList(),
            maxLocals = 3,
        )

        val stored = locals.store(index = 1, value = VerificationType.Double)

        assertEquals(
            listOf(VerificationType.Top, VerificationType.Double, VerificationType.Top),
            stored.slots,
        )
    }

    @Test
    fun `stores into a category two trailing slot invalidates the original pair`() {
        val locals = VerifierLocalVariables.fromCompact(
            locals = listOf(VerificationType.Long),
            maxLocals = 3,
        )

        val stored = locals.store(index = 1, value = VerificationType.Integer)

        assertEquals(
            listOf(VerificationType.Top, VerificationType.Integer, VerificationType.Top),
            stored.slots,
        )
    }

    @Test
    fun `rejects category two stores that exceed max locals`() {
        val locals = VerifierLocalVariables.fromCompact(
            locals = emptyList(),
            maxLocals = 2,
        )

        val exception = assertFailsWith<MethodVerificationException> {
            locals.store(index = 1, value = VerificationType.Long)
        }

        assertEquals(
            "Local variable index 1 with width 2 exceeds max_locals=2",
            exception.message,
        )
    }
}
