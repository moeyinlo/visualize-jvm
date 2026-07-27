package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmFieldDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
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
import kotlin.test.assertTrue

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
    fun `System registerNatives intrinsic is a simulated no op`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(systemRegisterNativesMethod())
            ?: error("System.registerNatives intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/System",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
    }

    @Test
    fun `System stream setter intrinsics update the guest static stream fields`() {
        val heap = JvmHeap()
        val input = heap.allocateObject("java/io/InputStream")
        val output = heap.allocateObject("java/io/PrintStream")
        val error = heap.allocateObject("java/io/PrintStream")
        val staticFields = JvmStaticFields()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = staticFields,
            currentClassName = "java/lang/System",
        )
        val setIn = JvmVmIntrinsics.Registry.resolve(systemSetIn0Method())
            ?: error("System.setIn0 intrinsic was not registered")
        val setOut = JvmVmIntrinsics.Registry.resolve(systemSetOut0Method())
            ?: error("System.setOut0 intrinsic was not registered")
        val setErr = JvmVmIntrinsics.Registry.resolve(systemSetErr0Method())
            ?: error("System.setErr0 intrinsic was not registered")

        assertEquals(null, setIn.invoke(context, JvmNativeMethodInvocation(null, listOf(input))))
        assertEquals(null, setOut.invoke(context, JvmNativeMethodInvocation(null, listOf(output))))
        assertEquals(null, setErr.invoke(context, JvmNativeMethodInvocation(null, listOf(error))))

        assertEquals(input, staticFields.get(JvmFieldReference("java/lang/System", "in", "Ljava/io/InputStream;")))
        assertEquals(output, staticFields.get(JvmFieldReference("java/lang/System", "out", "Ljava/io/PrintStream;")))
        assertEquals(error, staticFields.get(JvmFieldReference("java/lang/System", "err", "Ljava/io/PrintStream;")))
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
    fun `Runtime gc intrinsic delegates to the context GC handler`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Runtime")
        var gcRequests = 0
        val intrinsic = JvmVmIntrinsics.Registry.resolve(runtimeGcMethod())
            ?: error("Runtime.gc intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Runtime",
            gcHandler = { gcRequests += 1 },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(1, gcRequests)
    }

    @Test
    fun `Runtime memory intrinsics return context supplied byte counts`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Runtime")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Runtime",
            freeMemoryProvider = { 1_024L },
            totalMemoryProvider = { 4_096L },
            maxMemoryProvider = { 16_384L },
        )
        val freeMemory = JvmVmIntrinsics.Registry.resolve(runtimeFreeMemoryMethod())
            ?: error("Runtime.freeMemory intrinsic was not registered")
        val totalMemory = JvmVmIntrinsics.Registry.resolve(runtimeTotalMemoryMethod())
            ?: error("Runtime.totalMemory intrinsic was not registered")
        val maxMemory = JvmVmIntrinsics.Registry.resolve(runtimeMaxMemoryMethod())
            ?: error("Runtime.maxMemory intrinsic was not registered")
        val invocation = JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList())

        assertEquals(JvmLongValue(1_024L), freeMemory.invoke(context, invocation))
        assertEquals(JvmLongValue(4_096L), totalMemory.invoke(context, invocation))
        assertEquals(JvmLongValue(16_384L), maxMemory.invoke(context, invocation))
    }

    @Test
    fun `Runtime availableProcessors intrinsic returns the context supplied processor count`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Runtime")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(runtimeAvailableProcessorsMethod())
            ?: error("Runtime.availableProcessors intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Runtime",
            availableProcessorsProvider = { 12 },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = receiver, arguments = emptyList()),
        )

        assertEquals(JvmIntValue(12), result)
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
    fun `Class registerNatives intrinsic is a simulated no op`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classRegisterNativesMethod())
            ?: error("Class.registerNatives intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
    }

    @Test
    fun `Class getPrimitiveClass intrinsic returns primitive class mirrors`() {
        val heap = JvmHeap()
        val intName = heap.internString("int")
        val voidName = heap.internString("void")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classGetPrimitiveClassMethod())
            ?: error("Class.getPrimitiveClass intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val intMirror = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(intName)))
        val voidMirror = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(voidName)))

        assertEquals(heap.internClassMirror("int"), intMirror)
        assertEquals(heap.internClassMirror("void"), voidMirror)
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
    fun `Class isAssignableFrom intrinsic compares represented guest classes`() {
        val heap = JvmHeap()
        val baseMirror = heap.internClassMirror("pkg/Base")
        val childMirror = heap.internClassMirror("pkg/Child")
        val serviceMirror = heap.internClassMirror("pkg/Service")
        val arrayObjectMirror = heap.internClassMirror("[Ljava/lang/Object;")
        val arrayStringMirror = heap.internClassMirror("[Ljava/lang/String;")
        val intMirror = heap.internClassMirror("int")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classIsAssignableFromMethod())
            ?: error("Class.isAssignableFrom intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "pkg/Base", superclassName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "pkg/Child",
                        superclassName = "pkg/Base",
                        interfaceNames = listOf("pkg/Service"),
                    ),
                    JvmClassDefinition(internalName = "pkg/Service", isInterface = true),
                    JvmClassDefinition(internalName = "java/lang/String", superclassName = "java/lang/Object"),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(baseMirror, listOf(childMirror))),
        )
        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(serviceMirror, listOf(childMirror))),
        )
        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(arrayObjectMirror, listOf(arrayStringMirror))),
        )
        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(intMirror, listOf(intMirror))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, listOf(baseMirror))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(baseMirror, listOf(intMirror))),
        )
    }

    @Test
    fun `Class isInstance intrinsic tests guest object assignability`() {
        val heap = JvmHeap()
        val baseMirror = heap.internClassMirror("pkg/Base")
        val childMirror = heap.internClassMirror("pkg/Child")
        val serviceMirror = heap.internClassMirror("pkg/Service")
        val primitiveMirror = heap.internClassMirror("int")
        val child = heap.allocateObject("pkg/Child")
        val base = heap.allocateObject("pkg/Base")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classIsInstanceMethod())
            ?: error("Class.isInstance intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(internalName = "pkg/Base", superclassName = "java/lang/Object"),
                    JvmClassDefinition(
                        internalName = "pkg/Child",
                        superclassName = "pkg/Base",
                        interfaceNames = listOf("pkg/Service"),
                    ),
                    JvmClassDefinition(internalName = "pkg/Service", isInterface = true),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(baseMirror, listOf(child))),
        )
        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(serviceMirror, listOf(child))),
        )
        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, listOf(child))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, listOf(base))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(baseMirror, listOf(JvmNullValue))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(primitiveMirror, listOf(child))),
        )
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
    fun `Class getInterfaces0 intrinsic returns guest interface mirror arrays`() {
        val heap = JvmHeap()
        val childMirror = heap.internClassMirror("pkg/Child")
        val arrayMirror = heap.internClassMirror("[Ljava/lang/String;")
        val primitiveMirror = heap.internClassMirror("int")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classGetInterfaces0Method())
            ?: error("Class.getInterfaces0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Child",
                        superclassName = "java/lang/Object",
                        interfaceNames = listOf("pkg/Service", "pkg/Marker"),
                    ),
                    JvmClassDefinition(internalName = "pkg/Service", isInterface = true),
                    JvmClassDefinition(internalName = "pkg/Marker", isInterface = true),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val childInterfaces = intrinsic.invoke(context, JvmNativeMethodInvocation(childMirror, emptyList()))
            as JvmObjectReferenceValue
        val arrayInterfaces = intrinsic.invoke(context, JvmNativeMethodInvocation(arrayMirror, emptyList()))
            as JvmObjectReferenceValue
        val primitiveInterfaces = intrinsic.invoke(context, JvmNativeMethodInvocation(primitiveMirror, emptyList()))
            as JvmObjectReferenceValue

        assertEquals(
            JvmReferenceArrayPayload(
                mutableListOf(
                    heap.internClassMirror("pkg/Service"),
                    heap.internClassMirror("pkg/Marker"),
                ),
            ),
            heap.get(childInterfaces).payload,
        )
        assertEquals(
            JvmReferenceArrayPayload(
                mutableListOf(
                    heap.internClassMirror("java/lang/Cloneable"),
                    heap.internClassMirror("java/io/Serializable"),
                ),
            ),
            heap.get(arrayInterfaces).payload,
        )
        assertEquals(JvmReferenceArrayPayload(mutableListOf()), heap.get(primitiveInterfaces).payload)
    }

    @Test
    fun `Class desiredAssertionStatus0 intrinsic defaults guest assertions to disabled`() {
        val heap = JvmHeap()
        val classMirror = heap.internClassMirror("pkg/Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classDesiredAssertionStatus0Method())
            ?: error("Class.desiredAssertionStatus0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(classMirror)))

        assertEquals(JvmIntValue(0), result)
    }

    @Test
    fun `Class isHidden intrinsic defaults guest class mirrors to visible`() {
        val heap = JvmHeap()
        val classMirror = heap.internClassMirror("pkg/Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(classIsHiddenMethod())
            ?: error("Class.isHidden intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Class",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(classMirror, emptyList()))

        assertEquals(JvmIntValue(0), result)
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
    fun `Thread registerNatives intrinsic is a simulated no op`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadRegisterNativesMethod())
            ?: error("Thread.registerNatives intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
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
    fun `Thread currentCarrierThread intrinsic returns the current guest carrier thread mirror`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadCurrentCarrierThreadMethod())
            ?: error("Thread.currentCarrierThread intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            currentThreadId = "carrier-1",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        assertEquals(heap.internThread("carrier-1"), result)
        assertEquals(JvmThreadPayload("carrier-1"), heap.get(result).payload)
    }

    @Test
    fun `Thread findScopedValueBindings intrinsic returns null when no scoped values are modeled`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadFindScopedValueBindingsMethod())
            ?: error("Thread.findScopedValueBindings intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmNullValue, result)
    }

    @Test
    fun `Thread scopedValueCache intrinsic returns null when no scoped value cache is modeled`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadScopedValueCacheMethod())
            ?: error("Thread.scopedValueCache intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(JvmNullValue, result)
    }

    @Test
    fun `Thread setScopedValueCache intrinsic validates nullable object arrays as a simulated no op`() {
        val heap = JvmHeap()
        val cache = heap.allocateReferenceArray("java/lang/Object", 2)
        val notArray = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadSetScopedValueCacheMethod())
            ?: error("Thread.setScopedValueCache intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val cacheResult = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(cache)))
        val nullResult = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmNullValue)))
        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(notArray)))
        }

        assertEquals(null, cacheResult)
        assertEquals(null, nullResult)
        assertEquals("Thread.setScopedValueCache expects one nullable Object[] argument", exception.message)
    }

    @Test
    fun `Thread getThreads intrinsic returns the current guest thread array`() {
        val heap = JvmHeap()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadGetThreadsMethod())
            ?: error("Thread.getThreads intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            currentThreadId = "worker-2",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        ) as JvmObjectReferenceValue

        val arrayObject = heap.get(result)
        assertEquals("[Ljava/lang/Thread;", arrayObject.className)
        assertEquals(
            JvmReferenceArrayPayload(mutableListOf(heap.internThread("worker-2"))),
            arrayObject.payload,
        )
    }

    @Test
    fun `Thread clearInterruptEvent intrinsic is a simulated no op`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadClearInterruptEventMethod())
            ?: error("Thread.clearInterruptEvent intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
    }

    @Test
    fun `Thread setNativeName intrinsic validates receiver and guest string name as a simulated no op`() {
        val heap = JvmHeap()
        val thread = heap.internThread("worker-3")
        val name = heap.internString("guest-worker")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadSetNativeNameMethod())
            ?: error("Thread.setNativeName intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(thread, listOf(name)))

        assertEquals(null, result)
        assertEquals(JvmThreadPayload("worker-3"), heap.get(thread).payload)
    }

    @Test
    fun `Thread setPriority0 intrinsic validates receiver and int priority as a simulated no op`() {
        val heap = JvmHeap()
        val thread = heap.internThread("priority-worker")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadSetPriority0Method())
            ?: error("Thread.setPriority0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(thread, listOf(JvmIntValue(7))))

        assertEquals(null, result)
        assertEquals(JvmThreadPayload("priority-worker"), heap.get(thread).payload)
    }

    @Test
    fun `Thread interrupt0 intrinsic validates receiver as a simulated no op`() {
        val heap = JvmHeap()
        val thread = heap.internThread("interrupt-worker")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadInterrupt0Method())
            ?: error("Thread.interrupt0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(thread, emptyList()))

        assertEquals(null, result)
        assertEquals(JvmThreadPayload("interrupt-worker"), heap.get(thread).payload)
    }

    @Test
    fun `Thread start0 intrinsic validates receiver as a simulated no op`() {
        val heap = JvmHeap()
        val thread = heap.internThread("started-worker")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadStart0Method())
            ?: error("Thread.start0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(thread, emptyList()))

        assertEquals(null, result)
        assertEquals(JvmThreadPayload("started-worker"), heap.get(thread).payload)
    }

    @Test
    fun `Thread setCurrentThread intrinsic validates receiver and nullable guest thread as a simulated no op`() {
        val heap = JvmHeap()
        val receiver = heap.internThread("virtual-thread")
        val carrier = heap.internThread("carrier-thread")
        val notThread = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadSetCurrentThreadMethod())
            ?: error("Thread.setCurrentThread intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val threadResult = intrinsic.invoke(context, JvmNativeMethodInvocation(receiver, listOf(carrier)))
        val nullResult = intrinsic.invoke(context, JvmNativeMethodInvocation(receiver, listOf(JvmNullValue)))
        val exception = assertFailsWith<JvmUnsupportedInstructionException> {
            intrinsic.invoke(context, JvmNativeMethodInvocation(receiver, listOf(notThread)))
        }

        assertEquals(null, threadResult)
        assertEquals(null, nullResult)
        assertEquals("Thread.setCurrentThread expects one nullable Thread argument", exception.message)
    }

    @Test
    fun `Thread getStackTrace0 intrinsic returns an empty StackTraceElement array snapshot`() {
        val heap = JvmHeap()
        val receiver = heap.internThread("stacktrace-worker")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadGetStackTrace0Method())
            ?: error("Thread.getStackTrace0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(receiver, emptyList()))
            as JvmObjectReferenceValue

        val arrayObject = heap.get(result)
        assertEquals("[Ljava/lang/StackTraceElement;", arrayObject.className)
        assertEquals(JvmReferenceArrayPayload(mutableListOf()), arrayObject.payload)
    }

    @Test
    fun `Thread dumpThreads intrinsic returns empty stack snapshots for each guest thread`() {
        val heap = JvmHeap()
        val thread = heap.internThread("dump-worker")
        val threads = heap.allocateReferenceArray("java/lang/Thread", 1)
        (heap.get(threads).payload as JvmReferenceArrayPayload).elements[0] = thread
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadDumpThreadsMethod())
            ?: error("Thread.dumpThreads intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(threads)))
            as JvmObjectReferenceValue

        val outerObject = heap.get(result)
        val outerPayload = outerObject.payload as JvmReferenceArrayPayload
        assertEquals("[[Ljava/lang/StackTraceElement;", outerObject.className)
        assertEquals(1, outerPayload.elements.size)
        val inner = outerPayload.elements.single() as JvmObjectReferenceValue
        assertEquals("[Ljava/lang/StackTraceElement;", heap.get(inner).className)
        assertEquals(JvmReferenceArrayPayload(mutableListOf()), heap.get(inner).payload)
    }

    @Test
    fun `Thread getNextThreadIdOffset intrinsic exposes a stable synthetic offset`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadGetNextThreadIdOffsetMethod())
            ?: error("Thread.getNextThreadIdOffset intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val first = intrinsic.invoke(context, JvmNativeMethodInvocation(null, emptyList())) as JvmLongValue
        val second = intrinsic.invoke(context, JvmNativeMethodInvocation(null, emptyList())) as JvmLongValue

        assertNotEquals(0L, first.value)
        assertEquals(first, second)
    }

    @Test
    fun `Unsafe registerNatives intrinsic is a simulated no op`() {
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeRegisterNativesMethod())
            ?: error("Unsafe.registerNatives intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(receiver = null, arguments = emptyList()))

        assertEquals(null, result)
    }

    @Test
    fun `Unsafe getLongVolatile intrinsic reads synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetLongVolatileMethod())
            ?: error("Unsafe.getLongVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(JvmLongValue(42L), result)
    }

    @Test
    fun `Unsafe getLong intrinsic reads synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetLongMethod())
            ?: error("Unsafe.getLong intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(JvmLongValue(42L), result)
    }

    @Test
    fun `Unsafe getReferenceVolatile intrinsic reads synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val reference = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetReferenceVolatileMethod())
            ?: error("Unsafe.getReferenceVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to reference)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(reference, result)
    }
    @Test
    fun `Unsafe getReference intrinsic reads synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val reference = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetReferenceMethod())
            ?: error("Unsafe.getReference intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to reference)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(reference, result)
    }

    @Test
    fun `Unsafe compareAndExchangeReference intrinsic returns witness for synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val initial = heap.allocateObject("java/lang/Object")
        val replacement = heap.allocateObject("java/lang/String")
        val mismatchReplacement = heap.allocateObject("java/lang/Thread")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeReferenceMethod())
            ?: error("Unsafe.compareAndExchangeReference intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to initial))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), replacement, mismatchReplacement),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), initial, replacement),
            ),
        )

        assertEquals(initial, mismatch)
        assertEquals(initial, match)
        assertEquals(replacement, unsafeMemory.getStaticReference(offset = 7L))
    }

    @Test
    fun `Unsafe getAndSetReference intrinsic returns witness for synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val first = heap.allocateObject("java/lang/Object")
        val second = heap.allocateObject("java/lang/String")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetReferenceMethod())
            ?: error("Unsafe.getAndSetReference intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to first))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), second)),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), first)),
        )

        assertEquals(first, existing)
        assertEquals(second, unsafeMemory.getStaticReference(offset = 7L))
        assertEquals(JvmNullValue, defaulted)
        assertEquals(first, unsafeMemory.getStaticReference(offset = 9L))
    }

    @Test
    fun `Unsafe compareAndSetReference intrinsic updates synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val initial = heap.allocateObject("java/lang/Object")
        val replacement = heap.allocateObject("java/lang/String")
        val mismatchReplacement = heap.allocateObject("java/lang/Thread")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetReferenceMethod())
            ?: error("Unsafe.compareAndSetReference intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticReferenceSlots = mapOf(7L to initial))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), initial, replacement),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), initial, mismatchReplacement),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(replacement, unsafeMemory.getStaticReference(offset = 7L))
    }

    @Test
    fun `Unsafe putReference intrinsic writes synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val reference = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutReferenceMethod())
            ?: error("Unsafe.putReference intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), reference)),
        )

        assertEquals(null, result)
        assertEquals(reference, unsafeMemory.getStaticReference(offset = 7L))
    }

    @Test
    fun `Unsafe putReferenceVolatile intrinsic writes synthetic static reference slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val reference = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutReferenceVolatileMethod())
            ?: error("Unsafe.putReferenceVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), reference)),
        )

        assertEquals(null, result)
        assertEquals(reference, unsafeMemory.getStaticReference(offset = 7L))
    }

    @Test
    fun `Unsafe getIntVolatile intrinsic reads synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetIntVolatileMethod())
            ?: error("Unsafe.getIntVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(JvmIntValue(42), result)
    }

    @Test
    fun `Unsafe getInt intrinsic reads synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetIntMethod())
            ?: error("Unsafe.getInt intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42)),
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )

        assertEquals(JvmIntValue(42), result)
    }

    @Test
    fun `Unsafe getBoolean intrinsic reads synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetBooleanMethod())
            ?: error("Unsafe.getBoolean intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(1), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe getByte intrinsic reads synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetByteMethod())
            ?: error("Unsafe.getByte intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to (-2).toByte())),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(-2), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe getShort intrinsic reads synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetShortMethod())
            ?: error("Unsafe.getShort intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to (-2).toShort())),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(-2), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe getChar intrinsic reads synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetCharMethod())
            ?: error("Unsafe.getChar intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to '\u03A9')),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue('\u03A9'.code), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }


    @Test
    fun `Unsafe putChar intrinsic writes synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutCharMethod())
            ?: error("Unsafe.putChar intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('\u03A9'.code))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(Char.MAX_VALUE.code))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals('\u03A9', unsafeMemory.getStaticChar(offset = 7L))
        assertEquals(Char.MAX_VALUE, unsafeMemory.getStaticChar(offset = 9L))
    }

    @Test
    fun `Unsafe getFloat intrinsic reads synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetFloatMethod())
            ?: error("Unsafe.getFloat intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 1.25f)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmFloatValue(1.25f), setResult)
        assertEquals(JvmFloatValue(0.0f), defaultResult)
    }

    @Test
    fun `Unsafe getDouble intrinsic reads synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetDoubleMethod())
            ?: error("Unsafe.getDouble intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 1.25)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmDoubleValue(1.25), setResult)
        assertEquals(JvmDoubleValue(0.0), defaultResult)
    }

    @Test
    fun `Unsafe putFloat intrinsic writes synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutFloatMethod())
            ?: error("Unsafe.putFloat intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(1.25f))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmFloatValue(-2.5f))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals(1.25f, unsafeMemory.getStaticFloat(offset = 7L))
        assertEquals(-2.5f, unsafeMemory.getStaticFloat(offset = 9L))
    }

    @Test
    fun `Unsafe putDouble intrinsic writes synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutDoubleMethod())
            ?: error("Unsafe.putDouble intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(1.25))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmDoubleValue(-2.5))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals(1.25, unsafeMemory.getStaticDouble(offset = 7L))
        assertEquals(-2.5, unsafeMemory.getStaticDouble(offset = 9L))
    }
    @Test
    fun `Unsafe putByte intrinsic writes synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutByteMethod())
            ?: error("Unsafe.putByte intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(-2))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(127))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals((-2).toByte(), unsafeMemory.getStaticByte(offset = 7L))
        assertEquals(127.toByte(), unsafeMemory.getStaticByte(offset = 9L))
    }

    @Test
    fun `Unsafe putShort intrinsic writes synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutShortMethod())
            ?: error("Unsafe.putShort intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(-2))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(32767))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals((-2).toShort(), unsafeMemory.getStaticShort(offset = 7L))
        assertEquals(32767.toShort(), unsafeMemory.getStaticShort(offset = 9L))
    }

    @Test
    fun `Unsafe getByteVolatile intrinsic reads synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetByteVolatileMethod())
            ?: error("Unsafe.getByteVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to (-2).toByte())),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(-2), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe getShortVolatile intrinsic reads synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetShortVolatileMethod())
            ?: error("Unsafe.getShortVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to (-2).toShort())),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(-2), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }


    @Test
    fun `Unsafe getFloatVolatile intrinsic reads synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetFloatVolatileMethod())
            ?: error("Unsafe.getFloatVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 1.25f)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmFloatValue(1.25f), setResult)
        assertEquals(JvmFloatValue(0.0f), defaultResult)
    }

    @Test
    fun `Unsafe getDoubleVolatile intrinsic reads synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetDoubleVolatileMethod())
            ?: error("Unsafe.getDoubleVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 1.25)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmDoubleValue(1.25), setResult)
        assertEquals(JvmDoubleValue(0.0), defaultResult)
    }

    @Test
    fun `Unsafe putFloatVolatile intrinsic writes synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutFloatVolatileMethod())
            ?: error("Unsafe.putFloatVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(1.25f))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmFloatValue(-2.5f))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals(1.25f, unsafeMemory.getStaticFloat(offset = 7L))
        assertEquals(-2.5f, unsafeMemory.getStaticFloat(offset = 9L))
    }

    @Test
    fun `Unsafe putDoubleVolatile intrinsic writes synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutDoubleVolatileMethod())
            ?: error("Unsafe.putDoubleVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(1.25))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmDoubleValue(-2.5))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals(1.25, unsafeMemory.getStaticDouble(offset = 7L))
        assertEquals(-2.5, unsafeMemory.getStaticDouble(offset = 9L))
    }
    @Test
    fun `Unsafe putByteVolatile intrinsic writes synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutByteVolatileMethod())
            ?: error("Unsafe.putByteVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(-2))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(127))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals((-2).toByte(), unsafeMemory.getStaticByte(offset = 7L))
        assertEquals(127.toByte(), unsafeMemory.getStaticByte(offset = 9L))
    }


    @Test
    fun `Unsafe getCharVolatile intrinsic reads synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetCharVolatileMethod())
            ?: error("Unsafe.getCharVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to '\u03A9')),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue('\u03A9'.code), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe putCharVolatile intrinsic writes synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutCharVolatileMethod())
            ?: error("Unsafe.putCharVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('\u03A9'.code))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(Char.MAX_VALUE.code))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals('\u03A9', unsafeMemory.getStaticChar(offset = 7L))
        assertEquals(Char.MAX_VALUE, unsafeMemory.getStaticChar(offset = 9L))
    }
    @Test
    fun `Unsafe putShortVolatile intrinsic writes synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutShortVolatileMethod())
            ?: error("Unsafe.putShortVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val firstResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(-2))),
        )
        val secondResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(32767))),
        )

        assertEquals(null, firstResult)
        assertEquals(null, secondResult)
        assertEquals((-2).toShort(), unsafeMemory.getStaticShort(offset = 7L))
        assertEquals(32767.toShort(), unsafeMemory.getStaticShort(offset = 9L))
    }

    @Test
    fun `Unsafe getBooleanVolatile intrinsic reads synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetBooleanVolatileMethod())
            ?: error("Unsafe.getBooleanVolatile intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true)),
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L))),
        )
        val defaultResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L))),
        )

        assertEquals(JvmIntValue(1), setResult)
        assertEquals(JvmIntValue(0), defaultResult)
    }

    @Test
    fun `Unsafe putBoolean intrinsic writes synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutBooleanMethod())
            ?: error("Unsafe.putBoolean intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1))),
        )
        val clearResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(0))),
        )

        assertEquals(null, setResult)
        assertEquals(null, clearResult)
        assertEquals(true, unsafeMemory.getStaticBoolean(offset = 7L))
        assertEquals(false, unsafeMemory.getStaticBoolean(offset = 9L))
    }

    @Test
    fun `Unsafe putBooleanVolatile intrinsic writes synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutBooleanVolatileMethod())
            ?: error("Unsafe.putBooleanVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val setResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1))),
        )
        val clearResult = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(0))),
        )

        assertEquals(null, setResult)
        assertEquals(null, clearResult)
        assertEquals(true, unsafeMemory.getStaticBoolean(offset = 7L))
        assertEquals(false, unsafeMemory.getStaticBoolean(offset = 9L))
    }

    @Test
    fun `Unsafe putIntVolatile intrinsic writes synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutIntVolatileMethod())
            ?: error("Unsafe.putIntVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42))),
        )

        assertEquals(null, result)
        assertEquals(42, unsafeMemory.getStaticInt(offset = 7L))
    }

    @Test
    fun `Unsafe putInt intrinsic writes synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutIntMethod())
            ?: error("Unsafe.putInt intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42))),
        )

        assertEquals(null, result)
        assertEquals(42, unsafeMemory.getStaticInt(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetInt intrinsic updates synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetIntMethod())
            ?: error("Unsafe.compareAndSetInt intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(44)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43, unsafeMemory.getStaticInt(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeInt intrinsic returns witness for synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeIntMethod())
            ?: error("Unsafe.compareAndExchangeInt intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(2)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )

        assertEquals(JvmIntValue(42), mismatch)
        assertEquals(JvmIntValue(42), match)
        assertEquals(43, unsafeMemory.getStaticInt(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetLong intrinsic updates synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetLongMethod())
            ?: error("Unsafe.compareAndSetLong intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(42L), JvmLongValue(43L)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(42L), JvmLongValue(44L)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43L, unsafeMemory.getStaticLong(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetDouble intrinsic updates synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetDoubleMethod())
            ?: error("Unsafe.compareAndSetDouble intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 42.0))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(42.0), JvmDoubleValue(43.0)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(42.0), JvmDoubleValue(44.0)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43.0, unsafeMemory.getStaticDouble(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetFloat intrinsic updates synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetFloatMethod())
            ?: error("Unsafe.compareAndSetFloat intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 42.0f))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(42.0f), JvmFloatValue(43.0f)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(42.0f), JvmFloatValue(44.0f)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43.0f, unsafeMemory.getStaticFloat(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetBoolean intrinsic updates synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetBooleanMethod())
            ?: error("Unsafe.compareAndSetBoolean intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(0)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(1)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(false, unsafeMemory.getStaticBoolean(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetByte intrinsic updates synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetByteMethod())
            ?: error("Unsafe.compareAndSetByte intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to 42.toByte()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(44)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43.toByte(), unsafeMemory.getStaticByte(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetShort intrinsic updates synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetShortMethod())
            ?: error("Unsafe.compareAndSetShort intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to 42.toShort()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(44)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals(43.toShort(), unsafeMemory.getStaticShort(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndSetChar intrinsic updates synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndSetCharMethod())
            ?: error("Unsafe.compareAndSetChar intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to 'A'))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val success = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('A'.code), JvmIntValue('B'.code)),
            ),
        )
        val failure = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('A'.code), JvmIntValue('C'.code)),
            ),
        )

        assertEquals(JvmIntValue(1), success)
        assertEquals(JvmIntValue(0), failure)
        assertEquals('B', unsafeMemory.getStaticChar(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeLong intrinsic returns witness for synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeLongMethod())
            ?: error("Unsafe.compareAndExchangeLong intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(1L), JvmLongValue(2L)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(42L), JvmLongValue(43L)),
            ),
        )

        assertEquals(JvmLongValue(42L), mismatch)
        assertEquals(JvmLongValue(42L), match)
        assertEquals(43L, unsafeMemory.getStaticLong(offset = 7L))
    }

    @Test
    fun `Unsafe getAndAddLong intrinsic returns witness for synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndAddLongMethod())
            ?: error("Unsafe.getAndAddLong intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(5L))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmLongValue(-3L))),
        )

        assertEquals(JvmLongValue(42L), existing)
        assertEquals(47L, unsafeMemory.getStaticLong(offset = 7L))
        assertEquals(JvmLongValue(0L), defaulted)
        assertEquals(-3L, unsafeMemory.getStaticLong(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetLong intrinsic returns witness for synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetLongMethod())
            ?: error("Unsafe.getAndSetLong intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticLongSlots = mapOf(7L to 42L))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(43L))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmLongValue(-3L))),
        )

        assertEquals(JvmLongValue(42L), existing)
        assertEquals(43L, unsafeMemory.getStaticLong(offset = 7L))
        assertEquals(JvmLongValue(0L), defaulted)
        assertEquals(-3L, unsafeMemory.getStaticLong(offset = 9L))
    }

    @Test
    fun `Unsafe getAndAddInt intrinsic returns witness for synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndAddIntMethod())
            ?: error("Unsafe.getAndAddInt intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(5))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(-3))),
        )

        assertEquals(JvmIntValue(42), existing)
        assertEquals(47, unsafeMemory.getStaticInt(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals(-3, unsafeMemory.getStaticInt(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetInt intrinsic returns witness for synthetic static int slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetIntMethod())
            ?: error("Unsafe.getAndSetInt intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticIntSlots = mapOf(7L to 42))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(43))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(-3))),
        )

        assertEquals(JvmIntValue(42), existing)
        assertEquals(43, unsafeMemory.getStaticInt(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals(-3, unsafeMemory.getStaticInt(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetBoolean intrinsic returns witness for synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetBooleanMethod())
            ?: error("Unsafe.getAndSetBoolean intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(0))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(1))),
        )

        assertEquals(JvmIntValue(1), existing)
        assertEquals(false, unsafeMemory.getStaticBoolean(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals(true, unsafeMemory.getStaticBoolean(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetByte intrinsic returns witness for synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetByteMethod())
            ?: error("Unsafe.getAndSetByte intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to 42.toByte()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(43))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(-3))),
        )

        assertEquals(JvmIntValue(42), existing)
        assertEquals(43.toByte(), unsafeMemory.getStaticByte(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals((-3).toByte(), unsafeMemory.getStaticByte(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetShort intrinsic returns witness for synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetShortMethod())
            ?: error("Unsafe.getAndSetShort intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to 42.toShort()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(43))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue(-3))),
        )

        assertEquals(JvmIntValue(42), existing)
        assertEquals(43.toShort(), unsafeMemory.getStaticShort(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals((-3).toShort(), unsafeMemory.getStaticShort(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetChar intrinsic returns witness for synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetCharMethod())
            ?: error("Unsafe.getAndSetChar intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to 'A'))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('B'.code))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmIntValue('C'.code))),
        )

        assertEquals(JvmIntValue('A'.code), existing)
        assertEquals('B', unsafeMemory.getStaticChar(offset = 7L))
        assertEquals(JvmIntValue(0), defaulted)
        assertEquals('C', unsafeMemory.getStaticChar(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetFloat intrinsic returns witness for synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetFloatMethod())
            ?: error("Unsafe.getAndSetFloat intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 42.0f))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(43.0f))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmFloatValue(-3.0f))),
        )

        assertEquals(JvmFloatValue(42.0f), existing)
        assertEquals(43.0f, unsafeMemory.getStaticFloat(offset = 7L))
        assertEquals(JvmFloatValue(0.0f), defaulted)
        assertEquals(-3.0f, unsafeMemory.getStaticFloat(offset = 9L))
    }

    @Test
    fun `Unsafe getAndSetDouble intrinsic returns witness for synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetAndSetDoubleMethod())
            ?: error("Unsafe.getAndSetDouble intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 42.0))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val existing = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(43.0))),
        )
        val defaulted = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(9L), JvmDoubleValue(-3.0))),
        )

        assertEquals(JvmDoubleValue(42.0), existing)
        assertEquals(43.0, unsafeMemory.getStaticDouble(offset = 7L))
        assertEquals(JvmDoubleValue(0.0), defaulted)
        assertEquals(-3.0, unsafeMemory.getStaticDouble(offset = 9L))
    }
    @Test
    fun `Unsafe compareAndExchangeDouble intrinsic returns witness for synthetic static double slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeDoubleMethod())
            ?: error("Unsafe.compareAndExchangeDouble intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticDoubleSlots = mapOf(7L to 42.0))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(1.0), JvmDoubleValue(2.0)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmDoubleValue(42.0), JvmDoubleValue(43.0)),
            ),
        )

        assertEquals(JvmDoubleValue(42.0), mismatch)
        assertEquals(JvmDoubleValue(42.0), match)
        assertEquals(43.0, unsafeMemory.getStaticDouble(offset = 7L))
    }
    @Test
    fun `Unsafe compareAndExchangeFloat intrinsic returns witness for synthetic static float slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeFloatMethod())
            ?: error("Unsafe.compareAndExchangeFloat intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticFloatSlots = mapOf(7L to 42.0f))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(1.0f), JvmFloatValue(2.0f)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmFloatValue(42.0f), JvmFloatValue(43.0f)),
            ),
        )

        assertEquals(JvmFloatValue(42.0f), mismatch)
        assertEquals(JvmFloatValue(42.0f), match)
        assertEquals(43.0f, unsafeMemory.getStaticFloat(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeBoolean intrinsic returns witness for synthetic static boolean slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeBooleanMethod())
            ?: error("Unsafe.compareAndExchangeBoolean intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticBooleanSlots = mapOf(7L to true))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(0), JvmIntValue(0)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(0)),
            ),
        )

        assertEquals(JvmIntValue(1), mismatch)
        assertEquals(JvmIntValue(1), match)
        assertEquals(false, unsafeMemory.getStaticBoolean(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeByte intrinsic returns witness for synthetic static byte slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeByteMethod())
            ?: error("Unsafe.compareAndExchangeByte intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticByteSlots = mapOf(7L to 42.toByte()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(2)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )

        assertEquals(JvmIntValue(42), mismatch)
        assertEquals(JvmIntValue(42), match)
        assertEquals(43.toByte(), unsafeMemory.getStaticByte(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeShort intrinsic returns witness for synthetic static short slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeShortMethod())
            ?: error("Unsafe.compareAndExchangeShort intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticShortSlots = mapOf(7L to 42.toShort()))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(1), JvmIntValue(2)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue(42), JvmIntValue(43)),
            ),
        )

        assertEquals(JvmIntValue(42), mismatch)
        assertEquals(JvmIntValue(42), match)
        assertEquals(43.toShort(), unsafeMemory.getStaticShort(offset = 7L))
    }

    @Test
    fun `Unsafe compareAndExchangeChar intrinsic returns witness for synthetic static char slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCompareAndExchangeCharMethod())
            ?: error("Unsafe.compareAndExchangeChar intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory(staticCharSlots = mapOf(7L to 'A'))
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val mismatch = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('B'.code), JvmIntValue('C'.code)),
            ),
        )
        val match = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(7L), JvmIntValue('A'.code), JvmIntValue('C'.code)),
            ),
        )

        assertEquals(JvmIntValue('A'.code), mismatch)
        assertEquals(JvmIntValue('A'.code), match)
        assertEquals('C', unsafeMemory.getStaticChar(offset = 7L))
    }

    @Test
    fun `Unsafe putLongVolatile intrinsic writes synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutLongVolatileMethod())
            ?: error("Unsafe.putLongVolatile intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(42L))),
        )

        assertEquals(null, result)
        assertEquals(42L, unsafeMemory.getStaticLong(offset = 7L))
    }

    @Test
    fun `Unsafe putLong intrinsic writes synthetic static long slots`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafePutLongMethod())
            ?: error("Unsafe.putLong intrinsic was not registered")
        val unsafeMemory = JvmUnsafeSyntheticMemory()
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = unsafeMemory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(JvmNullValue, JvmLongValue(7L), JvmLongValue(42L))),
        )

        assertEquals(null, result)
        assertEquals(42L, unsafeMemory.getStaticLong(offset = 7L))
    }

    @Test
    fun `Unsafe arrayIndexScale0 intrinsic returns synthetic array element scales`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val byteArrayClass = heap.internClassMirror("[B")
        val charArrayClass = heap.internClassMirror("[C")
        val intArrayClass = heap.internClassMirror("[I")
        val longArrayClass = heap.internClassMirror("[J")
        val referenceArrayClass = heap.internClassMirror("[Ljava/lang/Object;")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeArrayIndexScale0Method())
            ?: error("Unsafe.arrayIndexScale0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        assertEquals(JvmIntValue(1), intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(byteArrayClass))))
        assertEquals(JvmIntValue(2), intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(charArrayClass))))
        assertEquals(JvmIntValue(4), intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(intArrayClass))))
        assertEquals(JvmIntValue(8), intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(longArrayClass))))
        assertEquals(JvmIntValue(4), intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(referenceArrayClass))))
    }

    @Test
    fun `Unsafe arrayBaseOffset0 intrinsic returns synthetic array base offset`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intArrayClass = heap.internClassMirror("[I")
        val referenceArrayClass = heap.internClassMirror("[Ljava/lang/Object;")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeArrayBaseOffset0Method())
            ?: error("Unsafe.arrayBaseOffset0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val intArrayBase = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(intArrayClass)))
        val referenceArrayBase = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(referenceArrayClass)))

        assertEquals(JvmIntValue(0), intArrayBase)
        assertEquals(JvmIntValue(0), referenceArrayBase)
    }

    @Test
    fun `Unsafe fullFence intrinsic is a simulated no op`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeFullFenceMethod())
            ?: error("Unsafe.fullFence intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, emptyList()))

        assertEquals(null, result)
    }

    @Test
    fun `Unsafe loadFence intrinsic is a simulated no op`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeLoadFenceMethod())
            ?: error("Unsafe.loadFence intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, emptyList()))

        assertEquals(null, result)
    }

    @Test
    fun `Unsafe storeFence intrinsic is a simulated no op`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeStoreFenceMethod())
            ?: error("Unsafe.storeFence intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, emptyList()))

        assertEquals(null, result)
    }

    @Test
    fun `Thread yield0 intrinsic delegates to the context yield handler`() {
        var yields = 0
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadYield0Method())
            ?: error("Thread.yield0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = JvmHeap(),
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            threadYieldHandler = { yields += 1 },
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(receiver = null, arguments = emptyList()),
        )

        assertEquals(null, result)
        assertEquals(1, yields)
    }

    @Test
    fun `Thread holdsLock intrinsic reports current guest monitor ownership`() {
        val heap = JvmHeap()
        val receiver = heap.allocateObject("java/lang/Object")
        val monitors = JvmMonitorState()
        monitors.enter(receiver, threadId = "owner")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadHoldsLockMethod())
            ?: error("Thread.holdsLock intrinsic was not registered")
        val ownerContext = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
            monitors = monitors,
            currentThreadId = "owner",
        )
        val otherContext = ownerContext.copy(currentThreadId = "other")

        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(ownerContext, JvmNativeMethodInvocation(null, listOf(receiver))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(otherContext, JvmNativeMethodInvocation(null, listOf(receiver))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(ownerContext, JvmNativeMethodInvocation(null, listOf(JvmNullValue))),
        )
    }

    @Test
    fun `Thread ensureMaterializedForStackWalk intrinsic is a simulated no op`() {
        val heap = JvmHeap()
        val continuation = heap.allocateObject("java/lang/Object")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(threadEnsureMaterializedForStackWalkMethod())
            ?: error("Thread.ensureMaterializedForStackWalk intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "java/lang/Thread",
        )

        val objectResult = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(continuation)))
        val nullResult = intrinsic.invoke(context, JvmNativeMethodInvocation(null, listOf(JvmNullValue)))

        assertEquals(null, objectResult)
        assertEquals(null, nullResult)
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
    fun `Unsafe allocateInstance intrinsic allocates an uninitialized guest object for a class mirror`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val targetClass = heap.internClassMirror("Example")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeAllocateInstanceMethod())
            ?: error("Unsafe.allocateInstance intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(targetClass))) as JvmObjectReferenceValue

        assertEquals("Example", heap.get(result).className)
    }
    @Test
    fun `Unsafe throwException intrinsic throws the supplied guest throwable`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val throwable = heap.allocateObject("java/lang/IllegalStateException")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeThrowExceptionMethod())
            ?: error("Unsafe.throwException intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val thrown = assertFailsWith<JvmThrownException> {
            intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(throwable)))
        }

        assertEquals(throwable, thrown.throwable)
        assertEquals("java/lang/IllegalStateException", thrown.guestClassName)
    }

    @Test
    fun `Unsafe park intrinsic parks the current scheduler thread`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val scheduler = JvmThreadScheduler()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeParkMethod())
            ?: error("Unsafe.park intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            threadScheduler = scheduler,
            currentThreadId = "worker",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmIntValue(0), JvmLongValue(0L))))

        assertEquals(null, result)
        assertEquals(
            JvmThreadSchedulingState.Parked(isAbsolute = false, time = 0L),
            scheduler.state("worker"),
        )
    }

    @Test
    fun `Unsafe unpark intrinsic resumes a parked guest thread`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val targetThread = heap.internThread("worker")
        val scheduler = JvmThreadScheduler()
        scheduler.parkThread(threadId = "worker", isAbsolute = false, time = 0L)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeUnparkMethod())
            ?: error("Unsafe.unpark intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            threadScheduler = scheduler,
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(targetThread)))

        assertEquals(null, result)
        assertEquals(JvmThreadSchedulingState.Runnable, scheduler.state("worker"))
    }

    @Test
    fun `Unsafe getLoadAverage0 intrinsic reports no simulated host load samples`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val samples = heap.allocateDoubleArray(3)
        val payload = heap.get(samples).payload as JvmDoubleArrayPayload
        payload.elements[0] = 1.0
        payload.elements[1] = 5.0
        payload.elements[2] = 15.0
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeGetLoadAverage0Method())
            ?: error("Unsafe.getLoadAverage0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(samples, JvmIntValue(3))))

        assertEquals(JvmIntValue(0), result)
        assertEquals(mutableListOf(1.0, 5.0, 15.0), payload.elements)
    }

    @Test
    fun `Unsafe writeback intrinsics are simulated cache no ops`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val writeback = JvmVmIntrinsics.Registry.resolve(unsafeWriteback0Method())
            ?: error("Unsafe.writeback0 intrinsic was not registered")
        val preSync = JvmVmIntrinsics.Registry.resolve(unsafeWritebackPreSync0Method())
            ?: error("Unsafe.writebackPreSync0 intrinsic was not registered")
        val postSync = JvmVmIntrinsics.Registry.resolve(unsafeWritebackPostSync0Method())
            ?: error("Unsafe.writebackPostSync0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        assertEquals(null, writeback.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmLongValue(0x1000L)))))
        assertEquals(null, preSync.invoke(context, JvmNativeMethodInvocation(unsafe, emptyList())))
        assertEquals(null, postSync.invoke(context, JvmNativeMethodInvocation(unsafe, emptyList())))
    }

    @Test
    fun `Unsafe allocateMemory0 intrinsic returns synthetic native addresses`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeAllocateMemory0Method())
            ?: error("Unsafe.allocateMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
        )

        val first = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmLongValue(8L))))
        val second = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmLongValue(16L))))

        assertTrue(first is JvmLongValue && first.value != 0L)
        assertTrue(second is JvmLongValue && second.value != 0L)
        assertNotEquals(first.value, second.value)
    }

    @Test
    fun `Unsafe freeMemory0 intrinsic releases synthetic native addresses`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val memory = JvmUnsafeSyntheticMemory()
        val address = memory.allocateNativeMemory(24L)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeFreeMemory0Method())
            ?: error("Unsafe.freeMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmLongValue(address))))

        assertEquals(null, result)
        assertEquals(null, memory.nativeMemoryBlockSize(address))
    }

    @Test
    fun `Unsafe reallocateMemory0 intrinsic resizes synthetic native addresses`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val memory = JvmUnsafeSyntheticMemory()
        val address = memory.allocateNativeMemory(24L)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeReallocateMemory0Method())
            ?: error("Unsafe.reallocateMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(JvmLongValue(address), JvmLongValue(40L))))

        assertTrue(result is JvmLongValue && result.value != 0L)
        assertEquals(40L, memory.nativeMemoryBlockSize(result.value))
        if (result.value != address) {
            assertEquals(null, memory.nativeMemoryBlockSize(address))
        }
    }

    @Test
    fun `Unsafe setMemory0 intrinsic fills synthetic native memory bytes`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val memory = JvmUnsafeSyntheticMemory()
        val address = memory.allocateNativeMemory(4L)
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeSetMemory0Method())
            ?: error("Unsafe.setMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(address + 1L), JvmLongValue(3L), JvmIntValue(0x7f)),
            ),
        )

        assertEquals(null, result)
        assertEquals(0, memory.nativeMemoryByte(address))
        assertEquals(0x7f.toByte(), memory.nativeMemoryByte(address + 1L))
        assertEquals(0x7f.toByte(), memory.nativeMemoryByte(address + 2L))
        assertEquals(0x7f.toByte(), memory.nativeMemoryByte(address + 3L))
    }

    @Test
    fun `Unsafe copyMemory0 intrinsic copies synthetic native memory bytes`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val memory = JvmUnsafeSyntheticMemory()
        val source = memory.allocateNativeMemory(4L)
        val target = memory.allocateNativeMemory(4L)
        memory.setNativeMemory(source, 4L, 0x11.toByte())
        memory.setNativeMemory(source + 1L, 2L, 0x22.toByte())
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCopyMemory0Method())
            ?: error("Unsafe.copyMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(JvmNullValue, JvmLongValue(source + 1L), JvmNullValue, JvmLongValue(target), JvmLongValue(2L)),
            ),
        )

        assertEquals(null, result)
        assertEquals(0x22.toByte(), memory.nativeMemoryByte(target))
        assertEquals(0x22.toByte(), memory.nativeMemoryByte(target + 1L))
        assertEquals(0, memory.nativeMemoryByte(target + 2L))
    }

    @Test
    fun `Unsafe copySwapMemory0 intrinsic swaps synthetic native memory element bytes`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val memory = JvmUnsafeSyntheticMemory()
        val source = memory.allocateNativeMemory(4L)
        val target = memory.allocateNativeMemory(4L)
        memory.setNativeMemory(source, 1L, 0x01.toByte())
        memory.setNativeMemory(source + 1L, 1L, 0x02.toByte())
        memory.setNativeMemory(source + 2L, 1L, 0x03.toByte())
        memory.setNativeMemory(source + 3L, 1L, 0x04.toByte())
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeCopySwapMemory0Method())
            ?: error("Unsafe.copySwapMemory0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val result = intrinsic.invoke(
            context,
            JvmNativeMethodInvocation(
                unsafe,
                listOf(
                    JvmNullValue,
                    JvmLongValue(source),
                    JvmNullValue,
                    JvmLongValue(target),
                    JvmLongValue(4L),
                    JvmLongValue(2L),
                ),
            ),
        )

        assertEquals(null, result)
        assertEquals(0x02.toByte(), memory.nativeMemoryByte(target))
        assertEquals(0x01.toByte(), memory.nativeMemoryByte(target + 1L))
        assertEquals(0x04.toByte(), memory.nativeMemoryByte(target + 2L))
        assertEquals(0x03.toByte(), memory.nativeMemoryByte(target + 3L))
    }

    @Test
    fun `Unsafe objectFieldOffset1 intrinsic assigns stable guest instance field offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val ownerClass = heap.internClassMirror("FieldOffsetOwner")
        val fieldName = heap.internString("value")
        val memory = JvmUnsafeSyntheticMemory()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())
            ?: error("Unsafe.objectFieldOffset1 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "FieldOffsetOwner",
                        fields = listOf(
                            JvmFieldDefinition(name = "value", descriptor = "I", isStatic = false),
                            JvmFieldDefinition(name = "shared", descriptor = "J", isStatic = true),
                        ),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )

        val first = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)))
        val second = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)))

        assertTrue(first is JvmLongValue && first.value != 0L)
        assertEquals(first, second)
    }

    @Test
    fun `Unsafe getInt intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("FieldOffsetOwner")
        val ownerClass = heap.internClassMirror("FieldOffsetOwner")
        val fieldName = heap.internString("value")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("FieldOffsetOwner", "value", "I"),
            JvmIntValue(1234),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "FieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "value", descriptor = "I", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getInt = JvmVmIntrinsics.Registry.resolve(unsafeGetIntMethod())
            ?: error("Unsafe.getInt intrinsic was not registered")

        val result = getInt.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmIntValue(1234), result)
    }

    @Test
    fun `Unsafe putInt intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("FieldOffsetOwner")
        val ownerClass = heap.internClassMirror("FieldOffsetOwner")
        val fieldName = heap.internString("value")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("FieldOffsetOwner", "value", "I")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "FieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "value", descriptor = "I", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putInt = JvmVmIntrinsics.Registry.resolve(unsafePutIntMethod())
            ?: error("Unsafe.putInt intrinsic was not registered")

        val result = putInt.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset, JvmIntValue(5678))))

        assertEquals(null, result)
        assertEquals(JvmIntValue(5678), heap.getInstanceField(target, field))
    }

    @Test
    fun `Unsafe getLong intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("LongFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("LongFieldOffsetOwner")
        val fieldName = heap.internString("counter")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("LongFieldOffsetOwner", "counter", "J"),
            JvmLongValue(9_876_543_210L),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "LongFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "J", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getLong = JvmVmIntrinsics.Registry.resolve(unsafeGetLongMethod())
            ?: error("Unsafe.getLong intrinsic was not registered")

        val result = getLong.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmLongValue(9_876_543_210L), result)
    }

    @Test
    fun `Unsafe putLong intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("LongFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("LongFieldOffsetOwner")
        val fieldName = heap.internString("counter")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("LongFieldOffsetOwner", "counter", "J")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "LongFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "counter", descriptor = "J", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putLong = JvmVmIntrinsics.Registry.resolve(unsafePutLongMethod())
            ?: error("Unsafe.putLong intrinsic was not registered")

        val result = putLong.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(target, offset, JvmLongValue(12_345_678_901L))),
        )

        assertEquals(null, result)
        assertEquals(JvmLongValue(12_345_678_901L), heap.getInstanceField(target, field))
    }
    @Test
    fun `Unsafe getReference intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ReferenceFieldOffsetOwner")
        val referent = heap.allocateObject("java/lang/Object")
        val ownerClass = heap.internClassMirror("ReferenceFieldOffsetOwner")
        val fieldName = heap.internString("next")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("ReferenceFieldOffsetOwner", "next", "Ljava/lang/Object;"),
            referent,
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ReferenceFieldOffsetOwner",
                        fields = listOf(
                            JvmFieldDefinition(name = "next", descriptor = "Ljava/lang/Object;", isStatic = false),
                        ),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getReference = JvmVmIntrinsics.Registry.resolve(unsafeGetReferenceMethod())
            ?: error("Unsafe.getReference intrinsic was not registered")

        val result = getReference.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(referent, result)
    }
    @Test
    fun `Unsafe putReference intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ReferenceFieldOffsetOwner")
        val referent = heap.allocateObject("java/lang/Object")
        val ownerClass = heap.internClassMirror("ReferenceFieldOffsetOwner")
        val fieldName = heap.internString("next")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("ReferenceFieldOffsetOwner", "next", "Ljava/lang/Object;")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ReferenceFieldOffsetOwner",
                        fields = listOf(
                            JvmFieldDefinition(name = "next", descriptor = "Ljava/lang/Object;", isStatic = false),
                        ),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putReference = JvmVmIntrinsics.Registry.resolve(unsafePutReferenceMethod())
            ?: error("Unsafe.putReference intrinsic was not registered")

        val result = putReference.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset, referent)))

        assertEquals(null, result)
        assertEquals(referent, heap.getInstanceField(target, field))
    }
    @Test
    fun `Unsafe getBoolean intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("BooleanFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("BooleanFieldOffsetOwner")
        val fieldName = heap.internString("enabled")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("BooleanFieldOffsetOwner", "enabled", "Z"),
            JvmIntValue(1),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "BooleanFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "enabled", descriptor = "Z", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getBoolean = JvmVmIntrinsics.Registry.resolve(unsafeGetBooleanMethod())
            ?: error("Unsafe.getBoolean intrinsic was not registered")

        val result = getBoolean.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmIntValue(1), result)
    }
    @Test
    fun `Unsafe putBoolean intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("BooleanFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("BooleanFieldOffsetOwner")
        val fieldName = heap.internString("enabled")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("BooleanFieldOffsetOwner", "enabled", "Z")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "BooleanFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "enabled", descriptor = "Z", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putBoolean = JvmVmIntrinsics.Registry.resolve(unsafePutBooleanMethod())
            ?: error("Unsafe.putBoolean intrinsic was not registered")

        val result = putBoolean.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset, JvmIntValue(1))))

        assertEquals(null, result)
        assertEquals(JvmIntValue(1), heap.getInstanceField(target, field))
    }
    @Test
    fun `Unsafe getByte intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ByteFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("ByteFieldOffsetOwner")
        val fieldName = heap.internString("tag")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("ByteFieldOffsetOwner", "tag", "B"),
            JvmIntValue(-12),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ByteFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "tag", descriptor = "B", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getByte = JvmVmIntrinsics.Registry.resolve(unsafeGetByteMethod())
            ?: error("Unsafe.getByte intrinsic was not registered")

        val result = getByte.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmIntValue(-12), result)
    }
    @Test
    fun `Unsafe putByte intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ByteFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("ByteFieldOffsetOwner")
        val fieldName = heap.internString("tag")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("ByteFieldOffsetOwner", "tag", "B")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ByteFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "tag", descriptor = "B", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putByte = JvmVmIntrinsics.Registry.resolve(unsafePutByteMethod())
            ?: error("Unsafe.putByte intrinsic was not registered")

        val result = putByte.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset, JvmIntValue(-34))))

        assertEquals(null, result)
        assertEquals(JvmIntValue(-34), heap.getInstanceField(target, field))
    }

    @Test
    fun `Unsafe getShort intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ShortFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("ShortFieldOffsetOwner")
        val fieldName = heap.internString("code")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("ShortFieldOffsetOwner", "code", "S"),
            JvmIntValue(-1234),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ShortFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "code", descriptor = "S", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getShort = JvmVmIntrinsics.Registry.resolve(unsafeGetShortMethod())
            ?: error("Unsafe.getShort intrinsic was not registered")

        val result = getShort.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmIntValue(-1234), result)
    }

    @Test
    fun `Unsafe putShort intrinsic writes guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("ShortFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("ShortFieldOffsetOwner")
        val fieldName = heap.internString("code")
        val memory = JvmUnsafeSyntheticMemory()
        val field = JvmFieldReference("ShortFieldOffsetOwner", "code", "S")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "ShortFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "code", descriptor = "S", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val putShort = JvmVmIntrinsics.Registry.resolve(unsafePutShortMethod())
            ?: error("Unsafe.putShort intrinsic was not registered")

        val result = putShort.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset, JvmIntValue(-2345))))

        assertEquals(null, result)
        assertEquals(JvmIntValue(-2345), heap.getInstanceField(target, field))
    }

    @Test
    fun `Unsafe getChar intrinsic reads guest object fields through synthetic offsets`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val target = heap.allocateObject("CharFieldOffsetOwner")
        val ownerClass = heap.internClassMirror("CharFieldOffsetOwner")
        val fieldName = heap.internString("mark")
        val memory = JvmUnsafeSyntheticMemory()
        heap.putInstanceField(
            target,
            JvmFieldReference("CharFieldOffsetOwner", "mark", "C"),
            JvmIntValue(0x2603),
        )
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "CharFieldOffsetOwner",
                        fields = listOf(JvmFieldDefinition(name = "mark", descriptor = "C", isStatic = false)),
                    ),
                ),
            ),
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            unsafeMemory = memory,
        )
        val offset = JvmVmIntrinsics.Registry.resolve(unsafeObjectFieldOffset1Method())!!.invoke(
            context,
            JvmNativeMethodInvocation(unsafe, listOf(ownerClass, fieldName)),
        ) as JvmLongValue
        val getChar = JvmVmIntrinsics.Registry.resolve(unsafeGetCharMethod())
            ?: error("Unsafe.getChar intrinsic was not registered")

        val result = getChar.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(target, offset)))

        assertEquals(JvmIntValue(0x2603), result)
    }

    @Test
    fun `Unsafe shouldBeInitialized0 intrinsic reflects guest class initialization state`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val preparedClass = heap.internClassMirror("PreparedExample")
        val initializedClass = heap.internClassMirror("InitializedExample")
        val initializationStates = JvmClassInitializationStates()
        initializationStates.startInitialization("InitializedExample", "main")
        initializationStates.completeInitialization("InitializedExample", "main")
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeShouldBeInitialized0Method())
            ?: error("Unsafe.shouldBeInitialized0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            classInitializationStates = initializationStates,
        )

        assertEquals(
            JvmIntValue(1),
            intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(preparedClass))),
        )
        assertEquals(
            JvmIntValue(0),
            intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(initializedClass))),
        )
    }

    @Test
    fun `Unsafe ensureClassInitialized0 intrinsic delegates class mirror initialization to context hook`() {
        val heap = JvmHeap()
        val unsafe = heap.allocateObject("jdk/internal/misc/Unsafe")
        val classMirror = heap.internClassMirror("EnsureExample")
        val initializationStates = JvmClassInitializationStates()
        val initializedClassNames = mutableListOf<String>()
        val intrinsic = JvmVmIntrinsics.Registry.resolve(unsafeEnsureClassInitialized0Method())
            ?: error("Unsafe.ensureClassInitialized0 intrinsic was not registered")
        val context = JvmNativeMethodContext(
            heap = heap,
            classHierarchy = JvmClassHierarchy.Empty,
            staticFields = JvmStaticFields(),
            currentClassName = "jdk/internal/misc/Unsafe",
            classInitializationStates = initializationStates,
            ensureClassInitializedHandler = { className ->
                initializedClassNames += className
                initializationStates.startInitialization(className, "main")
                initializationStates.completeInitialization(className, "main")
            },
        )

        val result = intrinsic.invoke(context, JvmNativeMethodInvocation(unsafe, listOf(classMirror)))

        assertEquals(null, result)
        assertEquals(listOf("EnsureExample"), initializedClassNames)
        assertEquals(
            JvmIntValue(0),
            JvmVmIntrinsics.Registry.resolve(unsafeShouldBeInitialized0Method())!!.invoke(
                context,
                JvmNativeMethodInvocation(unsafe, listOf(classMirror)),
            ),
        )
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
            systemRegisterNativesMethod(),
            systemSetIn0Method(),
            systemSetOut0Method(),
            systemSetErr0Method(),
            systemCurrentTimeMillisMethod(),
            systemNanoTimeMethod(),
            classRegisterNativesMethod(),
            classGetPrimitiveClassMethod(),
            classInitClassNameMethod(),
            classIsArrayMethod(),
            classIsPrimitiveMethod(),
            classIsInterfaceMethod(),
            classIsAssignableFromMethod(),
            classIsInstanceMethod(),
            classGetSuperclassMethod(),
            classGetInterfaces0Method(),
            classDesiredAssertionStatus0Method(),
            classIsHiddenMethod(),
            throwableFillInStackTraceMethod(),
            stringInternMethod(),
            threadRegisterNativesMethod(),
            threadCurrentThreadMethod(),
            threadCurrentCarrierThreadMethod(),
            threadFindScopedValueBindingsMethod(),
            threadScopedValueCacheMethod(),
            threadSetScopedValueCacheMethod(),
            threadGetThreadsMethod(),
            threadClearInterruptEventMethod(),
            threadSetNativeNameMethod(),
            threadSetPriority0Method(),
            threadInterrupt0Method(),
            threadStart0Method(),
            threadSetCurrentThreadMethod(),
            threadGetStackTrace0Method(),
            threadDumpThreadsMethod(),
            threadGetNextThreadIdOffsetMethod(),
            threadYield0Method(),
            threadHoldsLockMethod(),
            threadEnsureMaterializedForStackWalkMethod(),
            threadSleepMillisMethod(),
            threadSleepMillisNanosMethod(),
            threadSleepNanos0Method(),
            unsafeRegisterNativesMethod(),
            unsafeAllocateInstanceMethod(),
            unsafeThrowExceptionMethod(),
            unsafeParkMethod(),
            unsafeUnparkMethod(),
            unsafeGetLongVolatileMethod(),
            unsafeGetLongMethod(),
            unsafeGetReferenceVolatileMethod(),
            unsafeGetReferenceMethod(),
            unsafePutReferenceVolatileMethod(),
            unsafePutReferenceMethod(),
            unsafeCompareAndSetReferenceMethod(),
            unsafeCompareAndExchangeReferenceMethod(),
            unsafeGetAndSetReferenceMethod(),
            unsafeGetIntVolatileMethod(),
            unsafeGetBooleanVolatileMethod(),
            unsafeGetByteVolatileMethod(),
            unsafeGetShortVolatileMethod(),
            unsafeGetCharVolatileMethod(),
            unsafeGetFloatVolatileMethod(),
            unsafeGetDoubleVolatileMethod(),
            unsafePutByteVolatileMethod(),
            unsafePutDoubleVolatileMethod(),
            unsafeGetIntMethod(),
            unsafeGetBooleanMethod(),
            unsafeGetByteMethod(),
            unsafeGetShortMethod(),
            unsafeGetCharMethod(),
            unsafeGetFloatMethod(),
            unsafeGetDoubleMethod(),
            unsafePutByteMethod(),
            unsafePutShortMethod(),
            unsafePutCharMethod(),
            unsafePutFloatMethod(),
            unsafePutDoubleMethod(),
            unsafePutBooleanMethod(),
            unsafePutBooleanVolatileMethod(),
            unsafePutIntVolatileMethod(),
            unsafePutIntMethod(),
            unsafeCompareAndSetIntMethod(),
            unsafeCompareAndExchangeIntMethod(),
            unsafeCompareAndSetLongMethod(),
            unsafeCompareAndSetBooleanMethod(),
            unsafeCompareAndSetByteMethod(),
            unsafeCompareAndSetShortMethod(),
            unsafeCompareAndSetCharMethod(),
            unsafeCompareAndSetFloatMethod(),
            unsafeCompareAndSetDoubleMethod(),
            unsafeCompareAndExchangeLongMethod(),
            unsafeCompareAndExchangeBooleanMethod(),
            unsafeCompareAndExchangeByteMethod(),
            unsafeCompareAndExchangeShortMethod(),
            unsafeCompareAndExchangeCharMethod(),
            unsafeGetAndAddLongMethod(),
            unsafeGetAndSetLongMethod(),
            unsafeGetAndAddIntMethod(),
            unsafeGetAndSetIntMethod(),
            unsafeGetAndSetBooleanMethod(),
            unsafeGetAndSetByteMethod(),
            unsafeGetAndSetShortMethod(),
            unsafeGetAndSetCharMethod(),
            unsafeGetAndSetFloatMethod(),
            unsafeGetAndSetDoubleMethod(),
            unsafeCompareAndExchangeFloatMethod(),
            unsafeCompareAndExchangeDoubleMethod(),
            unsafePutLongVolatileMethod(),
            unsafePutLongMethod(),
            unsafeArrayIndexScale0Method(),
            unsafeArrayBaseOffset0Method(),
            unsafeGetLoadAverage0Method(),
            unsafeWriteback0Method(),
            unsafeWritebackPreSync0Method(),
            unsafeWritebackPostSync0Method(),
            unsafeAllocateMemory0Method(),
            unsafeReallocateMemory0Method(),
            unsafeSetMemory0Method(),
            unsafeCopyMemory0Method(),
            unsafeCopySwapMemory0Method(),
            unsafeObjectFieldOffset1Method(),
            unsafeFreeMemory0Method(),
            unsafeShouldBeInitialized0Method(),
            unsafeEnsureClassInitialized0Method(),
            unsafeFullFenceMethod(),
            unsafeLoadFenceMethod(),
            unsafeStoreFenceMethod(),
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

    private fun systemRegisterNativesMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun systemSetIn0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "setIn0",
        descriptor = "(Ljava/io/InputStream;)V",
        isStatic = true,
        isNative = true,
    )

    private fun systemSetOut0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "setOut0",
        descriptor = "(Ljava/io/PrintStream;)V",
        isStatic = true,
        isNative = true,
    )

    private fun systemSetErr0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/System",
        name = "setErr0",
        descriptor = "(Ljava/io/PrintStream;)V",
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

    private fun runtimeGcMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "gc",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun runtimeFreeMemoryMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "freeMemory",
        descriptor = "()J",
        isStatic = false,
        isNative = true,
    )

    private fun runtimeTotalMemoryMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "totalMemory",
        descriptor = "()J",
        isStatic = false,
        isNative = true,
    )

    private fun runtimeMaxMemoryMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "maxMemory",
        descriptor = "()J",
        isStatic = false,
        isNative = true,
    )

    private fun runtimeAvailableProcessorsMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Runtime",
        name = "availableProcessors",
        descriptor = "()I",
        isStatic = false,
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

    private fun classRegisterNativesMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun classGetPrimitiveClassMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "getPrimitiveClass",
        descriptor = "(Ljava/lang/String;)Ljava/lang/Class;",
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

    private fun classIsAssignableFromMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isAssignableFrom",
        descriptor = "(Ljava/lang/Class;)Z",
        isStatic = false,
        isNative = true,
    )

    private fun classIsInstanceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isInstance",
        descriptor = "(Ljava/lang/Object;)Z",
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

    private fun classGetInterfaces0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "getInterfaces0",
        descriptor = "()[Ljava/lang/Class;",
        isStatic = false,
        isNative = true,
    )

    private fun classDesiredAssertionStatus0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "desiredAssertionStatus0",
        descriptor = "(Ljava/lang/Class;)Z",
        isStatic = true,
        isNative = true,
    )

    private fun classIsHiddenMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Class",
        name = "isHidden",
        descriptor = "()Z",
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

    private fun threadRegisterNativesMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun threadCurrentThreadMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "currentThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
        isNative = true,
    )

    private fun threadCurrentCarrierThreadMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "currentCarrierThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
        isNative = true,
    )

    private fun threadFindScopedValueBindingsMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "findScopedValueBindings",
        descriptor = "()Ljava/lang/Object;",
        isStatic = true,
        isNative = true,
    )

    private fun threadScopedValueCacheMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "scopedValueCache",
        descriptor = "()[Ljava/lang/Object;",
        isStatic = true,
        isNative = true,
    )

    private fun threadSetScopedValueCacheMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "setScopedValueCache",
        descriptor = "([Ljava/lang/Object;)V",
        isStatic = true,
        isNative = true,
    )

    private fun threadGetThreadsMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "getThreads",
        descriptor = "()[Ljava/lang/Thread;",
        isStatic = true,
        isNative = true,
    )

    private fun threadClearInterruptEventMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "clearInterruptEvent",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun threadSetNativeNameMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "setNativeName",
        descriptor = "(Ljava/lang/String;)V",
        isStatic = false,
        isNative = true,
    )

    private fun threadSetPriority0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "setPriority0",
        descriptor = "(I)V",
        isStatic = false,
        isNative = true,
    )

    private fun threadInterrupt0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "interrupt0",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun threadStart0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "start0",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun threadSetCurrentThreadMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "setCurrentThread",
        descriptor = "(Ljava/lang/Thread;)V",
        isStatic = false,
        isNative = true,
    )

    private fun threadGetStackTrace0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "getStackTrace0",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun threadDumpThreadsMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "dumpThreads",
        descriptor = "([Ljava/lang/Thread;)[[Ljava/lang/StackTraceElement;",
        isStatic = true,
        isNative = true,
    )

    private fun threadGetNextThreadIdOffsetMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "getNextThreadIdOffset",
        descriptor = "()J",
        isStatic = true,
        isNative = true,
    )

    private fun threadYield0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "yield0",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun threadHoldsLockMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "holdsLock",
        descriptor = "(Ljava/lang/Object;)Z",
        isStatic = true,
        isNative = true,
    )

    private fun threadEnsureMaterializedForStackWalkMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "java/lang/Thread",
        name = "ensureMaterializedForStackWalk",
        descriptor = "(Ljava/lang/Object;)V",
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

    private fun unsafeRegisterNativesMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
        isNative = true,
    )

    private fun unsafeAllocateInstanceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "allocateInstance",
        descriptor = "(Ljava/lang/Class;)Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )
    private fun unsafeThrowExceptionMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "throwException",
        descriptor = "(Ljava/lang/Throwable;)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeParkMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "park",
        descriptor = "(ZJ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeUnparkMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "unpark",
        descriptor = "(Ljava/lang/Object;)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetLongVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getLongVolatile",
        descriptor = "(Ljava/lang/Object;J)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetLong",
        descriptor = "(Ljava/lang/Object;JJJ)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getLong",
        descriptor = "(Ljava/lang/Object;J)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetReferenceVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getReferenceVolatile",
        descriptor = "(Ljava/lang/Object;J)Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutReferenceVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putReferenceVolatile",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetReferenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getReference",
        descriptor = "(Ljava/lang/Object;J)Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutReferenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetReferenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeReferenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetReferenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;)Ljava/lang/Object;",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetIntVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getIntVolatile",
        descriptor = "(Ljava/lang/Object;J)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getInt",
        descriptor = "(Ljava/lang/Object;J)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetBooleanMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getBoolean",
        descriptor = "(Ljava/lang/Object;J)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetByteMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getByte",
        descriptor = "(Ljava/lang/Object;J)B",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetShortMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getShort",
        descriptor = "(Ljava/lang/Object;J)S",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetCharMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getChar",
        descriptor = "(Ljava/lang/Object;J)C",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutCharMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putChar",
        descriptor = "(Ljava/lang/Object;JC)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetFloatMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getFloat",
        descriptor = "(Ljava/lang/Object;J)F",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetDoubleMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getDouble",
        descriptor = "(Ljava/lang/Object;J)D",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutFloatMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putFloat",
        descriptor = "(Ljava/lang/Object;JF)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutDoubleMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putDouble",
        descriptor = "(Ljava/lang/Object;JD)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutByteMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putByte",
        descriptor = "(Ljava/lang/Object;JB)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutShortMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putShort",
        descriptor = "(Ljava/lang/Object;JS)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetByteVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getByteVolatile",
        descriptor = "(Ljava/lang/Object;J)B",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetShortVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getShortVolatile",
        descriptor = "(Ljava/lang/Object;J)S",
        isStatic = false,
        isNative = true,
    )


    private fun unsafeGetCharVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getCharVolatile",
        descriptor = "(Ljava/lang/Object;J)C",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutCharVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putCharVolatile",
        descriptor = "(Ljava/lang/Object;JC)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetFloatVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getFloatVolatile",
        descriptor = "(Ljava/lang/Object;J)F",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetDoubleVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getDoubleVolatile",
        descriptor = "(Ljava/lang/Object;J)D",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutFloatVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putFloatVolatile",
        descriptor = "(Ljava/lang/Object;JF)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutDoubleVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putDoubleVolatile",
        descriptor = "(Ljava/lang/Object;JD)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutByteVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putByteVolatile",
        descriptor = "(Ljava/lang/Object;JB)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutShortVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putShortVolatile",
        descriptor = "(Ljava/lang/Object;JS)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetBooleanVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getBooleanVolatile",
        descriptor = "(Ljava/lang/Object;J)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutBooleanMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putBoolean",
        descriptor = "(Ljava/lang/Object;JZ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutBooleanVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putBooleanVolatile",
        descriptor = "(Ljava/lang/Object;JZ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutIntVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putIntVolatile",
        descriptor = "(Ljava/lang/Object;JI)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putInt",
        descriptor = "(Ljava/lang/Object;JI)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetInt",
        descriptor = "(Ljava/lang/Object;JII)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeInt",
        descriptor = "(Ljava/lang/Object;JII)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeLong",
        descriptor = "(Ljava/lang/Object;JJJ)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndAddLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndAddLong",
        descriptor = "(Ljava/lang/Object;JJ)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetLong",
        descriptor = "(Ljava/lang/Object;JJ)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndAddIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndAddInt",
        descriptor = "(Ljava/lang/Object;JI)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetIntMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetInt",
        descriptor = "(Ljava/lang/Object;JI)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetBooleanMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetBoolean",
        descriptor = "(Ljava/lang/Object;JZ)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetByteMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetByte",
        descriptor = "(Ljava/lang/Object;JB)B",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetShortMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetShort",
        descriptor = "(Ljava/lang/Object;JS)S",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetCharMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetChar",
        descriptor = "(Ljava/lang/Object;JC)C",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetFloatMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetFloat",
        descriptor = "(Ljava/lang/Object;JF)F",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetAndSetDoubleMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getAndSetDouble",
        descriptor = "(Ljava/lang/Object;JD)D",
        isStatic = false,
        isNative = true,
    )
    private fun unsafeCompareAndExchangeDoubleMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeDouble",
        descriptor = "(Ljava/lang/Object;JDD)D",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeBooleanMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeBoolean",
        descriptor = "(Ljava/lang/Object;JZZ)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeByteMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeByte",
        descriptor = "(Ljava/lang/Object;JBB)B",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeShortMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeShort",
        descriptor = "(Ljava/lang/Object;JSS)S",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeCharMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeChar",
        descriptor = "(Ljava/lang/Object;JCC)C",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndExchangeFloatMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeFloat",
        descriptor = "(Ljava/lang/Object;JFF)F",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetDoubleMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetDouble",
        descriptor = "(Ljava/lang/Object;JDD)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetBooleanMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetBoolean",
        descriptor = "(Ljava/lang/Object;JZZ)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetByteMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetByte",
        descriptor = "(Ljava/lang/Object;JBB)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetShortMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetShort",
        descriptor = "(Ljava/lang/Object;JSS)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetCharMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetChar",
        descriptor = "(Ljava/lang/Object;JCC)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCompareAndSetFloatMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetFloat",
        descriptor = "(Ljava/lang/Object;JFF)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafePutLongMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putLong",
        descriptor = "(Ljava/lang/Object;JJ)V",
        isStatic = false,
        isNative = true,
    )
    private fun unsafePutLongVolatileMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putLongVolatile",
        descriptor = "(Ljava/lang/Object;JJ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeArrayIndexScale0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "arrayIndexScale0",
        descriptor = "(Ljava/lang/Class;)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeArrayBaseOffset0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "arrayBaseOffset0",
        descriptor = "(Ljava/lang/Class;)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeGetLoadAverage0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getLoadAverage0",
        descriptor = "([DI)I",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeWriteback0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "writeback0",
        descriptor = "(J)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeWritebackPreSync0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "writebackPreSync0",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeWritebackPostSync0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "writebackPostSync0",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeAllocateMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "allocateMemory0",
        descriptor = "(J)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeReallocateMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "reallocateMemory0",
        descriptor = "(JJ)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeSetMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "setMemory0",
        descriptor = "(Ljava/lang/Object;JJB)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCopyMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "copyMemory0",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;JJ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeCopySwapMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "copySwapMemory0",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;JJJ)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeObjectFieldOffset1Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "objectFieldOffset1",
        descriptor = "(Ljava/lang/Class;Ljava/lang/String;)J",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeFreeMemory0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "freeMemory0",
        descriptor = "(J)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeShouldBeInitialized0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "shouldBeInitialized0",
        descriptor = "(Ljava/lang/Class;)Z",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeEnsureClassInitialized0Method(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "ensureClassInitialized0",
        descriptor = "(Ljava/lang/Class;)V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeFullFenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "fullFence",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeLoadFenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "loadFence",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )

    private fun unsafeStoreFenceMethod(): JvmResolvedMethod = JvmResolvedMethod(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "storeFence",
        descriptor = "()V",
        isStatic = false,
        isNative = true,
    )
}
