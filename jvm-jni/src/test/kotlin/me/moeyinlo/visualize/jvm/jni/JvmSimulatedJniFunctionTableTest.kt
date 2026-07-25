package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmSimulatedJniFunctionTableTest {
    @Test
    fun `function table delegates reference helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions
        val objectReference = heap.allocateObject("Example")

        assertEquals(0, functions.ensureLocalCapacity(16))
        assertEquals(16, environment.ensuredLocalCapacity)
        assertEquals(0, functions.pushLocalFrame(4))

        val scopedHandle = handles.newObjectHandle(objectReference)
        val duplicateHandle = functions.newLocalRef(scopedHandle)
        val globalHandle = functions.newGlobalRef(scopedHandle)
        val weakGlobalHandle = functions.newWeakGlobalRef(scopedHandle)

        assertEquals(true, functions.isSameObject(scopedHandle, duplicateHandle))
        assertEquals(JvmJniReferenceType.Local, functions.getObjectRefType(scopedHandle))
        assertEquals(JvmJniReferenceType.Local, functions.getObjectRefType(duplicateHandle))
        assertEquals(JvmJniReferenceType.Global, functions.getObjectRefType(globalHandle))
        assertEquals(JvmJniReferenceType.WeakGlobal, functions.getObjectRefType(weakGlobalHandle))

        val reboundHandle = functions.popLocalFrame(scopedHandle)

        assertEquals(0, environment.localFrameDepth)
        assertEquals(JvmJniReferenceType.Invalid, functions.getObjectRefType(scopedHandle))
        assertEquals(JvmJniReferenceType.Invalid, functions.getObjectRefType(duplicateHandle))
        assertEquals(JvmJniReferenceType.Local, functions.getObjectRefType(reboundHandle))
        assertEquals(objectReference, handles.resolveObject(reboundHandle!!))
        assertEquals(objectReference, handles.resolveObject(globalHandle!!))
        assertEquals(objectReference, handles.resolveObject(weakGlobalHandle!!))

        functions.deleteLocalRef(reboundHandle)
        functions.deleteGlobalRef(globalHandle)
        functions.deleteWeakGlobalRef(weakGlobalHandle)

        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(reboundHandle)
        }
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(globalHandle)
        }
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(weakGlobalHandle)
        }
    }
}
