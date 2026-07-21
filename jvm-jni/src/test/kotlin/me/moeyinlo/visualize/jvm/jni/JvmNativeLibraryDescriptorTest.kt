package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeLibraryDescriptorTest {
    @Test
    fun `native library descriptor exposes exports by guest method signature`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
            symbolName = "Java_pkg_NativeApi_call",
        )
        val descriptor = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("build/native/native-api.dll"),
            exports = listOf(export),
        )

        assertEquals(export, descriptor.exportFor("pkg/NativeApi", "call", "(I)J", isStatic = true))
        assertEquals(null, descriptor.exportFor("pkg/NativeApi", "call", "()V", isStatic = true))
        assertEquals("JNI_OnLoad", descriptor.onLoadSymbol)
    }

    @Test
    fun `native library descriptor rejects blank names and duplicate guest exports`() {
        val export = JvmNativeMethodExportDescriptor(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "()V",
            isStatic = false,
            symbolName = "Java_pkg_NativeApi_call",
        )

        assertFailsWith<IllegalArgumentException> {
            JvmNativeLibraryDescriptor(
                logicalName = " ",
                path = Path.of("native-api.dll"),
                exports = emptyList(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmNativeLibraryDescriptor(
                logicalName = "native-api",
                path = Path.of("native-api.dll"),
                exports = listOf(export, export.copy(symbolName = "Java_pkg_NativeApi_call__2")),
            )
        }
    }
}
