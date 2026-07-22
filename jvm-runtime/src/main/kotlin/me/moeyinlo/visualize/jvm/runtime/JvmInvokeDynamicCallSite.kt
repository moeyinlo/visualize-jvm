package me.moeyinlo.visualize.jvm.runtime

data class JvmInvokeDynamicCallSiteKey(
    val ownerClassName: String,
    val bytecodeOffset: Int,
) {
    init {
        require(ownerClassName.isNotBlank()) { "invokedynamic owner class name must not be blank" }
        require(bytecodeOffset >= 0) { "invokedynamic bytecode offset must be non-negative: $bytecodeOffset" }
    }
}

data class JvmInvokeDynamicCallSiteSpec(
    val constantPoolIndex: JvmRuntimeConstantPoolIndex,
    val bootstrapMethodIndex: Int,
    val name: String,
    val descriptor: String,
) {
    init {
        require(bootstrapMethodIndex >= 0) { "bootstrap method index must be non-negative: $bootstrapMethodIndex" }
        require(name.isNotBlank()) { "invokedynamic call site name must not be blank" }
        require(descriptor.isNotBlank()) { "invokedynamic call site descriptor must not be blank" }
    }
}

data class JvmLinkedInvokeDynamicCallSite(
    val spec: JvmInvokeDynamicCallSiteSpec,
    val targetMethod: JvmResolvedMethod,
)

class JvmInvokeDynamicCallSiteRegistry {
    private val linkedCallSites = linkedMapOf<JvmInvokeDynamicCallSiteKey, JvmLinkedInvokeDynamicCallSite>()

    fun linked(key: JvmInvokeDynamicCallSiteKey): JvmLinkedInvokeDynamicCallSite? = linkedCallSites[key]

    fun bind(
        key: JvmInvokeDynamicCallSiteKey,
        callSite: JvmLinkedInvokeDynamicCallSite,
    ): JvmLinkedInvokeDynamicCallSite {
        val existing = linkedCallSites[key]
        if (existing != null) {
            if (existing != callSite) {
                throw JvmInvokeDynamicLinkageException(
                    "invokedynamic call site ${key.ownerClassName}@${key.bytecodeOffset} is already linked",
                )
            }
            return existing
        }
        linkedCallSites[key] = callSite
        return callSite
    }
}

class JvmInvokeDynamicLinkageException(message: String) : IllegalStateException(message)