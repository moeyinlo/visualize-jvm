package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmValue
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
    fun `function table delegates int field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "count", descriptor = "I", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "count", "I")

        assertEquals(0, functions.getIntField(objectHandle, fieldHandle))

        functions.setIntField(objectHandle, fieldHandle, 42)

        assertEquals(42, functions.getIntField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates long field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "total", descriptor = "J", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "total", "J")

        assertEquals(0L, functions.getLongField(objectHandle, fieldHandle))

        functions.setLongField(objectHandle, fieldHandle, 4_294_967_296L)

        assertEquals(4_294_967_296L, functions.getLongField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates float field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "ratio", descriptor = "F", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "ratio", "F")

        assertEquals(0.0f, functions.getFloatField(objectHandle, fieldHandle))

        functions.setFloatField(objectHandle, fieldHandle, 1.5f)

        assertEquals(1.5f, functions.getFloatField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates double field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "score", descriptor = "D", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "score", "D")

        assertEquals(0.0, functions.getDoubleField(objectHandle, fieldHandle))

        functions.setDoubleField(objectHandle, fieldHandle, 2.25)

        assertEquals(2.25, functions.getDoubleField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates object field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "Holder",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "LExample;",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions
        val holderClassHandle = functions.findClass("Holder")
        val holderHandle = handles.newObjectHandle(heap.allocateObject("Holder"))
        val valueReference = heap.allocateObject("Example")
        val valueHandle = handles.newObjectHandle(valueReference)
        val fieldHandle = functions.getFieldId(holderClassHandle, "value", "LExample;")

        assertEquals(null, functions.getObjectField(holderHandle, fieldHandle))

        functions.setObjectField(holderHandle, fieldHandle, valueHandle)

        val readHandle = functions.getObjectField(holderHandle, fieldHandle)
        assertEquals(valueReference, handles.resolveObject(readHandle!!))

        functions.setObjectField(holderHandle, fieldHandle, null)

        assertEquals(null, functions.getObjectField(holderHandle, fieldHandle))
    }

    @Test
    fun `function table delegates boolean field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "enabled", descriptor = "Z", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "enabled", "Z")

        assertEquals(false, functions.getBooleanField(objectHandle, fieldHandle))

        functions.setBooleanField(objectHandle, fieldHandle, true)

        assertEquals(true, functions.getBooleanField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates byte field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "level", descriptor = "B", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "level", "B")

        assertEquals(0, functions.getByteField(objectHandle, fieldHandle))

        functions.setByteField(objectHandle, fieldHandle, -7)

        assertEquals(-7, functions.getByteField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates char field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "letter", descriptor = "C", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "letter", "C")

        assertEquals(0, functions.getCharField(objectHandle, fieldHandle))

        functions.setCharField(objectHandle, fieldHandle, 'J'.code)

        assertEquals('J'.code, functions.getCharField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates short field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "delta", descriptor = "S", isStatic = false),
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
        val fieldHandle = functions.getFieldId(classHandle, "delta", "S")

        assertEquals(0, functions.getShortField(objectHandle, fieldHandle))

        functions.setShortField(objectHandle, fieldHandle, 1024)

        assertEquals(1024, functions.getShortField(objectHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static int field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "count", descriptor = "I", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "count", "I")

        assertEquals(0, functions.getStaticIntField(classHandle, fieldHandle))

        functions.setStaticIntField(classHandle, fieldHandle, 99)

        assertEquals(99, functions.getStaticIntField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static long field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "total", descriptor = "J", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "total", "J")

        assertEquals(0L, functions.getStaticLongField(classHandle, fieldHandle))

        functions.setStaticLongField(classHandle, fieldHandle, 4_294_967_296L)

        assertEquals(4_294_967_296L, functions.getStaticLongField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static float field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "ratio", descriptor = "F", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "ratio", "F")

        assertEquals(0.0f, functions.getStaticFloatField(classHandle, fieldHandle))

        functions.setStaticFloatField(classHandle, fieldHandle, 1.5f)

        assertEquals(1.5f, functions.getStaticFloatField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static double field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "score", descriptor = "D", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "score", "D")

        assertEquals(0.0, functions.getStaticDoubleField(classHandle, fieldHandle))

        functions.setStaticDoubleField(classHandle, fieldHandle, 2.25)

        assertEquals(2.25, functions.getStaticDoubleField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static object field helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "Holder",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "LExample;",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions
        val holderClassHandle = functions.findClass("Holder")
        val valueReference = heap.allocateObject("Example")
        val valueHandle = handles.newObjectHandle(valueReference)
        val fieldHandle = functions.getStaticFieldId(holderClassHandle, "value", "LExample;")

        assertEquals(null, functions.getStaticObjectField(holderClassHandle, fieldHandle))

        functions.setStaticObjectField(holderClassHandle, fieldHandle, valueHandle)

        val readHandle = functions.getStaticObjectField(holderClassHandle, fieldHandle)
        assertEquals(valueReference, handles.resolveObject(readHandle!!))

        functions.setStaticObjectField(holderClassHandle, fieldHandle, null)

        assertEquals(null, functions.getStaticObjectField(holderClassHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static boolean field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "enabled", descriptor = "Z", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "enabled", "Z")

        assertEquals(false, functions.getStaticBooleanField(classHandle, fieldHandle))

        functions.setStaticBooleanField(classHandle, fieldHandle, true)

        assertEquals(true, functions.getStaticBooleanField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static byte field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "level", descriptor = "B", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "level", "B")

        assertEquals(0, functions.getStaticByteField(classHandle, fieldHandle))

        functions.setStaticByteField(classHandle, fieldHandle, -7)

        assertEquals(-7, functions.getStaticByteField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static char field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "letter", descriptor = "C", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "letter", "C")

        assertEquals(0, functions.getStaticCharField(classHandle, fieldHandle))

        functions.setStaticCharField(classHandle, fieldHandle, 'J'.code)

        assertEquals('J'.code, functions.getStaticCharField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates static short field helpers to one simulated JNI environment`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "delta", descriptor = "S", isStatic = true),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val fieldHandle = functions.getStaticFieldId(classHandle, "delta", "S")

        assertEquals(0, functions.getStaticShortField(classHandle, fieldHandle))

        functions.setStaticShortField(classHandle, fieldHandle, 1024)

        assertEquals(1024, functions.getStaticShortField(classHandle, fieldHandle))
    }

    @Test
    fun `function table delegates void method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableVoidUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "run", descriptor = "()V", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) {
                    calls += RecordedFunctionTableVoidUpcall(receiver, method, arguments)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "run", "()V")

        functions.callVoidMethod(objectHandle, methodHandle, emptyList())

        assertEquals(
            listOf(
                RecordedFunctionTableVoidUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "run",
                        descriptor = "()V",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates object method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableObjectUpcall>()
        val resultReference = heap.allocateObject("java/lang/Object")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "pick", descriptor = "()Ljava/lang/Object;", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callObjectMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmReferenceValue {
                    calls += RecordedFunctionTableObjectUpcall(receiver, method, arguments)
                    return resultReference
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "pick", "()Ljava/lang/Object;")

        val resultHandle = functions.callObjectMethod(objectHandle, methodHandle, emptyList())

        assertEquals(resultReference, handles.resolveObject(resultHandle!!))
        assertEquals(
            listOf(
                RecordedFunctionTableObjectUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "pick",
                        descriptor = "()Ljava/lang/Object;",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates boolean method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableBooleanUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "enabled", descriptor = "()Z", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callBooleanMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmBooleanValue {
                    calls += RecordedFunctionTableBooleanUpcall(receiver, method, arguments)
                    return JvmBooleanValue(true)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "enabled", "()Z")

        val result = functions.callBooleanMethod(objectHandle, methodHandle, emptyList())

        assertEquals(true, result)
        assertEquals(
            listOf(
                RecordedFunctionTableBooleanUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "enabled",
                        descriptor = "()Z",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates byte method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableByteUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "code", descriptor = "()B", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callByteMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmByteValue {
                    calls += RecordedFunctionTableByteUpcall(receiver, method, arguments)
                    return JvmByteValue(-5)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "code", "()B")

        val result = functions.callByteMethod(objectHandle, methodHandle, emptyList())

        assertEquals(-5, result)
        assertEquals(
            listOf(
                RecordedFunctionTableByteUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "code",
                        descriptor = "()B",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates char method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableCharUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "letter", descriptor = "()C", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callCharMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmCharValue {
                    calls += RecordedFunctionTableCharUpcall(receiver, method, arguments)
                    return JvmCharValue('q'.code)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "letter", "()C")

        val result = functions.callCharMethod(objectHandle, methodHandle, emptyList())

        assertEquals('q'.code, result)
        assertEquals(
            listOf(
                RecordedFunctionTableCharUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "letter",
                        descriptor = "()C",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates short method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableShortUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "delta", descriptor = "()S", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callShortMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmShortValue {
                    calls += RecordedFunctionTableShortUpcall(receiver, method, arguments)
                    return JvmShortValue(-77)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "delta", "()S")

        val result = functions.callShortMethod(objectHandle, methodHandle, emptyList())

        assertEquals(-77, result)
        assertEquals(
            listOf(
                RecordedFunctionTableShortUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "delta",
                        descriptor = "()S",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `function table delegates int method upcalls to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFunctionTableIntUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = false),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callIntMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmIntValue {
                    calls += RecordedFunctionTableIntUpcall(receiver, method, arguments)
                    return JvmIntValue(4242)
                }
            },
        )
        val functions = environment.functions
        val classHandle = functions.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = functions.getMethodId(classHandle, "answer", "()I")

        val result = functions.callIntMethod(objectHandle, methodHandle, emptyList())

        assertEquals(4242, result)
        assertEquals(
            listOf(
                RecordedFunctionTableIntUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "answer",
                        descriptor = "()I",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
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

    @Test
    fun `function table delegates int array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newIntArray(4)

        functions.setIntArrayRegion(arrayHandle, 1, intArrayOf(100, 200, 300))
        assertContentEquals(
            intArrayOf(0, 100, 200, 300),
            functions.getIntArrayElements(arrayHandle),
        )
        assertContentEquals(intArrayOf(100, 200), functions.getIntArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getIntArrayElements(arrayHandle)
        committedElements[0] = 400
        functions.releaseIntArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            intArrayOf(400, 100, 200, 300),
            functions.getIntArrayElements(arrayHandle),
        )

        val abortedElements = functions.getIntArrayElements(arrayHandle)
        abortedElements[1] = 500
        functions.releaseIntArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            intArrayOf(400, 100, 200, 300),
            functions.getIntArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates long array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newLongArray(4)

        functions.setLongArrayRegion(arrayHandle, 1, longArrayOf(1_000L, 2_000L, 3_000L))
        assertContentEquals(
            longArrayOf(0L, 1_000L, 2_000L, 3_000L),
            functions.getLongArrayElements(arrayHandle),
        )
        assertContentEquals(longArrayOf(1_000L, 2_000L), functions.getLongArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getLongArrayElements(arrayHandle)
        committedElements[0] = 4_000L
        functions.releaseLongArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            longArrayOf(4_000L, 1_000L, 2_000L, 3_000L),
            functions.getLongArrayElements(arrayHandle),
        )

        val abortedElements = functions.getLongArrayElements(arrayHandle)
        abortedElements[1] = 5_000L
        functions.releaseLongArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            longArrayOf(4_000L, 1_000L, 2_000L, 3_000L),
            functions.getLongArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates float array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newFloatArray(4)

        functions.setFloatArrayRegion(arrayHandle, 1, floatArrayOf(1.5f, 2.5f, 3.5f))
        assertContentEquals(
            floatArrayOf(0.0f, 1.5f, 2.5f, 3.5f),
            functions.getFloatArrayElements(arrayHandle),
        )
        assertContentEquals(floatArrayOf(1.5f, 2.5f), functions.getFloatArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getFloatArrayElements(arrayHandle)
        committedElements[0] = 4.5f
        functions.releaseFloatArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            floatArrayOf(4.5f, 1.5f, 2.5f, 3.5f),
            functions.getFloatArrayElements(arrayHandle),
        )

        val abortedElements = functions.getFloatArrayElements(arrayHandle)
        abortedElements[1] = 5.5f
        functions.releaseFloatArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            floatArrayOf(4.5f, 1.5f, 2.5f, 3.5f),
            functions.getFloatArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates double array element and region helpers to one simulated JNI environment`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val functions = environment.functions
        val arrayHandle = functions.newDoubleArray(4)

        functions.setDoubleArrayRegion(arrayHandle, 1, doubleArrayOf(1.25, 2.25, 3.25))
        assertContentEquals(
            doubleArrayOf(0.0, 1.25, 2.25, 3.25),
            functions.getDoubleArrayElements(arrayHandle),
        )
        assertContentEquals(doubleArrayOf(1.25, 2.25), functions.getDoubleArrayRegion(arrayHandle, 1, 2))

        val committedElements = functions.getDoubleArrayElements(arrayHandle)
        committedElements[0] = 4.25
        functions.releaseDoubleArrayElements(arrayHandle, committedElements, JvmJniArrayReleaseMode.Commit)
        assertContentEquals(
            doubleArrayOf(4.25, 1.25, 2.25, 3.25),
            functions.getDoubleArrayElements(arrayHandle),
        )

        val abortedElements = functions.getDoubleArrayElements(arrayHandle)
        abortedElements[1] = 5.25
        functions.releaseDoubleArrayElements(arrayHandle, abortedElements, JvmJniArrayReleaseMode.Abort)
        assertContentEquals(
            doubleArrayOf(4.25, 1.25, 2.25, 3.25),
            functions.getDoubleArrayElements(arrayHandle),
        )
    }

    @Test
    fun `function table delegates object array creation and element helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "Example", superclassName = "java/lang/Object"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions
        val elementClassHandle = functions.findClass("Example")
        val initialReference = heap.allocateObject("Example")
        val replacementReference = heap.allocateObject("Example")
        val initialHandle = handles.newObjectHandle(initialReference)
        val replacementHandle = handles.newObjectHandle(replacementReference)

        val arrayHandle = functions.newObjectArray(3, elementClassHandle, initialHandle)

        assertEquals(3, functions.getArrayLength(arrayHandle))
        assertEquals(initialReference, handles.resolveObject(functions.getObjectArrayElement(arrayHandle, 0)!!))
        assertEquals(initialReference, handles.resolveObject(functions.getObjectArrayElement(arrayHandle, 1)!!))
        assertEquals(initialReference, handles.resolveObject(functions.getObjectArrayElement(arrayHandle, 2)!!))

        functions.setObjectArrayElement(arrayHandle, 1, null)
        assertEquals(null, functions.getObjectArrayElement(arrayHandle, 1))

        functions.setObjectArrayElement(arrayHandle, 2, replacementHandle)
        assertEquals(replacementReference, handles.resolveObject(functions.getObjectArrayElement(arrayHandle, 2)!!))
    }

    @Test
    fun `function table delegates object array region helpers to one simulated JNI environment`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "Example", superclassName = "java/lang/Object"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val functions = environment.functions
        val elementClassHandle = functions.findClass("Example")
        val firstReference = heap.allocateObject("Example")
        val secondReference = heap.allocateObject("Example")
        val firstHandle = handles.newObjectHandle(firstReference)
        val secondHandle = handles.newObjectHandle(secondReference)
        val arrayHandle = functions.newObjectArray(4, elementClassHandle, null)

        functions.setObjectArrayRegion(arrayHandle, 1, listOf(firstHandle, null, secondHandle))

        val region = functions.getObjectArrayRegion(arrayHandle, 1, 3)
        assertEquals(3, region.size)
        assertEquals(firstReference, handles.resolveObject(region[0]!!))
        assertEquals(null, region[1])
        assertEquals(secondReference, handles.resolveObject(region[2]!!))
    }
}

private data class RecordedFunctionTableVoidUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableObjectUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableBooleanUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableByteUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableCharUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableShortUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFunctionTableIntUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)
