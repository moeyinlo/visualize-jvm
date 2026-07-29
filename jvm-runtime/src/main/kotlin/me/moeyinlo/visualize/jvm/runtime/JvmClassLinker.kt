package me.moeyinlo.visualize.jvm.runtime

fun interface JvmLinkingVerifier {
    fun verify(definition: JvmClassDefinition)

    companion object {
        val NoOp: JvmLinkingVerifier = JvmLinkingVerifier { }
    }
}

class JvmClassLinker(
    private val verifier: JvmLinkingVerifier,
) {
    private val linkedClassesByName = linkedMapOf<String, JvmLinkedClass>()

    fun link(definition: JvmClassDefinition): JvmLinkedClass {
        val className = definition.internalName
        require(className.isNotBlank()) { "class internal name must not be blank" }
        if (className in linkedClassesByName) {
            throw JvmClassLinkageException("Class $className is already linked")
        }

        try {
            verifier.verify(definition)
        } catch (exception: Throwable) {
            throw JvmClassVerificationException(
                className = className,
                cause = exception,
            )
        }

        val linkedClass = JvmLinkedClass(
            definition = definition,
            state = JvmClassLinkState.Verified,
        )
        linkedClassesByName[className] = linkedClass
        return linkedClass
    }

    fun linkedClass(className: String): JvmLinkedClass? {
        require(className.isNotBlank()) { "class internal name must not be blank" }
        return linkedClassesByName[className]
    }
}

data class JvmLinkedClass(
    val definition: JvmClassDefinition,
    val state: JvmClassLinkState,
)

enum class JvmClassLinkState {
    Verified,
}

class JvmClassLinkageException(message: String) : IllegalStateException(message)

class JvmClassVerificationException(
    val className: String,
    override val cause: Throwable,
) : IllegalStateException("Verification failed for $className: ${cause.message}", cause) {
    val guestThrowableClassName: String = "java/lang/VerifyError"
}
