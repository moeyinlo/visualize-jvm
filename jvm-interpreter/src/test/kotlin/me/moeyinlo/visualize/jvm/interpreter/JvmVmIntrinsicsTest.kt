package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
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
}
