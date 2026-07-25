package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmNativeResolutionPolicyTest {
    @Test
    fun `default policy tries VM intrinsic before simulated JNI fallback`() {
        val policy = JvmNativeResolutionPolicy.Default

        assertEquals(
            listOf(
                JvmNativeExecutionEnvironment.VmIntrinsic,
                JvmNativeExecutionEnvironment.SimulatedJni,
            ),
            policy.environments,
        )
    }

    @Test
    fun `policy can be restricted to simulated JNI only`() {
        val policy = JvmNativeResolutionPolicy.SimulatedJniOnly

        assertEquals(
            listOf(JvmNativeExecutionEnvironment.SimulatedJni),
            policy.environments,
        )
    }

    @Test
    fun `policy rejects empty resolution environment order`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeResolutionPolicy(environments = emptyList())
        }

        assertEquals("native resolution policy must include at least one environment", exception.message)
    }

    @Test
    fun `policy rejects duplicate resolution environments`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeResolutionPolicy(
                environments = listOf(
                    JvmNativeExecutionEnvironment.SimulatedJni,
                    JvmNativeExecutionEnvironment.SimulatedJni,
                ),
            )
        }

        assertEquals("native resolution policy must not contain duplicate environments", exception.message)
    }
}