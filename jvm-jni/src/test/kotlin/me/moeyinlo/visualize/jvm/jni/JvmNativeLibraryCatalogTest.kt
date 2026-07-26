package me.moeyinlo.visualize.jvm.jni

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeLibraryCatalogTest {
    @Test
    fun `catalog resolves descriptors by logical library name`() {
        val descriptor = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("native-api.dll"),
        )
        val catalog = JvmNativeLibraryCatalog(listOf(descriptor))

        assertEquals(descriptor, catalog.resolve("native-api"))
        assertEquals(null, catalog.resolve("missing"))
    }

    @Test
    fun `catalog rejects duplicate logical library names`() {
        val first = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("first.dll"),
        )
        val second = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = Path.of("second.dll"),
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeLibraryCatalog(listOf(first, second))
        }

        assertEquals("duplicate native library descriptor for native-api", exception.message)
    }
    @Test
    fun `resolveOrThrow reports missing descriptors as native load errors`() {
        val catalog = JvmNativeLibraryCatalog(emptyList())

        val exception = assertFailsWith<JvmNativeLibraryLoadException> {
            catalog.resolveOrThrow("native-api")
        }

        assertEquals("native library descriptor native-api is not configured", exception.message)
    }
}