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
            JvmNativeIntrinsicRegistry(bindings.toIntrinsicBindingMap())
    }
}

private fun Array<out JvmNativeMethodBinding>.toIntrinsicBindingMap(): Map<JvmNativeMethodSignature, JvmNativeMethodBinding> {
    val duplicate = groupingBy(JvmNativeMethodBinding::signature)
        .eachCount()
        .entries
        .firstOrNull { (_, count) -> count > 1 }
    require(duplicate == null) {
        "duplicate VM intrinsic binding ${duplicate!!.key.formatForDiagnostic()}"
    }
    return associateBy(JvmNativeMethodBinding::signature)
}

private fun JvmNativeMethodSignature.formatForDiagnostic(): String =
    "$ownerClassName.$methodName:$methodDescriptor static=$isStatic"
