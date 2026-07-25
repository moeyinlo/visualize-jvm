package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmHostBoundaryEventTest {
    @Test
    fun `records delegated and returned events for static host method calls`() {
        val heap = JvmHeap()
        val recorder = JvmHostBoundaryEventRecorder()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostBoundaryFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "add",
            descriptor = "(II)I",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(JvmIntValue(2), JvmIntValue(3)),
            heap = heap,
            boundaryEvents = recorder,
        )

        assertEquals(JvmIntValue(5), result)
        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = mirror.guestInternalName,
                    methodName = "add",
                    descriptor = "(II)I",
                    detail = "static args=2",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Returned,
                    className = mirror.guestInternalName,
                    methodName = "add",
                    descriptor = "(II)I",
                    detail = "return=int",
                ),
            ),
            recorder.snapshots(),
        )
    }

    @Test
    fun `records delegated and returned events for instance host method calls`() {
        val heap = JvmHeap()
        val recorder = JvmHostBoundaryEventRecorder()
        val identityMap = JvmHostIdentityMap()
        val hostReceiver = HostBoundaryFixture()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostBoundaryFixture::class.java)
        val guestReceiver = heap.allocateObject(mirror.guestInternalName)
        identityMap.bind(guestReceiver, hostReceiver)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "scale",
            descriptor = "(I)I",
        )

        val result = JvmHostMethodInvoker.invokeInstance(
            method = method,
            receiver = guestReceiver,
            arguments = listOf(JvmIntValue(4)),
            heap = heap,
            identityMap = identityMap,
            boundaryEvents = recorder,
        )

        assertEquals(JvmIntValue(12), result)
        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = mirror.guestInternalName,
                    methodName = "scale",
                    descriptor = "(I)I",
                    detail = "instance args=1",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Returned,
                    className = mirror.guestInternalName,
                    methodName = "scale",
                    descriptor = "(I)I",
                    detail = "return=int",
                ),
            ),
            recorder.snapshots(),
        )
    }

    @Test
    fun `records delegated and failed events for translated host throwables`() {
        val heap = JvmHeap()
        val recorder = JvmHostBoundaryEventRecorder()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostBoundaryFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "fail",
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
                    methodName = "fail",
                    descriptor = "()V",
                    detail = "static args=0",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Failed,
                    className = mirror.guestInternalName,
                    methodName = "fail",
                    descriptor = "()V",
                    detail = "translated=java.lang.IllegalArgumentException",
                ),
            ),
            recorder.snapshots(),
        )
    }

    class HostBoundaryFixture {
        fun scale(value: Int): Int = value * 3

        companion object {
            @JvmStatic
            fun add(left: Int, right: Int): Int = left + right

            @JvmStatic
            fun fail() {
                throw IllegalArgumentException("boom")
            }
        }
    }
}