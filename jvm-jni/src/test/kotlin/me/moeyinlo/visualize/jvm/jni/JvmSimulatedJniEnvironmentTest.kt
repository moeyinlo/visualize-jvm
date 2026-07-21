package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmSimulatedJniEnvironmentTest {
    @Test
    fun `FindClass returns a class handle for loaded guest classes`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )

        val classHandle = environment.findClass("Example")

        assertEquals("Example", handles.resolveClass(classHandle))
    }

    @Test
    fun `FindClass throws guest NoClassDefFoundError when the class is not loaded`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            environment.findClass("Missing")
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("Missing", exception.message)
    }

    @Test
    fun `GetStaticMethodID returns a method handle for loaded static guest methods`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "()I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val methodHandle = environment.getStaticMethodId(classHandle, "answer", "()I")

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            handles.resolveMethodId(methodHandle),
        )
    }

    @Test
    fun `GetStaticMethodID throws guest NoSuchMethodError for missing or non static methods`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "instanceOnly",
                                descriptor = "()I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        assertFailsWith<JvmNoSuchMethodError> {
            environment.getStaticMethodId(classHandle, "missing", "()I")
        }
        assertFailsWith<JvmNoSuchMethodError> {
            environment.getStaticMethodId(classHandle, "instanceOnly", "()I")
        }
    }

    @Test
    fun `GetMethodID returns a method handle for loaded instance guest methods`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "value",
                                descriptor = "()I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val methodHandle = environment.getMethodId(classHandle, "value", "()I")

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "value",
                descriptor = "()I",
                isStatic = false,
            ),
            handles.resolveMethodId(methodHandle),
        )
    }

    @Test
    fun `GetMethodID throws guest NoSuchMethodError for missing or static methods`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "staticOnly",
                                descriptor = "()I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        assertFailsWith<JvmNoSuchMethodError> {
            environment.getMethodId(classHandle, "missing", "()I")
        }
        assertFailsWith<JvmNoSuchMethodError> {
            environment.getMethodId(classHandle, "staticOnly", "()I")
        }
    }

    @Test
    fun `GetObjectClass returns runtime class handle for guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        val classHandle = environment.getObjectClass(objectHandle)

        assertEquals("Example", handles.resolveClass(classHandle))
    }

    @Test
    fun `IsInstanceOf returns true when a guest object is assignable to the target class`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Base"),
                    JvmClassDefinition(internalName = "Example", superclassName = "Base"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Base")

        val result = environment.isInstanceOf(objectHandle, classHandle)

        assertEquals(true, result)
    }

    @Test
    fun `IsInstanceOf returns false when a guest object is not assignable to the target class`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(internalName = "Unrelated"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Unrelated")

        val result = environment.isInstanceOf(objectHandle, classHandle)

        assertEquals(false, result)
    }

    @Test
    fun `IsInstanceOf returns true for null guest object handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val result = environment.isInstanceOf(null, classHandle)

        assertEquals(true, result)
    }

    @Test
    fun `GetFieldID returns a field handle for loaded instance guest fields`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertEquals(
            JvmResolvedField(
                ownerClassName = "Example",
                name = "value",
                descriptor = "I",
                isStatic = false,
            ),
            handles.resolveFieldId(fieldHandle),
        )
    }

    @Test
    fun `GetFieldID throws guest NoSuchFieldError for missing or static fields`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "staticOnly",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        assertFailsWith<JvmNoSuchFieldError> {
            environment.getFieldId(classHandle, "missing", "I")
        }
        assertFailsWith<JvmNoSuchFieldError> {
            environment.getFieldId(classHandle, "staticOnly", "I")
        }
    }

    @Test
    fun `GetStaticFieldID returns a field handle for loaded static guest fields`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertEquals(
            JvmResolvedField(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
                isStatic = true,
            ),
            handles.resolveFieldId(fieldHandle),
        )
    }

    @Test
    fun `GetStaticFieldID throws guest NoSuchFieldError for missing or instance fields`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "instanceOnly",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        assertFailsWith<JvmNoSuchFieldError> {
            environment.getStaticFieldId(classHandle, "missing", "I")
        }
        assertFailsWith<JvmNoSuchFieldError> {
            environment.getStaticFieldId(classHandle, "instanceOnly", "I")
        }
    }

    @Test
    fun `GetIntField reads a stored guest int instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "value", descriptor = "I"),
            JvmIntValue(42),
        )

        val result = environment.getIntField(objectHandle, fieldHandle)

        assertEquals(42, result)
    }

    @Test
    fun `GetIntField reads default zero for an unwritten guest int instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        val result = environment.getIntField(objectHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetIntField rejects non int guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "wide", "J")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getIntField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetIntField writes a guest int instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        environment.setIntField(objectHandle, fieldHandle, 73)

        assertEquals(
            JvmIntValue(73),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "value", descriptor = "I"),
            ),
        )
    }

    @Test
    fun `SetIntField rejects non int guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "wide", "J")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setIntField(objectHandle, fieldHandle, 73)
        }
    }

    @Test
    fun `GetObjectField reads a stored guest object instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val childReference = heap.allocateObject("Child")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "child", "LChild;")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            childReference,
        )

        val resultHandle = environment.getObjectField(objectHandle, fieldHandle)

        assertEquals(childReference, handles.resolveObject(resultHandle!!))
    }

    @Test
    fun `GetObjectField returns null for an unwritten guest object instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "child", "LChild;")

        val resultHandle = environment.getObjectField(objectHandle, fieldHandle)

        assertEquals(null, resultHandle)
    }

    @Test
    fun `GetObjectField rejects non reference guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")
        heap.putInstanceField(
            handles.resolveObject(objectHandle),
            JvmFieldReference(ownerClassName = "Example", name = "value", descriptor = "I"),
            JvmIntValue(1),
        )

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getObjectField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetObjectField writes a guest object instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val childReference = heap.allocateObject("Child")
        val objectHandle = handles.newObjectHandle(objectReference)
        val childHandle = handles.newObjectHandle(childReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "child", "LChild;")

        environment.setObjectField(objectHandle, fieldHandle, childHandle)

        assertEquals(
            childReference,
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            ),
        )
    }

    @Test
    fun `SetObjectField writes guest null to an object instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "child", "LChild;")

        environment.setObjectField(objectHandle, fieldHandle, null)

        assertEquals(
            JvmNullValue,
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            ),
        )
    }

    @Test
    fun `SetObjectField rejects non reference guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val valueHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setObjectField(objectHandle, fieldHandle, valueHandle)
        }
    }

    @Test
    fun `GetLongField reads a stored guest long instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "wide", "J")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "wide", descriptor = "J"),
            JvmLongValue(4_294_967_296L),
        )

        val result = environment.getLongField(objectHandle, fieldHandle)

        assertEquals(4_294_967_296L, result)
    }

    @Test
    fun `GetLongField reads default zero for an unwritten guest long instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "wide", "J")

        val result = environment.getLongField(objectHandle, fieldHandle)

        assertEquals(0L, result)
    }

    @Test
    fun `GetLongField rejects non long guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getLongField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetLongField writes a guest long instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "wide", "J")

        environment.setLongField(objectHandle, fieldHandle, 9_876_543_210L)

        assertEquals(
            JvmLongValue(9_876_543_210L),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "wide", descriptor = "J"),
            ),
        )
    }

    @Test
    fun `SetLongField rejects non long guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setLongField(objectHandle, fieldHandle, 9_876_543_210L)
        }
    }

    @Test
    fun `GetFloatField reads a stored guest float instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "ratio", "F")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "ratio", descriptor = "F"),
            JvmFloatValue(1.5f),
        )

        val result = environment.getFloatField(objectHandle, fieldHandle)

        assertEquals(1.5f, result)
    }

    @Test
    fun `GetFloatField reads default zero for an unwritten guest float instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "ratio", "F")

        val result = environment.getFloatField(objectHandle, fieldHandle)

        assertEquals(0.0f, result)
    }

    @Test
    fun `GetFloatField rejects non float guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getFloatField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetFloatField writes a guest float instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "ratio", "F")

        environment.setFloatField(objectHandle, fieldHandle, 2.5f)

        assertEquals(
            JvmFloatValue(2.5f),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "ratio", descriptor = "F"),
            ),
        )
    }

    @Test
    fun `SetFloatField rejects non float guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setFloatField(objectHandle, fieldHandle, 2.5f)
        }
    }

    @Test
    fun `GetDoubleField reads a stored guest double instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "precise",
                                descriptor = "D",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "precise", "D")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "precise", descriptor = "D"),
            JvmDoubleValue(3.25),
        )

        val result = environment.getDoubleField(objectHandle, fieldHandle)

        assertEquals(3.25, result)
    }

    @Test
    fun `GetDoubleField reads default zero for an unwritten guest double instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "precise",
                                descriptor = "D",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "precise", "D")

        val result = environment.getDoubleField(objectHandle, fieldHandle)

        assertEquals(0.0, result)
    }

    @Test
    fun `GetDoubleField rejects non double guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "ratio", "F")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getDoubleField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetDoubleField writes a guest double instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "precise",
                                descriptor = "D",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "precise", "D")

        environment.setDoubleField(objectHandle, fieldHandle, 6.5)

        assertEquals(
            JvmDoubleValue(6.5),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "precise", descriptor = "D"),
            ),
        )
    }

    @Test
    fun `SetDoubleField rejects non double guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "ratio", "F")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setDoubleField(objectHandle, fieldHandle, 6.5)
        }
    }

    @Test
    fun `GetBooleanField reads a stored guest boolean instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "enabled",
                                descriptor = "Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "enabled", "Z")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "enabled", descriptor = "Z"),
            JvmBooleanValue(true),
        )

        val result = environment.getBooleanField(objectHandle, fieldHandle)

        assertEquals(true, result)
    }

    @Test
    fun `GetBooleanField reads default false for an unwritten guest boolean instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "enabled",
                                descriptor = "Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "enabled", "Z")

        val result = environment.getBooleanField(objectHandle, fieldHandle)

        assertEquals(false, result)
    }

    @Test
    fun `GetBooleanField rejects non boolean guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getBooleanField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetBooleanField writes a guest boolean instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "enabled",
                                descriptor = "Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "enabled", "Z")

        environment.setBooleanField(objectHandle, fieldHandle, true)

        assertEquals(
            JvmBooleanValue(true),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "enabled", descriptor = "Z"),
            ),
        )
    }

    @Test
    fun `SetBooleanField writes false to a guest boolean instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "enabled",
                                descriptor = "Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "enabled", "Z")

        environment.setBooleanField(objectHandle, fieldHandle, false)

        assertEquals(
            JvmBooleanValue(false),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "enabled", descriptor = "Z"),
            ),
        )
    }

    @Test
    fun `SetBooleanField rejects non boolean guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setBooleanField(objectHandle, fieldHandle, true)
        }
    }

    @Test
    fun `GetByteField reads a stored guest byte instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "small",
                                descriptor = "B",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "small", "B")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "small", descriptor = "B"),
            JvmByteValue(-7),
        )

        val result = environment.getByteField(objectHandle, fieldHandle)

        assertEquals(-7, result)
    }

    @Test
    fun `GetByteField reads default zero for an unwritten guest byte instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "small",
                                descriptor = "B",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "small", "B")

        val result = environment.getByteField(objectHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetByteField rejects non byte guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getByteField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetByteField writes a guest byte instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "small",
                                descriptor = "B",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "small", "B")

        environment.setByteField(objectHandle, fieldHandle, -8)

        assertEquals(
            JvmByteValue(-8),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "small", descriptor = "B"),
            ),
        )
    }

    @Test
    fun `SetByteField rejects non byte guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setByteField(objectHandle, fieldHandle, -8)
        }
    }

    @Test
    fun `GetCharField reads a stored guest char instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "letter", "C")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "letter", descriptor = "C"),
            JvmCharValue('λ'.code),
        )

        val result = environment.getCharField(objectHandle, fieldHandle)

        assertEquals('λ'.code, result)
    }

    @Test
    fun `GetCharField reads default zero for an unwritten guest char instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "letter", "C")

        val result = environment.getCharField(objectHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetCharField rejects non char guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getCharField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetCharField writes a guest char instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "letter", "C")

        environment.setCharField(objectHandle, fieldHandle, '界'.code)

        assertEquals(
            JvmCharValue('界'.code),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "letter", descriptor = "C"),
            ),
        )
    }

    @Test
    fun `SetCharField rejects non char guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setCharField(objectHandle, fieldHandle, '界'.code)
        }
    }

    @Test
    fun `GetShortField reads a stored guest short instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "narrow", "S")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "S"),
            JvmShortValue(-1234),
        )

        val result = environment.getShortField(objectHandle, fieldHandle)

        assertEquals(-1234, result)
    }

    @Test
    fun `GetShortField reads default zero for an unwritten guest short instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "narrow", "S")

        val result = environment.getShortField(objectHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetShortField rejects non short guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getShortField(objectHandle, fieldHandle)
        }
    }

    @Test
    fun `SetShortField writes a guest short instance field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "narrow", "S")

        environment.setShortField(objectHandle, fieldHandle, -5678)

        assertEquals(
            JvmShortValue(-5678),
            heap.getInstanceField(
                objectReference,
                JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "S"),
            ),
        )
    }

    @Test
    fun `SetShortField rejects non short guest field handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "value",
                                descriptor = "I",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "value", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setShortField(objectHandle, fieldHandle, -5678)
        }
    }

    @Test
    fun `GetStaticIntField reads a stored guest static int field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I"),
            JvmIntValue(42),
        )

        val result = environment.getStaticIntField(classHandle, fieldHandle)

        assertEquals(42, result)
    }

    @Test
    fun `GetStaticIntField reads default zero for an unwritten guest static int field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        val result = environment.getStaticIntField(classHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetStaticIntField rejects non int guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wide", "J")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticIntField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticIntField writes a guest static int field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        environment.setStaticIntField(classHandle, fieldHandle, 99)

        assertEquals(
            JvmIntValue(99),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I"),
            ),
        )
    }

    @Test
    fun `SetStaticIntField rejects non int guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wide", "J")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticIntField(classHandle, fieldHandle, 99)
        }
    }

    @Test
    fun `GetStaticLongField reads a stored guest static long field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wide", "J")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "wide", descriptor = "J"),
            JvmLongValue(4_294_967_296L),
        )

        val result = environment.getStaticLongField(classHandle, fieldHandle)

        assertEquals(4_294_967_296L, result)
    }

    @Test
    fun `GetStaticLongField reads default zero for an unwritten guest static long field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wide", "J")

        val result = environment.getStaticLongField(classHandle, fieldHandle)

        assertEquals(0L, result)
    }

    @Test
    fun `GetStaticLongField rejects non long guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticLongField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticLongField writes a guest static long field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wide",
                                descriptor = "J",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wide", "J")

        environment.setStaticLongField(classHandle, fieldHandle, 9_876_543_210L)

        assertEquals(
            JvmLongValue(9_876_543_210L),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "wide", descriptor = "J"),
            ),
        )
    }

    @Test
    fun `SetStaticLongField rejects non long guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticLongField(classHandle, fieldHandle, 9_876_543_210L)
        }
    }

    @Test
    fun `GetStaticFloatField reads a stored guest static float field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "ratio", "F")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "ratio", descriptor = "F"),
            JvmFloatValue(1.5f),
        )

        val result = environment.getStaticFloatField(classHandle, fieldHandle)

        assertEquals(1.5f, result)
    }

    @Test
    fun `GetStaticFloatField reads default zero for an unwritten guest static float field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "ratio", "F")

        val result = environment.getStaticFloatField(classHandle, fieldHandle)

        assertEquals(0.0f, result)
    }

    @Test
    fun `GetStaticFloatField rejects non float guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticFloatField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticFloatField writes a guest static float field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "ratio",
                                descriptor = "F",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "ratio", "F")

        environment.setStaticFloatField(classHandle, fieldHandle, 2.5f)

        assertEquals(
            JvmFloatValue(2.5f),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "ratio", descriptor = "F"),
            ),
        )
    }

    @Test
    fun `SetStaticFloatField rejects non float guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticFloatField(classHandle, fieldHandle, 2.5f)
        }
    }

    @Test
    fun `GetStaticDoubleField reads a stored guest static double field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wideRatio",
                                descriptor = "D",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wideRatio", "D")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "wideRatio", descriptor = "D"),
            JvmDoubleValue(1.25),
        )

        val result = environment.getStaticDoubleField(classHandle, fieldHandle)

        assertEquals(1.25, result)
    }

    @Test
    fun `GetStaticDoubleField reads default zero for an unwritten guest static double field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wideRatio",
                                descriptor = "D",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wideRatio", "D")

        val result = environment.getStaticDoubleField(classHandle, fieldHandle)

        assertEquals(0.0, result)
    }

    @Test
    fun `GetStaticDoubleField rejects non double guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticDoubleField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticDoubleField writes a guest static double field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "wideRatio",
                                descriptor = "D",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "wideRatio", "D")

        environment.setStaticDoubleField(classHandle, fieldHandle, 3.5)

        assertEquals(
            JvmDoubleValue(3.5),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "wideRatio", descriptor = "D"),
            ),
        )
    }

    @Test
    fun `SetStaticDoubleField rejects non double guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticDoubleField(classHandle, fieldHandle, 3.5)
        }
    }

    @Test
    fun `GetStaticBooleanField reads a stored guest static boolean field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "flag",
                                descriptor = "Z",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "flag", "Z")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "flag", descriptor = "Z"),
            JvmBooleanValue(true),
        )

        val result = environment.getStaticBooleanField(classHandle, fieldHandle)

        assertEquals(true, result)
    }

    @Test
    fun `GetStaticBooleanField reads default false for an unwritten guest static boolean field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "flag",
                                descriptor = "Z",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "flag", "Z")

        val result = environment.getStaticBooleanField(classHandle, fieldHandle)

        assertEquals(false, result)
    }

    @Test
    fun `GetStaticBooleanField rejects non boolean guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticBooleanField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticBooleanField writes a guest static boolean field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "flag",
                                descriptor = "Z",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "flag", "Z")

        environment.setStaticBooleanField(classHandle, fieldHandle, true)

        assertEquals(
            JvmBooleanValue(true),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "flag", descriptor = "Z"),
            ),
        )
    }

    @Test
    fun `SetStaticBooleanField rejects non boolean guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticBooleanField(classHandle, fieldHandle, true)
        }
    }

    @Test
    fun `GetStaticObjectField reads a stored guest static reference field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
            handles = handles,
        )
        val childReference = heap.allocateObject("Child")
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "child", "LChild;")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            childReference,
        )

        val resultHandle = environment.getStaticObjectField(classHandle, fieldHandle)

        assertEquals(childReference, handles.resolveObject(resultHandle!!))
    }

    @Test
    fun `GetStaticObjectField reads null for an unwritten guest static reference field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "child", "LChild;")

        val resultHandle = environment.getStaticObjectField(classHandle, fieldHandle)

        assertEquals(null, resultHandle)
    }

    @Test
    fun `GetStaticObjectField rejects non reference guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticObjectField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticObjectField writes a guest static reference field`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            staticFields = staticFields,
            handles = handles,
        )
        val childReference = heap.allocateObject("Child")
        val childHandle = handles.newObjectHandle(childReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "child", "LChild;")

        environment.setStaticObjectField(classHandle, fieldHandle, childHandle)

        assertEquals(
            childReference,
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            ),
        )
    }

    @Test
    fun `SetStaticObjectField writes guest null to a static reference field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "child",
                                descriptor = "LChild;",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "child", "LChild;")

        environment.setStaticObjectField(classHandle, fieldHandle, null)

        assertEquals(
            JvmNullValue,
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;"),
            ),
        )
    }

    @Test
    fun `SetStaticObjectField rejects non reference guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticObjectField(classHandle, fieldHandle, null)
        }
    }

    @Test
    fun `GetStaticByteField reads a stored guest static byte field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "B",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "B")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "B"),
            JvmByteValue(-8),
        )

        val result = environment.getStaticByteField(classHandle, fieldHandle)

        assertEquals(-8, result)
    }

    @Test
    fun `GetStaticByteField reads default zero for an unwritten guest static byte field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "B",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "B")

        val result = environment.getStaticByteField(classHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetStaticByteField rejects non byte guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticByteField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticByteField writes a guest static byte field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "B",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "B")

        environment.setStaticByteField(classHandle, fieldHandle, -7)

        assertEquals(
            JvmByteValue(-7),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "B"),
            ),
        )
    }

    @Test
    fun `SetStaticByteField rejects non byte guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticByteField(classHandle, fieldHandle, -7)
        }
    }

    @Test
    fun `GetStaticCharField reads a stored guest static char field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "letter", "C")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "letter", descriptor = "C"),
            JvmCharValue('λ'.code),
        )

        val result = environment.getStaticCharField(classHandle, fieldHandle)

        assertEquals('λ'.code, result)
    }

    @Test
    fun `GetStaticCharField reads default zero for an unwritten guest static char field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "letter", "C")

        val result = environment.getStaticCharField(classHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetStaticCharField rejects non char guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticCharField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticCharField writes a guest static char field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "letter",
                                descriptor = "C",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "letter", "C")

        environment.setStaticCharField(classHandle, fieldHandle, '界'.code)

        assertEquals(
            JvmCharValue('界'.code),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "letter", descriptor = "C"),
            ),
        )
    }

    @Test
    fun `SetStaticCharField rejects non char guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticCharField(classHandle, fieldHandle, '界'.code)
        }
    }

    @Test
    fun `GetStaticShortField reads a stored guest static short field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "S")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "S"),
            JvmShortValue(-1234),
        )

        val result = environment.getStaticShortField(classHandle, fieldHandle)

        assertEquals(-1234, result)
    }

    @Test
    fun `GetStaticShortField reads default zero for an unwritten guest static short field`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "S")

        val result = environment.getStaticShortField(classHandle, fieldHandle)

        assertEquals(0, result)
    }

    @Test
    fun `GetStaticShortField rejects non short guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.getStaticShortField(classHandle, fieldHandle)
        }
    }

    @Test
    fun `SetStaticShortField writes a guest static short field`() {
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "narrow",
                                descriptor = "S",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            staticFields = staticFields,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "narrow", "S")

        environment.setStaticShortField(classHandle, fieldHandle, -5678)

        assertEquals(
            JvmShortValue(-5678),
            staticFields.get(
                JvmFieldReference(ownerClassName = "Example", name = "narrow", descriptor = "S"),
            ),
        )
    }

    @Test
    fun `SetStaticShortField rejects non short guest static field handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "counter",
                                descriptor = "I",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "counter", "I")

        assertFailsWith<JvmJniFieldAccessException> {
            environment.setStaticShortField(classHandle, fieldHandle, -5678)
        }
    }

    @Test
    fun `NewStringUTF allocates a guest java lang String and returns a local handle`() {
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

        val stringHandle = environment.newStringUtf("hello, \u4e16\u754c")

        val stringReference = handles.resolveObject(stringHandle)
        val stringObject = heap.get(stringReference)
        assertEquals("java/lang/String", stringObject.className)
        assertEquals(JvmStringPayload("hello, \u4e16\u754c"), stringObject.payload)
    }

    @Test
    fun `GetStringUTFLength returns modified UTF byte length for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(
            heap.allocateString("\u0000A\u00a2\u20ac\ud83d\ude00"),
        )

        val result = environment.getStringUtfLength(stringHandle)

        assertEquals(14, result)
    }

    @Test
    fun `GetStringUTFLength rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.getStringUtfLength(objectHandle)
        }
    }

    @Test
    fun `GetStringLength returns UTF-16 code unit length for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(
            heap.allocateString("A\ud83d\ude00\u0000\u754c"),
        )

        val result = environment.getStringLength(stringHandle)

        assertEquals(5, result)
    }

    @Test
    fun `GetStringLength rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.getStringLength(objectHandle)
        }
    }

    @Test
    fun `NewString allocates a guest java lang String from UTF-16 code units`() {
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
        val utf16 = charArrayOf('A', '\ud83d', '\ude00', '\u0000', '\u754c')

        val stringHandle = environment.newString(utf16, 5)

        val stringObject = heap.get(handles.resolveObject(stringHandle))
        assertEquals("java/lang/String", stringObject.className)
        assertEquals(JvmStringPayload("A\ud83d\ude00\u0000\u754c"), stringObject.payload)
    }

    @Test
    fun `NewString copies only the requested UTF-16 code unit prefix`() {
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
        val utf16 = charArrayOf('J', 'V', 'M', '!')

        val stringHandle = environment.newString(utf16, 3)

        val stringObject = heap.get(handles.resolveObject(stringHandle))
        assertEquals(JvmStringPayload("JVM"), stringObject.payload)
    }

    @Test
    fun `GetStringChars returns copied UTF-16 code units for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(
            heap.allocateString("A\ud83d\ude00\u0000\u754c"),
        )

        val result = environment.getStringChars(stringHandle)

        assertContentEquals(
            charArrayOf('A', '\ud83d', '\ude00', '\u0000', '\u754c'),
            result,
        )
    }

    @Test
    fun `GetStringChars rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.getStringChars(objectHandle)
        }
    }

    @Test
    fun `GetStringUTFChars returns copied modified UTF-8 bytes for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(
            heap.allocateString("\u0000A\u00a2\u20ac\ud83d\ude00"),
        )

        val result = environment.getStringUtfChars(stringHandle)

        assertContentEquals(
            byteArrayOf(
                0xc0.toByte(), 0x80.toByte(),
                0x41,
                0xc2.toByte(), 0xa2.toByte(),
                0xe2.toByte(), 0x82.toByte(), 0xac.toByte(),
                0xed.toByte(), 0xa0.toByte(), 0xbd.toByte(),
                0xed.toByte(), 0xb8.toByte(), 0x80.toByte(),
            ),
            result,
        )
    }

    @Test
    fun `GetStringUTFChars rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.getStringUtfChars(objectHandle)
        }
    }

    @Test
    fun `ReleaseStringChars accepts copied UTF-16 buffers for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(heap.allocateString("JVM"))
        val chars = environment.getStringChars(stringHandle)

        environment.releaseStringChars(stringHandle, chars)

        assertContentEquals(charArrayOf('J', 'V', 'M'), chars)
    }

    @Test
    fun `ReleaseStringChars rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.releaseStringChars(objectHandle, charArrayOf('x'))
        }
    }

    @Test
    fun `ReleaseStringUTFChars accepts copied modified UTF-8 buffers for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(heap.allocateString("\u0000JVM"))
        val bytes = environment.getStringUtfChars(stringHandle)

        environment.releaseStringUtfChars(stringHandle, bytes)

        assertContentEquals(
            byteArrayOf(0xc0.toByte(), 0x80.toByte(), 0x4a, 0x56, 0x4d),
            bytes,
        )
    }

    @Test
    fun `ReleaseStringUTFChars rejects non string guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniStringAccessException> {
            environment.releaseStringUtfChars(objectHandle, byteArrayOf(0x78))
        }
    }

    @Test
    fun `GetArrayLength returns primitive guest array lengths`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = handles.newObjectHandle(heap.allocateIntArray(4))
        val byteArrayHandle = handles.newObjectHandle(heap.allocateByteArray(2))

        assertEquals(4, environment.getArrayLength(intArrayHandle))
        assertEquals(2, environment.getArrayLength(byteArrayHandle))
    }

    @Test
    fun `GetArrayLength returns reference guest array lengths`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = handles.newObjectHandle(heap.allocateReferenceArray("java/lang/String", 3))

        val result = environment.getArrayLength(arrayHandle)

        assertEquals(3, result)
    }

    @Test
    fun `GetArrayLength rejects non array guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getArrayLength(objectHandle)
        }
    }

    @Test
    fun `NewBooleanArray allocates a false filled guest boolean array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newBooleanArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[Z", arrayObject.className)
        assertEquals(
            JvmBooleanArrayPayload(mutableListOf(false, false, false)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewBooleanArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newBooleanArray(-1)
        }
    }

    @Test
    fun `NewByteArray allocates a zero filled guest byte array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newByteArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[B", arrayObject.className)
        assertEquals(
            JvmByteArrayPayload(mutableListOf(0.toByte(), 0.toByte(), 0.toByte())),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewByteArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newByteArray(-1)
        }
    }

    @Test
    fun `NewCharArray allocates a nul filled guest char array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newCharArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[C", arrayObject.className)
        assertEquals(
            JvmCharArrayPayload(mutableListOf('\u0000', '\u0000', '\u0000')),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewCharArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newCharArray(-1)
        }
    }

    @Test
    fun `NewShortArray allocates a zero filled guest short array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newShortArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[S", arrayObject.className)
        assertEquals(
            JvmShortArrayPayload(mutableListOf(0.toShort(), 0.toShort(), 0.toShort())),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewShortArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newShortArray(-1)
        }
    }

    @Test
    fun `NewIntArray allocates a zero filled guest int array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newIntArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[I", arrayObject.className)
        assertEquals(
            JvmIntArrayPayload(mutableListOf(0, 0, 0)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewIntArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newIntArray(-1)
        }
    }

    @Test
    fun `NewLongArray allocates a zero filled guest long array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newLongArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[J", arrayObject.className)
        assertEquals(
            JvmLongArrayPayload(mutableListOf(0L, 0L, 0L)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewLongArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newLongArray(-1)
        }
    }

    @Test
    fun `NewFloatArray allocates a positive zero filled guest float array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newFloatArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[F", arrayObject.className)
        assertEquals(
            JvmFloatArrayPayload(mutableListOf(0.0f, 0.0f, 0.0f)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewFloatArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newFloatArray(-1)
        }
    }

    @Test
    fun `NewDoubleArray allocates a positive zero filled guest double array`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val arrayHandle = environment.newDoubleArray(3)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[D", arrayObject.className)
        assertEquals(
            JvmDoubleArrayPayload(mutableListOf(0.0, 0.0, 0.0)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewDoubleArray rejects negative lengths`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertFailsWith<IllegalArgumentException> {
            environment.newDoubleArray(-1)
        }
    }

    @Test
    fun `GetBooleanArrayRegion returns a copied guest boolean array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newBooleanArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmBooleanArrayPayload
        payload.elements[1] = true
        payload.elements[3] = true

        val result = environment.getBooleanArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(booleanArrayOf(true, false, true), result)
        result[0] = false
        assertEquals(true, payload.elements[1])
    }

    @Test
    fun `GetBooleanArrayRegion rejects non boolean arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getBooleanArrayRegion(intArrayHandle, start = 0, length = 1)
        }

        val booleanArrayHandle = environment.newBooleanArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getBooleanArrayRegion(booleanArrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetBooleanArrayRegion writes a native boolean buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newBooleanArray(5)

        environment.setBooleanArrayRegion(
            arrayHandle,
            start = 1,
            values = booleanArrayOf(true, false, true),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmBooleanArrayPayload
        assertEquals(
            mutableListOf(false, true, false, true, false),
            payload.elements,
        )
    }

    @Test
    fun `SetBooleanArrayRegion rejects non boolean arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setBooleanArrayRegion(intArrayHandle, start = 0, values = booleanArrayOf(true))
        }

        val booleanArrayHandle = environment.newBooleanArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setBooleanArrayRegion(booleanArrayHandle, start = 2, values = booleanArrayOf(true, false))
        }
    }

    @Test
    fun `GetByteArrayRegion returns a copied guest byte array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmByteArrayPayload
        payload.elements[1] = (-7).toByte()
        payload.elements[3] = 12.toByte()

        val result = environment.getByteArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(byteArrayOf((-7).toByte(), 0.toByte(), 12.toByte()), result)
        result[0] = 99.toByte()
        assertEquals((-7).toByte(), payload.elements[1])
    }

    @Test
    fun `GetByteArrayRegion rejects non byte arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getByteArrayRegion(intArrayHandle, start = 0, length = 1)
        }

        val byteArrayHandle = environment.newByteArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getByteArrayRegion(byteArrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetByteArrayRegion writes a native byte buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(5)

        environment.setByteArrayRegion(
            arrayHandle,
            start = 1,
            values = byteArrayOf((-7).toByte(), 0.toByte(), 12.toByte()),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmByteArrayPayload
        assertEquals(
            mutableListOf(0.toByte(), (-7).toByte(), 0.toByte(), 12.toByte(), 0.toByte()),
            payload.elements,
        )
    }

    @Test
    fun `SetByteArrayRegion rejects non byte arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setByteArrayRegion(intArrayHandle, start = 0, values = byteArrayOf(1))
        }

        val byteArrayHandle = environment.newByteArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setByteArrayRegion(byteArrayHandle, start = 2, values = byteArrayOf(1, 2))
        }
    }

    @Test
    fun `GetCharArrayRegion returns a copied guest char array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newCharArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmCharArrayPayload
        payload.elements[1] = 'A'
        payload.elements[3] = '?'

        val result = environment.getCharArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(charArrayOf('A', '\u0000', '?'), result)
        result[0] = 'Z'
        assertEquals('A', payload.elements[1])
    }

    @Test
    fun `GetCharArrayRegion rejects non char arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getCharArrayRegion(intArrayHandle, start = 0, length = 1)
        }

        val charArrayHandle = environment.newCharArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getCharArrayRegion(charArrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetCharArrayRegion writes a native char buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newCharArray(5)

        environment.setCharArrayRegion(
            arrayHandle,
            start = 1,
            values = charArrayOf('A', '\u0000', '?'),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmCharArrayPayload
        assertEquals(
            mutableListOf('\u0000', 'A', '\u0000', '?', '\u0000'),
            payload.elements,
        )
    }

    @Test
    fun `SetCharArrayRegion rejects non char arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val intArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setCharArrayRegion(intArrayHandle, start = 0, values = charArrayOf('x'))
        }

        val charArrayHandle = environment.newCharArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setCharArrayRegion(charArrayHandle, start = 2, values = charArrayOf('x', 'y'))
        }
    }

    @Test
    fun `getShortArrayRegion returns a copied guest short array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newShortArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmShortArrayPayload
        payload.elements[1] = (-123).toShort()
        payload.elements[3] = 456.toShort()

        val result = environment.getShortArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(shortArrayOf((-123).toShort(), 0.toShort(), 456.toShort()), result)
        result[0] = 999.toShort()
        assertEquals((-123).toShort(), payload.elements[1])
    }

    @Test
    fun `getShortArrayRegion rejects non short arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getShortArrayRegion(wrongArrayHandle, start = 0, length = 1)
        }

        val arrayHandle = environment.newShortArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getShortArrayRegion(arrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `setShortArrayRegion writes a native short buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newShortArray(5)

        environment.setShortArrayRegion(
            arrayHandle,
            start = 1,
            values = shortArrayOf((-123).toShort(), 0.toShort(), 456.toShort()),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmShortArrayPayload
        assertEquals(
            mutableListOf(0.toShort(), (-123).toShort(), 0.toShort(), 456.toShort(), 0.toShort()),
            payload.elements,
        )
    }

    @Test
    fun `setShortArrayRegion rejects non short arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setShortArrayRegion(wrongArrayHandle, start = 0, values = shortArrayOf())
        }

        val arrayHandle = environment.newShortArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setShortArrayRegion(arrayHandle, start = 2, values = shortArrayOf((-123).toShort(), 0.toShort(), 456.toShort()))
        }
    }

    @Test
    fun `GetIntArrayRegion returns a copied guest int array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmIntArrayPayload
        payload.elements[1] = -7
        payload.elements[3] = 12

        val result = environment.getIntArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(intArrayOf(-7, 0, 12), result)
        result[0] = 99
        assertEquals(-7, payload.elements[1])
    }

    @Test
    fun `GetIntArrayRegion rejects non int arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newByteArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getIntArrayRegion(wrongArrayHandle, start = 0, length = 1)
        }

        val arrayHandle = environment.newIntArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getIntArrayRegion(arrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetIntArrayRegion writes a native int buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(5)

        environment.setIntArrayRegion(
            arrayHandle,
            start = 1,
            values = intArrayOf(-7, 0, 12),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmIntArrayPayload
        assertEquals(
            mutableListOf(0, -7, 0, 12, 0),
            payload.elements,
        )
    }

    @Test
    fun `SetIntArrayRegion rejects non int arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newByteArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setIntArrayRegion(wrongArrayHandle, start = 0, values = intArrayOf())
        }

        val arrayHandle = environment.newIntArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setIntArrayRegion(arrayHandle, start = 2, values = intArrayOf(-7, 0, 12))
        }
    }

    @Test
    fun `GetLongArrayRegion returns a copied guest long array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newLongArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmLongArrayPayload
        payload.elements[1] = -7L
        payload.elements[3] = 12L

        val result = environment.getLongArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(longArrayOf(-7L, 0L, 12L), result)
        result[0] = 99L
        assertEquals(-7L, payload.elements[1])
    }

    @Test
    fun `GetLongArrayRegion rejects non long arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getLongArrayRegion(wrongArrayHandle, start = 0, length = 1)
        }

        val arrayHandle = environment.newLongArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getLongArrayRegion(arrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetLongArrayRegion writes a native long buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newLongArray(5)

        environment.setLongArrayRegion(
            arrayHandle,
            start = 1,
            values = longArrayOf(-7L, 0L, 12L),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmLongArrayPayload
        assertEquals(
            mutableListOf(0L, -7L, 0L, 12L, 0L),
            payload.elements,
        )
    }

    @Test
    fun `SetLongArrayRegion rejects non long arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setLongArrayRegion(wrongArrayHandle, start = 0, values = longArrayOf())
        }

        val arrayHandle = environment.newLongArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setLongArrayRegion(arrayHandle, start = 2, values = longArrayOf(-7L, 0L, 12L))
        }
    }

    @Test
    fun `GetFloatArrayRegion returns a copied guest float array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newFloatArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmFloatArrayPayload
        payload.elements[1] = -1.5f
        payload.elements[3] = 2.25f

        val result = environment.getFloatArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(floatArrayOf(-1.5f, 0.0f, 2.25f), result)
        result[0] = 99.0f
        assertEquals(-1.5f, payload.elements[1])
    }

    @Test
    fun `GetFloatArrayRegion rejects non float arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getFloatArrayRegion(wrongArrayHandle, start = 0, length = 1)
        }

        val arrayHandle = environment.newFloatArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getFloatArrayRegion(arrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `NewObjectArray allocates a null filled guest reference array`() {
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
        val elementClassHandle = environment.findClass("java/lang/String")

        val arrayHandle = environment.newObjectArray(3, elementClassHandle, null)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals("[Ljava/lang/String;", arrayObject.className)
        assertEquals(
            JvmReferenceArrayPayload(
                mutableListOf(JvmNullValue, JvmNullValue, JvmNullValue),
            ),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewObjectArray fills every element with an assignable initial object`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Base"),
                    JvmClassDefinition(internalName = "Example", superclassName = "Base"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val elementClassHandle = environment.findClass("Base")
        val initialReference = heap.allocateObject("Example")
        val initialHandle = handles.newObjectHandle(initialReference)

        val arrayHandle = environment.newObjectArray(2, elementClassHandle, initialHandle)

        val arrayObject = heap.get(handles.resolveObject(arrayHandle))
        assertEquals(
            JvmReferenceArrayPayload(mutableListOf(initialReference, initialReference)),
            arrayObject.payload,
        )
    }

    @Test
    fun `NewObjectArray rejects non assignable initial objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Target"),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val elementClassHandle = environment.findClass("Target")
        val initialHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.newObjectArray(2, elementClassHandle, initialHandle)
        }
    }

    @Test
    fun `GetObjectArrayElement returns null for guest null array slots`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val arrayHandle = environment.newObjectArray(1, classHandle, null)

        val result = environment.getObjectArrayElement(arrayHandle, 0)

        assertEquals(null, result)
    }

    @Test
    fun `GetObjectArrayElement returns a local handle for guest reference array slots`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val initialReference = heap.allocateString("value")
        val initialHandle = handles.newObjectHandle(initialReference)
        val arrayHandle = environment.newObjectArray(1, classHandle, initialHandle)

        val result = environment.getObjectArrayElement(arrayHandle, 0)

        assertEquals(initialReference, handles.resolveObject(result!!))
    }

    @Test
    fun `GetObjectArrayElement rejects primitive guest arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = handles.newObjectHandle(heap.allocateIntArray(1))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getObjectArrayElement(arrayHandle, 0)
        }
    }

    @Test
    fun `GetObjectArrayElement rejects out of bounds indexes`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val arrayHandle = environment.newObjectArray(1, classHandle, null)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getObjectArrayElement(arrayHandle, 1)
        }
    }

    @Test
    fun `SetObjectArrayElement writes nullable guest reference array slots`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val initialReference = heap.allocateString("value")
        val initialHandle = handles.newObjectHandle(initialReference)
        val arrayHandle = environment.newObjectArray(2, classHandle, null)

        environment.setObjectArrayElement(arrayHandle, 0, initialHandle)
        environment.setObjectArrayElement(arrayHandle, 1, null)

        assertEquals(
            initialReference,
            handles.resolveObject(environment.getObjectArrayElement(arrayHandle, 0)!!),
        )
        assertEquals(null, environment.getObjectArrayElement(arrayHandle, 1))
    }

    @Test
    fun `SetObjectArrayElement accepts subclass values for reference arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Base"),
                    JvmClassDefinition(internalName = "Child", superclassName = "Base"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classHandle = environment.findClass("Base")
        val childReference = heap.allocateObject("Child")
        val childHandle = handles.newObjectHandle(childReference)
        val arrayHandle = environment.newObjectArray(1, classHandle, null)

        environment.setObjectArrayElement(arrayHandle, 0, childHandle)

        assertEquals(
            childReference,
            handles.resolveObject(environment.getObjectArrayElement(arrayHandle, 0)!!),
        )
    }

    @Test
    fun `SetObjectArrayElement rejects non assignable values`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Target"),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classHandle = environment.findClass("Target")
        val arrayHandle = environment.newObjectArray(1, classHandle, null)
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayElement(arrayHandle, 0, otherHandle)
        }
    }

    @Test
    fun `SetObjectArrayElement rejects primitive arrays and out of bounds indexes`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val primitiveArrayHandle = handles.newObjectHandle(heap.allocateIntArray(1))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayElement(primitiveArrayHandle, 0, null)
        }

        val referenceArrayHandle = handles.newObjectHandle(heap.allocateReferenceArray("java/lang/Object", 1))
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayElement(referenceArrayHandle, 1, null)
        }
    }

    @Test
    fun `GetObjectArrayRegion returns copied nullable local handles for a reference array range`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val firstReference = heap.allocateString("first")
        val secondReference = heap.allocateString("second")
        val arrayHandle = environment.newObjectArray(4, classHandle, null)
        environment.setObjectArrayElement(
            arrayHandle,
            1,
            handles.newObjectHandle(firstReference),
        )
        environment.setObjectArrayElement(
            arrayHandle,
            3,
            handles.newObjectHandle(secondReference),
        )

        val result = environment.getObjectArrayRegion(arrayHandle, start = 1, length = 3)

        assertEquals(3, result.size)
        assertEquals(firstReference, handles.resolveObject(result[0]!!))
        assertEquals(null, result[1])
        assertEquals(secondReference, handles.resolveObject(result[2]!!))
    }

    @Test
    fun `GetObjectArrayRegion rejects primitive arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val primitiveArrayHandle = handles.newObjectHandle(heap.allocateIntArray(3))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getObjectArrayRegion(primitiveArrayHandle, start = 0, length = 1)
        }

        val referenceArrayHandle = handles.newObjectHandle(heap.allocateReferenceArray("java/lang/Object", 3))
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getObjectArrayRegion(referenceArrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetObjectArrayRegion writes nullable handles into a guest reference array range`() {
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
        val classHandle = environment.findClass("java/lang/String")
        val firstReference = heap.allocateString("first")
        val secondReference = heap.allocateString("second")
        val firstHandle = handles.newObjectHandle(firstReference)
        val secondHandle = handles.newObjectHandle(secondReference)
        val arrayHandle = environment.newObjectArray(4, classHandle, null)

        environment.setObjectArrayRegion(
            arrayHandle,
            start = 1,
            values = listOf(firstHandle, null, secondHandle),
        )

        assertEquals(
            firstReference,
            handles.resolveObject(environment.getObjectArrayElement(arrayHandle, 1)!!),
        )
        assertEquals(null, environment.getObjectArrayElement(arrayHandle, 2))
        assertEquals(
            secondReference,
            handles.resolveObject(environment.getObjectArrayElement(arrayHandle, 3)!!),
        )
    }

    @Test
    fun `SetObjectArrayRegion rejects primitive arrays invalid ranges and non assignable values`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Target"),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val primitiveArrayHandle = handles.newObjectHandle(heap.allocateIntArray(3))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayRegion(primitiveArrayHandle, start = 0, values = listOf(null))
        }

        val targetClassHandle = environment.findClass("Target")
        val referenceArrayHandle = environment.newObjectArray(2, targetClassHandle, null)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayRegion(referenceArrayHandle, start = 1, values = listOf(null, null))
        }

        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setObjectArrayRegion(referenceArrayHandle, start = 0, values = listOf(otherHandle))
        }
    }

}
