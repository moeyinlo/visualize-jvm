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
    @Test
    fun `class initialization states release waiters on terminal outcomes`() {
        val states = JvmClassInitializationStates()

        states.startInitialization("pkg/Example", threadId = "main")
        states.recordInitializationWaiter("pkg/Example", threadId = "worker-1")
        states.recordInitializationWaiter("pkg/Example", threadId = "worker-2")
        states.recordInitializationWaiter("pkg/Example", threadId = "worker-1")

        assertEquals(listOf("worker-1", "worker-2"), states.waitingThreads("pkg/Example"))
        assertEquals(
            listOf("worker-1", "worker-2"),
            states.completeInitialization("pkg/Example", threadId = "main"),
        )
        assertEquals(emptyList(), states.waitingThreads("pkg/Example"))

        states.startInitialization("pkg/Failing", threadId = "main")
        states.recordInitializationWaiter("pkg/Failing", threadId = "worker")

        assertEquals(
            listOf("worker"),
            states.failInitialization("pkg/Failing", threadId = "main", errorClassName = "java/lang/Error"),
        )
        assertEquals(emptyList(), states.waitingThreads("pkg/Failing"))
    }

}
