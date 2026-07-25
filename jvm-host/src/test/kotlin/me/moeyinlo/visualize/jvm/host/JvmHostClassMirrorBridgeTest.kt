package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmHostClassMirrorBridgeTest {
    @Test
    fun `converts guest class mirror references to host classes`() {
        val heap = JvmHeap()
        val reference = heap.internClassMirror("java/lang/String")

        val hostClass = JvmHostClassMirrorBridge.toHost(reference, Class::class.java, heap)

        assertEquals(String::class.java, hostClass)
    }

    @Test
    fun `converts host class returns to guest class mirror references`() {
        val heap = JvmHeap()

        val guestValue = JvmHostClassMirrorBridge.fromHost(String::class.java, Class::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        assertEquals("java/lang/Class", heapObject.className)
        assertEquals(JvmClassPayload("java/lang/String"), heapObject.payload)
    }

    @Test
    fun `preserves null class mirror values`() {
        val heap = JvmHeap()

        assertEquals(null, JvmHostClassMirrorBridge.toHost(JvmNullValue, Class::class.java, heap))
        assertSame(JvmNullValue, JvmHostClassMirrorBridge.fromHost(null, Class::class.java, heap))
    }

    @Test
    fun `rejects non class mirror guest references`() {
        val heap = JvmHeap()
        val reference = heap.allocateString("not a class")

        val exception = assertFailsWith<JvmHostClassMirrorBridgeException> {
            JvmHostClassMirrorBridge.toHost(reference, Class::class.java, heap)
        }

        assertEquals("Guest reference java/lang/String is not java/lang/Class", exception.message)
    }
}
