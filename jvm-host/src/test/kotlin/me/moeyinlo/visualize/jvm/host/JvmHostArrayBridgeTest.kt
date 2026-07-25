package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmHostArrayBridgeTest {
    @Test
    fun `converts guest primitive arrays to host primitive arrays`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(3)
        val payload = heap.get(reference).payload as JvmIntArrayPayload
        payload.elements[0] = 4
        payload.elements[1] = 5
        payload.elements[2] = 6

        val hostValue = JvmHostArrayBridge.toHost(reference, IntArray::class.java, heap)

        assertContentEquals(intArrayOf(4, 5, 6), hostValue as IntArray)
    }

    @Test
    fun `converts host primitive arrays to guest primitive arrays`() {
        val heap = JvmHeap()

        val guestValue = JvmHostArrayBridge.fromHost(intArrayOf(7, 8), IntArray::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        assertEquals("[I", heapObject.className)
        assertEquals(mutableListOf(7, 8), (heapObject.payload as JvmIntArrayPayload).elements)
    }

    @Test
    fun `converts guest string arrays to host string arrays`() {
        val heap = JvmHeap()
        val reference = heap.allocateReferenceArray("java/lang/String", 2)
        val payload = heap.get(reference).payload as JvmReferenceArrayPayload
        payload.elements[0] = heap.allocateString("a")
        payload.elements[1] = JvmNullValue

        val hostValue = JvmHostArrayBridge.toHost(reference, Array<String>::class.java, heap)

        assertEquals(listOf("a", null), (hostValue as Array<*>).toList())
    }

    @Test
    fun `converts host string arrays to guest string arrays`() {
        val heap = JvmHeap()

        val guestValue = JvmHostArrayBridge.fromHost(arrayOf("x", null), Array<String>::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        assertEquals("[Ljava/lang/String;", heapObject.className)
        val elements = (heapObject.payload as JvmReferenceArrayPayload).elements
        assertEquals(JvmStringPayload("x"), heap.get(elements[0] as JvmObjectReferenceValue).payload)
        assertSame(JvmNullValue, elements[1])
    }

    @Test
    fun `rejects non array host target types`() {
        val heap = JvmHeap()
        val reference = heap.allocateIntArray(0)

        val exception = assertFailsWith<JvmHostArrayBridgeException> {
            JvmHostArrayBridge.toHost(reference, String::class.java, heap)
        }

        assertEquals("Host array bridge target type must be an array: java.lang.String", exception.message)
    }
}
