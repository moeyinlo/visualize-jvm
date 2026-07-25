package me.moeyinlo.visualize.jvm.nativecall

data class JvmNativeResolutionPolicy(
    val environments: List<JvmNativeExecutionEnvironment>,
) {
    init {
        require(environments.isNotEmpty()) { "native resolution policy must include at least one environment" }
        require(environments.toSet().size == environments.size) {
            "native resolution policy must not contain duplicate environments"
        }
    }

    companion object {
        val Default: JvmNativeResolutionPolicy = JvmNativeResolutionPolicy(
            environments = listOf(
                JvmNativeExecutionEnvironment.VmIntrinsic,
                JvmNativeExecutionEnvironment.SimulatedJni,
            ),
        )

        val SimulatedJniOnly: JvmNativeResolutionPolicy = JvmNativeResolutionPolicy(
            environments = listOf(JvmNativeExecutionEnvironment.SimulatedJni),
        )
    }
}