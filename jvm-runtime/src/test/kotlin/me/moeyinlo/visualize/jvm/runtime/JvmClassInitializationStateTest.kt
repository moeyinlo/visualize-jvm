package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmClassInitializationStateTest {
    @Test
    fun `class initialization states default to prepared before active initialization`() {
        val states = JvmClassInitializationStates()

        assertEquals(
            JvmClassInitializationState.Prepared,
            states.get("pkg/Example"),
        )
    }

    @Test
    fun `class initialization states track initializing thread and terminal outcomes`() {
        val states = JvmClassInitializationStates()

        states.startInitialization("pkg/Example", threadId = "main")
        assertEquals(
            JvmClassInitializationState.Initializing(threadId = "main"),
            states.get("pkg/Example"),
        )

        states.completeInitialization("pkg/Example", threadId = "main")
        assertEquals(
            JvmClassInitializationState.Initialized,
            states.get("pkg/Example"),
        )
    }

    @Test
    fun `class initialization states reject completion by a different thread`() {
        val states = JvmClassInitializationStates()

        states.startInitialization("pkg/Example", threadId = "main")

        val exception = assertFailsWith<JvmClassInitializationStateException> {
            states.completeInitialization("pkg/Example", threadId = "worker")
        }
        assertEquals(
            "Class pkg/Example initialization is owned by thread main, not worker",
            exception.message,
        )
    }
}
