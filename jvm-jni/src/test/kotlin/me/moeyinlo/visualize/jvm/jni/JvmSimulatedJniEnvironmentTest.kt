package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDirectByteBufferPayload
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
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorOwnershipException
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmMethodDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmSimulatedJniEnvironmentTest {
    @Test
    fun `GetVersion returns the supported JNI version`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertEquals(0x00180000, environment.getVersion())
    }

    @Test
    fun `GetJavaVM returns the owning simulated JavaVM when the environment is VM-bound`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val javaVm = JvmSimulatedJavaVm(environment)

        val result = environment.getJavaVm()

        assertEquals(JvmJniStatus.Ok, result.status)
        assertSame(javaVm, result.javaVm)
    }

    @Test
    fun `JNIEnv function table delegates GetJavaVM to the owning simulated JavaVM`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val javaVm = JvmSimulatedJavaVm(environment)

        val result = environment.functions.getJavaVm()

        assertEquals(JvmJniStatus.Ok, result.status)
        assertSame(javaVm, result.javaVm)
    }

    @Test
    fun `GetJavaVM reports JNI error when the environment is not VM-bound`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val result = environment.getJavaVm()

        assertEquals(JvmJniStatus.Err, result.status)
        assertEquals(null, result.javaVm)
    }

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
    fun `GetSuperclass returns a superclass handle for loaded guest classes and null for root classes`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "Example", superclassName = "java/lang/Object"),
                ),
            ),
            handles = handles,
        )
        val objectHandle = environment.findClass("java/lang/Object")
        val classHandle = environment.findClass("Example")

        val superclassHandle = environment.getSuperclass(classHandle)

        assertEquals("java/lang/Object", handles.resolveClass(superclassHandle!!))
        assertEquals(null, environment.getSuperclass(objectHandle))
    }

    @Test
    fun `IsAssignableFrom returns whether source class objects can be cast to target class`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "Base", superclassName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "Derived", superclassName = "Base"),
                ),
            ),
        )
        val baseHandle = environment.findClass("Base")
        val derivedHandle = environment.findClass("Derived")

        assertEquals(true, environment.isAssignableFrom(derivedHandle, baseHandle))
        assertEquals(true, environment.isAssignableFrom(baseHandle, baseHandle))
        assertEquals(false, environment.isAssignableFrom(baseHandle, derivedHandle))
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
    fun `GetStaticMethodID rejects class initializer lookup`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "<clinit>",
                                descriptor = "()V",
                                isStatic = true,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            environment.getStaticMethodId(classHandle, "<clinit>", "()V")
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.<clinit>:()V", exception.message)
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
    fun `GetMethodID rejects class initializer lookup`() {
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "<clinit>",
                                descriptor = "()V",
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val classHandle = environment.findClass("Example")

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            environment.getMethodId(classHandle, "<clinit>", "()V")
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.<clinit>:()V", exception.message)
    }

    @Test
    fun `CallVoidMethod routes instance method upcalls through the configured dispatcher`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedVoidUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "accept",
                                descriptor = "(I)V",
                                isStatic = false,
                            ),
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
                    calls += RecordedVoidUpcall(receiver, method, arguments)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "accept", "(I)V")

        environment.callVoidMethod(objectHandle, methodHandle, listOf(JvmIntValue(7)))

        assertEquals(
            listOf(
                RecordedVoidUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "accept",
                        descriptor = "(I)V",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(7)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallVoidMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "accept",
                                descriptor = "()V",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            heap = heap,
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "accept", "()V")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callVoidMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallVoidMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualVoidMethod routes explicit declaring class method upcalls through the configured dispatcher`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedVoidUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "accept",
                                descriptor = "(I)V",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "accept",
                                descriptor = "(I)V",
                                isStatic = false,
                            ),
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
                    calls += RecordedVoidUpcall(receiver, method, arguments)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "accept", "(I)V")

        environment.callNonvirtualVoidMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(7)),
        )

        assertEquals(
            listOf(
                RecordedVoidUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "accept",
                        descriptor = "(I)V",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(7)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallObjectMethod routes instance method upcalls and returns a local object handle`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedObjectUpcall>()
        val resultReference = heap.allocateObject("java/lang/Object")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "select",
                                descriptor = "(I)Ljava/lang/Object;",
                                isStatic = false,
                            ),
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
                    calls += RecordedObjectUpcall(receiver, method, arguments)
                    return resultReference
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "select", "(I)Ljava/lang/Object;")

        val resultHandle = environment.callObjectMethod(objectHandle, methodHandle, listOf(JvmIntValue(3)))

        assertEquals(resultReference, handles.resolveObject(resultHandle!!))
        assertEquals(
            listOf(
                RecordedObjectUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "select",
                        descriptor = "(I)Ljava/lang/Object;",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(3)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallObjectMethod returns jclass handles for guest Class mirror results`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val resultReference = heap.internClassMirror("Child")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Child"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "type",
                                descriptor = "()Ljava/lang/Class;",
                                isStatic = false,
                            ),
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
                ): JvmReferenceValue = resultReference
            },
        )
        val classHandle = environment.findClass("Example")
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val methodHandle = environment.getMethodId(classHandle, "type", "()Ljava/lang/Class;")

        val resultHandle = environment.callObjectMethod(objectHandle, methodHandle)

        assertEquals("Child", handles.resolveClass(resultHandle!!))
    }

    @Test
    fun `CallObjectMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedObjectUpcall>()
        val resultReference = heap.allocateString("Example")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(internalName = "java/lang/String"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "mirrorName",
                                descriptor = "()Ljava/lang/String;",
                                isStatic = false,
                            ),
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
                    calls += RecordedObjectUpcall(receiver, method, arguments)
                    return resultReference
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "mirrorName", "()Ljava/lang/String;")

        val resultHandle = environment.callObjectMethod(receiverClassHandle, methodHandle)

        assertEquals(resultReference, handles.resolveObject(resultHandle!!))
        assertEquals(
            listOf(
                RecordedObjectUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "mirrorName",
                        descriptor = "()Ljava/lang/String;",
                        isStatic = false,
                    ),
                    arguments = emptyList(),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallObjectMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "select",
                                descriptor = "()Ljava/lang/Object;",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmReferenceValue = error("CallObjectMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "select", "()Ljava/lang/Object;")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callObjectMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallObjectMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualObjectMethod routes explicit declaring class method upcalls and returns a local object handle`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedObjectUpcall>()
        val resultReference = heap.allocateObject("java/lang/Object")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "select",
                                descriptor = "(I)Ljava/lang/Object;",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "select",
                                descriptor = "(I)Ljava/lang/Object;",
                                isStatic = false,
                            ),
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
                    calls += RecordedObjectUpcall(receiver, method, arguments)
                    return resultReference
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "select", "(I)Ljava/lang/Object;")

        val resultHandle = environment.callNonvirtualObjectMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(3)),
        )

        assertEquals(resultReference, handles.resolveObject(resultHandle!!))
        assertEquals(
            listOf(
                RecordedObjectUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "select",
                        descriptor = "(I)Ljava/lang/Object;",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(3)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallNonvirtualObjectMethod returns jclass handles for guest Class mirror results`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val resultReference = heap.internClassMirror("Child")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Child"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "type",
                                descriptor = "()Ljava/lang/Class;",
                                isStatic = false,
                            ),
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
                ): JvmReferenceValue = resultReference
            },
        )
        val classHandle = environment.findClass("Example")
        val objectHandle = handles.newObjectHandle(heap.allocateObject("Example"))
        val methodHandle = environment.getMethodId(classHandle, "type", "()Ljava/lang/Class;")

        val resultHandle = environment.callNonvirtualObjectMethod(
            objectHandle = objectHandle,
            classHandle = classHandle,
            methodIdHandle = methodHandle,
        )

        assertEquals("Child", handles.resolveClass(resultHandle!!))
    }

    @Test
    fun `CallBooleanMethod routes instance method upcalls and returns a JNI boolean`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedBooleanUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "(I)Z",
                                isStatic = false,
                            ),
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
                    calls += RecordedBooleanUpcall(receiver, method, arguments)
                    return JvmBooleanValue(true)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "enabled", "(I)Z")

        val result = environment.callBooleanMethod(objectHandle, methodHandle, listOf(JvmIntValue(9)))

        assertEquals(true, result)
        assertEquals(
            listOf(
                RecordedBooleanUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "enabled",
                        descriptor = "(I)Z",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(9)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallBooleanMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedBooleanUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "isMirror",
                                descriptor = "()Z",
                                isStatic = false,
                            ),
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
                    calls += RecordedBooleanUpcall(receiver, method, arguments)
                    return JvmBooleanValue(true)
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "isMirror", "()Z")

        val result = environment.callBooleanMethod(receiverClassHandle, methodHandle)

        assertEquals(true, result)
        assertEquals(
            listOf(
                RecordedBooleanUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "isMirror",
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
    fun `CallBooleanMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "()Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmBooleanValue = error("CallBooleanMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "enabled", "()Z")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callBooleanMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallBooleanMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualBooleanMethod routes explicit declaring class method upcalls and returns a JNI boolean`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedBooleanUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "(I)Z",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "(I)Z",
                                isStatic = false,
                            ),
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
                    calls += RecordedBooleanUpcall(receiver, method, arguments)
                    return JvmBooleanValue(false)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "enabled", "(I)Z")

        val result = environment.callNonvirtualBooleanMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(9)),
        )

        assertEquals(false, result)
        assertEquals(
            listOf(
                RecordedBooleanUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "enabled",
                        descriptor = "(I)Z",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(9)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallByteMethod routes instance method upcalls and returns a JNI byte`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedByteUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "(I)B",
                                isStatic = false,
                            ),
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
                    calls += RecordedByteUpcall(receiver, method, arguments)
                    return JvmByteValue(-12)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "code", "(I)B")

        val result = environment.callByteMethod(objectHandle, methodHandle, listOf(JvmIntValue(11)))

        assertEquals(-12, result)
        assertEquals(
            listOf(
                RecordedByteUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "code",
                        descriptor = "(I)B",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(11)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallByteMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedByteUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "mirrorCode",
                                descriptor = "()B",
                                isStatic = false,
                            ),
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
                    calls += RecordedByteUpcall(receiver, method, arguments)
                    return JvmByteValue(-7)
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "mirrorCode", "()B")

        val result = environment.callByteMethod(receiverClassHandle, methodHandle)

        assertEquals(-7, result)
        assertEquals(
            listOf(
                RecordedByteUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "mirrorCode",
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
    fun `CallByteMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "()B",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmByteValue = error("CallByteMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "code", "()B")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callByteMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallByteMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualByteMethod routes explicit declaring class method upcalls and returns a JNI byte`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedByteUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "(I)B",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "(I)B",
                                isStatic = false,
                            ),
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
                    calls += RecordedByteUpcall(receiver, method, arguments)
                    return JvmByteValue(-13)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "code", "(I)B")

        val result = environment.callNonvirtualByteMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(11)),
        )

        assertEquals(-13, result)
        assertEquals(
            listOf(
                RecordedByteUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "code",
                        descriptor = "(I)B",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(11)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallCharMethod routes instance method upcalls and returns a JNI char`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedCharUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "(I)C",
                                isStatic = false,
                            ),
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
                    calls += RecordedCharUpcall(receiver, method, arguments)
                    return JvmCharValue('x'.code)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "letter", "(I)C")

        val result = environment.callCharMethod(objectHandle, methodHandle, listOf(JvmIntValue(13)))

        assertEquals('x'.code, result)
        assertEquals(
            listOf(
                RecordedCharUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "letter",
                        descriptor = "(I)C",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(13)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallCharMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedCharUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "mirrorLetter",
                                descriptor = "()C",
                                isStatic = false,
                            ),
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
                    calls += RecordedCharUpcall(receiver, method, arguments)
                    return JvmCharValue('C'.code)
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "mirrorLetter", "()C")

        val result = environment.callCharMethod(receiverClassHandle, methodHandle)

        assertEquals('C'.code, result)
        assertEquals(
            listOf(
                RecordedCharUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "mirrorLetter",
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
    fun `CallCharMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "()C",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmCharValue = error("CallCharMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "letter", "()C")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callCharMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallCharMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualCharMethod routes explicit declaring class method upcalls and returns a JNI char`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedCharUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "(I)C",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "(I)C",
                                isStatic = false,
                            ),
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
                    calls += RecordedCharUpcall(receiver, method, arguments)
                    return JvmCharValue('z'.code)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "letter", "(I)C")

        val result = environment.callNonvirtualCharMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(13)),
        )

        assertEquals('z'.code, result)
        assertEquals(
            listOf(
                RecordedCharUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "letter",
                        descriptor = "(I)C",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(13)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallShortMethod routes instance method upcalls and returns a JNI short`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedShortUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "(I)S",
                                isStatic = false,
                            ),
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
                    calls += RecordedShortUpcall(receiver, method, arguments)
                    return JvmShortValue(-1234)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "delta", "(I)S")

        val result = environment.callShortMethod(objectHandle, methodHandle, listOf(JvmIntValue(17)))

        assertEquals(-1234, result)
        assertEquals(
            listOf(
                RecordedShortUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "delta",
                        descriptor = "(I)S",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(17)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallShortMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedShortUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "mirrorDelta",
                                descriptor = "()S",
                                isStatic = false,
                            ),
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
                    calls += RecordedShortUpcall(receiver, method, arguments)
                    return JvmShortValue(-321)
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "mirrorDelta", "()S")

        val result = environment.callShortMethod(receiverClassHandle, methodHandle)

        assertEquals(-321, result)
        assertEquals(
            listOf(
                RecordedShortUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "mirrorDelta",
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
    fun `CallShortMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "()S",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmShortValue = error("CallShortMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "delta", "()S")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callShortMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallShortMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualShortMethod routes explicit declaring class method upcalls and returns a JNI short`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedShortUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "(I)S",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "(I)S",
                                isStatic = false,
                            ),
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
                    calls += RecordedShortUpcall(receiver, method, arguments)
                    return JvmShortValue(-4321)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "delta", "(I)S")

        val result = environment.callNonvirtualShortMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(17)),
        )

        assertEquals(-4321, result)
        assertEquals(
            listOf(
                RecordedShortUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "delta",
                        descriptor = "(I)S",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(17)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallIntMethod routes instance method upcalls and returns a JNI int`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedIntUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "(I)I",
                                isStatic = false,
                            ),
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
                    calls += RecordedIntUpcall(receiver, method, arguments)
                    return JvmIntValue(123456)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "answer", "(I)I")

        val result = environment.callIntMethod(objectHandle, methodHandle, listOf(JvmIntValue(19)))

        assertEquals(123456, result)
        assertEquals(
            listOf(
                RecordedIntUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "answer",
                        descriptor = "(I)I",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(19)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallIntMethod accepts jclass receivers as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedIntUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(
                        internalName = "java/lang/Class",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "mirrorAnswer",
                                descriptor = "()I",
                                isStatic = false,
                            ),
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
                    calls += RecordedIntUpcall(receiver, method, arguments)
                    return JvmIntValue(1234)
                }
            },
        )
        val receiverClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val methodHandle = environment.getMethodId(classClassHandle, "mirrorAnswer", "()I")

        val result = environment.callIntMethod(receiverClassHandle, methodHandle)

        assertEquals(1234, result)
        assertEquals(
            listOf(
                RecordedIntUpcall(
                    receiver = heap.internClassMirror("Example"),
                    method = JvmResolvedMethod(
                        ownerClassName = "java/lang/Class",
                        name = "mirrorAnswer",
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
    fun `CallIntMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
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
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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
                ): JvmIntValue = error("CallIntMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "answer", "()I")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callIntMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallIntMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualIntMethod routes explicit declaring class method upcalls and returns a JNI int`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedIntUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "(I)I",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "(I)I",
                                isStatic = false,
                            ),
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
                    calls += RecordedIntUpcall(receiver, method, arguments)
                    return JvmIntValue(654321)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "answer", "(I)I")

        val result = environment.callNonvirtualIntMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(19)),
        )

        assertEquals(654321, result)
        assertEquals(
            listOf(
                RecordedIntUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "answer",
                        descriptor = "(I)I",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(19)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallLongMethod routes instance method upcalls and returns a JNI long`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedLongUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "wide",
                                descriptor = "(I)J",
                                isStatic = false,
                            ),
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

                override fun callLongMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmLongValue {
                    calls += RecordedLongUpcall(receiver, method, arguments)
                    return JvmLongValue(9_876_543_210L)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "wide", "(I)J")

        val result = environment.callLongMethod(objectHandle, methodHandle, listOf(JvmIntValue(23)))

        assertEquals(9_876_543_210L, result)
        assertEquals(
            listOf(
                RecordedLongUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "wide",
                        descriptor = "(I)J",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(23)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallLongMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "wide",
                                descriptor = "()J",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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

                override fun callLongMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmLongValue = error("CallLongMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "wide", "()J")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callLongMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallLongMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualLongMethod routes explicit declaring class method upcalls and returns a JNI long`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedLongUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "counter",
                                descriptor = "(I)J",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "counter",
                                descriptor = "(I)J",
                                isStatic = false,
                            ),
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

                override fun callLongMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmLongValue {
                    calls += RecordedLongUpcall(receiver, method, arguments)
                    return JvmLongValue(9876543210L)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "counter", "(I)J")

        val result = environment.callNonvirtualLongMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(23)),
        )

        assertEquals(9876543210L, result)
        assertEquals(
            listOf(
                RecordedLongUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "counter",
                        descriptor = "(I)J",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(23)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallFloatMethod routes instance method upcalls and returns a JNI float`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFloatUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "(I)F",
                                isStatic = false,
                            ),
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

                override fun callFloatMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmFloatValue {
                    calls += RecordedFloatUpcall(receiver, method, arguments)
                    return JvmFloatValue(0.75f)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "ratio", "(I)F")

        val result = environment.callFloatMethod(objectHandle, methodHandle, listOf(JvmIntValue(3)))

        assertEquals(0.75f, result)
        assertEquals(
            listOf(
                RecordedFloatUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "ratio",
                        descriptor = "(I)F",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(3)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallFloatMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "()F",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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

                override fun callFloatMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmFloatValue = error("CallFloatMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "ratio", "()F")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callFloatMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallFloatMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualFloatMethod routes explicit declaring class method upcalls and returns a JNI float`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedFloatUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "(I)F",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "(I)F",
                                isStatic = false,
                            ),
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

                override fun callFloatMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmFloatValue {
                    calls += RecordedFloatUpcall(receiver, method, arguments)
                    return JvmFloatValue(6.25f)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "ratio", "(I)F")

        val result = environment.callNonvirtualFloatMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(29)),
        )

        assertEquals(6.25f, result)
        assertEquals(
            listOf(
                RecordedFloatUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "ratio",
                        descriptor = "(I)F",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(29)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallDoubleMethod routes instance method upcalls and returns a JNI double`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedDoubleUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "(I)D",
                                isStatic = false,
                            ),
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

                override fun callDoubleMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmDoubleValue {
                    calls += RecordedDoubleUpcall(receiver, method, arguments)
                    return JvmDoubleValue(12.5)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val receiver = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(classHandle, "score", "(I)D")

        val result = environment.callDoubleMethod(objectHandle, methodHandle, listOf(JvmIntValue(4)))

        assertEquals(12.5, result)
        assertEquals(
            listOf(
                RecordedDoubleUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "score",
                        descriptor = "(I)D",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(4)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallDoubleMethod rejects receiver that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "()D",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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

                override fun callDoubleMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmDoubleValue = error("CallDoubleMethod must not enter dispatcher for an incompatible receiver")
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getMethodId(classHandle, "score", "()D")
        val otherHandle = handles.newObjectHandle(heap.allocateObject("Other"))

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callDoubleMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallDoubleMethod requires receiver Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallNonvirtualDoubleMethod routes explicit declaring class method upcalls and returns a JNI double`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedDoubleUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "(I)D",
                                isStatic = false,
                            ),
                        ),
                    ),
                    JvmClassDefinition(
                        internalName = "Derived",
                        superclassName = "Base",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "(I)D",
                                isStatic = false,
                            ),
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

                override fun callDoubleMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmDoubleValue {
                    calls += RecordedDoubleUpcall(receiver, method, arguments)
                    return JvmDoubleValue(9.75)
                }
            },
        )
        val baseClassHandle = environment.findClass("Base")
        val receiver = heap.allocateObject("Derived")
        val objectHandle = handles.newObjectHandle(receiver)
        val methodHandle = environment.getMethodId(baseClassHandle, "score", "(I)D")

        val result = environment.callNonvirtualDoubleMethod(
            objectHandle = objectHandle,
            classHandle = baseClassHandle,
            methodIdHandle = methodHandle,
            arguments = listOf(JvmIntValue(31)),
        )

        assertEquals(9.75, result)
        assertEquals(
            listOf(
                RecordedDoubleUpcall(
                    receiver = receiver,
                    method = JvmResolvedMethod(
                        ownerClassName = "Base",
                        name = "score",
                        descriptor = "(I)D",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(31)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticVoidMethod routes static method upcalls`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticVoidUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "boot",
                                descriptor = "(I)V",
                                isStatic = true,
                            ),
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

                override fun callStaticVoidMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) {
                    calls += RecordedStaticVoidUpcall(method, arguments)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "boot", "(I)V")

        environment.callStaticVoidMethod(classHandle, methodHandle, listOf(JvmIntValue(7)))

        assertEquals(
            listOf(
                RecordedStaticVoidUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "boot",
                        descriptor = "(I)V",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(7)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticVoidMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "boot",
                                descriptor = "()V",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticVoidMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallStaticVoidMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "boot", "()V")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticVoidMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticVoidMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticObjectMethod routes static method upcalls and returns a local object handle`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticObjectUpcall>()
        val resultReference = heap.allocateObject("java/lang/Object")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "pick",
                                descriptor = "(I)Ljava/lang/Object;",
                                isStatic = true,
                            ),
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

                override fun callStaticObjectMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmReferenceValue {
                    calls += RecordedStaticObjectUpcall(method, arguments)
                    return resultReference
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "pick", "(I)Ljava/lang/Object;")

        val resultHandle = environment.callStaticObjectMethod(classHandle, methodHandle, listOf(JvmIntValue(8)))

        assertEquals(resultReference, handles.resolveObject(resultHandle!!))
        assertEquals(
            listOf(
                RecordedStaticObjectUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "pick",
                        descriptor = "(I)Ljava/lang/Object;",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(8)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticObjectMethod returns jclass handles for guest Class mirror results`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val resultReference = heap.internClassMirror("Result")
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Result"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "pickClass",
                                descriptor = "()Ljava/lang/Class;",
                                isStatic = true,
                            ),
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

                override fun callStaticObjectMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmReferenceValue = resultReference
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "pickClass", "()Ljava/lang/Class;")

        val resultHandle = environment.callStaticObjectMethod(classHandle, methodHandle)

        assertEquals("Result", handles.resolveClass(resultHandle!!))
    }

    @Test
    fun `CallStaticObjectMethod rejects class that is not assignable to method owner`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "pick",
                                descriptor = "()Ljava/lang/Object;",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
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

                override fun callStaticObjectMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmReferenceValue = error("CallStaticObjectMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "pick", "()Ljava/lang/Object;")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticObjectMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticObjectMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticBooleanMethod routes static method upcalls and returns a JNI boolean`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticBooleanUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "(I)Z",
                                isStatic = true,
                            ),
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

                override fun callStaticBooleanMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmBooleanValue {
                    calls += RecordedStaticBooleanUpcall(method, arguments)
                    return JvmBooleanValue(true)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "enabled", "(I)Z")

        val result = environment.callStaticBooleanMethod(classHandle, methodHandle, listOf(JvmIntValue(9)))

        assertEquals(true, result)
        assertEquals(
            listOf(
                RecordedStaticBooleanUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "enabled",
                        descriptor = "(I)Z",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(9)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticBooleanMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "enabled",
                                descriptor = "()Z",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticBooleanMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmBooleanValue = error("CallStaticBooleanMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "enabled", "()Z")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticBooleanMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticBooleanMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticByteMethod routes static method upcalls and returns a JNI byte`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticByteUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "(I)B",
                                isStatic = true,
                            ),
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

                override fun callStaticByteMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmByteValue {
                    calls += RecordedStaticByteUpcall(method, arguments)
                    return JvmByteValue(-12)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "code", "(I)B")

        val result = environment.callStaticByteMethod(classHandle, methodHandle, listOf(JvmIntValue(10)))

        assertEquals(-12, result)
        assertEquals(
            listOf(
                RecordedStaticByteUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "code",
                        descriptor = "(I)B",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(10)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticByteMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "code",
                                descriptor = "()B",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticByteMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmByteValue = error("CallStaticByteMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "code", "()B")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticByteMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticByteMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticCharMethod routes static method upcalls and returns a JNI char`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticCharUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "(I)C",
                                isStatic = true,
                            ),
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

                override fun callStaticCharMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmCharValue {
                    calls += RecordedStaticCharUpcall(method, arguments)
                    return JvmCharValue('x'.code)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "letter", "(I)C")

        val result = environment.callStaticCharMethod(classHandle, methodHandle, listOf(JvmIntValue(11)))

        assertEquals('x'.code, result)
        assertEquals(
            listOf(
                RecordedStaticCharUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "letter",
                        descriptor = "(I)C",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(11)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticCharMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "letter",
                                descriptor = "()C",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticCharMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmCharValue = error("CallStaticCharMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "letter", "()C")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticCharMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticCharMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticShortMethod routes static method upcalls and returns a JNI short`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticShortUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "(I)S",
                                isStatic = true,
                            ),
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

                override fun callStaticShortMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmShortValue {
                    calls += RecordedStaticShortUpcall(method, arguments)
                    return JvmShortValue(-123)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "delta", "(I)S")

        val result = environment.callStaticShortMethod(classHandle, methodHandle, listOf(JvmIntValue(12)))

        assertEquals(-123, result)
        assertEquals(
            listOf(
                RecordedStaticShortUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "delta",
                        descriptor = "(I)S",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(12)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticShortMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "delta",
                                descriptor = "()S",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticShortMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmShortValue = error("CallStaticShortMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "delta", "()S")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticShortMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticShortMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticIntMethod routes static method upcalls and returns a JNI int`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticIntUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "answer",
                                descriptor = "(I)I",
                                isStatic = true,
                            ),
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

                override fun callStaticIntMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmIntValue {
                    calls += RecordedStaticIntUpcall(method, arguments)
                    return JvmIntValue(123456)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "answer", "(I)I")

        val result = environment.callStaticIntMethod(classHandle, methodHandle, listOf(JvmIntValue(13)))

        assertEquals(123456, result)
        assertEquals(
            listOf(
                RecordedStaticIntUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "answer",
                        descriptor = "(I)I",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(13)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticIntMethod rejects class that is not assignable to method owner`() {
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
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticIntMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmIntValue = error("CallStaticIntMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "answer", "()I")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticIntMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticIntMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticLongMethod routes static method upcalls and returns a JNI long`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticLongUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "wide",
                                descriptor = "(I)J",
                                isStatic = true,
                            ),
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

                override fun callStaticLongMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmLongValue {
                    calls += RecordedStaticLongUpcall(method, arguments)
                    return JvmLongValue(9_876_543_210L)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "wide", "(I)J")

        val result = environment.callStaticLongMethod(classHandle, methodHandle, listOf(JvmIntValue(14)))

        assertEquals(9_876_543_210L, result)
        assertEquals(
            listOf(
                RecordedStaticLongUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "wide",
                        descriptor = "(I)J",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(14)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticLongMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "wide",
                                descriptor = "()J",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticLongMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmLongValue = error("CallStaticLongMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "wide", "()J")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticLongMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticLongMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticFloatMethod routes static method upcalls and returns a JNI float`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticFloatUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "(I)F",
                                isStatic = true,
                            ),
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

                override fun callStaticFloatMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmFloatValue {
                    calls += RecordedStaticFloatUpcall(method, arguments)
                    return JvmFloatValue(6.25f)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "ratio", "(I)F")

        val result = environment.callStaticFloatMethod(classHandle, methodHandle, listOf(JvmIntValue(15)))

        assertEquals(6.25f, result)
        assertEquals(
            listOf(
                RecordedStaticFloatUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "ratio",
                        descriptor = "(I)F",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(15)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticFloatMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "ratio",
                                descriptor = "()F",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticFloatMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmFloatValue = error("CallStaticFloatMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "ratio", "()F")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticFloatMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticFloatMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `CallStaticDoubleMethod routes static method upcalls and returns a JNI double`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedStaticDoubleUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "(I)D",
                                isStatic = true,
                            ),
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

                override fun callStaticDoubleMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmDoubleValue {
                    calls += RecordedStaticDoubleUpcall(method, arguments)
                    return JvmDoubleValue(12.5)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val methodHandle = environment.getStaticMethodId(classHandle, "score", "(I)D")

        val result = environment.callStaticDoubleMethod(classHandle, methodHandle, listOf(JvmIntValue(16)))

        assertEquals(12.5, result)
        assertEquals(
            listOf(
                RecordedStaticDoubleUpcall(
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "score",
                        descriptor = "(I)D",
                        isStatic = true,
                    ),
                    arguments = listOf(JvmIntValue(16)),
                ),
            ),
            calls,
        )
    }

    @Test
    fun `CallStaticDoubleMethod rejects class that is not assignable to method owner`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "score",
                                descriptor = "()D",
                                isStatic = true,
                            ),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            handles = handles,
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("CallVoidMethod must not be used")

                override fun callStaticDoubleMethod(
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ): JvmDoubleValue = error("CallStaticDoubleMethod must not enter dispatcher for an incompatible class")
            },
        )
        val exampleHandle = environment.findClass("Example")
        val otherHandle = environment.findClass("Other")
        val methodHandle = environment.getStaticMethodId(exampleHandle, "score", "()D")

        val exception = assertFailsWith<JvmJniMethodAccessException> {
            environment.callStaticDoubleMethod(otherHandle, methodHandle)
        }

        assertEquals(
            "CallStaticDoubleMethod requires class Other to be assignable to Example",
            exception.message,
        )
    }

    @Test
    fun `GetStringRegion copies a UTF-16 character range from guest strings`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val stringHandle = handles.newObjectHandle(heap.allocateString("abcdef"))

        val region = environment.getStringRegion(stringHandle, start = 2, length = 3)

        assertContentEquals(charArrayOf('c', 'd', 'e'), region)
    }

    @Test
    fun `GetStringRegion rejects ranges outside the guest string`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val stringHandle = handles.newObjectHandle(heap.allocateString("abc"))

        val exception = assertFailsWith<JvmJniStringAccessException> {
            environment.getStringRegion(stringHandle, start = 2, length = 2)
        }

        assertEquals("GetStringRegion range 2..4 is outside string length 3", exception.message)
    }

    @Test
    fun `GetStringUTFRegion copies a modified UTF-8 character range from guest strings`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val stringHandle = handles.newObjectHandle(heap.allocateString("a\u0000cdef"))

        val region = environment.getStringUtfRegion(stringHandle, start = 1, length = 3)

        assertContentEquals(byteArrayOf(0xc0.toByte(), 0x80.toByte(), 0x63, 0x64), region)
    }

    @Test
    fun `GetStringUTFRegion rejects ranges outside the guest string`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val stringHandle = handles.newObjectHandle(heap.allocateString("abc"))

        val exception = assertFailsWith<JvmJniStringAccessException> {
            environment.getStringUtfRegion(stringHandle, start = 2, length = 2)
        }

        assertEquals("GetStringUTFRegion range 2..4 is outside string length 3", exception.message)
    }

    @Test
    fun `AllocObject allocates an uninitialized guest object without invoking a constructor`() {
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
            upcallDispatcher = object : JvmJniUpcallDispatcher {
                override fun callVoidMethod(
                    receiver: JvmObjectReferenceValue,
                    method: JvmResolvedMethod,
                    arguments: List<JvmValue>,
                ) = error("AllocObject must not invoke a constructor")
            },
        )
        val classHandle = environment.findClass("Example")

        val objectHandle = environment.allocObject(classHandle)
        val objectReference = handles.resolveObject(objectHandle)

        assertEquals("Example", heap.get(objectReference).className)
        assertEquals(false, heap.isInitialized(objectReference))
    }

    @Test
    fun `NewObject allocates a guest object and invokes the constructor through the configured dispatcher`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val calls = mutableListOf<RecordedNewObjectUpcall>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "<init>",
                                descriptor = "(I)V",
                                isStatic = false,
                            ),
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
                    assertEquals(false, heap.isInitialized(receiver))
                    calls += RecordedNewObjectUpcall(receiver, method, arguments)
                }
            },
        )
        val classHandle = environment.findClass("Example")
        val constructorHandle = environment.getMethodId(classHandle, "<init>", "(I)V")

        val objectHandle = environment.newObject(classHandle, constructorHandle, listOf(JvmIntValue(41)))
        val objectReference = handles.resolveObject(objectHandle)

        assertEquals("Example", heap.get(objectReference).className)
        assertEquals(true, heap.isInitialized(objectReference))
        assertEquals(
            listOf(
                RecordedNewObjectUpcall(
                    receiver = objectReference,
                    method = JvmResolvedMethod(
                        ownerClassName = "Example",
                        name = "<init>",
                        descriptor = "(I)V",
                        isStatic = false,
                    ),
                    arguments = listOf(JvmIntValue(41)),
                ),
            ),
            calls,
        )
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
    fun `GetObjectClass throws guest NoClassDefFoundError when runtime class is not loaded`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Missing")
        val objectHandle = handles.newObjectHandle(objectReference)

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            environment.getObjectClass(objectHandle)
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("Missing", exception.message)
    }

    @Test
    fun `GetObjectClass accepts jclass handles as jobject references`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("Example")

        val result = environment.getObjectClass(classHandle)

        assertEquals("java/lang/Class", handles.resolveClass(result))
    }

    @Test
    fun `IsInstanceOf throws guest NoClassDefFoundError when source runtime class is not loaded`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Missing")
        val objectHandle = handles.newObjectHandle(objectReference)
        val targetClassHandle = handles.newClassHandle("java/lang/Object")

        val exception = assertFailsWith<JvmNoClassDefFoundError> {
            environment.isInstanceOf(objectHandle, targetClassHandle)
        }

        assertEquals("java/lang/NoClassDefFoundError", exception.guestClassName)
        assertEquals("Missing", exception.message)
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
    fun `IsInstanceOf accepts jclass handles as jobject references`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        val sourceClassHandle = environment.findClass("Example")
        val classClassHandle = environment.findClass("java/lang/Class")
        val exampleClassHandle = environment.findClass("Example")

        assertEquals(true, environment.isInstanceOf(sourceClassHandle, classClassHandle))
        assertEquals(false, environment.isInstanceOf(sourceClassHandle, exampleClassHandle))
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
    fun `GetObjectField returns jclass handles for stored guest class mirrors`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "type",
                                descriptor = "Ljava/lang/Class;",
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
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "type", "Ljava/lang/Class;")
        heap.putInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "type", descriptor = "Ljava/lang/Class;"),
            heap.internClassMirror("Child"),
        )

        val resultHandle = environment.getObjectField(objectHandle, fieldHandle)

        assertEquals("Child", handles.resolveClass(resultHandle!!))
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
    fun `SetObjectField accepts jclass handles for guest class mirror fields`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "type",
                                descriptor = "Ljava/lang/Class;",
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
        val objectHandle = handles.newObjectHandle(objectReference)
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getFieldId(classHandle, "type", "Ljava/lang/Class;")
        val valueHandle = environment.findClass("Child")

        environment.setObjectField(objectHandle, fieldHandle, valueHandle)

        val storedValue = heap.getInstanceField(
            objectReference,
            JvmFieldReference(ownerClassName = "Example", name = "type", descriptor = "Ljava/lang/Class;"),
        ) as JvmObjectReferenceValue
        assertEquals("java/lang/Class", heap.get(storedValue).className)
        assertEquals(JvmClassPayload("Child"), heap.get(storedValue).payload)
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
    fun `GetStaticObjectField returns jclass handles for stored guest static class mirrors`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "type",
                                descriptor = "Ljava/lang/Class;",
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
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "type", "Ljava/lang/Class;")
        staticFields.put(
            JvmFieldReference(ownerClassName = "Example", name = "type", descriptor = "Ljava/lang/Class;"),
            heap.internClassMirror("Child"),
        )

        val resultHandle = environment.getStaticObjectField(classHandle, fieldHandle)

        assertEquals("Child", handles.resolveClass(resultHandle!!))
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
    fun `SetStaticObjectField accepts jclass handles for guest static class mirror fields`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val staticFields = JvmStaticFields()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(
                                name = "type",
                                descriptor = "Ljava/lang/Class;",
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
        val classHandle = environment.findClass("Example")
        val fieldHandle = environment.getStaticFieldId(classHandle, "type", "Ljava/lang/Class;")
        val valueHandle = environment.findClass("Child")

        environment.setStaticObjectField(classHandle, fieldHandle, valueHandle)

        val storedValue = staticFields.get(
            JvmFieldReference(ownerClassName = "Example", name = "type", descriptor = "Ljava/lang/Class;"),
        ) as JvmObjectReferenceValue
        assertEquals("java/lang/Class", heap.get(storedValue).className)
        assertEquals(JvmClassPayload("Child"), heap.get(storedValue).payload)
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
    fun `GetStringCritical returns copied UTF-16 code units for guest strings`() {
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
        val stringHandle = handles.newObjectHandle(heap.allocateString("critical"))

        val result = environment.getStringCritical(stringHandle)

        assertContentEquals(charArrayOf('c', 'r', 'i', 't', 'i', 'c', 'a', 'l'), result)
    }

    @Test
    fun `GetStringCritical rejects non string guest object handles`() {
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
            environment.getStringCritical(objectHandle)
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
    fun `ReleaseStringCritical accepts copied UTF-16 buffers for guest strings`() {
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
        val chars = environment.getStringCritical(stringHandle)

        environment.releaseStringCritical(stringHandle, chars)

        assertContentEquals(charArrayOf('J', 'V', 'M'), chars)
    }

    @Test
    fun `ReleaseStringCritical rejects non string guest object handles`() {
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
            environment.releaseStringCritical(objectHandle, charArrayOf('x'))
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
    fun `GetPrimitiveArrayCritical returns copied primitive array elements`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(3)
        environment.setIntArrayRegion(arrayHandle, start = 0, values = intArrayOf(1, 2, 3))

        val result = environment.getPrimitiveArrayCritical(arrayHandle)

        val ints = result as JvmJniPrimitiveArrayCritical.Ints
        assertContentEquals(intArrayOf(1, 2, 3), ints.elements)
    }

    @Test
    fun `GetPrimitiveArrayCritical rejects reference arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = handles.newObjectHandle(heap.allocateReferenceArray("java/lang/String", 1))

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getPrimitiveArrayCritical(arrayHandle)
        }
    }

    @Test
    fun `ReleasePrimitiveArrayCritical copies primitive critical buffers back to guest arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(3)
        environment.setIntArrayRegion(arrayHandle, start = 0, values = intArrayOf(1, 2, 3))
        val critical = environment.getPrimitiveArrayCritical(arrayHandle) as JvmJniPrimitiveArrayCritical.Ints
        critical.elements[1] = 42

        environment.releasePrimitiveArrayCritical(arrayHandle, critical)

        assertContentEquals(intArrayOf(1, 42, 3), environment.getIntArrayElements(arrayHandle))
    }

    @Test
    fun `ReleasePrimitiveArrayCritical honors JNI_ABORT for primitive critical buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(2)
        environment.setByteArrayRegion(arrayHandle, start = 0, values = byteArrayOf(1, 2))
        val critical = environment.getPrimitiveArrayCritical(arrayHandle) as JvmJniPrimitiveArrayCritical.Bytes
        critical.elements[0] = 9

        environment.releasePrimitiveArrayCritical(arrayHandle, critical, JvmJniArrayReleaseMode.Abort)

        assertContentEquals(byteArrayOf(1, 2), environment.getByteArrayElements(arrayHandle))
    }

    @Test
    fun `NewDirectByteBuffer creates a guest direct byte buffer for a native address`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )

        val bufferHandle = environment.newDirectByteBuffer(address = 0x2000L, capacity = 128L)

        val bufferReference = handles.resolveObject(bufferHandle)
        val bufferObject = heap.get(bufferReference)
        assertEquals("java/nio/DirectByteBuffer", bufferObject.className)
        assertEquals(JvmDirectByteBufferPayload(address = 0x2000L, capacity = 128L), bufferObject.payload)
    }

    @Test
    fun `GetDirectBufferAddress returns the simulated native address for direct byte buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val bufferHandle = environment.newDirectByteBuffer(address = 0x3000L, capacity = 256L)

        val address = environment.getDirectBufferAddress(bufferHandle)

        assertEquals(0x3000L, address)
    }

    @Test
    fun `GetDirectBufferAddress rejects non direct byte buffer handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("java/lang/Object"))

        assertFailsWith<JvmJniDirectBufferAccessException> {
            environment.getDirectBufferAddress(objectHandle)
        }
    }

    @Test
    fun `GetDirectBufferCapacity returns the simulated capacity for direct byte buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val bufferHandle = environment.newDirectByteBuffer(address = 0x4000L, capacity = 512L)

        val capacity = environment.getDirectBufferCapacity(bufferHandle)

        assertEquals(512L, capacity)
    }

    @Test
    fun `GetDirectBufferCapacity rejects non direct byte buffer handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("java/lang/Object"))

        assertFailsWith<JvmJniDirectBufferAccessException> {
            environment.getDirectBufferCapacity(objectHandle)
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
    fun `SetFloatArrayRegion writes a native float buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newFloatArray(5)

        environment.setFloatArrayRegion(
            arrayHandle,
            start = 1,
            values = floatArrayOf(-1.5f, 0.0f, 2.25f),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmFloatArrayPayload
        assertEquals(
            mutableListOf(0.0f, -1.5f, 0.0f, 2.25f, 0.0f),
            payload.elements,
        )
    }

    @Test
    fun `SetFloatArrayRegion rejects non float arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newIntArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setFloatArrayRegion(wrongArrayHandle, start = 0, values = floatArrayOf())
        }

        val arrayHandle = environment.newFloatArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setFloatArrayRegion(arrayHandle, start = 2, values = floatArrayOf(-1.5f, 0.0f, 2.25f))
        }
    }

    @Test
    fun `GetDoubleArrayRegion returns a copied guest double array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newDoubleArray(5)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmDoubleArrayPayload
        payload.elements[1] = -1.5
        payload.elements[3] = 2.25

        val result = environment.getDoubleArrayRegion(arrayHandle, start = 1, length = 3)

        assertContentEquals(doubleArrayOf(-1.5, 0.0, 2.25), result)
        result[0] = 99.0
        assertEquals(-1.5, payload.elements[1])
    }

    @Test
    fun `GetDoubleArrayRegion rejects non double arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newFloatArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getDoubleArrayRegion(wrongArrayHandle, start = 0, length = 1)
        }

        val arrayHandle = environment.newDoubleArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.getDoubleArrayRegion(arrayHandle, start = 2, length = 2)
        }
    }

    @Test
    fun `SetDoubleArrayRegion writes a native double buffer into a guest array range`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newDoubleArray(5)

        environment.setDoubleArrayRegion(
            arrayHandle,
            start = 1,
            values = doubleArrayOf(-1.5, 0.0, 2.25),
        )

        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmDoubleArrayPayload
        assertEquals(
            mutableListOf(0.0, -1.5, 0.0, 2.25, 0.0),
            payload.elements,
        )
    }

    @Test
    fun `SetDoubleArrayRegion rejects non double arrays and invalid ranges`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val wrongArrayHandle = environment.newFloatArray(3)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.setDoubleArrayRegion(wrongArrayHandle, start = 0, values = doubleArrayOf())
        }

        val arrayHandle = environment.newDoubleArray(3)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.setDoubleArrayRegion(arrayHandle, start = 2, values = doubleArrayOf(-1.5, 0.0, 2.25))
        }
    }

    @Test
    fun `GetBooleanArrayElements returns a copied guest boolean array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newBooleanArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmBooleanArrayPayload
        payload.elements[0] = true
        payload.elements[2] = true

        val result = environment.getBooleanArrayElements(arrayHandle)

        assertContentEquals(booleanArrayOf(true, false, true), result)
        result[0] = false
        assertEquals(true, payload.elements[0])
    }

    @Test
    fun `GetBooleanArrayElements rejects non boolean arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newByteArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getBooleanArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseBooleanArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newBooleanArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmBooleanArrayPayload
        val defaultBuffer = booleanArrayOf(true, false, true)
        val commitBuffer = booleanArrayOf(false, true, false)

        environment.releaseBooleanArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf(true, false, true), payload.elements)

        environment.releaseBooleanArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(false, true, false), payload.elements)
    }

    @Test
    fun `ReleaseBooleanArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newBooleanArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmBooleanArrayPayload
        payload.elements[0] = true

        environment.releaseBooleanArrayElements(
            arrayHandle,
            booleanArrayOf(false, true),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(true, false), payload.elements)
    }

    @Test
    fun `ReleaseBooleanArrayElements rejects non boolean arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newByteArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseBooleanArrayElements(wrongArrayHandle, booleanArrayOf(false))
        }

        val arrayHandle = environment.newBooleanArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseBooleanArrayElements(arrayHandle, booleanArrayOf(true))
        }
    }

    @Test
    fun `GetByteArrayElements returns a copied guest byte array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmByteArrayPayload
        payload.elements[0] = (-1).toByte()
        payload.elements[2] = 7.toByte()

        val result = environment.getByteArrayElements(arrayHandle)

        assertContentEquals(byteArrayOf((-1).toByte(), 0.toByte(), 7.toByte()), result)
        result[0] = 99.toByte()
        assertEquals((-1).toByte(), payload.elements[0])
    }

    @Test
    fun `GetByteArrayElements rejects non byte arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newBooleanArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getByteArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseByteArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmByteArrayPayload
        val defaultBuffer = byteArrayOf((-1).toByte(), 0.toByte(), 7.toByte())
        val commitBuffer = byteArrayOf(8.toByte(), (-2).toByte(), 0.toByte())

        environment.releaseByteArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf((-1).toByte(), 0.toByte(), 7.toByte()), payload.elements)

        environment.releaseByteArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8.toByte(), (-2).toByte(), 0.toByte()), payload.elements)
    }

    @Test
    fun `ReleaseByteArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newByteArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmByteArrayPayload
        payload.elements[0] = 3.toByte()

        environment.releaseByteArrayElements(
            arrayHandle,
            byteArrayOf(4.toByte(), 5.toByte()),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3.toByte(), 0.toByte()), payload.elements)
    }

    @Test
    fun `ReleaseByteArrayElements rejects non byte arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newBooleanArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseByteArrayElements(wrongArrayHandle, byteArrayOf(0.toByte()))
        }

        val arrayHandle = environment.newByteArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseByteArrayElements(arrayHandle, byteArrayOf(1.toByte()))
        }
    }

    @Test
    fun `GetCharArrayElements returns a copied guest char array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newCharArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmCharArrayPayload
        payload.elements[0] = 'a'
        payload.elements[2] = '\uD83D'

        val result = environment.getCharArrayElements(arrayHandle)

        assertContentEquals(charArrayOf('a', '\u0000', '\uD83D'), result)
        result[0] = 'z'
        assertEquals('a', payload.elements[0])
    }

    @Test
    fun `GetCharArrayElements rejects non char arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newByteArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getCharArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseCharArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newCharArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmCharArrayPayload
        val defaultBuffer = charArrayOf('a', '\u0000', '\uD83D')
        val commitBuffer = charArrayOf('x', 'y', 'z')

        environment.releaseCharArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf('a', '\u0000', '\uD83D'), payload.elements)

        environment.releaseCharArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf('x', 'y', 'z'), payload.elements)
    }

    @Test
    fun `ReleaseCharArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newCharArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmCharArrayPayload
        payload.elements[0] = 'a'

        environment.releaseCharArrayElements(
            arrayHandle,
            charArrayOf('b', 'c'),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf('a', '\u0000'), payload.elements)
    }

    @Test
    fun `ReleaseCharArrayElements rejects non char arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newByteArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseCharArrayElements(wrongArrayHandle, charArrayOf('a'))
        }

        val arrayHandle = environment.newCharArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseCharArrayElements(arrayHandle, charArrayOf('a'))
        }
    }

    @Test
    fun `GetShortArrayElements returns a copied guest short array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newShortArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmShortArrayPayload
        payload.elements[0] = (-123).toShort()
        payload.elements[2] = 456.toShort()

        val result = environment.getShortArrayElements(arrayHandle)

        assertContentEquals(shortArrayOf((-123).toShort(), 0.toShort(), 456.toShort()), result)
        result[0] = 99.toShort()
        assertEquals((-123).toShort(), payload.elements[0])
    }

    @Test
    fun `GetShortArrayElements rejects non short arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newIntArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getShortArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseShortArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newShortArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmShortArrayPayload
        val defaultBuffer = shortArrayOf((-123).toShort(), 0.toShort(), 456.toShort())
        val commitBuffer = shortArrayOf(8.toShort(), (-2).toShort(), 0.toShort())

        environment.releaseShortArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf((-123).toShort(), 0.toShort(), 456.toShort()), payload.elements)

        environment.releaseShortArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8.toShort(), (-2).toShort(), 0.toShort()), payload.elements)
    }

    @Test
    fun `ReleaseShortArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newShortArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmShortArrayPayload
        payload.elements[0] = 3.toShort()

        environment.releaseShortArrayElements(
            arrayHandle,
            shortArrayOf(4.toShort(), 5.toShort()),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3.toShort(), 0.toShort()), payload.elements)
    }

    @Test
    fun `ReleaseShortArrayElements rejects non short arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newIntArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseShortArrayElements(wrongArrayHandle, shortArrayOf(0.toShort()))
        }

        val arrayHandle = environment.newShortArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseShortArrayElements(arrayHandle, shortArrayOf(1.toShort()))
        }
    }

    @Test
    fun `GetIntArrayElements returns a copied guest int array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmIntArrayPayload
        payload.elements[0] = -7
        payload.elements[2] = 12

        val result = environment.getIntArrayElements(arrayHandle)

        assertContentEquals(intArrayOf(-7, 0, 12), result)
        result[0] = 99
        assertEquals(-7, payload.elements[0])
    }

    @Test
    fun `GetIntArrayElements rejects non int arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newLongArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getIntArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseIntArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmIntArrayPayload
        val defaultBuffer = intArrayOf(-7, 0, 12)
        val commitBuffer = intArrayOf(8, -2, 0)

        environment.releaseIntArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf(-7, 0, 12), payload.elements)

        environment.releaseIntArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8, -2, 0), payload.elements)
    }

    @Test
    fun `ReleaseIntArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newIntArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmIntArrayPayload
        payload.elements[0] = 3

        environment.releaseIntArrayElements(
            arrayHandle,
            intArrayOf(4, 5),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3, 0), payload.elements)
    }

    @Test
    fun `ReleaseIntArrayElements rejects non int arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newLongArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseIntArrayElements(wrongArrayHandle, intArrayOf(0))
        }

        val arrayHandle = environment.newIntArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseIntArrayElements(arrayHandle, intArrayOf(1))
        }
    }

    @Test
    fun `GetLongArrayElements returns a copied guest long array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newLongArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmLongArrayPayload
        payload.elements[0] = -7L
        payload.elements[2] = 12L

        val result = environment.getLongArrayElements(arrayHandle)

        assertContentEquals(longArrayOf(-7L, 0L, 12L), result)
        result[0] = 99L
        assertEquals(-7L, payload.elements[0])
    }

    @Test
    fun `GetLongArrayElements rejects non long arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newIntArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getLongArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseLongArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newLongArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmLongArrayPayload
        val defaultBuffer = longArrayOf(-7L, 0L, 12L)
        val commitBuffer = longArrayOf(8L, -2L, 0L)

        environment.releaseLongArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf(-7L, 0L, 12L), payload.elements)

        environment.releaseLongArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8L, -2L, 0L), payload.elements)
    }

    @Test
    fun `ReleaseLongArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newLongArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmLongArrayPayload
        payload.elements[0] = 3L

        environment.releaseLongArrayElements(
            arrayHandle,
            longArrayOf(4L, 5L),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3L, 0L), payload.elements)
    }

    @Test
    fun `ReleaseLongArrayElements rejects non long arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newIntArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseLongArrayElements(wrongArrayHandle, longArrayOf(0L))
        }

        val arrayHandle = environment.newLongArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseLongArrayElements(arrayHandle, longArrayOf(1L))
        }
    }

    @Test
    fun `GetFloatArrayElements returns a copied guest float array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newFloatArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmFloatArrayPayload
        payload.elements[0] = -1.5f
        payload.elements[2] = 2.25f

        val result = environment.getFloatArrayElements(arrayHandle)

        assertContentEquals(floatArrayOf(-1.5f, 0.0f, 2.25f), result)
        result[0] = 99.0f
        assertEquals(-1.5f, payload.elements[0])
    }

    @Test
    fun `GetFloatArrayElements rejects non float arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newDoubleArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getFloatArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseFloatArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newFloatArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmFloatArrayPayload
        val defaultBuffer = floatArrayOf(-1.5f, 0.0f, 2.25f)
        val commitBuffer = floatArrayOf(8.0f, -2.5f, 0.0f)

        environment.releaseFloatArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf(-1.5f, 0.0f, 2.25f), payload.elements)

        environment.releaseFloatArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8.0f, -2.5f, 0.0f), payload.elements)
    }

    @Test
    fun `ReleaseFloatArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newFloatArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmFloatArrayPayload
        payload.elements[0] = 3.5f

        environment.releaseFloatArrayElements(
            arrayHandle,
            floatArrayOf(4.5f, 5.5f),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3.5f, 0.0f), payload.elements)
    }

    @Test
    fun `ReleaseFloatArrayElements rejects non float arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newDoubleArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseFloatArrayElements(wrongArrayHandle, floatArrayOf(0.0f))
        }

        val arrayHandle = environment.newFloatArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseFloatArrayElements(arrayHandle, floatArrayOf(1.0f))
        }
    }

    @Test
    fun `GetDoubleArrayElements returns a copied guest double array buffer`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newDoubleArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmDoubleArrayPayload
        payload.elements[0] = -1.5
        payload.elements[2] = 2.25

        val result = environment.getDoubleArrayElements(arrayHandle)

        assertContentEquals(doubleArrayOf(-1.5, 0.0, 2.25), result)
        result[0] = 99.0
        assertEquals(-1.5, payload.elements[0])
    }

    @Test
    fun `GetDoubleArrayElements rejects non double arrays`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newFloatArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.getDoubleArrayElements(wrongArrayHandle)
        }
    }

    @Test
    fun `ReleaseDoubleArrayElements copies back default and commit buffers`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newDoubleArray(3)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmDoubleArrayPayload
        val defaultBuffer = doubleArrayOf(-1.5, 0.0, 2.25)
        val commitBuffer = doubleArrayOf(8.0, -2.5, 0.0)

        environment.releaseDoubleArrayElements(arrayHandle, defaultBuffer)
        assertEquals(mutableListOf(-1.5, 0.0, 2.25), payload.elements)

        environment.releaseDoubleArrayElements(
            arrayHandle,
            commitBuffer,
            JvmJniArrayReleaseMode.Commit,
        )
        assertEquals(mutableListOf(8.0, -2.5, 0.0), payload.elements)
    }

    @Test
    fun `ReleaseDoubleArrayElements aborts without copying back`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val arrayHandle = environment.newDoubleArray(2)
        val payload = heap.get(handles.resolveObject(arrayHandle)).payload as JvmDoubleArrayPayload
        payload.elements[0] = 3.5

        environment.releaseDoubleArrayElements(
            arrayHandle,
            doubleArrayOf(4.5, 5.5),
            JvmJniArrayReleaseMode.Abort,
        )

        assertEquals(mutableListOf(3.5, 0.0), payload.elements)
    }

    @Test
    fun `ReleaseDoubleArrayElements rejects non double arrays and mismatched buffers`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        val wrongArrayHandle = environment.newFloatArray(1)

        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseDoubleArrayElements(wrongArrayHandle, doubleArrayOf(0.0))
        }

        val arrayHandle = environment.newDoubleArray(2)
        assertFailsWith<JvmJniArrayAccessException> {
            environment.releaseDoubleArrayElements(arrayHandle, doubleArrayOf(1.0))
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
    fun `NewObjectArray accepts jclass initial elements for guest Class arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classClassHandle = environment.findClass("java/lang/Class")
        val childClassHandle = environment.findClass("Child")

        val arrayHandle = environment.newObjectArray(2, classClassHandle, childClassHandle)

        val arrayReference = handles.resolveObject(arrayHandle)
        val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        array.elements.forEach { element ->
            val classMirror = element as JvmObjectReferenceValue
            assertEquals("java/lang/Class", heap.get(classMirror).className)
            assertEquals(JvmClassPayload("Child"), heap.get(classMirror).payload)
        }
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
    fun `GetObjectArrayElement returns jclass handles for guest class mirror array slots`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classClassHandle = environment.findClass("java/lang/Class")
        val arrayHandle = environment.newObjectArray(1, classClassHandle, null)
        val arrayReference = handles.resolveObject(arrayHandle)
        val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        array.elements[0] = heap.internClassMirror("Child")

        val result = environment.getObjectArrayElement(arrayHandle, 0)

        assertEquals("Child", handles.resolveClass(result!!))
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
    fun `SetObjectArrayElement accepts jclass handles for guest Class arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "Child"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classClassHandle = environment.findClass("java/lang/Class")
        val childClassHandle = environment.findClass("Child")
        val arrayHandle = environment.newObjectArray(1, classClassHandle, null)

        environment.setObjectArrayElement(arrayHandle, 0, childClassHandle)

        val arrayReference = handles.resolveObject(arrayHandle)
        val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val storedValue = array.elements[0] as JvmObjectReferenceValue
        assertEquals("java/lang/Class", heap.get(storedValue).className)
        assertEquals(JvmClassPayload("Child"), heap.get(storedValue).payload)
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
    fun `GetObjectArrayRegion returns jclass handles for guest class mirror array slots`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "First"),
                    JvmClassDefinition(internalName = "Second"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classClassHandle = environment.findClass("java/lang/Class")
        val arrayHandle = environment.newObjectArray(3, classClassHandle, null)
        val arrayReference = handles.resolveObject(arrayHandle)
        val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        array.elements[0] = heap.internClassMirror("First")
        array.elements[2] = heap.internClassMirror("Second")

        val result = environment.getObjectArrayRegion(arrayHandle, start = 0, length = 3)

        assertEquals("First", handles.resolveClass(result[0]!!))
        assertEquals(null, result[1])
        assertEquals("Second", handles.resolveClass(result[2]!!))
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
    fun `SetObjectArrayRegion accepts jclass handles for guest Class arrays`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Class"),
                    JvmClassDefinition(internalName = "First"),
                    JvmClassDefinition(internalName = "Second"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classClassHandle = environment.findClass("java/lang/Class")
        val firstClassHandle = environment.findClass("First")
        val secondClassHandle = environment.findClass("Second")
        val arrayHandle = environment.newObjectArray(3, classClassHandle, null)

        environment.setObjectArrayRegion(arrayHandle, start = 0, values = listOf(firstClassHandle, null, secondClassHandle))

        val arrayReference = handles.resolveObject(arrayHandle)
        val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
        val firstValue = array.elements[0] as JvmObjectReferenceValue
        val secondValue = array.elements[2] as JvmObjectReferenceValue
        assertEquals(JvmClassPayload("First"), heap.get(firstValue).payload)
        assertEquals(JvmNullValue, array.elements[1])
        assertEquals(JvmClassPayload("Second"), heap.get(secondValue).payload)
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

    @Test
    fun `Throw records a pending guest throwable until ExceptionClear`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val throwableReference = heap.allocateObject("java/lang/IllegalStateException")
        val throwableHandle = handles.newObjectHandle(throwableReference)

        assertEquals(false, environment.exceptionCheck())
        assertEquals(null, environment.exceptionOccurred())
        assertEquals(null, environment.pendingExceptionReference)

        assertEquals(0, environment.throwObject(throwableHandle))

        assertEquals(true, environment.exceptionCheck())
        assertEquals(throwableReference, environment.pendingExceptionReference)
        val occurredHandle = environment.exceptionOccurred()
        assertEquals(throwableReference, handles.resolveObject(occurredHandle!!))

        environment.exceptionClear()

        assertEquals(false, environment.exceptionCheck())
        assertEquals(null, environment.exceptionOccurred())
        assertEquals(null, environment.pendingExceptionReference)
    }

    @Test
    fun `ExceptionOccurred returns local handles without clearing the pending exception`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val throwableReference = heap.allocateObject("java/lang/RuntimeException")
        val throwableHandle = handles.newObjectHandle(throwableReference)

        environment.throwObject(throwableHandle)

        val firstOccurredHandle = environment.exceptionOccurred()
        val secondOccurredHandle = environment.exceptionOccurred()

        assertEquals(throwableReference, handles.resolveObject(firstOccurredHandle!!))
        assertEquals(throwableReference, handles.resolveObject(secondOccurredHandle!!))
        assertEquals(true, environment.exceptionCheck())
    }

    @Test
    fun `ThrowNew allocates a guest throwable with a detail message and records it pending`() {
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
        )
        val throwableClassHandle = environment.findClass("java/lang/IllegalArgumentException")

        assertEquals(0, environment.throwNew(throwableClassHandle, "bad argument"))

        val pendingHandle = environment.exceptionOccurred()
        val pendingReference = handles.resolveObject(pendingHandle!!)
        val pendingObject = heap.get(pendingReference)
        val detailMessageField = JvmFieldReference(
            ownerClassName = "java/lang/Throwable",
            name = "detailMessage",
            descriptor = "Ljava/lang/String;",
        )
        val detailMessageReference = heap.getInstanceField(pendingReference, detailMessageField) as JvmObjectReferenceValue

        assertEquals("java/lang/IllegalArgumentException", pendingObject.className)
        assertEquals(JvmStringPayload("bad argument"), heap.get(detailMessageReference).payload)
        assertEquals(true, environment.exceptionCheck())
    }

    @Test
    fun `Throw rejects non throwable guest object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "NotThrowable", superclassName = "java/lang/Object"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val objectHandle = handles.newObjectHandle(heap.allocateObject("NotThrowable"))

        val exception = assertFailsWith<JvmJniExceptionAccessException> {
            environment.throwObject(objectHandle)
        }

        assertEquals("JNI exception helper requires java/lang/Throwable, got NotThrowable", exception.message)
        assertEquals(false, environment.exceptionCheck())
    }

    @Test
    fun `ThrowNew rejects non throwable guest class handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Object"),
                    JvmClassDefinition(internalName = "NotThrowable", superclassName = "java/lang/Object"),
                ),
            ),
            handles = handles,
        )
        val classHandle = environment.findClass("NotThrowable")

        val exception = assertFailsWith<JvmJniExceptionAccessException> {
            environment.throwNew(classHandle, "bad")
        }

        assertEquals("JNI exception helper requires java/lang/Throwable, got NotThrowable", exception.message)
        assertEquals(false, environment.exceptionCheck())
    }

    @Test
    fun `ExceptionDescribe reports pending throwable and clears it`() {
        val reported = mutableListOf<String>()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            handles = handles,
            exceptionReporter = { text -> reported += text },
        )
        val throwableClassHandle = handles.newClassHandle("java/lang/IllegalStateException")
        environment.throwNew(throwableClassHandle, "broken")

        environment.exceptionDescribe()

        assertEquals(listOf("java/lang/IllegalStateException: broken"), reported)
        assertEquals(false, environment.exceptionCheck())
        assertEquals(null, environment.pendingExceptionReference)
    }

    @Test
    fun `ExceptionDescribe is a no op when there is no pending exception`() {
        val reported = mutableListOf<String>()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            exceptionReporter = { text -> reported += text },
        )

        environment.exceptionDescribe()

        assertEquals(emptyList(), reported)
        assertEquals(false, environment.exceptionCheck())
    }

    @Test
    fun `FatalError raises a non returning simulated JNI fatal error`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val fatal = assertFailsWith<JvmJniFatalError> {
            environment.fatalError("native invariant failed")
        }

        assertEquals("native invariant failed", fatal.message)
        assertEquals(false, environment.exceptionCheck())
    }

    @Test
    fun `FatalError accepts null messages as an empty fatal detail`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val fatal = assertFailsWith<JvmJniFatalError> {
            environment.fatalError(null)
        }

        assertEquals("", fatal.message)
    }

    @Test
    fun `DeleteLocalRef and DeleteGlobalRef reject handles from the wrong reference scope`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val localHandle = handles.newObjectHandle(objectReference)
        val globalHandle = environment.newGlobalRef(localHandle)!!

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteLocalRef(globalHandle)
        }
        assertEquals(objectReference, handles.resolveObject(globalHandle))

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteGlobalRef(localHandle)
        }
        assertEquals(objectReference, handles.resolveObject(localHandle))
    }

    @Test
    fun `NewGlobalRef survives local frame pop until DeleteGlobalRef`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        environment.pushLocalFrame(2)
        val localHandle = handles.newObjectHandle(objectReference)

        val globalHandle = environment.newGlobalRef(localHandle)
        val nullHandle = environment.newGlobalRef(null)
        environment.popLocalFrame(null)

        val liveGlobalHandle = globalHandle!!
        assertEquals(null, nullHandle)
        assertEquals(objectReference, handles.resolveObject(liveGlobalHandle))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(localHandle)
        }

        environment.deleteGlobalRef(null)
        environment.deleteGlobalRef(liveGlobalHandle)

        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(liveGlobalHandle)
        }
    }

    @Test
    fun `NewGlobalRef creates class global handles that survive local frame pop until DeleteGlobalRef`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        environment.pushLocalFrame(2)
        val localClassHandle = environment.findClass("Example")

        val globalClassHandle = environment.newGlobalRef(localClassHandle)!!
        environment.popLocalFrame(null)

        assertEquals("Example", handles.resolveClass(globalClassHandle))
        assertEquals(JvmJniReferenceType.Global, environment.getObjectRefType(globalClassHandle))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveClass(localClassHandle)
        }

        environment.deleteGlobalRef(globalClassHandle)
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveClass(globalClassHandle)
        }
    }

    @Test
    fun `NewWeakGlobalRef survives local frame pop until DeleteWeakGlobalRef`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        environment.pushLocalFrame(2)
        val localHandle = handles.newObjectHandle(objectReference)

        val weakGlobalHandle = environment.newWeakGlobalRef(localHandle)
        val nullHandle = environment.newWeakGlobalRef(null)
        environment.popLocalFrame(null)

        val liveWeakGlobalHandle = weakGlobalHandle!!
        assertEquals(null, nullHandle)
        assertEquals(objectReference, handles.resolveObject(liveWeakGlobalHandle))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(localHandle)
        }

        environment.deleteWeakGlobalRef(null)
        environment.deleteWeakGlobalRef(liveWeakGlobalHandle)

        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(liveWeakGlobalHandle)
        }
    }

    @Test
    fun `NewWeakGlobalRef creates class weak global handles that survive local frame pop until DeleteWeakGlobalRef`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        environment.pushLocalFrame(2)
        val localClassHandle = environment.findClass("Example")

        val weakGlobalClassHandle = environment.newWeakGlobalRef(localClassHandle)!!
        environment.popLocalFrame(null)

        assertEquals("Example", handles.resolveClass(weakGlobalClassHandle))
        assertEquals(JvmJniReferenceType.WeakGlobal, environment.getObjectRefType(weakGlobalClassHandle))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveClass(localClassHandle)
        }

        environment.deleteWeakGlobalRef(weakGlobalClassHandle)
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveClass(weakGlobalClassHandle)
        }
    }

    @Test
    fun `DeleteWeakGlobalRef enforces weak global reference scope`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val localHandle = handles.newObjectHandle(objectReference)
        val globalHandle = environment.newGlobalRef(localHandle)!!
        val weakGlobalHandle = environment.newWeakGlobalRef(localHandle)!!

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteLocalRef(weakGlobalHandle)
        }
        assertEquals(objectReference, handles.resolveObject(weakGlobalHandle))

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteGlobalRef(weakGlobalHandle)
        }
        assertEquals(objectReference, handles.resolveObject(weakGlobalHandle))

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteWeakGlobalRef(localHandle)
        }
        assertEquals(objectReference, handles.resolveObject(localHandle))

        assertFailsWith<JvmJniHandleScopeException> {
            environment.deleteWeakGlobalRef(globalHandle)
        }
        assertEquals(objectReference, handles.resolveObject(globalHandle))
    }

    @Test
    fun `GetObjectRefType reports local global weak and invalid references`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val localHandle = handles.newObjectHandle(objectReference)
        val globalHandle = environment.newGlobalRef(localHandle)!!
        val weakGlobalHandle = environment.newWeakGlobalRef(localHandle)!!
        val deletedLocalHandle = handles.newObjectHandle(objectReference)

        environment.deleteLocalRef(deletedLocalHandle)

        assertEquals(JvmJniReferenceType.Local, environment.getObjectRefType(localHandle))
        assertEquals(JvmJniReferenceType.Global, environment.getObjectRefType(globalHandle))
        assertEquals(JvmJniReferenceType.WeakGlobal, environment.getObjectRefType(weakGlobalHandle))
        assertEquals(JvmJniReferenceType.Invalid, environment.getObjectRefType(deletedLocalHandle))
        assertEquals(JvmJniReferenceType.Invalid, environment.getObjectRefType(null))
    }

    @Test
    fun `IsSameObject compares nullable object local handles by guest reference identity`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val firstReference = heap.allocateObject("Example")
        val secondReference = heap.allocateObject("Example")
        val firstHandle = handles.newObjectHandle(firstReference)
        val duplicatedFirstHandle = environment.newLocalRef(firstHandle)
        val secondHandle = handles.newObjectHandle(secondReference)

        assertEquals(true, environment.isSameObject(null, null))
        assertEquals(false, environment.isSameObject(firstHandle, null))
        assertEquals(false, environment.isSameObject(null, firstHandle))
        assertEquals(true, environment.isSameObject(firstHandle, duplicatedFirstHandle))
        assertEquals(false, environment.isSameObject(firstHandle, secondHandle))
    }

    @Test
    fun `IsSameObject compares class reference handles by guest class identity`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "First"),
                    JvmClassDefinition(internalName = "Second"),
                ),
            ),
            handles = handles,
        )
        val firstLocalHandle = environment.findClass("First")
        val firstGlobalHandle = environment.newGlobalRef(firstLocalHandle)
        val firstWeakGlobalHandle = environment.newWeakGlobalRef(firstLocalHandle)
        val secondLocalHandle = environment.findClass("Second")

        assertEquals(true, environment.isSameObject(firstLocalHandle, firstGlobalHandle))
        assertEquals(true, environment.isSameObject(firstLocalHandle, firstWeakGlobalHandle))
        assertEquals(false, environment.isSameObject(firstLocalHandle, secondLocalHandle))
    }

    @Test
    fun `IsSameObject compares jclass handles with guest Class mirror object handles`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                    JvmClassDefinition(internalName = "Other"),
                ),
            ),
            heap = heap,
            handles = handles,
        )
        val classHandle = environment.findClass("Example")
        val classMirrorHandle = handles.newObjectHandle(heap.internClassMirror("Example"))
        val otherClassMirrorHandle = handles.newObjectHandle(heap.internClassMirror("Other"))

        assertEquals(true, environment.isSameObject(classHandle, classMirrorHandle))
        assertEquals(false, environment.isSameObject(classHandle, otherClassMirrorHandle))
    }

    @Test
    fun `NewLocalRef duplicates object local handles and preserves null references`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val originalHandle = handles.newObjectHandle(objectReference)

        val duplicatedHandle = environment.newLocalRef(originalHandle)
        val nullHandle = environment.newLocalRef(null)
        environment.deleteLocalRef(originalHandle)

        assertEquals(null, nullHandle)
        assertEquals(objectReference, handles.resolveObject(duplicatedHandle!!))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(originalHandle)
        }
    }

    @Test
    fun `NewLocalRef duplicates class local handles`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        val originalHandle = environment.findClass("Example")

        val duplicatedHandle = environment.newLocalRef(originalHandle)

        assertEquals("Example", handles.resolveClass(duplicatedHandle!!))
        assertEquals(false, duplicatedHandle == originalHandle)
        environment.deleteLocalRef(originalHandle)
        assertEquals("Example", handles.resolveClass(duplicatedHandle))
    }

    @Test
    fun `DeleteLocalRef deletes object local handles and ignores null references`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        environment.deleteLocalRef(null)
        environment.deleteLocalRef(objectHandle)

        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(objectHandle)
        }
    }

    @Test
    fun `EnsureLocalCapacity records the requested local reference capacity`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertEquals(0, environment.ensureLocalCapacity(8))
        assertEquals(8, environment.ensuredLocalCapacity)

        assertEquals(0, environment.ensureLocalCapacity(3))
        assertEquals(8, environment.ensuredLocalCapacity)

        assertEquals(0, environment.ensureLocalCapacity(32))
        assertEquals(32, environment.ensuredLocalCapacity)
    }

    @Test
    fun `EnsureLocalCapacity rejects negative capacities`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val exception = assertFailsWith<IllegalArgumentException> {
            environment.ensureLocalCapacity(-1)
        }

        assertEquals("JNI local capacity must be non-negative: -1", exception.message)
    }

    @Test
    fun `PushLocalFrame records nested local frame depth and requested capacity`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        assertEquals(0, environment.localFrameDepth)
        assertEquals(0, environment.maxLocalFrameCapacity)

        assertEquals(0, environment.pushLocalFrame(4))
        assertEquals(1, environment.localFrameDepth)
        assertEquals(4, environment.maxLocalFrameCapacity)

        assertEquals(0, environment.pushLocalFrame(2))
        assertEquals(2, environment.localFrameDepth)
        assertEquals(4, environment.maxLocalFrameCapacity)

        assertEquals(0, environment.pushLocalFrame(16))
        assertEquals(3, environment.localFrameDepth)
        assertEquals(16, environment.maxLocalFrameCapacity)
    }

    @Test
    fun `PushLocalFrame rejects non positive capacities`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val zero = assertFailsWith<IllegalArgumentException> {
            environment.pushLocalFrame(0)
        }
        val negative = assertFailsWith<IllegalArgumentException> {
            environment.pushLocalFrame(-1)
        }

        assertEquals("JNI local frame capacity must be positive: 0", zero.message)
        assertEquals("JNI local frame capacity must be positive: -1", negative.message)
        assertEquals(0, environment.localFrameDepth)
    }

    @Test
    fun `PopLocalFrame pops the current local frame and returns null results`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)
        environment.pushLocalFrame(4)
        environment.pushLocalFrame(2)

        val result = environment.popLocalFrame(null)

        assertEquals(null, result)
        assertEquals(1, environment.localFrameDepth)

        environment.popLocalFrame(null)

        assertEquals(0, environment.localFrameDepth)
    }

    @Test
    fun `PopLocalFrame rebinds non null object results into the previous local frame`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val objectReference = heap.allocateObject("Example")
        environment.pushLocalFrame(4)
        val resultHandle = handles.newObjectHandle(objectReference)

        val reboundResultHandle = environment.popLocalFrame(resultHandle)

        assertEquals(objectReference, handles.resolveObject(reboundResultHandle!!))
        assertEquals(false, reboundResultHandle == resultHandle)
        assertEquals(0, environment.localFrameDepth)
    }

    @Test
    fun `PopLocalFrame rebinds non null class results into the previous local frame`() {
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            handles = handles,
        )
        environment.pushLocalFrame(4)
        val resultHandle = environment.findClass("Example")

        val reboundResultHandle = environment.popLocalFrame(resultHandle)

        assertEquals("Example", handles.resolveClass(reboundResultHandle!!))
        assertEquals(false, reboundResultHandle == resultHandle)
        assertEquals(0, environment.localFrameDepth)
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveClass(resultHandle)
        }
    }

    @Test
    fun `PopLocalFrame deletes handles allocated in the popped local frame`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
        )
        val parentReference = heap.allocateObject("Parent")
        val parentHandle = handles.newObjectHandle(parentReference)
        environment.pushLocalFrame(2)
        val scopedHandle = environment.newStringUtf("scoped")

        environment.popLocalFrame(null)

        assertEquals(parentReference, handles.resolveObject(parentHandle))
        assertFailsWith<JvmJniInvalidHandleException> {
            handles.resolveObject(scopedHandle)
        }
    }

    @Test
    fun `PopLocalFrame rejects local frame underflow`() {
        val environment = JvmSimulatedJniEnvironment(classHierarchy = JvmClassHierarchy.Empty)

        val exception = assertFailsWith<JvmJniLocalFrameException> {
            environment.popLocalFrame(null)
        }

        assertEquals("JNI local frame stack is empty", exception.message)
        assertEquals(0, environment.localFrameDepth)
    }

    @Test
    fun `MonitorEnter records reentrant guest monitor ownership`() {
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
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        assertEquals(1, environment.monitorEnter(objectHandle))
        assertEquals(1, monitors.holdCount(objectReference, "jni-thread"))

        assertEquals(2, environment.monitorEnter(objectHandle))
        assertEquals(2, monitors.holdCount(objectReference, "jni-thread"))
    }

    @Test
    fun `MonitorEnter and MonitorExit accept jclass handles as guest Class mirror objects`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val monitors = JvmMonitorState()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Example"),
                ),
            ),
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "jni-thread",
        )
        val classHandle = environment.findClass("Example")
        val classMirrorReference = heap.internClassMirror("Example")

        assertEquals(1, environment.monitorEnter(classHandle))
        assertEquals(1, monitors.holdCount(classMirrorReference, "jni-thread"))

        assertEquals(0, environment.monitorExit(classHandle))
        assertEquals(0, monitors.holdCount(classMirrorReference, "jni-thread"))
    }

    @Test
    fun `MonitorEnter rejects monitors owned by another simulated JNI thread`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val monitors = JvmMonitorState()
        val ownerEnvironment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "owner-thread",
        )
        val blockedEnvironment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "blocked-thread",
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        ownerEnvironment.monitorEnter(objectHandle)

        assertFailsWith<JvmMonitorOwnershipException> {
            blockedEnvironment.monitorEnter(objectHandle)
        }
        assertEquals(0, monitors.holdCount(objectReference, "blocked-thread"))
    }

    @Test
    fun `MonitorExit decrements and releases guest monitor ownership`() {
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
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)
        environment.monitorEnter(objectHandle)
        environment.monitorEnter(objectHandle)

        assertEquals(1, environment.monitorExit(objectHandle))
        assertEquals(1, monitors.holdCount(objectReference, "jni-thread"))

        assertEquals(0, environment.monitorExit(objectHandle))
        assertEquals(0, monitors.holdCount(objectReference, "jni-thread"))
    }

    @Test
    fun `MonitorExit rejects unowned monitors and monitors owned by another simulated JNI thread`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val monitors = JvmMonitorState()
        val ownerEnvironment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "owner-thread",
        )
        val blockedEnvironment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy.Empty,
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "blocked-thread",
        )
        val objectReference = heap.allocateObject("Example")
        val objectHandle = handles.newObjectHandle(objectReference)

        assertFailsWith<JvmMonitorOwnershipException> {
            ownerEnvironment.monitorExit(objectHandle)
        }

        ownerEnvironment.monitorEnter(objectHandle)

        assertFailsWith<JvmMonitorOwnershipException> {
            blockedEnvironment.monitorExit(objectHandle)
        }
        assertEquals(1, monitors.holdCount(objectReference, "owner-thread"))
    }

    @Test
    fun `JNI data helpers mutate one guest state for refs strings arrays fields and monitors`() {
        val heap = JvmHeap()
        val handles = JvmJniHandleTable()
        val monitors = JvmMonitorState()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        fields = listOf(
                            JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false),
                            JvmFieldDefinition(name = "child", descriptor = "LChild;", isStatic = false),
                            JvmFieldDefinition(name = "shared", descriptor = "I", isStatic = true),
                        ),
                    ),
                    JvmClassDefinition(internalName = "Child"),
                    JvmClassDefinition(internalName = "java/lang/String"),
                ),
            ),
            heap = heap,
            handles = handles,
            monitors = monitors,
            currentThreadId = "jni-thread",
        )
        val objectReference = heap.allocateObject("Example")
        val childReference = heap.allocateObject("Child")
        val objectHandle = handles.newObjectHandle(objectReference)
        val childHandle = handles.newObjectHandle(childReference)
        val exampleClassHandle = environment.findClass("Example")
        val childClassHandle = environment.findClass("Child")
        val counterFieldHandle = environment.getFieldId(exampleClassHandle, "counter", "I")
        val childFieldHandle = environment.getFieldId(exampleClassHandle, "child", "LChild;")
        val sharedFieldHandle = environment.getStaticFieldId(exampleClassHandle, "shared", "I")

        val stringHandle = environment.newStringUtf("guest")
        assertEquals(5, environment.getStringLength(stringHandle))
        assertContentEquals(charArrayOf('g', 'u', 'e', 's', 't'), environment.getStringChars(stringHandle))

        environment.setIntField(objectHandle, counterFieldHandle, 42)
        environment.setObjectField(objectHandle, childFieldHandle, childHandle)
        environment.setStaticIntField(exampleClassHandle, sharedFieldHandle, 7)

        assertEquals(42, environment.getIntField(objectHandle, counterFieldHandle))
        assertEquals(childReference, handles.resolveObject(environment.getObjectField(objectHandle, childFieldHandle)!!))
        assertEquals(7, environment.getStaticIntField(exampleClassHandle, sharedFieldHandle))

        val objectArrayHandle = environment.newObjectArray(2, childClassHandle, null)
        environment.setObjectArrayRegion(objectArrayHandle, start = 0, values = listOf(childHandle, null))
        assertEquals(childReference, handles.resolveObject(environment.getObjectArrayElement(objectArrayHandle, 0)!!))
        assertEquals(null, environment.getObjectArrayElement(objectArrayHandle, 1))

        val intArrayHandle = environment.newIntArray(2)
        environment.setIntArrayRegion(intArrayHandle, start = 0, values = intArrayOf(3, 4))
        val intElements = environment.getIntArrayElements(intArrayHandle)
        intElements[1] = 9
        environment.releaseIntArrayElements(intArrayHandle, intElements)
        assertContentEquals(intArrayOf(3, 9), environment.getIntArrayRegion(intArrayHandle, start = 0, length = 2))

        assertEquals(1, environment.monitorEnter(objectHandle))
        assertEquals(0, environment.monitorExit(objectHandle))
        assertEquals(0, monitors.holdCount(objectReference, "jni-thread"))
    }

}

private data class RecordedVoidUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedObjectUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedBooleanUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedByteUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedCharUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedShortUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedIntUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedLongUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedFloatUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedDoubleUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticVoidUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticObjectUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticBooleanUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticByteUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticCharUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticShortUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticIntUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticLongUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticFloatUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedStaticDoubleUpcall(
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)

private data class RecordedNewObjectUpcall(
    val receiver: JvmObjectReferenceValue,
    val method: JvmResolvedMethod,
    val arguments: List<JvmValue>,
)
