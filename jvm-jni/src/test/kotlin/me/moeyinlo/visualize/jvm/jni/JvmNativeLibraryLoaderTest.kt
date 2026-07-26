package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmNativeLibraryLoaderTest {
    @Test
    fun `loadLibrary resolves logical names through catalog before lifecycle loading`() {
        val descriptor = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val backend = JvmPanamaDowncallBackend { path, symbolName ->
            if (path == descriptor.path && symbolName == "JNI_OnLoad") {
                JvmNativeSymbolAddress(symbolName, 0x1111L)
            } else {
                null
            }
        }
        val registry = JvmNativeLibraryRegistry()
        val invocations = mutableListOf<JvmNativeDowncallInvocation>()
        val loader = JvmNativeLibraryLoader(
            catalog = JvmNativeLibraryCatalog(listOf(descriptor)),
            lifecycle = JvmNativeLibraryLifecycle(
                backend = backend,
                registry = registry,
                invokeDowncall = { invocation ->
                    invocations += invocation
                    JvmNativeDowncallReturn.IntPrimitive(JvmJniVersions.Version24)
                },
            ),
        )
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )

        val loaded = loader.loadLibrary("native-api", javaVm)

        assertEquals(descriptor, loaded.library)
        assertEquals(JvmJniVersions.Version24, loaded.onLoadVersion)
        assertEquals(loaded, registry.loadedLibrary("native-api"))
        assertEquals("JNI_OnLoad", invocations.single().target.symbolName)
    }
    @Test
    fun `unloadLibrary delegates logical unload through lifecycle`() {
        val descriptor = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val onUnload = JvmNativeDowncallTarget(
            library = descriptor,
            guestMethod = null,
            symbolName = "JNI_OnUnload",
            address = 0x2222L,
        )
        val registry = JvmNativeLibraryRegistry()
        val invocations = mutableListOf<JvmNativeDowncallInvocation>()
        val loader = JvmNativeLibraryLoader(
            catalog = JvmNativeLibraryCatalog(listOf(descriptor)),
            lifecycle = JvmNativeLibraryLifecycle(
                backend = JvmPanamaDowncallBackend { _, _ -> null },
                registry = registry,
                invokeDowncall = { invocation ->
                    invocations += invocation
                    JvmNativeDowncallReturn.Void
                },
            ),
        )
        val binding = JvmNativeLibraryBinding(
            library = descriptor,
            onLoadTarget = null,
            onUnloadTarget = onUnload,
            exportTargets = emptyMap(),
        )
        val loaded = registry.markLoaded(binding, onLoadVersion = null)
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )

        val unloaded = loader.unloadLibrary("native-api", javaVm)

        assertEquals(loaded, unloaded)
        assertEquals(null, registry.loadedLibrary("native-api"))
        assertEquals(onUnload, invocations.single().target)
    }
}