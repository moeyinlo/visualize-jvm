package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionMode
import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionPolicy
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmJdkHostDelegationTest {
    @Test
    fun `default policy delegates platform classes that host loader can mirror`() {
        val policy = JvmClassExecutionPolicy.Default
        val loader = JvmIsolatedHostClassLoader()

        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("java/lang/String"))
        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("java/util/ArrayList"))

        val stringMirror = loader.loadClassMirror("java/lang/String")
        val listMirror = loader.loadClassMirror("java/util/ArrayList")

        assertEquals(String::class.java, stringMirror.hostClass)
        assertEquals(java.util.ArrayList::class.java, listMirror.hostClass)
    }

    @Test
    fun `invokes delegated JDK static methods through host bridge`() {
        val heap = JvmHeap()
        val recorder = JvmHostBoundaryEventRecorder()
        val mirror = JvmIsolatedHostClassLoader().loadClassMirror("java/lang/Integer")
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "parseInt",
            descriptor = "(Ljava/lang/String;)I",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(heap.allocateString("42")),
            heap = heap,
            boundaryEvents = recorder,
        )

        assertEquals(JvmIntValue(42), result)
        assertEquals(
            listOf(
                JvmHostBoundaryEventSnapshot(
                    sequence = 1,
                    action = JvmHostBoundaryAction.Delegated,
                    className = "java/lang/Integer",
                    methodName = "parseInt",
                    descriptor = "(Ljava/lang/String;)I",
                    detail = "static args=1",
                ),
                JvmHostBoundaryEventSnapshot(
                    sequence = 2,
                    action = JvmHostBoundaryAction.Returned,
                    className = "java/lang/Integer",
                    methodName = "parseInt",
                    descriptor = "(Ljava/lang/String;)I",
                    detail = "return=int",
                ),
            ),
            recorder.snapshots(),
        )
    }

    @Test
    fun `invokes delegated JDK instance methods through host bridge`() {
        val heap = JvmHeap()
        val mirror = JvmIsolatedHostClassLoader().loadClassMirror("java/lang/String")
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "substring",
            descriptor = "(II)Ljava/lang/String;",
        )

        val result = JvmHostMethodInvoker.invokeInstance(
            method = method,
            receiver = heap.allocateString("visualize"),
            arguments = listOf(JvmIntValue(1), JvmIntValue(4)),
            heap = heap,
        )

        val reference = result as JvmObjectReferenceValue
        assertEquals(JvmStringPayload("isu"), heap.get(reference).payload)
    }

    @Test
    fun `reads delegated JDK static fields through host bridge`() {
        val heap = JvmHeap()
        val mirror = JvmIsolatedHostClassLoader().loadClassMirror("java/lang/Integer")
        val field = JvmHostFieldResolver.resolveStaticField(
            owner = mirror,
            name = "MAX_VALUE",
            descriptor = "I",
        )

        val result = JvmHostFieldAccessor.getStatic(
            field = field,
            heap = heap,
        )

        assertEquals(JvmIntValue(Int.MAX_VALUE), result)
    }
}