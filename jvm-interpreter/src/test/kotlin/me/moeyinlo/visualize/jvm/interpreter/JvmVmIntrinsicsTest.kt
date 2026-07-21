package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    private fun objectGetClassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "getClass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )
}
