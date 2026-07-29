package me.moeyinlo.visualize.jvm.runtime

data class JvmStartupRequest(
    val mainClassName: String,
    val arguments: List<String> = emptyList(),
) {
    init {
        require(mainClassName.isNotBlank()) { "startup main class name must not be blank" }
    }
}

data class JvmStartupEntryPoint(
    val className: String,
    val method: JvmMethodDefinition,
    val arguments: List<String>,
)

object JvmStartupResolver {
    private const val MainMethodName = "main"
    private const val MainMethodDescriptor = "([Ljava/lang/String;)V"

    fun resolveMainMethod(
        methodArea: JvmMethodArea,
        request: JvmStartupRequest,
    ): JvmStartupEntryPoint {
        val entry = try {
            methodArea.getClass(request.mainClassName)
        } catch (exception: JvmMethodAreaAccessException) {
            throw JvmStartupException(
                className = request.mainClassName,
                guestThrowableClassName = "java/lang/NoClassDefFoundError",
                message = request.mainClassName,
                cause = exception,
            )
        }
        val method = entry.definition.methods.firstOrNull { method -> method.isJvmStartupMainMethod() }
            ?: throw JvmStartupException(
                className = request.mainClassName,
                guestThrowableClassName = "java/lang/NoSuchMethodError",
                message = "${request.mainClassName}.$MainMethodName:$MainMethodDescriptor",
            )
        return JvmStartupEntryPoint(
            className = request.mainClassName,
            method = method,
            arguments = request.arguments.toList(),
        )
    }

    private fun JvmMethodDefinition.isJvmStartupMainMethod(): Boolean =
        name == MainMethodName &&
            descriptor == MainMethodDescriptor &&
            isStatic &&
            !isPrivate &&
            !isProtected &&
            !isPackagePrivate
}

class JvmStartupException(
    val className: String,
    val guestThrowableClassName: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
