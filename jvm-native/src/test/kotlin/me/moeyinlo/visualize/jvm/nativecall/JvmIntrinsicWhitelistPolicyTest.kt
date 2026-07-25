package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JvmIntrinsicWhitelistPolicyTest {
    @Test
    fun `unrestricted intrinsic whitelist allows every native owner`() {
        val signature = signature(ownerClassName = "example/NativePeer")

        assertTrue(JvmIntrinsicWhitelistPolicy.Unrestricted.allows(signature))
    }

    @Test
    fun `explicit intrinsic whitelist allows only configured native owners`() {
        val policy = JvmIntrinsicWhitelistPolicy.onlyOwners(setOf("example/AllowedNative"))

        assertTrue(policy.allows(signature(ownerClassName = "example/AllowedNative")))
        assertFalse(policy.allows(signature(ownerClassName = "example/DeniedNative")))
    }

    @Test
    fun `explicit intrinsic whitelist rejects blank native owners`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmIntrinsicWhitelistPolicy.onlyOwners(setOf("example/AllowedNative", ""))
        }

        assertEquals("intrinsic whitelist owner class name must not be blank", exception.message)
    }

    @Test
    fun `native resolution policy skips intrinsic environment for non whitelisted owners`() {
        val policy = JvmNativeResolutionPolicy(
            environments = listOf(
                JvmNativeExecutionEnvironment.VmIntrinsic,
                JvmNativeExecutionEnvironment.SimulatedJni,
            ),
            intrinsicWhitelist = JvmIntrinsicWhitelistPolicy.onlyOwners(setOf("example/AllowedNative")),
        )
        val deniedSignature = signature(ownerClassName = "example/DeniedNative")

        assertFalse(policy.allowsEnvironment(JvmNativeExecutionEnvironment.VmIntrinsic, deniedSignature))
        assertTrue(policy.allowsEnvironment(JvmNativeExecutionEnvironment.SimulatedJni, deniedSignature))
    }

    private fun signature(ownerClassName: String): JvmNativeMethodSignature =
        JvmNativeMethodSignature(
            ownerClassName = ownerClassName,
            methodName = "nativeCall",
            methodDescriptor = "()V",
            isStatic = false,
        )
}