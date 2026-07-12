package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JvmHeapTest {
    @Test
    fun `heap allocates objects with opaque positive guest identities`() {
        val heap = JvmHeap()

        val first = heap.allocateObject("java/lang/String")
        val second = heap.allocateObject("java/lang/Class")

        assertEquals(JvmReferenceId(1), first.referenceId)
        assertEquals(JvmReferenceId(2), second.referenceId)
        assertNotEquals(first, second)
        assertEquals(JvmHeapObject("java/lang/String"), heap.get(first))
        assertEquals(JvmHeapObject("java/lang/Class"), heap.get(second))
    }

    @Test
    fun `heap allocates string objects with guest payloads`() {
        val heap = JvmHeap()

        val reference = heap.allocateString("hello\u0000world")

        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("hello\u0000world"),
            ),
            heap.get(reference),
        )
    }

    @Test
    fun `heap interns string objects with identical code points`() {
        val heap = JvmHeap()

        val first = heap.internString("same")
        val second = heap.internString("same")
        val distinct = heap.internString("different")

        assertEquals(first, second)
        assertNotEquals(first, distinct)
        assertEquals(JvmReferenceId(1), first.referenceId)
        assertEquals(JvmReferenceId(2), distinct.referenceId)
    }
}
