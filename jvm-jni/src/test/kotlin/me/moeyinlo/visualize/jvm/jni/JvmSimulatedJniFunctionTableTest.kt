package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertContentEquals
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

    @Test
    fun `function table delegates string helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/String"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions

        val utfHandle = functions.newStringUtf("\u0000JVM")
        val utfReference = handles.resolveObject(utfHandle)
        assertEquals("java/lang/String", heap.get(utfReference).className)
        assertEquals(JvmStringPayload("\u0000JVM"), heap.get(utfReference).payload)
        assertEquals(4, functions.getStringLength(utfHandle))
        assertEquals(5, functions.getStringUtfLength(utfHandle))
        assertContentEquals(charArrayOf('\u0000', 'J', 'V', 'M'), functions.getStringChars(utfHandle))
        assertContentEquals(
            byteArrayOf(0xc0.toByte(), 0x80.toByte(), 0x4a, 0x56, 0x4d),
            functions.getStringUtfChars(utfHandle),
        )

        val copiedChars = functions.getStringChars(utfHandle)
        functions.releaseStringChars(utfHandle, copiedChars)
        assertContentEquals(charArrayOf('\u0000', 'J', 'V', 'M'), copiedChars)

        val copiedUtf = functions.getStringUtfChars(utfHandle)
        functions.releaseStringUtfChars(utfHandle, copiedUtf)
        assertContentEquals(byteArrayOf(0xc0.toByte(), 0x80.toByte(), 0x4a, 0x56, 0x4d), copiedUtf)

        val utf16Handle = functions.newString(charArrayOf('A', '\ud83d', '\ude00', 'x'), 3)
        val utf16Reference = handles.resolveObject(utf16Handle)
        assertEquals(JvmStringPayload("A\ud83d\ude00"), heap.get(utf16Reference).payload)
        assertEquals(3, functions.getStringLength(utf16Handle))
    }

    @Test
    fun `function table delegates monitor helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val monitors = JvmMonitorState()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "jni-thread",
        )
        val functions = environment.functions
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        assertEquals(1, functions.monitorEnter(objectHandle))
        assertEquals(1, monitors.holdCount(objectReference, "jni-thread"))

        assertEquals(2, functions.monitorEnter(objectHandle))
        assertEquals(2, monitors.holdCount(objectReference, "jni-thread"))

        assertEquals(1, functions.monitorExit(objectHandle))
        assertEquals(1, monitors.holdCount(objectReference, "jni-thread"))

        assertEquals(0, functions.monitorExit(objectHandle))
        assertEquals(0, monitors.holdCount(objectReference, "jni-thread"))
    }

    @Test
    fun `function table delegates primitive array creation and length helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions

        val arrays: List<Pair<JvmJniHandleId, String>> = listOf(
            functions.newBooleanArray(1) to "[Z",
            functions.newByteArray(2) to "[B",
            functions.newCharArray(3) to "[C",
            functions.newShortArray(4) to "[S",
            functions.newIntArray(5) to "[I",
            functions.newLongArray(6) to "[J",
            functions.newFloatArray(7) to "[F",
            functions.newDoubleArray(8) to "[D",
        )

        arrays.forEachIndexed { index, (arrayHandle, expectedClassName) ->
            val arrayReference = handles.resolveObject(arrayHandle)
            assertEquals(expectedClassName, heap.get(arrayReference).className)
            assertEquals(index + 1, functions.getArrayLength(arrayHandle))
        }
    }

    @Test
    fun `function table delegates boolean array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newBooleanArray(4)

        functions.setBooleanArrayRegion(arrayHandle, 1, booleanArrayOf(true, false, true))
        assertContentEquals(
            booleanArrayOf(false, true, false, true),
            functions.getBooleanArrayElements(arrayHandle),
        )
        assertContentEquals(booleanArrayOf(true, false), functions.getBooleanArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getBooleanArrayElements(arrayHandle)
        committedElements[0] = true
        functions.releaseBooleanArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            booleanArrayOf(true, true, false, true),
            functions.getBooleanArrayElements(arrayHandle),
        )

        val abortedElements = functions.getBooleanArrayElements(arrayHandle)
        abortedElements[1] = false
        functions.releaseBooleanArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            booleanArrayOf(true, true, false, true),
            functions.getBooleanArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates byte array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newByteArray(4)

        functions.setByteArrayRegion(arrayHandle, 1, byteArrayOf(7, 8, 9))
        assertContentEquals(
            byteArrayOf(0, 7, 8, 9),
            functions.getByteArrayElements(arrayHandle),
        )
        assertContentEquals(byteArrayOf(7, 8), functions.getByteArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getByteArrayElements(arrayHandle)
        committedElements[0] = 6
        functions.releaseByteArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            byteArrayOf(6, 7, 8, 9),
            functions.getByteArrayElements(arrayHandle),
        )

        val abortedElements = functions.getByteArrayElements(arrayHandle)
        abortedElements[1] = 5
        functions.releaseByteArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            byteArrayOf(6, 7, 8, 9),
            functions.getByteArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates char array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newCharArray(4)

        functions.setCharArrayRegion(arrayHandle, 1, charArrayOf('j', 'v', 'm'))
        assertContentEquals(
            charArrayOf('\u0000', 'j', 'v', 'm'),
            functions.getCharArrayElements(arrayHandle),
        )
        assertContentEquals(charArrayOf('j', 'v'), functions.getCharArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getCharArrayElements(arrayHandle)
        committedElements[0] = 'J'
        functions.releaseCharArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            charArrayOf('J', 'j', 'v', 'm'),
            functions.getCharArrayElements(arrayHandle),
        )

        val abortedElements = functions.getCharArrayElements(arrayHandle)
        abortedElements[1] = 'x'
        functions.releaseCharArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            charArrayOf('J', 'j', 'v', 'm'),
            functions.getCharArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates short array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newShortArray(4)

        functions.setShortArrayRegion(arrayHandle, 1, shortArrayOf(10, 20, 30))
        assertContentEquals(
            shortArrayOf(0, 10, 20, 30),
            functions.getShortArrayElements(arrayHandle),
        )
        assertContentEquals(shortArrayOf(10, 20), functions.getShortArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getShortArrayElements(arrayHandle)
        committedElements[0] = 40
        functions.releaseShortArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            shortArrayOf(40, 10, 20, 30),
            functions.getShortArrayElements(arrayHandle),
        )

        val abortedElements = functions.getShortArrayElements(arrayHandle)
        abortedElements[1] = 50
        functions.releaseShortArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            shortArrayOf(40, 10, 20, 30),
            functions.getShortArrayElements(arrayHandle),
        )
    }
}
