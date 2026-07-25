package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmNativeIntrinsicRegistryTest {
    @Test
    fun `intrinsic registry resolves exact VM intrinsic bindings`() {
        val signature = signature("example/NativePeer", "fastHash", "([B)I", isStatic = true)
        val binding = JvmNativeMethodBinding(
            signature = signature,
            environment = JvmNativeExecutionEnvironment.VmIntrinsic,
            bindingName = "example.NativePeer.fastHash",
        )
        val registry = JvmNativeIntrinsicRegistry.from(binding)

        assertEquals(binding, registry.resolve(signature))
        assertNull(registry.resolve(signature.copy(methodName = "missing")))
    }

    @Test
    fun `empty intrinsic registry leaves native methods unresolved`() {
        assertNull(
            JvmNativeIntrinsicRegistry.Empty.resolve(
                signature("example/NativePeer", "fastHash", "([B)I", isStatic = true),
            ),
        )
    }

    @Test
    fun `intrinsic registry rejects non intrinsic binding environments`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeIntrinsicRegistry.from(
                JvmNativeMethodBinding(
                    signature = signature("example/NativePeer", "jniOnly", "()V", isStatic = false),
                    environment = JvmNativeExecutionEnvironment.SimulatedJni,
                    bindingName = "Java_example_NativePeer_jniOnly",
                ),
            )
        }

        assertEquals("intrinsic registry accepts only VM intrinsic bindings", exception.message)
    }

    private fun signature(
        ownerClassName: String,
        methodName: String,
        methodDescriptor: String,
        isStatic: Boolean,
    ): JvmNativeMethodSignature =
        JvmNativeMethodSignature(
            ownerClassName = ownerClassName,
            methodName = methodName,
            methodDescriptor = methodDescriptor,
            isStatic = isStatic,
        )
}