package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmOperandStackTest {
    @Test
    fun `operand stack pops values in last in first out order`() {
        val stack = JvmOperandStack(maxStack = 2)
        val first = JvmIntValue(1)
        val second = JvmNullValue

        stack.push(first)
        stack.push(second)

        assertEquals(2, stack.slotDepth)
        assertEquals(2, stack.valueCount)
        assertEquals(second, stack.pop())
        assertEquals(first, stack.pop())
        assertEquals(0, stack.slotDepth)
    }

    @Test
    fun `category two values consume two operand stack slots`() {
        val stack = JvmOperandStack(maxStack = 3)

        stack.push(JvmIntValue(1))
        stack.push(JvmLongValue(2L))

        assertEquals(3, stack.slotDepth)
        assertEquals(2, stack.valueCount)
        assertEquals(JvmLongValue(2L), stack.pop())
        assertEquals(1, stack.slotDepth)
    }

    @Test
    fun `peek returns top value without popping`() {
        val stack = JvmOperandStack(maxStack = 2)
        val top = JvmObjectReferenceValue(JvmReferenceId(9))

        stack.push(JvmIntValue(1))
        stack.push(top)

        assertEquals(top, stack.peek())
        assertEquals(2, stack.valueCount)
        assertEquals(2, stack.slotDepth)
    }

    @Test
    fun `operand stack reports underflow for empty pop and peek`() {
        val stack = JvmOperandStack(maxStack = 1)

        assertFailsWith<JvmOperandStackUnderflowException> { stack.pop() }
        assertFailsWith<JvmOperandStackUnderflowException> { stack.peek() }
    }

    @Test
    fun `operand stack rejects pushes beyond max stack without mutating existing contents`() {
        val stack = JvmOperandStack(maxStack = 1)

        stack.push(JvmIntValue(1))

        assertFailsWith<JvmOperandStackOverflowException> { stack.push(JvmLongValue(2L)) }
        assertEquals(1, stack.slotDepth)
        assertEquals(JvmIntValue(1), stack.peek())
    }

    @Test
    fun `operand stack can be rebuilt from snapshot values`() {
        val reference = JvmObjectReferenceValue(JvmReferenceId(9))

        val stack = JvmOperandStack.fromValues(
            maxStack = 4,
            values = listOf(JvmIntValue(1), JvmLongValue(2L), reference),
        )

        assertEquals(listOf(JvmIntValue(1), JvmLongValue(2L), reference), stack.toList())
        assertEquals(4, stack.slotDepth)
        assertEquals(3, stack.valueCount)
    }

    @Test
    fun `operand stack rebuild rejects snapshots beyond max stack`() {
        assertFailsWith<JvmOperandStackOverflowException> {
            JvmOperandStack.fromValues(
                maxStack = 1,
                values = listOf(JvmIntValue(1), JvmLongValue(2L)),
            )
        }
    }
}
