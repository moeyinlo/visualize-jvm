package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmLayeredNativeMethodResolverTest {
    @Test
    fun `layered resolver returns whitelisted VM intrinsic before simulated JNI fallback`() {
        val signature = signature(ownerClassName = "example/AllowedNative")
        val intrinsicBinding = binding(
            signature = signature,
            environment = JvmNativeExecutionEnvironment.VmIntrinsic,
            bindingName = "intrinsic.example.AllowedNative.call",
        )
        val resolver = JvmLayeredNativeMethodResolver(
            policy = JvmNativeResolutionPolicy(
                environments = listOf(
                    JvmNativeExecutionEnvironment.VmIntrinsic,
                    JvmNativeExecutionEnvironment.SimulatedJni,
                ),
                intrinsicWhitelist = JvmIntrinsicWhitelistPolicy.onlyOwners(setOf("example/AllowedNative")),
            ),
            intrinsicResolver = JvmNativeIntrinsicRegistry.from(intrinsicBinding),
            simulatedJniResolver = JvmNativeMethodResolver {
                error("simulated JNI fallback must not be queried when intrinsic hits first")
            },
        )

        assertEquals(intrinsicBinding, resolver.resolve(signature))
    }


    @Test
    fun `layered resolver falls back to simulated JNI when whitelisted intrinsic misses`() {
        val signature = signature(ownerClassName = "example/AllowedNative")
        val simulatedBinding = binding(
            signature = signature,
            environment = JvmNativeExecutionEnvironment.SimulatedJni,
            bindingName = "Java_example_AllowedNative_call",
        )
        val events = JvmNativeCallEventRecorder()
        val resolver = JvmLayeredNativeMethodResolver(
            policy = JvmNativeResolutionPolicy(
                environments = listOf(
                    JvmNativeExecutionEnvironment.VmIntrinsic,
                    JvmNativeExecutionEnvironment.SimulatedJni,
                ),
                intrinsicWhitelist = JvmIntrinsicWhitelistPolicy.onlyOwners(setOf("example/AllowedNative")),
            ),
            intrinsicResolver = JvmNativeIntrinsicRegistry.Empty,
            simulatedJniResolver = JvmNativeMethodResolver { candidate ->
                if (candidate == signature) simulatedBinding else null
            },
            nativeCallEvents = events,
        )

        assertEquals(simulatedBinding, resolver.resolve(signature))
        assertEquals(
            listOf(
                JvmNativeCallEventSnapshot(
                    sequence = 1,
                    depth = 0,
                    action = JvmNativeCallAction.FellBackToSimulatedJni,
                    signature = signature,
                    environment = JvmNativeExecutionEnvironment.SimulatedJni,
                    bindingName = "Java_example_AllowedNative_call",
                    detail = "intrinsic miss",
                ),
            ),
            events.snapshots(),
        )
    }

    private fun signature(ownerClassName: String): JvmNativeMethodSignature =
        JvmNativeMethodSignature(
            ownerClassName = ownerClassName,
            methodName = "call",
            methodDescriptor = "()V",
            isStatic = true,
        )

    private fun binding(
        signature: JvmNativeMethodSignature,
        environment: JvmNativeExecutionEnvironment,
        bindingName: String,
    ): JvmNativeMethodBinding =
        JvmNativeMethodBinding(
            signature = signature,
            environment = environment,
            bindingName = bindingName,
        )
}