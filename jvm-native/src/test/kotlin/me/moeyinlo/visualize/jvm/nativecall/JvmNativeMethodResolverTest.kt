package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmNativeMethodResolverTest {
    @Test
    fun `native method signatures identify one guest native declaration`() {
        val signature = JvmNativeMethodSignature(
            ownerClassName = "example/NativePeer",
            methodName = "hash",
            methodDescriptor = "([B)J",
            isStatic = true,
        )

        assertEquals("example/NativePeer", signature.ownerClassName)
        assertEquals("hash", signature.methodName)
        assertEquals("([B)J", signature.methodDescriptor)
        assertEquals(true, signature.isStatic)
    }

    @Test
    fun `native method signatures reject blank identity components`() {
        assertFailsWith<IllegalArgumentException> {
            JvmNativeMethodSignature("", "hash", "()I", isStatic = true)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmNativeMethodSignature("example/NativePeer", "", "()I", isStatic = true)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmNativeMethodSignature("example/NativePeer", "hash", "", isStatic = true)
        }
    }

    @Test
    fun `resolver contract returns opaque binding metadata or null`() {
        val signature = JvmNativeMethodSignature(
            ownerClassName = "example/NativePeer",
            methodName = "hash",
            methodDescriptor = "([B)J",
            isStatic = true,
        )
        val binding = JvmNativeMethodBinding(
            signature = signature,
            environment = JvmNativeExecutionEnvironment.SimulatedJni,
            bindingName = "Java_example_NativePeer_hash___3B",
        )
        val resolver = JvmNativeMethodResolver { candidate ->
            if (candidate == signature) binding else null
        }

        assertEquals(binding, resolver.resolve(signature))
        assertNull(
            resolver.resolve(
                signature.copy(methodName = "missing"),
            ),
        )
    }

    @Test
    fun `native method bindings validate that binding name is opaque but not blank`() {
        val signature = JvmNativeMethodSignature(
            ownerClassName = "example/NativePeer",
            methodName = "hash",
            methodDescriptor = "([B)J",
            isStatic = true,
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeMethodBinding(
                signature = signature,
                environment = JvmNativeExecutionEnvironment.SimulatedJni,
                bindingName = "",
            )
        }

        assertEquals("native binding name must not be blank", exception.message)
    }
}