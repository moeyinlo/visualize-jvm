package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
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

    @Test
    fun `function table delegates class member lookup helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(name = "baseValue", descriptor = "()I", isStatic = false),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Example",
                        superclassName = "Base",
                        fields = listOf(
                            JvmFieldDefinition(name = "count", descriptor = "I", isStatic = false),
                            JvmFieldDefinition(name = "total", descriptor = "J", isStatic = true),
                        ),
                        methods = listOf(
                            JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false),
                            JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions

        val classHandle = functions.findClass("Example")
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val objectClassHandle = functions.getObjectClass(objectHandle)
        val baseClassHandle = functions.findClass("Base")
        val methodHandle = functions.getMethodId(classHandle, "value", "()I")
        val inheritedMethodHandle = functions.getMethodId(classHandle, "baseValue", "()I")
        val staticMethodHandle = functions.getStaticMethodId(classHandle, "answer", "()I")
        val fieldHandle = functions.getFieldId(classHandle, "count", "I")
        val staticFieldHandle = functions.getStaticFieldId(classHandle, "total", "J")

        assertEquals("Example", handles.resolveClass(classHandle))
        assertEquals("Example", handles.resolveClass(objectClassHandle))
        assertEquals(true, functions.isInstanceOf(objectHandle, baseClassHandle))
        assertEquals(true, functions.isInstanceOf(null, baseClassHandle))
        assertEquals(
            JvmResolvedMethod(ownerClassName = "Example", name = "value", descriptor = "()I", isStatic = false),
            handles.resolveMethodId(methodHandle),
        )
        assertEquals(
            JvmResolvedMethod(ownerClassName = "Base", name = "baseValue", descriptor = "()I", isStatic = false),
            handles.resolveMethodId(inheritedMethodHandle),
        )
        assertEquals(
            JvmResolvedMethod(ownerClassName = "Example", name = "answer", descriptor = "()I", isStatic = true),
            handles.resolveMethodId(staticMethodHandle),
        )
        assertEquals(
            JvmResolvedField(ownerClassName = "Example", name = "count", descriptor = "I", isStatic = false),
            handles.resolveFieldId(fieldHandle),
        )
        assertEquals(
            JvmResolvedField(ownerClassName = "Example", name = "total", descriptor = "J", isStatic = true),
            handles.resolveFieldId(staticFieldHandle),
        )
    }
}
