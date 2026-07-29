package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmThrowablePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmHostExceptionTranslationTest {
    @Test
    fun `translates static host method throwables into guest throwables`() {
        val heap = JvmHeap()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(ThrowingHostFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "throwStatic",
            descriptor = "()V",
        )

        val exception = assertFailsWith<JvmHostTranslatedException> {
            JvmHostMethodInvoker.invokeStatic(
                method = method,
                arguments = emptyList(),
                heap = heap,
            )
        }

        assertIs<IllegalArgumentException>(exception.hostThrowable)
        val guestThrowable = exception.guestThrowable
        val heapObject = heap.get(guestThrowable)
        assertEquals("java/lang/IllegalArgumentException", heapObject.className)
        assertTrue(
            (heapObject.payload as JvmThrowablePayload).stackTrace.any { frame ->
                frame.declaringClass == "me/moeyinlo/visualize/jvm/host/JvmHostExceptionTranslationTest\$ThrowingHostFixture" &&
                    frame.methodName == "throwStatic"
            },
        )
    }

    @Test
    fun `static host method throwable records opaque failed boundary event`() {
        val heap = JvmHeap()
        val recorder = JvmHostBoundaryEventRecorder()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(ThrowingHostFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "throwStatic",
            descriptor = "()V",
        )

        assertFailsWith<JvmHostTranslatedException> {
            JvmHostMethodInvoker.invokeStatic(
                method = method,
                arguments = emptyList(),
                heap = heap,
                boundaryEvents = recorder,
            )
        }

        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = mirror.guestInternalName,
                    methodName = "throwStatic",
                    descriptor = "()V",
                    detail = "static args=0",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Failed,
                    className = mirror.guestInternalName,
                    methodName = "throwStatic",
                    descriptor = "()V",
                    detail = "translated=java.lang.IllegalArgumentException",
                ),
            ),
            recorder.snapshots(),
        )
    }

    @Test
    fun `translates instance host method throwables into guest throwables`() {
        val heap = JvmHeap()
        val hostReceiver = ThrowingHostFixture()
        val guestReceiver = heap.allocateObject("me/moeyinlo/visualize/jvm/host/JvmHostExceptionTranslationTest\$ThrowingHostFixture")
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(guestReceiver, hostReceiver)
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(ThrowingHostFixture::class.java)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "throwInstance",
            descriptor = "(I)I",
        )

        val exception = assertFailsWith<JvmHostTranslatedException> {
            JvmHostMethodInvoker.invokeInstance(
                method = method,
                receiver = guestReceiver,
                arguments = listOf(JvmIntValue(7)),
                heap = heap,
                identityMap = identityMap,
            )
        }

        assertIs<IllegalStateException>(exception.hostThrowable)
        assertSame(exception.hostThrowable, exception.cause)
        assertEquals("java/lang/IllegalStateException", heap.get(exception.guestThrowable).className)
    }

    @Test
    fun `instance host method throwable records opaque failed boundary event`() {
        val heap = JvmHeap()
        val hostReceiver = ThrowingHostFixture()
        val guestReceiver = heap.allocateObject("me/moeyinlo/visualize/jvm/host/JvmHostExceptionTranslationTest\$ThrowingHostFixture")
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(guestReceiver, hostReceiver)
        val recorder = JvmHostBoundaryEventRecorder()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(ThrowingHostFixture::class.java)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "throwInstance",
            descriptor = "(I)I",
        )

        assertFailsWith<JvmHostTranslatedException> {
            JvmHostMethodInvoker.invokeInstance(
                method = method,
                receiver = guestReceiver,
                arguments = listOf(JvmIntValue(7)),
                heap = heap,
                identityMap = identityMap,
                boundaryEvents = recorder,
            )
        }

        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = mirror.guestInternalName,
                    methodName = "throwInstance",
                    descriptor = "(I)I",
                    detail = "instance args=1",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Failed,
                    className = mirror.guestInternalName,
                    methodName = "throwInstance",
                    descriptor = "(I)I",
                    detail = "translated=java.lang.IllegalStateException",
                ),
            ),
            recorder.snapshots(),
        )
    }

    class ThrowingHostFixture {
        fun throwInstance(value: Int): Int {
            throw IllegalStateException("bad instance $value")
        }

        companion object {
            @JvmStatic
            fun throwStatic() {
                throw IllegalArgumentException("bad static")
            }
        }
    }
}
