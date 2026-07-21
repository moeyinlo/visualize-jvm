package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmThreadStackTest {
    @Test
    fun `thread stack pushes frames and exposes the current frame`() {
        val stack = JvmThreadStack()
        val first = TestFrame("first")
        val second = TestFrame("second")

        stack.push(first)
        stack.push(second)

        assertEquals(2, stack.depth)
        assertSame(second, stack.currentFrame())
        assertEquals(listOf(first, second), stack.toList())
    }

    @Test
    fun `thread stack pops frames in last in first out order`() {
        val stack = JvmThreadStack()
        val first = TestFrame("first")
        val second = TestFrame("second")
        stack.push(first)
        stack.push(second)

        assertSame(second, stack.pop())
        assertSame(first, stack.pop())
        assertEquals(0, stack.depth)
    }

    @Test
    fun `thread stack rejects negative maximum frame count`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmThreadStack(maxFrames = -1)
        }

        assertEquals("max frame count must be non-negative: -1", exception.message)
    }

    @Test
    fun `fixed size thread stack rejects overflow`() {
        val stack = JvmThreadStack(maxFrames = 1)
        stack.push(TestFrame("only"))

        val exception = assertFailsWith<JvmStackOverflowException> {
            stack.push(TestFrame("overflow"))
        }

        assertEquals("JVM stack depth 2 exceeds max_frames=1", exception.message)
    }

    @Test
    fun `empty thread stack rejects pop and current frame access`() {
        val stack = JvmThreadStack()

        assertFailsWith<JvmStackUnderflowException> { stack.pop() }
        assertFailsWith<JvmStackUnderflowException> { stack.currentFrame() }
    }

    private data class TestFrame(val name: String) : JvmFrame
}
