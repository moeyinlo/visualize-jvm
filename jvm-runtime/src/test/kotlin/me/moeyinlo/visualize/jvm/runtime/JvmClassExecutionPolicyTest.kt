package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmClassExecutionPolicyTest {
    @Test
    fun `default policy interprets application classes and host delegates platform classes`() {
        val policy = JvmClassExecutionPolicy.Default

        assertEquals(JvmClassExecutionMode.Interpreted, policy.modeFor("example/Main"))
        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("java/lang/String"))
        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("jdk/internal/misc/Unsafe"))
    }

    @Test
    fun `policy host delegates explicitly whitelisted classes`() {
        val policy = JvmClassExecutionPolicy(
            hostDelegatedClassNames = setOf("example/FastNativePeer"),
        )

        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("example/FastNativePeer"))
        assertEquals(JvmClassExecutionMode.Interpreted, policy.modeFor("example/SlowGuest"))
    }

    @Test
    fun `explicit interpreted classes override host delegated package prefixes`() {
        val policy = JvmClassExecutionPolicy(
            hostDelegatedPackagePrefixes = setOf("example/stdlib/"),
            interpretedClassNames = setOf("example/stdlib/Instrumented"),
        )

        assertEquals(JvmClassExecutionMode.Interpreted, policy.modeFor("example/stdlib/Instrumented"))
        assertEquals(JvmClassExecutionMode.HostDelegated, policy.modeFor("example/stdlib/FastPeer"))
    }
}
