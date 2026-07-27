package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JvmHostActiveUseHandlerTest {
    @Test
    fun `none handler does not claim host delegated active use`() {
        val initializationStates = JvmClassInitializationStates()

        val handled = JvmHostActiveUseHandler.None.handleActiveUse(
            className = "java/lang/Integer",
            classInitializationStates = initializationStates,
        )

        assertFalse(handled)
        assertEquals(JvmClassInitializationState.Prepared, initializationStates.get("java/lang/Integer"))
    }
}
