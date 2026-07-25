package me.moeyinlo.visualize.jvm.jni

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
