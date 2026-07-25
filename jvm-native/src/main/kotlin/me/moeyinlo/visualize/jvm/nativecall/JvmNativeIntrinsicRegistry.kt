package me.moeyinlo.visualize.jvm.nativecall

class JvmNativeIntrinsicRegistry private constructor(
    private val bindings: Map<JvmNativeMethodSignature, JvmNativeMethodBinding>,
) : JvmNativeMethodResolver {
    init {
        require(bindings.values.all { binding -> binding.environment == JvmNativeExecutionEnvironment.VmIntrinsic }) {
            "intrinsic registry accepts only VM intrinsic bindings"
        }
    }

    override fun resolve(signature: JvmNativeMethodSignature): JvmNativeMethodBinding? = bindings[signature]

    companion object {
        val Empty: JvmNativeIntrinsicRegistry = JvmNativeIntrinsicRegistry(emptyMap())

        fun from(vararg bindings: JvmNativeMethodBinding): JvmNativeIntrinsicRegistry =
            JvmNativeIntrinsicRegistry(
                bindings.associateBy(JvmNativeMethodBinding::signature),
            )
    }
}