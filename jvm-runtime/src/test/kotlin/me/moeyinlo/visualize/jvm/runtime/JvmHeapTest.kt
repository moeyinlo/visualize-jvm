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
}
