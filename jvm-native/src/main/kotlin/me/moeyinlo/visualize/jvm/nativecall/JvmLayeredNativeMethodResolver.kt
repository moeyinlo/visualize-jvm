package me.moeyinlo.visualize.jvm.nativecall

class JvmLayeredNativeMethodResolver(
    private val policy: JvmNativeResolutionPolicy = JvmNativeResolutionPolicy.Default,
    private val intrinsicResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
    private val simulatedJniResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
    private val hostDowncallResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
    private val nativeCallEvents: JvmNativeCallEventSink = JvmNativeCallEventSink.None,
) : JvmNativeMethodResolver {
    override fun resolve(signature: JvmNativeMethodSignature): JvmNativeMethodBinding? {
        var intrinsicMissed = false
        for (environment in policy.environments) {
            if (!policy.allowsEnvironment(environment, signature)) {
                continue
            }
            val binding = resolverFor(environment).resolve(signature)
            if (binding == null) {
                if (environment == JvmNativeExecutionEnvironment.VmIntrinsic) {
                    intrinsicMissed = true
                }
                continue
            }
            require(binding.environment == environment) {
                "native resolver for $environment returned ${binding.environment} binding"
            }
            if (environment == JvmNativeExecutionEnvironment.SimulatedJni && intrinsicMissed) {
                nativeCallEvents.record(
                    action = JvmNativeCallAction.FellBackToSimulatedJni,
                    depth = 0,
                    frame = JvmNativeMethodFrame.fromBinding(binding),
                    detail = "intrinsic miss",
                )
            }
            return binding
        }
        return null
    }

    private fun resolverFor(environment: JvmNativeExecutionEnvironment): JvmNativeMethodResolver =
        when (environment) {
            JvmNativeExecutionEnvironment.VmIntrinsic -> intrinsicResolver
            JvmNativeExecutionEnvironment.SimulatedJni -> simulatedJniResolver
            JvmNativeExecutionEnvironment.HostDowncall -> hostDowncallResolver
        }
}