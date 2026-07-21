package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import kotlin.test.Test
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

}
