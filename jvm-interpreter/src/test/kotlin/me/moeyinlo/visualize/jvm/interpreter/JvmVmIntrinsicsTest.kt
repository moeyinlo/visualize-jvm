package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmThreadPayload
import me.moeyinlo.visualize.jvm.runtime.JvmThreadScheduler
import me.moeyinlo.visualize.jvm.runtime.JvmThreadSchedulingState
import me.moeyinlo.visualize.jvm.runtime.JvmThrowablePayload
import me.moeyinlo.visualize.jvm.runtime.JvmVmTerminationResult
import me.moeyinlo.visualize.jvm.runtime.JvmVmTerminationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class JvmVmIntrinsicsTest {
    @Test
    fun `native registry resolves signature polymorphic call sites through the erased declaration key`() {
        val erasedKey = JvmNativeMethodKey(
            ownerClassName = "java/lang/invoke/MethodHandle",
            name = "invokeExact",
            descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
            isStatic = false,
        )
        val intrinsic = JvmNativeMethodIntrinsic { _, _ -> JvmLongValue(7L) }
        val registry = JvmNativeMethodRegistry.from(erasedKey to intrinsic)
        val callSiteMethod = JvmResolvedMethod(
            ownerClassName = "java/lang/invoke/MethodHandle",
            name = "invokeExact",
            descriptor = "(Ljava/lang/String;I)J",
            isStatic = false,
            isNative = true,
            isVarargs = true,
            signaturePolymorphicDeclarationDescriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
        )

        assertEquals(intrinsic, registry.resolve(callSiteMethod))
    }

    @Test
    fun `native registry keeps ordinary native method keys descriptor exact`() {
        val erasedKey = JvmNativeMethodKey(
            ownerClassName = "Example",
            name = "nativeValue",
            descriptor = "()I",
            isStatic = false,
        )
        val intrinsic = JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(42) }
        val registry = JvmNativeMethodRegistry.from(erasedKey to intrinsic)
        val mismatchedMethod = JvmResolvedMethod(
            ownerClassName = "Example",
            name = "nativeValue",
            descriptor = "()J",
            isStatic = false,
            isNative = true,
        )

        assertEquals(null, registry.resolve(mismatchedMethod))
    }

    @Test
    fun `native registry uses intrinsics only for whitelisted owners before simulated JNI fallback`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "GuestNative",
            name = "value",
            descriptor = "()I",
            isStatic = true,
        )
        val intrinsic = JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(1) }
        val simulatedJni = JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(2) }
        val registry = JvmNativeMethodRegistry(
            intrinsics = mapOf(key to intrinsic),
            simulatedJni = mapOf(key to simulatedJni),
            intrinsicOwnerWhitelist = setOf("java/lang/System"),
        )

        val resolved = registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "GuestNative",
                name = "value",
                descriptor = "()I",
                isStatic = true,
                isNative = true,
            ),
        )

        assertEquals(simulatedJni, resolved)
    }

    @Test
    fun `native registry resolves whitelisted owner intrinsics before simulated JNI fallback`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "java/lang/System",
            name = "value",
            descriptor = "()I",
            isStatic = true,
        )
        val intrinsic = JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(1) }
        val simulatedJni = JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(2) }
        val registry = JvmNativeMethodRegistry(
            intrinsics = mapOf(key to intrinsic),
            simulatedJni = mapOf(key to simulatedJni),
            intrinsicOwnerWhitelist = setOf("java/lang/System"),
        )

        val resolved = registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/System",
                name = "value",
                descriptor = "()I",
                isStatic = true,
                isNative = true,
            ),
        )

        assertEquals(intrinsic, resolved)
    }


    @Test
    fun `intrinsic hit uses Kotlin implementation`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "java/lang/System",
            name = "customValue",
            descriptor = "()I",
            isStatic = true,
        )
        var invoked = false
        val intrinsic = JvmNativeMethodIntrinsic { context, invocation ->
            invoked = true
            assertEquals("java/lang/System", context.currentClassName)
            assertEquals(null, invocation.receiver)
            assertEquals(emptyList(), invocation.arguments)
            JvmIntValue(42)
        }
        val registry = JvmNativeMethodRegistry(
            intrinsics = mapOf(key to intrinsic),
            simulatedJni = mapOf(key to JvmNativeMethodIntrinsic { _, _ -> JvmIntValue(7) }),
            intrinsicOwnerWhitelist = setOf("java/lang/System"),
        )
        val resolved = registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/System",
                name = "customValue",
                descriptor = "()I",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = resolved.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmIntValue(42), result)
        assertEquals(true, invoked)
    }


    @Test
    fun `System mapLibraryName intrinsic returns the platform mapped library name`() {
        val heap = JvmHeap()
        val logicalName = heap.internString("tiny-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/System",
                name = "mapLibraryName",
                descriptor = "(Ljava/lang/String;)Ljava/lang/String;",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("System.mapLibraryName intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(logicalName)),
        ) as JvmObjectReferenceValue

        assertEquals(
            java.lang.System.mapLibraryName("tiny-native"),
            (heap.get(result).payload as JvmStringPayload).value,
        )
    }

    @Test
    fun `System loadLibrary intrinsic delegates logical names to the native library load hook`() {
        val loadedLogicalNames = mutableListOf<String>()
        val heap = JvmHeap()
        val libraryName = heap.internString("tiny-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/System",
                name = "loadLibrary",
                descriptor = "(Ljava/lang/String;)V",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("System.loadLibrary intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            loadNativeLibraryHandler = { logicalName -> loadedLogicalNames += logicalName },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(libraryName)),
        )

        assertEquals(null, result)
        assertEquals(listOf("tiny-native"), loadedLogicalNames)
    }

    @Test
    fun `Runtime loadLibrary0 intrinsic delegates logical names to the native library load hook`() {
        val loadedLogicalNames = mutableListOf<String>()
        val heap = JvmHeap()
        val runtime = heap.allocateObject("java/lang/Runtime")
        val fromClass = heap.internClassMirror("Caller")
        val libraryName = heap.internString("runtime-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/Runtime",
                name = "loadLibrary0",
                descriptor = "(Ljava/lang/Class;Ljava/lang/String;)V",
                isStatic = false,
                isNative = true,
            ),
        ) ?: error("Runtime.loadLibrary0 intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Runtime",
            loadNativeLibraryHandler = { logicalName -> loadedLogicalNames += logicalName },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = runtime, arguments = listOf(fromClass, libraryName)),
        )

        assertEquals(null, result)
        assertEquals(listOf("runtime-native"), loadedLogicalNames)
    }

    @Test
    fun `NativeLibraries load intrinsic delegates library names to the native library load hook`() {
        val loadedLogicalNames = mutableListOf<String>()
        val heap = JvmHeap()
        val nativeLibrary = heap.allocateObject("jdk/internal/loader/NativeLibraries\$NativeLibraryImpl")
        val libraryName = heap.internString("jdk-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "jdk/internal/loader/NativeLibraries",
                name = "load",
                descriptor = "(Ljdk/internal/loader/NativeLibraries\$NativeLibraryImpl;Ljava/lang/String;ZZ)Z",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("NativeLibraries.load intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/loader/NativeLibraries",
            loadNativeLibraryHandler = { logicalName -> loadedLogicalNames += logicalName },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(nativeLibrary, libraryName, JvmIntValue(0), JvmIntValue(1)),
            ),
        )

        assertEquals(JvmIntValue(1), result)
        assertEquals(listOf("jdk-native"), loadedLogicalNames)
    }

    @Test
    fun `NativeLibraries load intrinsic rejects non NativeLibraryImpl argument`() {
        val heap = JvmHeap()
        val nativeLibrary = heap.allocateObject("java/lang/Object")
        val libraryName = heap.internString("jdk-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "jdk/internal/loader/NativeLibraries",
                name = "load",
                descriptor = "(Ljdk/internal/loader/NativeLibraries\$NativeLibraryImpl;Ljava/lang/String;ZZ)Z",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("NativeLibraries.load intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/loader/NativeLibraries",
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(
                    receiver = null,
                    arguments = listOf(nativeLibrary, libraryName, JvmIntValue(0), JvmIntValue(1)),
                ),
            )
        }

        assertEquals(
            "NativeLibraries.load first argument must be a jdk/internal/loader/NativeLibraries\$NativeLibraryImpl object",
            exception.message,
        )
    }

    @Test
    fun `NativeLibraries load intrinsic rejects invalid boolean flags`() {
        val heap = JvmHeap()
        val nativeLibrary = heap.allocateObject("jdk/internal/loader/NativeLibraries\$NativeLibraryImpl")
        val libraryName = heap.internString("jdk-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "jdk/internal/loader/NativeLibraries",
                name = "load",
                descriptor = "(Ljdk/internal/loader/NativeLibraries\$NativeLibraryImpl;Ljava/lang/String;ZZ)Z",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("NativeLibraries.load intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/loader/NativeLibraries",
        )

        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(
                    receiver = null,
                    arguments = listOf(nativeLibrary, libraryName, JvmIntValue(2), JvmIntValue(1)),
                ),
            )
        }

        assertEquals("NativeLibraries.load boolean flags must be 0 or 1", exception.message)
    }
    @Test
    fun `NativeLibraries findBuiltinLib intrinsic reports no host builtin library`() {
        val heap = JvmHeap()
        val libraryName = heap.internString("java")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "jdk/internal/loader/NativeLibraries",
                name = "findBuiltinLib",
                descriptor = "(Ljava/lang/String;)Ljava/lang/String;",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("NativeLibraries.findBuiltinLib intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/loader/NativeLibraries",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(libraryName)),
        )

        assertEquals(JvmNullValue, result)
    }

    @Test
    fun `NativeLibraries unload intrinsic delegates library names to the native library unload hook`() {
        val unloadedLogicalNames = mutableListOf<String>()
        val heap = JvmHeap()
        val libraryName = heap.internString("jdk-native")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "jdk/internal/loader/NativeLibraries",
                name = "unload",
                descriptor = "(Ljava/lang/String;ZJ)V",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("NativeLibraries.unload intrinsic should resolve")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/loader/NativeLibraries",
            unloadNativeLibraryHandler = { logicalName -> unloadedLogicalNames += logicalName },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(libraryName, JvmIntValue(0), JvmLongValue(0x1234L)),
            ),
        )

        assertEquals(null, result)
        assertEquals(listOf("jdk-native"), unloadedLogicalNames)
    }

    @Test
    fun `intrinsic miss enters simulated JNI implementation`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "java/lang/System",
            name = "jniFallbackValue",
            descriptor = "()I",
            isStatic = true,
        )
        var simulatedJniInvoked = false
        val registry = JvmNativeMethodRegistry(
            intrinsics = emptyMap(),
            simulatedJni = mapOf(
                key to JvmNativeMethodIntrinsic { context, invocation ->
                    simulatedJniInvoked = true
                    assertEquals("java/lang/System", context.currentClassName)
                    assertEquals(null, invocation.receiver)
                    assertEquals(emptyList(), invocation.arguments)
                    JvmIntValue(99)
                },
            ),
            intrinsicOwnerWhitelist = setOf("java/lang/System"),
        )
        val resolved = registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "java/lang/System",
                name = "jniFallbackValue",
                descriptor = "()I",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("simulated JNI fallback should resolve")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = resolved.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmIntValue(99), result)
        assertEquals(true, simulatedJniInvoked)
    }


    @Test
    fun `non whitelisted method skips intrinsic lookup and executes simulated JNI`() {
        val key = JvmNativeMethodKey(
            ownerClassName = "GuestNative",
            name = "value",
            descriptor = "()I",
            isStatic = true,
        )
        var intrinsicInvoked = false
        var simulatedJniInvoked = false
        val registry = JvmNativeMethodRegistry(
            intrinsics = mapOf(
                key to JvmNativeMethodIntrinsic { _, _ ->
                    intrinsicInvoked = true
                    JvmIntValue(1)
                },
            ),
            simulatedJni = mapOf(
                key to JvmNativeMethodIntrinsic { _, _ ->
                    simulatedJniInvoked = true
                    JvmIntValue(2)
                },
            ),
            intrinsicOwnerWhitelist = setOf("java/lang/System"),
        )
        val resolved = registry.resolve(
            JvmResolvedMethod(
                ownerClassName = "GuestNative",
                name = "value",
                descriptor = "()I",
                isStatic = true,
                isNative = true,
            ),
        ) ?: error("simulated JNI fallback should resolve")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "GuestNative",
        )

        val result = resolved.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmIntValue(2), result)
        assertEquals(false, intrinsicInvoked)
        assertEquals(true, simulatedJniInvoked)
    }

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

    @Test
    fun `Object clone intrinsic shallow clones Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val field = JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I")
        heap.putInstanceField(receiver, field, JvmIntValue(42))
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "Example",
                        interfaceNames = listOf("java/lang/Cloneable"),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        assertEquals("Example", heap.get(result).className)
        assertEquals(JvmIntValue(42), heap.getInstanceField(result, field))
    }

    @Test
    fun `Object clone intrinsic shallow clones guest arrays`() {
        val heap = JvmHeap()
        val receiver = heap.allocateIntArray(2)
        val originalPayload = heap.get(receiver).payload as JvmIntArrayPayload
        originalPayload.elements[0] = 3
        originalPayload.elements[1] = 4
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = null,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertNotEquals(receiver, result)
        val clonedPayload = heap.get(result).payload as JvmIntArrayPayload
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
        originalPayload.elements[1] = 9
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
    }

    @Test
    fun `Object clone intrinsic rejects non Cloneable guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectCloneMethod())
            ?: error("Object.clone intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(listOf(JvmClassDefinition(internalName = "Example"))),
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
            )
        }
    }

    @Test
    fun `Object wait intrinsic releases the receiver monitor and records the current thread as waiting`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "main")
        monitors.enter(receiver, threadId = "main")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectWaitLongMethod())
            ?: error("Object.wait intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "main",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmLongValue(0L))),
        )

        assertEquals(null, result)
        assertEquals(0, monitors.holdCount(receiver, threadId = "main"))
        assertEquals(listOf("main"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object wait intrinsic records scheduler waiting state when scheduler is present`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val receiver = heap.allocateObject("Example")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "main")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "main")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectWaitLongMethod())
            ?: error("Object.wait intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            threadScheduler = scheduler,
            currentThreadId = "main",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmLongValue(0L))),
        )

        assertEquals(null, result)
        assertEquals(0, monitors.holdCount(receiver, threadId = "main"))
        assertEquals(listOf("main"), monitors.waitingThreads(receiver))
        assertEquals(
            JvmThreadSchedulingState.WaitingOnMonitor(
                reference = receiver,
                releasedHoldCount = 2,
            ),
            scheduler.state("main"),
        )
    }

    @Test
    fun `Object notify intrinsic wakes one waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyMethod())
            ?: error("Object.notify intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(listOf("second"), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notify intrinsic moves one waiting thread through scheduler handoff when scheduler is present`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val receiver = heap.allocateObject("Example")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "first")
        scheduler.waitForMonitorNotification(monitors, receiver, threadId = "first")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "second")
        scheduler.waitForMonitorNotification(monitors, receiver, threadId = "second")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyMethod())
            ?: error("Object.notify intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            threadScheduler = scheduler,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = receiver,
                ownerThreadId = "owner",
            ),
            scheduler.state("first"),
        )
        assertEquals(
            JvmThreadSchedulingState.WaitingOnMonitor(
                reference = receiver,
                releasedHoldCount = 1,
            ),
            scheduler.state("second"),
        )
        assertEquals(listOf("second"), monitors.waitingThreads(receiver))
        assertEquals(listOf("first"), monitors.blockedThreads(receiver))
    }

    @Test
    fun `Object notifyAll intrinsic wakes every waiting thread from the receiver monitor`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val receiver = heap.allocateObject("Example")
        monitors.enter(receiver, threadId = "first")
        monitors.waitForNotification(receiver, threadId = "first")
        monitors.enter(receiver, threadId = "second")
        monitors.waitForNotification(receiver, threadId = "second")
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyAllMethod())
            ?: error("Object.notifyAll intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(emptyList(), monitors.waitingThreads(receiver))
    }

    @Test
    fun `Object notifyAll intrinsic moves all waiting threads through scheduler handoff when scheduler is present`() {
        val heap = JvmHeap()
        val monitors = JvmMonitorState()
        val scheduler = JvmThreadScheduler()
        val receiver = heap.allocateObject("Example")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "first")
        scheduler.waitForMonitorNotification(monitors, receiver, threadId = "first")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "second")
        scheduler.waitForMonitorNotification(monitors, receiver, threadId = "second")
        scheduler.tryEnterMonitor(monitors, receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(objectNotifyAllMethod())
            ?: error("Object.notifyAll intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "Example",
            monitors = monitors,
            threadScheduler = scheduler,
            currentThreadId = "owner",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = receiver,
                ownerThreadId = "owner",
            ),
            scheduler.state("first"),
        )
        assertEquals(
            JvmThreadSchedulingState.BlockedOnMonitor(
                reference = receiver,
                ownerThreadId = "owner",
            ),
            scheduler.state("second"),
        )
        assertEquals(emptyList(), monitors.waitingThreads(receiver))
        assertEquals(listOf("first", "second"), monitors.blockedThreads(receiver))
    }

    @Test
    fun `System arraycopy intrinsic copies primitive arrays with overlap semantics`() {
        val heap = JvmHeap()
        val array = heap.allocateIntArray(5)
        val payload = heap.get(array).payload as JvmIntArrayPayload
        payload.elements[0] = 1
        payload.elements[1] = 2
        payload.elements[2] = 3
        payload.elements[3] = 4
        payload.elements[4] = 5
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(array, JvmIntValue(0), array, JvmIntValue(1), JvmIntValue(3)),
            ),
        )

        assertEquals(null, result)
        assertEquals(mutableListOf(1, 1, 2, 3, 5), payload.elements)
    }

    @Test
    fun `System arraycopy intrinsic copies assignable reference array elements`() {
        val heap = JvmHeap()
        val child = heap.allocateObject("Child")
        val source = heap.allocateReferenceArray("Child", 2)
        val target = heap.allocateReferenceArray("Base", 2)
        val sourcePayload = heap.get(source).payload as JvmReferenceArrayPayload
        sourcePayload.elements[0] = child
        sourcePayload.elements[1] = JvmNullValue
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "Base"),
                    JvmClassDefinition(internalName = "Child", superclassName = "Base"),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                receiver = null,
                arguments = listOf(source, JvmIntValue(0), target, JvmIntValue(0), JvmIntValue(2)),
            ),
        )

        assertEquals(
            JvmReferenceArrayPayload(mutableListOf(child, JvmNullValue)),
            heap.get(target).payload,
        )
    }

    @Test
    fun `System arraycopy intrinsic rejects mismatched primitive array types`() {
        val heap = JvmHeap()
        val source = heap.allocateIntArray(1)
        val target = heap.allocateLongArray(1)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemArraycopyMethod())
            ?: error("System.arraycopy intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(
                context,
                JvmNativeMethodInvocation(
                    receiver = null,
                    arguments = listOf(source, JvmIntValue(0), target, JvmIntValue(0), JvmIntValue(1)),
                ),
            )
        }
    }

    @Test
    fun `System identityHashCode intrinsic returns a stable identity hash for guest objects`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemIdentityHashCodeMethod())
            ?: error("System.identityHashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val firstHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(receiver)),
        )
        val secondHash = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(receiver)),
        )

        assertEquals(firstHash, secondHash)
        assertEquals(JvmIntValue::class, firstHash!!::class)
    }

    @Test
    fun `System identityHashCode intrinsic returns zero for null`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemIdentityHashCodeMethod())
            ?: error("System.identityHashCode intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(JvmNullValue)),
        )

        assertEquals(JvmIntValue(0), result)
    }

    @Test
    fun `System currentTimeMillis intrinsic returns the context wall clock value`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemCurrentTimeMillisMethod())
            ?: error("System.currentTimeMillis intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            currentTimeMillisProvider = { 123_456_789L },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmLongValue(123_456_789L), result)
    }

    @Test
    fun `System nanoTime intrinsic returns the context monotonic clock value`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemNanoTimeMethod())
            ?: error("System.nanoTime intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            nanoTimeProvider = { 987_654_321L },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmLongValue(987_654_321L), result)
    }

    @Test
    fun `System exit intrinsic terminates the VM with the supplied status`() {
        val terminationState = JvmVmTerminationState()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemExitMethod())
            ?: error("System.exit intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
            terminationState = terminationState,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(JvmIntValue(23))),
        )

        assertEquals(null, result)
        assertEquals(JvmVmTerminationResult.Normal(23), terminationState.result)
    }

    @Test
    fun `Runtime exit intrinsic terminates the VM with the supplied status`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Runtime")
        val terminationState = JvmVmTerminationState()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(runtimeExitMethod())
            ?: error("Runtime.exit intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Runtime",
            terminationState = terminationState,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmIntValue(42))),
        )

        assertEquals(null, result)
        assertEquals(JvmVmTerminationResult.Normal(42), terminationState.result)
    }

    @Test
    fun `Shutdown beforeHalt intrinsic is a simulated no op`() {
        val terminationState = JvmVmTerminationState()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(shutdownBeforeHaltMethod())
            ?: error("Shutdown.beforeHalt intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Shutdown",
            terminationState = terminationState,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(null, terminationState.result)
    }

    @Test
    fun `Shutdown halt0 intrinsic terminates the VM with the supplied status`() {
        val terminationState = JvmVmTerminationState()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(shutdownHalt0Method())
            ?: error("Shutdown.halt0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Shutdown",
            terminationState = terminationState,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = listOf(JvmIntValue(64))),
        )

        assertEquals(null, result)
        assertEquals(JvmVmTerminationResult.Normal(64), terminationState.result)
    }

    @Test
    fun `Class initClassName intrinsic returns the guest binary name string`() {
        val heap = JvmHeap()
        val classMirror = heap.internClassMirror("pkg/Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classInitClassNameMethod())
            ?: error("Class.initClassName intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = classMirror, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertEquals(JvmStringPayload("pkg.Example"), heap.get(result).payload)
    }

    @Test
    fun `Class query intrinsics report array primitive and interface mirrors`() {
        val heap = JvmHeap()
        val arrayMirror = heap.internClassMirror("[Ljava/lang/String;")
        val primitiveMirror = heap.internClassMirror("int")
        val interfaceMirror = heap.internClassMirror("pkg/Service")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(JvmClassDefinition(internalName = "pkg/Service", isInterface = true)),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val isArray = JvmVmIntrinsics.Registry.resolve(classIsArrayMethod())
            ?: error("Class.isArray intrinsic was not registered")
        val isPrimitive = JvmVmIntrinsics.Registry.resolve(classIsPrimitiveMethod())
            ?: error("Class.isPrimitive intrinsic was not registered")
        val isInterface = JvmVmIntrinsics.Registry.resolve(classIsInterfaceMethod())
            ?: error("Class.isInterface intrinsic was not registered")

        assertEquals(JvmIntValue(1), isArray.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList())))
        assertEquals(JvmIntValue(0), isArray.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList())))
        assertEquals(JvmIntValue(1), isPrimitive.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList())))
        assertEquals(JvmIntValue(0), isPrimitive.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList())))
        assertEquals(JvmIntValue(1), isInterface.invoke(context, JvmNativeMethodInvocation(interfaceMirror, emptyList())))
    }

    @Test
    fun `Class getSuperclass intrinsic returns object for arrays and declared superclass for ordinary classes`() {
        val heap = JvmHeap()
        val arrayMirror = heap.internClassMirror("[I")
        val childMirror = heap.internClassMirror("pkg/Child")
        val objectMirror = heap.internClassMirror("java/lang/Object")
        val primitiveMirror = heap.internClassMirror("int")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classGetSuperclassMethod())
            ?: error("Class.getSuperclass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(JvmClassDefinition(internalName = "pkg/Child", superclassName = "pkg/Base")),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val arraySuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList()))
        val childSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, emptyList()))
        val objectSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(objectMirror, emptyList()))
        val primitiveSuperclass = intrinsic.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList()))

        assertEquals(heap.internClassMirror("java/lang/Object"), arraySuperclass)
        assertEquals(heap.internClassMirror("pkg/Base"), childSuperclass)
        assertEquals(JvmNullValue, objectSuperclass)
        assertEquals(JvmNullValue, primitiveSuperclass)
    }

    @Test
    fun `Throwable fillInStackTrace intrinsic records context stack trace and returns receiver`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Throwable")
        val frame = JvmStackTraceFrame(
            declaringClass = "pkg/Example",
            methodName = "call",
            fileName = "Example.java",
            lineNumber = 42,
        )
        val intrinsic = JvmVmIntrinsics.Registry.resolve(throwableFillInStackTraceMethod())
            ?: error("Throwable.fillInStackTrace intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "java/lang/Throwable", superclassName = "java/lang/Object"),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Throwable",
            stackTraceProvider = { listOf(frame) },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = listOf(JvmIntValue(0))),
        )

        assertEquals(receiver, result)
        assertEquals(JvmThrowablePayload(listOf(frame)), heap.get(receiver).payload)
    }

    @Test
    fun `String intern intrinsic returns the canonical guest string reference`() {
        val heap = JvmHeap()
        val receiver = heap.allocateString("hello")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(stringInternMethod())
            ?: error("String.intern intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/String",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertEquals(heap.internString("hello"), result)
        assertEquals(JvmStringPayload("hello"), heap.get(result).payload)
    }

    @Test
    fun `Thread currentThread intrinsic returns the current guest thread mirror`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadCurrentThreadMethod())
            ?: error("Thread.currentThread intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            currentThreadId = "worker-1",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        ) as JvmObjectReferenceValue
        val repeated = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(result, repeated)
        assertEquals("java/lang/Thread", heap.get(result).className)
        assertEquals(JvmThreadPayload("worker-1"), heap.get(result).payload)
    }

    @Test
    fun `Thread sleep intrinsics delegate validated guest sleep requests to the context`() {
        val heap = JvmHeap()
        val sleeps = mutableListOf<Pair<Long, Int>>()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            threadSleepHandler = { millis, nanos -> sleeps += millis to nanos },
        )
        val sleepMillis = JvmVmIntrinsics.Registry.resolve(threadSleepMillisMethod())
            ?: error("Thread.sleep(J) intrinsic was not registered")
        val sleepMillisNanos = JvmVmIntrinsics.Registry.resolve(threadSleepMillisNanosMethod())
            ?: error("Thread.sleep(JI) intrinsic was not registered")
        val sleepNanos0 = JvmVmIntrinsics.Registry.resolve(threadSleepNanos0Method())
            ?: error("Thread.sleepNanos0(J) intrinsic was not registered")

        assertEquals(
            null,
            sleepMillis.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(5L)))),
        )
        assertEquals(
            null,
            sleepMillisNanos.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(6L), JvmIntValue(7)))),
        )
        assertEquals(
            null,
            sleepNanos0.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(1_234_567L)))),
        )

        assertEquals(listOf(5L to 0, 6L to 7, 1L to 234_567), sleeps)
    }

    @Test
    fun `Thread sleep intrinsics reject negative and out of range guest sleep requests`() {
        val heap = JvmHeap()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )
        val sleepMillis = JvmVmIntrinsics.Registry.resolve(threadSleepMillisMethod())
            ?: error("Thread.sleep(J) intrinsic was not registered")
        val sleepMillisNanos = JvmVmIntrinsics.Registry.resolve(threadSleepMillisNanosMethod())
            ?: error("Thread.sleep(JI) intrinsic was not registered")

        assertFailsWith<JvmUnsupportedInstructionException> {
            sleepMillis.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmLongValue(-1L))))
        }
        assertFailsWith<JvmUnsupportedInstructionException> {
            sleepMillisNanos.invoke(
                context,
                JvmNativeMethodInvocation(null, listOf(JvmLongValue(0L), JvmIntValue(1_000_000))),
            )
        }
    }

    @Test
    fun `VM intrinsic registry resolves the Phase 15 native intrinsic surface`() {
        val phase15Methods = listOf(
            objectGetClassMethod(),
            objectHashCodeMethod(),
            objectCloneMethod(),
            objectWaitLongMethod(),
            objectNotifyMethod(),
            objectNotifyAllMethod(),
            systemArraycopyMethod(),
            systemIdentityHashCodeMethod(),
            systemCurrentTimeMillisMethod(),
            systemNanoTimeMethod(),
            classInitClassNameMethod(),
            classIsArrayMethod(),
            classIsPrimitiveMethod(),
            classIsInterfaceMethod(),
            classGetSuperclassMethod(),
            throwableFillInStackTraceMethod(),
            stringInternMethod(),
            threadCurrentThreadMethod(),
            threadSleepMillisMethod(),
            threadSleepMillisNanosMethod(),
            threadSleepNanos0Method(),
        )

        phase15Methods.forEach { method ->
            assertNotNull(
                JvmVmIntrinsics.Registry.resolve(method),
                "Expected intrinsic for ${method.ownerClassName}.${method.name}:${method.descriptor}",
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

    private fun objectCloneMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "clone",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun objectWaitLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "(J)V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notify",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun objectNotifyAllMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Object",
        name = "notifyAll",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun systemArraycopyMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "arraycopy",
        descriptor = "(Ljava/lang/Object;ILjava/lang/Object;II)V",
        isStatic = true,
        isNative = true,
    )

    private fun systemIdentityHashCodeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "identityHashCode",
        descriptor = "(Ljava/lang/Object;)I",
        isStatic = true,
        isNative = true,
    )

    private fun systemCurrentTimeMillisMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "currentTimeMillis",
        descriptor = "()J",
        isStatic = true,
        isNative = true,
    )

    private fun systemNanoTimeMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "nanoTime",
        descriptor = "()J",
        isStatic = true,
        isNative = true,
    )

    private fun systemExitMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "exit",
        descriptor = "(I)V",
        isStatic = true,
        isNative = true,
    )

    private fun runtimeExitMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "exit",
        descriptor = "(I)V",
        isStatic = false,
        isNative = true,
    )

    private fun shutdownBeforeHaltMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Shutdown",
        name = "beforeHalt",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun shutdownHalt0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Shutdown",
        name = "halt0",
        descriptor = "(I)V",
        isStatic = true,
        isNative = true,
    )

    private fun classInitClassNameMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "initClassName",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
        isNative = true,
    )

    private fun classIsArrayMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isArray",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classIsPrimitiveMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isPrimitive",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classIsInterfaceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isInterface",
        descriptor = "()Z",
        isStatic = false,
        isNative = true,
    )

    private fun classGetSuperclassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "getSuperclass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )

    private fun throwableFillInStackTraceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Throwable",
        name = "fillInStackTrace",
        descriptor = "(I)Ljava/lang/Throwable;",
        isStatic = false,
        isNative = true,
    )

    private fun stringInternMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/String",
        name = "intern",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
        isNative = true,
    )

    private fun threadCurrentThreadMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "currentThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepMillisMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(J)V",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepMillisNanosMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(JI)V",
        isStatic = true,
        isNative = true,
    )

    private fun threadSleepNanos0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "sleepNanos0",
        descriptor = "(J)V",
        isStatic = true,
        isNative = true,
    )
}
