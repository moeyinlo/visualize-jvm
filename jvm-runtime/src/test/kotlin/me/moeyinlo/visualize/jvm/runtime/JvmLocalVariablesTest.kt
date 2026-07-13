package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmLocalVariablesTest {
    @Test
    fun `local variables store and load category one values by index`() {
        val locals = JvmLocalVariables(maxLocals = 3)
        val reference = JvmObjectReferenceValue(JvmReferenceId(7))

        locals.store(0, JvmIntValue(42))
        locals.store(1, JvmFloatValue(1.5f))
        locals.store(2, reference)

        assertEquals(JvmIntValue(42), locals.load(0))
        assertEquals(JvmFloatValue(1.5f), locals.load(1))
        assertEquals(reference, locals.load(2))
    }

    @Test
    fun `category two locals occupy two consecutive slots and load from the lower index`() {
        val locals = JvmLocalVariables(maxLocals = 3)

        locals.store(1, JvmLongValue(9L))

        assertEquals(JvmLongValue(9L), locals.load(1))
        assertFailsWith<JvmLocalVariablesInvalidSlotException> { locals.load(2) }
    }

    @Test
    fun `storing into either word of a category two local invalidates the previous value`() {
        val locals = JvmLocalVariables(maxLocals = 3)

        locals.store(0, JvmDoubleValue(2.0))
        locals.store(1, JvmIntValue(5))

        assertFailsWith<JvmLocalVariablesInvalidSlotException> { locals.load(0) }
        assertEquals(JvmIntValue(5), locals.load(1))
    }

    @Test
    fun `locals reject out of bounds indexes and uninitialized loads`() {
        val locals = JvmLocalVariables(maxLocals = 1)

        assertFailsWith<JvmLocalVariablesInvalidSlotException> { locals.load(0) }
        assertFailsWith<JvmLocalVariablesIndexException> { locals.load(1) }
        assertFailsWith<JvmLocalVariablesIndexException> { locals.store(0, JvmLongValue(1L)) }
    }
}
