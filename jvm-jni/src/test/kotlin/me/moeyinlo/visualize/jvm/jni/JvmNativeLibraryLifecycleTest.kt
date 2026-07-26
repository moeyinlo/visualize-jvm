package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import me.moeyinlo.visualize.jvm.runtime.JvmClassDefinition
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeLibraryLifecycleTest {
    @Test
    fun `load invokes JNI_OnLoad and registers accepted version`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val backend = JvmPanamaDowncallBackend { path, symbolName ->
            if (path == library.path && symbolName == "JNI_OnLoad") {
                JvmNativeSymbolAddress(symbolName, 0x1111L)
            } else {
                null
            }
        }
        val registry = JvmNativeLibraryRegistry()
        val invocations = mutableListOf<JvmNativeDowncallInvocation>()
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = backend,
            registry = registry,
            invokeDowncall = { invocation ->
                invocations += invocation
                JvmNativeDowncallReturn.IntPrimitive(JvmJniVersions.Version24)
            },
        )
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )

        val loaded = lifecycle.load(library, javaVm)

        assertEquals(JvmJniVersions.Version24, loaded.onLoadVersion)
        assertEquals(loaded, registry.loadedLibrary("native-api"))
        assertEquals(1, invocations.size)
        assertEquals("JNI_OnLoad", invocations.single().target.symbolName)
        assertEquals(
            listOf(
                JvmNativeDowncallArgument.SimulatedJavaVm(javaVm),
                JvmNativeDowncallArgument.ReservedNull,
            ),
            invocations.single().arguments,
        )
    }
    @Test
    fun `load invokes JNI_OnLoad inside an automatic JNI local frame`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val backend = JvmPanamaDowncallBackend { path, symbolName ->
            if (path == library.path && symbolName == "JNI_OnLoad") {
                JvmNativeSymbolAddress(symbolName, 0x1111L)
            } else {
                null
            }
        }
        val registry = JvmNativeLibraryRegistry()
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        var frameDepthDuringOnLoad = -1
        var liveHandlesDuringOnLoad = -1
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = backend,
            registry = registry,
            invokeDowncall = { invocation ->
                val downcallJavaVm = (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJavaVm).javaVm
                val downcallEnvironment = downcallJavaVm.getEnv(JvmJniVersions.Version24).environment!!
                frameDepthDuringOnLoad = downcallEnvironment.localFrameDepth
                downcallEnvironment.handles.newObjectHandle(heap.allocateObject("java/lang/Object"))
                liveHandlesDuringOnLoad = downcallEnvironment.handles.liveHandleCount
                JvmNativeDowncallReturn.IntPrimitive(JvmJniVersions.Version24)
            },
        )

        lifecycle.load(library, javaVm)

        assertEquals(1, frameDepthDuringOnLoad)
        assertEquals(1, liveHandlesDuringOnLoad)
        assertEquals(0, environment.localFrameDepth)
        assertEquals(0, environment.handles.localFrameDepth)
        assertEquals(0, environment.handles.liveHandleCount)
    }

    @Test
    fun `load propagates pending JNI_OnLoad guest exceptions without registering library`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val backend = JvmPanamaDowncallBackend { path, symbolName ->
            if (path == library.path && symbolName == "JNI_OnLoad") {
                JvmNativeSymbolAddress(symbolName, 0x1111L)
            } else {
                null
            }
        }
        val registry = JvmNativeLibraryRegistry()
        val heap = JvmHeap()
        val classHierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "java/lang/Throwable"),
                JvmClassDefinition(internalName = "java/lang/IllegalStateException", superclassName = "java/lang/Throwable"),
            ),
        )
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = classHierarchy,
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val throwable = heap.allocateObject("java/lang/IllegalStateException")
        val javaVm = JvmSimulatedJavaVm(environment)
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = backend,
            registry = registry,
            invokeDowncall = { invocation ->
                val downcallJavaVm = (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJavaVm).javaVm
                val downcallEnvironment = downcallJavaVm.getEnv(JvmJniVersions.Version24).environment!!
                downcallEnvironment.throwObject(downcallEnvironment.handles.newObjectHandle(throwable))
                JvmNativeDowncallReturn.IntPrimitive(JvmJniVersions.Version24)
            },
        )

        val thrown = assertFailsWith<JvmNativeGuestException> {
            lifecycle.load(library, javaVm)
        }

        assertEquals(throwable, thrown.throwable)
        assertEquals(null, registry.loadedLibrary("native-api"))
        assertEquals(false, environment.exceptionCheck())
        assertEquals(0, environment.localFrameDepth)
        assertEquals(0, environment.handles.localFrameDepth)
        assertEquals(0, environment.handles.liveHandleCount)
    }

    @Test
    fun `unload invokes JNI_OnUnload and returns unloaded library`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val onUnload = JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = "JNI_OnUnload",
            address = 0x2222L,
        )
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = onUnload,
            exportTargets = emptyMap(),
        )
        val registry = JvmNativeLibraryRegistry()
        val loaded = registry.markLoaded(binding, onLoadVersion = null)
        val invocations = mutableListOf<JvmNativeDowncallInvocation>()
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = JvmPanamaDowncallBackend { _, _ -> null },
            registry = registry,
            invokeDowncall = { invocation ->
                invocations += invocation
                JvmNativeDowncallReturn.Void
            },
        )
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )

        val unloaded = lifecycle.unload("native-api", javaVm)

        assertEquals(loaded, unloaded)
        assertEquals(null, registry.loadedLibrary("native-api"))
        assertEquals(1, invocations.size)
        assertEquals(onUnload, invocations.single().target)
        assertEquals(
            listOf(
                JvmNativeDowncallArgument.SimulatedJavaVm(javaVm),
                JvmNativeDowncallArgument.ReservedNull,
            ),
            invocations.single().arguments,
        )
    }
    @Test
    fun `unload invokes JNI_OnUnload inside an automatic JNI local frame`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val onUnload = JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = "JNI_OnUnload",
            address = 0x2222L,
        )
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = onUnload,
            exportTargets = emptyMap(),
        )
        val registry = JvmNativeLibraryRegistry()
        registry.markLoaded(binding, onLoadVersion = null)
        val heap = JvmHeap()
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            heap = heap,
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)
        var frameDepthDuringOnUnload = -1
        var liveHandlesDuringOnUnload = -1
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = JvmPanamaDowncallBackend { _, _ -> null },
            registry = registry,
            invokeDowncall = { invocation ->
                val downcallJavaVm = (invocation.arguments[0] as JvmNativeDowncallArgument.SimulatedJavaVm).javaVm
                val downcallEnvironment = downcallJavaVm.getEnv(JvmJniVersions.Version24).environment!!
                frameDepthDuringOnUnload = downcallEnvironment.localFrameDepth
                downcallEnvironment.handles.newObjectHandle(heap.allocateObject("java/lang/Object"))
                liveHandlesDuringOnUnload = downcallEnvironment.handles.liveHandleCount
                JvmNativeDowncallReturn.Void
            },
        )

        lifecycle.unload("native-api", javaVm)

        assertEquals(1, frameDepthDuringOnUnload)
        assertEquals(1, liveHandlesDuringOnUnload)
        assertEquals(0, environment.localFrameDepth)
        assertEquals(0, environment.handles.localFrameDepth)
        assertEquals(0, environment.handles.liveHandleCount)
    }

    @Test
    fun `duplicate load is rejected before invoking JNI_OnLoad again`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val backend = JvmPanamaDowncallBackend { path, symbolName ->
            if (path == library.path && symbolName == "JNI_OnLoad") {
                JvmNativeSymbolAddress(symbolName, 0x1111L)
            } else {
                null
            }
        }
        val registry = JvmNativeLibraryRegistry()
        val invocations = mutableListOf<JvmNativeDowncallInvocation>()
        val lifecycle = JvmNativeLibraryLifecycle(
            backend = backend,
            registry = registry,
            invokeDowncall = { invocation ->
                invocations += invocation
                JvmNativeDowncallReturn.IntPrimitive(JvmJniVersions.Version24)
            },
        )
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )
        lifecycle.load(library, javaVm)

        val exception = assertFailsWith<JvmNativeLibraryLoadException> {
            lifecycle.load(library, javaVm)
        }

        assertEquals("native library native-api is already loaded", exception.message)
        assertEquals(1, invocations.size)
    }
}
