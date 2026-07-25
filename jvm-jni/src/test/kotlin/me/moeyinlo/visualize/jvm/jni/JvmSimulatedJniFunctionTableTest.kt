package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
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

    @Test
    fun `function table delegates exception helpers to one simulated JNI environment`() {
        val reported = mutableListOf<String>()
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "java/lang/Throwable", superclassName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "java/lang/IllegalArgumentException",
                        superclassName = "java/lang/Throwable",
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            exceptionReporter = { text -> reported += text },
        )
        val functions = environment.functions
        val throwableClassHandle = functions.findClass("java/lang/IllegalArgumentException")

        assertEquals(false, functions.exceptionCheck())
        assertEquals(null, functions.exceptionOccurred())
        assertEquals(0, functions.throwNew(throwableClassHandle, "bad argument"))

        val pendingHandle = functions.exceptionOccurred()
        val pendingReference = handles.resolveObject(pendingHandle!!)
        val detailMessageField = JvmFieldReference(
            ownerClassName = "java/lang/Throwable",
            name = "detailMessage",
            descriptor = "Ljava/lang/String;",
        )
        val detailMessageReference =
            heap.getInstanceField(pendingReference, detailMessageField) as JvmObjectReferenceValue

        assertEquals("java/lang/IllegalArgumentException", heap.get(pendingReference).className)
        assertEquals(JvmStringPayload("bad argument"), heap.get(detailMessageReference).payload)
        assertEquals(true, functions.exceptionCheck())

        functions.exceptionDescribe()
        assertEquals(listOf("java/lang/IllegalArgumentException: bad argument"), reported)
        assertEquals(true, functions.exceptionCheck())

        functions.exceptionClear()
        assertEquals(false, functions.exceptionCheck())
        assertEquals(null, functions.exceptionOccurred())

        val throwableReference = heap.allocateObject("java/lang/IllegalArgumentException")
        val throwableHandle = handles.newObjectHandle(throwableReference)
        assertEquals(0, functions.throwObject(throwableHandle))
        assertEquals(throwableReference, handles.resolveObject(functions.exceptionOccurred()!!))

        val fatal = assertFailsWith<JvmJniFatalError> {
            functions.fatalError("native invariant failed")
        }
        assertEquals("native invariant failed", fatal.message)
    }
}
