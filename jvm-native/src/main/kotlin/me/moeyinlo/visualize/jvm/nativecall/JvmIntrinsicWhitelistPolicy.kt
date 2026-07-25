package me.moeyinlo.visualize.jvm.nativecall

class JvmIntrinsicWhitelistPolicy private constructor(
    private val allowedOwnerClassNames: Set<String>?,
) {
    init {
        allowedOwnerClassNames?.forEach { ownerClassName ->
            require(ownerClassName.isNotBlank()) { "intrinsic whitelist owner class name must not be blank" }
        }
    }

    fun allows(signature: JvmNativeMethodSignature): Boolean =
        allowedOwnerClassNames == null || signature.ownerClassName in allowedOwnerClassNames

    companion object {
        val Unrestricted: JvmIntrinsicWhitelistPolicy = JvmIntrinsicWhitelistPolicy(null)

        fun onlyOwners(ownerClassNames: Set<String>): JvmIntrinsicWhitelistPolicy =
            JvmIntrinsicWhitelistPolicy(ownerClassNames.toSet())
    }
}
