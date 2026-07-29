package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassLoaderIdentity
import me.moeyinlo.visualize.jvm.runtime.JvmLoadedClassKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmJniNativeMethodRegistryTest {
    @Test
    fun `registered native methods are keyed by class name and JNI signature`() {
        val registry = JvmJniNativeMethodRegistry()
        val descriptor = JvmJniNativeMethodDescriptor(
            name = "a",
            descriptor = "(I)V",
            functionAddress = 0x1234L,
        )

        assertEquals(0, registry.register(className = "pkg/NativeApi", methods = listOf(descriptor)))

        assertEquals(
            JvmJniRegisteredNativeMethod(
                className = "pkg/NativeApi",
                name = "a",
                descriptor = "(I)V",
                functionAddress = 0x1234L,
            ),
            registry.resolve(className = "pkg/NativeApi", name = "a", descriptor = "(I)V"),
        )
        assertEquals(null, registry.resolve(className = "pkg/NativeApi", name = "a", descriptor = "()V"))
    }

    @Test
    fun `registered native methods distinguish same class name by loaded class key`() {
        val registry = JvmJniNativeMethodRegistry()
        val firstKey = JvmLoadedClassKey(
            internalName = "pkg/NativeApi",
            definingLoader = JvmClassLoaderIdentity.UserDefined(1L, "first-loader"),
        )
        val secondKey = JvmLoadedClassKey(
            internalName = "pkg/NativeApi",
            definingLoader = JvmClassLoaderIdentity.UserDefined(2L, "second-loader"),
        )

        registry.register(
            className = "pkg/NativeApi",
            loadedClassKey = firstKey,
            methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x1111L)),
        )
        registry.register(
            className = "pkg/NativeApi",
            loadedClassKey = secondKey,
            methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x2222L)),
        )

        assertEquals(
            JvmJniRegisteredNativeMethod(
                className = "pkg/NativeApi",
                name = "a",
                descriptor = "()V",
                functionAddress = 0x1111L,
                loadedClassKey = firstKey,
            ),
            registry.resolve(
                className = "pkg/NativeApi",
                loadedClassKey = firstKey,
                name = "a",
                descriptor = "()V",
            ),
        )
        assertEquals(
            JvmJniRegisteredNativeMethod(
                className = "pkg/NativeApi",
                name = "a",
                descriptor = "()V",
                functionAddress = 0x2222L,
                loadedClassKey = secondKey,
            ),
            registry.resolve(
                className = "pkg/NativeApi",
                loadedClassKey = secondKey,
                name = "a",
                descriptor = "()V",
            ),
        )
        assertEquals(null, registry.resolve(className = "pkg/NativeApi", name = "a", descriptor = "()V"))
    }

    @Test
    fun `unregister removes only one class registered native methods`() {
        val registry = JvmJniNativeMethodRegistry()
        registry.register(
            className = "pkg/First",
            methods = listOf(JvmJniNativeMethodDescriptor("a", "()V", 0x1111L)),
        )
        registry.register(
            className = "pkg/Second",
            methods = listOf(JvmJniNativeMethodDescriptor("b", "()V", 0x2222L)),
        )

        assertEquals(0, registry.unregister(className = "pkg/First"))

        assertEquals(null, registry.resolve(className = "pkg/First", name = "a", descriptor = "()V"))
        assertEquals(
            JvmJniRegisteredNativeMethod("pkg/Second", "b", "()V", 0x2222L),
            registry.resolve(className = "pkg/Second", name = "b", descriptor = "()V"),
        )
    }

    @Test
    fun `registered natives can be resolved as downcall targets`() {
        val registry = JvmJniNativeMethodRegistry()
        val library = JvmNativeLibraryDescriptor(
            logicalName = "native-api",
            path = java.nio.file.Path.of("native-api.dll"),
        )
        registry.register(
            className = "pkg/NativeApi",
            methods = listOf(JvmJniNativeMethodDescriptor("a", "()I", 0x1234L)),
        )

        assertEquals(
            JvmNativeDowncallTarget(
                library = library,
                guestMethod = JvmNativeGuestMethodSignature(
                    ownerClassName = "pkg/NativeApi",
                    methodName = "a",
                    methodDescriptor = "()I",
                    isStatic = false,
                ),
                symbolName = "RegisterNatives:pkg/NativeApi.a:()I",
                address = 0x1234L,
            ),
            registry.resolveDowncallTarget(
                library = library,
                className = "pkg/NativeApi",
                name = "a",
                descriptor = "()I",
                isStatic = false,
            ),
        )
    }
    @Test
    fun `duplicate native registrations for one class are rejected`() {
        val registry = JvmJniNativeMethodRegistry()

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                className = "pkg/NativeApi",
                methods = listOf(
                    JvmJniNativeMethodDescriptor("a", "()V", 0x1111L),
                    JvmJniNativeMethodDescriptor("a", "()V", 0x2222L),
                ),
            )
        }
    }

    @Test
    fun `native method descriptors reject null-equivalent function pointers`() {
        assertFailsWith<IllegalArgumentException> {
            JvmJniNativeMethodDescriptor("a", "()V", 0L)
        }
    }
}
