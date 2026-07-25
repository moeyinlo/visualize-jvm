package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmHostStringBridgeTest {
    @Test
    fun `converts guest string references to host strings`() {
        val heap = JvmHeap()
        val reference = heap.allocateString("guest text")

        val hostValue = JvmHostStringBridge.toHost(reference, String::class.java, heap)

        assertEquals("guest text", hostValue)
    }

    @Test
    fun `converts guest null references to host null strings`() {
        val heap = JvmHeap()

        val hostValue = JvmHostStringBridge.toHost(JvmNullValue, String::class.java, heap)

        assertEquals(null, hostValue)
    }

    @Test
    fun `converts host string returns to guest string references`() {
        val heap = JvmHeap()

        val guestValue = JvmHostStringBridge.fromHost("host text", String::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        assertEquals("java/lang/String", heapObject.className)
        assertEquals(JvmStringPayload("host text"), heapObject.payload)
    }

    @Test
    fun `converts host null string returns to guest null`() {
        val heap = JvmHeap()

        val guestValue = JvmHostStringBridge.fromHost(null, String::class.java, heap)

        assertSame(JvmNullValue, guestValue)
    }

    @Test
    fun `rejects non string guest references`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/Object")

        val exception = assertFailsWith<JvmHostStringBridgeException> {
            JvmHostStringBridge.toHost(reference, String::class.java, heap)
        }

        assertEquals("Guest reference java/lang/Object is not java/lang/String", exception.message)
    }
}
