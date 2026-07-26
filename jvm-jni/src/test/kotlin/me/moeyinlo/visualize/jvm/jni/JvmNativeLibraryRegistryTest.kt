package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeLibraryRegistryTest {
    @Test
    fun `loaded libraries retain binding and JNI_OnLoad version`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "()I",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(export),
        )
        val target = JvmNativeDowncallTarget(library, export.guestMethod, export.symbolName, 0x1234L)
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = null,
            exportTargets = mapOf(export.guestMethod to target),
        )
        val registry = JvmNativeLibraryRegistry()

        val loaded = registry.markLoaded(binding, onLoadVersion = JvmJniVersions.Version24)

        assertEquals(library, loaded.library)
        assertEquals(JvmJniVersions.Version24, loaded.onLoadVersion)
        assertEquals(loaded, registry.loadedLibrary("native-api"))
        assertEquals(target, registry.resolveExport(export.guestMethod))
    }

    @Test
    fun `duplicate native libraries are rejected by logical name`() {
        val library = JvmNativeLibraryDescriptor("native-api", Path.of("native-api.dll"))
        val binding = JvmNativeLibraryBinding(library, onLoadTarget = null, onUnloadTarget = null, exportTargets = emptyMap())
        val registry = JvmNativeLibraryRegistry()
        registry.markLoaded(binding, onLoadVersion = null)

        assertFailsWith<JvmNativeLibraryLoadException> {
            registry.markLoaded(binding, onLoadVersion = null)
        }
    }
    @Test
    fun `unloading native library removes exports and allows reload`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "()I",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(export),
        )
        val target = JvmNativeDowncallTarget(library, export.guestMethod, export.symbolName, 0x1234L)
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = null,
            exportTargets = mapOf(export.guestMethod to target),
        )
        val registry = JvmNativeLibraryRegistry()
        val loaded = registry.markLoaded(binding, onLoadVersion = JvmJniVersions.Version24)

        assertEquals(loaded, registry.markUnloaded("native-api"))

        assertEquals(null, registry.loadedLibrary("native-api"))
        assertEquals(null, registry.resolveExport(export.guestMethod))
        assertEquals(loaded, registry.markLoaded(binding, onLoadVersion = JvmJniVersions.Version24))
    }
    @Test
    fun `loaded library prepares JNI_OnUnload invocation when finalizer is present`() {
        val library = JvmNativeLibraryDescriptor("native-api", Path.of("native-api.dll"))
        val onUnload = JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = "JNI_OnUnload",
            address = 0x5678L,
        )
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = onUnload,
            exportTargets = emptyMap(),
        )
        val registry = JvmNativeLibraryRegistry()
        registry.markLoaded(binding, onLoadVersion = JvmJniVersions.Version24)
        val environment = JvmSimulatedJniEnvironment(
            classHierarchy = JvmClassHierarchy(),
            staticFields = JvmStaticFields(),
        )
        val javaVm = JvmSimulatedJavaVm(environment)

        val invocation = registry.prepareOnUnloadInvocation("native-api", javaVm)

        assertEquals(onUnload, invocation?.target)
        assertEquals(
            listOf(
                JvmNativeDowncallArgument.SimulatedJavaVm(javaVm),
                JvmNativeDowncallArgument.ReservedNull,
            ),
            invocation?.arguments,
        )
    }
    @Test
    fun `unload returns loaded library and prepared finalizer while removing exports`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "()I",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(export),
        )
        val onUnload = JvmNativeDowncallTarget(
            library = library,
            guestMethod = null,
            symbolName = "JNI_OnUnload",
            address = 0x5678L,
        )
        val binding = JvmNativeLibraryBinding(
            library = library,
            onLoadTarget = null,
            onUnloadTarget = onUnload,
            exportTargets = mapOf(
                export.guestMethod to JvmNativeDowncallTarget(library, export.guestMethod, export.symbolName, 0x1234L),
            ),
        )
        val registry = JvmNativeLibraryRegistry()
        val loaded = registry.markLoaded(binding, onLoadVersion = JvmJniVersions.Version24)
        val javaVm = JvmSimulatedJavaVm(
            JvmSimulatedJniEnvironment(
                classHierarchy = JvmClassHierarchy(),
                staticFields = JvmStaticFields(),
            ),
        )

        val request = registry.unload("native-api", javaVm)

        assertEquals(loaded, request?.loadedLibrary)
        assertEquals(onUnload, request?.onUnloadInvocation?.target)
        assertEquals(null, registry.loadedLibrary("native-api"))
        assertEquals(null, registry.resolveExport(export.guestMethod))
    }
}
