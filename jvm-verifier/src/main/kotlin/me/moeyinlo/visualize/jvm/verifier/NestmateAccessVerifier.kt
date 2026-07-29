package me.moeyinlo.visualize.jvm.verifier

object NestmateAccessVerifier {
    fun verify(access: NestmateAccess) {
        if (access.currentClass.runtimeNestHostKey == access.targetClass.runtimeNestHostKey) {
            return
        }
        throw MethodVerificationException(
            "Nestmate access to ${access.targetClass.internalName}.${access.memberName}:${access.descriptor} " +
                "from ${access.currentClass.internalName} requires the same runtime nest host",
        )
    }
}

data class NestmateAccess(
    val currentClass: NestmateVerifierClass,
    val targetClass: NestmateVerifierClass,
    val memberName: String,
    val descriptor: String,
) {
    init {
        require(memberName.isNotBlank()) { "member name must not be blank" }
        require(descriptor.isNotBlank()) { "member descriptor must not be blank" }
    }
}

data class NestmateVerifierClass(
    val internalName: String,
    val definingLoader: String = "bootstrap",
    val nestHostInternalName: String = internalName,
) {
    init {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
        require(definingLoader.isNotBlank()) { "defining loader must not be blank" }
        require(nestHostInternalName.isNotBlank()) { "nest host internal name must not be blank" }
    }

    val runtimeNestHostKey: NestmateClassKey = NestmateClassKey(
        internalName = nestHostInternalName,
        loader = definingLoader,
    )
}

data class NestmateClassKey(
    val internalName: String,
    val loader: String = "bootstrap",
)
