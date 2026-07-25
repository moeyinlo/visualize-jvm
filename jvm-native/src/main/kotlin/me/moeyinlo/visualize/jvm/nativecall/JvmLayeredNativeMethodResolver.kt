package me.moeyinlo.visualize.jvm.nativecall

class JvmLayeredNativeMethodResolver(
    private val policy: JvmNativeResolutionPolicy = JvmNativeResolutionPolicy.Default,
    private val intrinsicResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
    private val simulatedJniResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
    private val hostDowncallResolver: JvmNativeMethodResolver = JvmNativeMethodResolver.Empty,
) : JvmNativeMethodResolver {
    override fun resolve(signature: JvmNativeMethodSignature): JvmNativeMethodBinding? {
        for (environment in policy.environments) {
            if (!policy.allowsEnvironment(environment, signature)) {
                continue
            }
            val binding = resolverFor(environment).resolve(signature) ?: continue
            require(binding.environment == environment) {
                "native resolver for $environment returned ${binding.environment} binding"
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