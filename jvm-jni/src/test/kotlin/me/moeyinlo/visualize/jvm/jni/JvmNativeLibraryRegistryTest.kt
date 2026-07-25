package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
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
        val binding = JvmNativeLibraryBinding(library, onLoadTarget = null, exportTargets = emptyMap())
        val registry = JvmNativeLibraryRegistry()
        registry.markLoaded(binding, onLoadVersion = null)

        assertFailsWith<JvmNativeLibraryLoadException> {
            registry.markLoaded(binding, onLoadVersion = null)
        }
    }
}
