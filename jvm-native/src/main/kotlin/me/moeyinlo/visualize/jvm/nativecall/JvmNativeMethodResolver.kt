package me.moeyinlo.visualize.jvm.nativecall

data class JvmNativeMethodSignature(
    val ownerClassName: String,
    val methodName: String,
    val methodDescriptor: String,
    val isStatic: Boolean,
) {
    init {
        require(ownerClassName.isNotBlank()) { "native method owner class name must not be blank" }
        require(methodName.isNotBlank()) { "native method name must not be blank" }
        require(methodDescriptor.isNotBlank()) { "native method descriptor must not be blank" }
    }
}

data class JvmNativeMethodBinding(
    val signature: JvmNativeMethodSignature,
    val environment: JvmNativeExecutionEnvironment,
    val bindingName: String,
) {
    init {
        require(bindingName.isNotBlank()) { "native binding name must not be blank" }
    }
}

fun interface JvmNativeMethodResolver {
    fun resolve(signature: JvmNativeMethodSignature): JvmNativeMethodBinding?

    fun resolveOrThrow(signature: JvmNativeMethodSignature): JvmNativeMethodBinding =
        resolve(signature) ?: throw JvmUnresolvedNativeMethodException(signature)

    companion object {
        val Empty: JvmNativeMethodResolver = JvmNativeMethodResolver { null }
    }
}

class JvmUnresolvedNativeMethodException(
    val signature: JvmNativeMethodSignature,
) : IllegalStateException(
    "Unresolved native method ${signature.ownerClassName}.${signature.methodName}:" +
        "${signature.methodDescriptor} static=${signature.isStatic}",
) {
    val guestThrowableClassName: String = "java/lang/UnsatisfiedLinkError"
}