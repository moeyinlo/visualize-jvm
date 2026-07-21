package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmPanamaDowncallBackendTest {
    @Test
    fun `Panama backend resolves descriptor exports through injected symbol lookup`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(
                JvmNativeMethodExportDescriptor(
                    ownerClassName = "pkg/NativeApi",
                    methodName = "call",
                    methodDescriptor = "(I)J",
                    isStatic = true,
                    symbolName = "Java_pkg_NativeApi_call",
                ),
            ),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                if (path == library.path && symbolName == "Java_pkg_NativeApi_call") {
                    JvmNativeSymbolAddress(symbolName, 0x1234L)
                } else {
                    null
                }
            },
        )

        val target = backend.resolveExport(library, library.exports.single())

        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = library.exports.single().guestMethod,
                symbolName = "Java_pkg_NativeApi_call",
                address = 0x1234L,
            ),
            target,
        )
    }

    @Test
    fun `Panama backend reports unresolved native symbols`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = emptyList(),
        )
        val backend = JvmPanamaDowncallBackend(symbolLookup = JvmNativeSymbolLookup { _, _ -> null })

        assertFailsWith<JvmNativeSymbolResolutionException> {
            backend.resolveSymbol(library, "Java_pkg_NativeApi_missing")
        }
    }

    @Test
    fun `Panama backend binds optional JNI_OnLoad symbols`() {
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = emptyList(),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                if (path == library.path && symbolName == "JNI_OnLoad") {
                    JvmNativeSymbolAddress(symbolName, 0x4567L)
                } else {
                    null
                }
            },
        )
        val missingOnLoadBackend = JvmPanamaDowncallBackend(symbolLookup = JvmNativeSymbolLookup { _, _ -> null })

        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = null,
                symbolName = "JNI_OnLoad",
                address = 0x4567L,
            ),
            backend.bindOnLoad(library),
        )
        assertEquals(null, missingOnLoadBackend.bindOnLoad(library))
    }

    @Test
    fun `Panama backend binds Java native exports by guest signature`() {
        val staticExport = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call__I",
        )
        val instanceExport = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "poke",
            methodDescriptor = "()V",
            isStatic = false,
            symbolName = "Java_pkg_NativeApi_poke",
        )
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
            exports = listOf(staticExport, instanceExport),
        )
        val backend = JvmPanamaDowncallBackend(
            symbolLookup = JvmNativeSymbolLookup { path, symbolName ->
                when {
                    path == library.path && symbolName == staticExport.symbolName ->
                        JvmNativeSymbolAddress(symbolName, 0x1111L)
                    path == library.path && symbolName == instanceExport.symbolName ->
                        JvmNativeSymbolAddress(symbolName, 0x2222L)
                    else -> null
                }
            },
        )

        val targets = backend.bindExports(library)

        assertEquals(
            mapOf(
                staticExport.guestMethod to JvmNativeDowncallTarget(
                    library = library,
                    guestMethod = staticExport.guestMethod,
                    symbolName = staticExport.symbolName,
                    address = 0x1111L,
                ),
                instanceExport.guestMethod to JvmNativeDowncallTarget(
                    library = library,
                    guestMethod = instanceExport.guestMethod,
                    symbolName = instanceExport.symbolName,
                    address = 0x2222L,
                ),
            ),
            targets,
        )
    }
}
