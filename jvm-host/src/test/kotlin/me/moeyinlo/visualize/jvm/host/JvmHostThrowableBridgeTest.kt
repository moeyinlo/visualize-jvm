package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmThrowablePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class JvmHostThrowableBridgeTest {
    @Test
    fun `converts guest throwable references to host throwables`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/RuntimeException")
        heap.recordThrowableStackTrace(
            reference = reference,
            stackTrace = listOf(
                JvmStackTraceFrame(
                    declaringClass = "demo/Guest",
                    methodName = "run",
                    fileName = "Guest.java",
                    lineNumber = 42,
                ),
            ),
        )

        val hostThrowable = JvmHostThrowableBridge.toHost(reference, Throwable::class.java, heap)

        assertIs<RuntimeException>(hostThrowable)
        assertEquals("demo.Guest", hostThrowable.stackTrace.single().className)
        assertEquals("run", hostThrowable.stackTrace.single().methodName)
        assertEquals("Guest.java", hostThrowable.stackTrace.single().fileName)
        assertEquals(42, hostThrowable.stackTrace.single().lineNumber)
    }

    @Test
    fun `converts guest throwable detail messages to host throwable messages`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/RuntimeException")
        heap.recordThrowableDetailMessage(reference, heap.internString("guest message"))

        val hostThrowable = JvmHostThrowableBridge.toHost(reference, Throwable::class.java, heap)

        assertIs<RuntimeException>(hostThrowable)
        assertEquals("guest message", hostThrowable.message)
    }

    @Test
    fun `converts guest throwable causes to host throwable causes`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("java/lang/RuntimeException")
        val cause = heap.allocateObject("java/lang/IllegalArgumentException")
        heap.recordThrowableCause(reference, cause)

        val hostThrowable = JvmHostThrowableBridge.toHost(reference, Throwable::class.java, heap)

        assertIs<RuntimeException>(hostThrowable)
        assertIs<IllegalArgumentException>(hostThrowable.cause)
    }

    @Test
    fun `converts host throwables to guest throwable references`() {
        val heap = JvmHeap()
        val hostThrowable = IllegalArgumentException("bad").also { throwable ->
            throwable.stackTrace = arrayOf(StackTraceElement("demo.Host", "call", "Host.java", 7))
        }

        val guestValue = JvmHostThrowableBridge.fromHost(hostThrowable, Throwable::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        assertEquals("java/lang/IllegalArgumentException", heapObject.className)
        val payload = heapObject.payload as JvmThrowablePayload
        assertEquals(
            listOf(
                JvmStackTraceFrame(
                    declaringClass = "demo/Host",
                    methodName = "call",
                    fileName = "Host.java",
                    lineNumber = 7,
                ),
            ),
            payload.stackTrace,
        )
        val detailMessage = payload.detailMessage as JvmObjectReferenceValue
        val detailMessagePayload = heap.get(detailMessage).payload as JvmStringPayload
        assertEquals("bad", detailMessagePayload.value)
    }

    @Test
    fun `converts host throwable causes to guest throwable causes`() {
        val heap = JvmHeap()
        val hostThrowable = RuntimeException("outer", IllegalArgumentException("inner"))

        val guestValue = JvmHostThrowableBridge.fromHost(hostThrowable, Throwable::class.java, heap)

        val reference = guestValue as JvmObjectReferenceValue
        val heapObject = heap.get(reference)
        val payload = heapObject.payload as JvmThrowablePayload
        val cause = payload.cause as JvmObjectReferenceValue
        assertEquals("java/lang/IllegalArgumentException", heap.get(cause).className)
    }

    @Test
    fun `preserves null throwable values`() {
        val heap = JvmHeap()

        assertEquals(null, JvmHostThrowableBridge.toHost(JvmNullValue, Throwable::class.java, heap))
        assertSame(JvmNullValue, JvmHostThrowableBridge.fromHost(null, Throwable::class.java, heap))
    }

    @Test
    fun `rejects non throwable guest references`() {
        val heap = JvmHeap()
        val reference = heap.allocateString("not throwable")

        val exception = assertFailsWith<JvmHostThrowableBridgeException> {
            JvmHostThrowableBridge.toHost(reference, Throwable::class.java, heap)
        }

        assertEquals("Guest reference java/lang/String is not a Throwable", exception.message)
    }
}
