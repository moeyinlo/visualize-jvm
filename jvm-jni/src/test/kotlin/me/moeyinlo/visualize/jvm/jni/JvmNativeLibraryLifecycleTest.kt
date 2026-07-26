package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals

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
}