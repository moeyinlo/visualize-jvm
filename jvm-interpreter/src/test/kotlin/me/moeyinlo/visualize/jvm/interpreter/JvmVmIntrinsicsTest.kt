package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class JvmVmIntrinsicsTest {
    @Test
    fun `Object getClass intrinsic returns an interned guest class mirror for the receiver class`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectGetClassMethod())
            ?: error("Object.getClass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        val classMirror = result as JvmObjectReferenceValue
        assertEquals(classMirror, secondResult)
        val classMirrorObject = heap.get(classMirror)
        assertEquals("java/lang/Class", classMirrorObject.className)
        assertEquals(JvmClassPayload("Example"), classMirrorObject.payload)
    }

    @Test
    fun `Object getClass intrinsic rejects missing receivers`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectGetClassMethod())
            ?: error("Object.getClass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object hashCode intrinsic returns a stable identity hash for guest objects`() {
        val heap = JvmHeap()
        val firstReceiver = heap.allocateObject("Example")
        val secondReceiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectHashCodeMethod())
            ?: error("Object.hashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val firstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = firstReceiver, arguments = emptyList()),
        )
        val repeatedFirstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = firstReceiver, arguments = emptyList()),
        )
        val secondHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = secondReceiver, arguments = emptyList()),
        )

        assertEquals(firstHash, repeatedFirstHash)
        assertNotEquals(firstHash, secondHash)
        assertEquals(JvmIntValue::class, firstHash!!::class)
    }

    @Test
    fun `Object hashCode intrinsic rejects missing receivers`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectHashCodeMethod())
            ?: error("Object.hashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object clone intrinsic shallow clones Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val field = JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I")
        heap.putInstanceField(receiver, field, JvmIntValue(42))
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        interfaceNames = listOf("java/lang/Cloneable"),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        assertEquals("Example", heap.get(result).className)
        assertEquals(JvmIntValue(42), heap.getInstanceField(result, field))
    }

    @Test
    fun `Object clone intrinsic shallow clones guest arrays`() {
        val heap = JvmHeap()
        val receiver = heap.allocateIntArray(2)
        val originalPayload = heap.get(receiver).payload as JvmIntArrayPayload
        originalPayload.elements[0] = 3
        originalPayload.elements[1] = 4
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        val clonedPayload = heap.get(result).payload as JvmIntArrayPayload
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
        originalPayload.elements[1] = 9
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
    }

    @Test
    fun `Object clone intrinsic rejects non Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(listOf(JvmClassDefinition(internalName = "Example"))),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object wait intrinsic releases the receiver monitor and records the current thread as waiting`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "main")
        monitors.enter(receiver, threadId = "main")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectWaitLongMethod())
            ?: error("Object.wait intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "main",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmLongValue(0L))),
        )

        assertEquals(null, result)
        assertEquals(0, monitors.holdCount(receiver, threadId = "main"))
        assertEquals(listOf("main"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notify intrinsic wakes one waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyMethod())
            ?: error("Object.notify intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(listOf("second"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notifyAll intrinsic wakes every waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyAllMethod())
            ?: error("Object.notifyAll intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(emptyList(), monitors.waitingThreads(receiver))
    }

    private fun objectGetClassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "getClass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )

    private fun objectHashCodeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "hashCode",
        descriptor = "()I",
        isStatic = false,
        isNative = true,
    )

    private fun objectCloneMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "clone",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun objectWaitLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "(J)V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notify",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyAllMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notifyAll",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )
}
