package me.moeyinlo.visualize.jvm.runtime

sealed interface JvmMethodCompletion {
    val method: JvmResolvedMethod

    data class Normal(
        override val method: JvmResolvedMethod,
        val returnValue: JvmValue?,
    ) : JvmMethodCompletion

    data class Abrupt(
        override val method: JvmResolvedMethod,
        val throwable: JvmObjectReferenceValue,
    ) : JvmMethodCompletion
}

fun JvmResolvedMethod.normalCompletion(returnValue: JvmValue? = null): JvmMethodCompletion.Normal {
    val returnDescriptor = descriptor.returnDescriptor()
    if (returnDescriptor == "V") {
        if (returnValue != null) {
            throw JvmMethodCompletionException(
                "Method $ownerClassName.$name$descriptor returns void but completed with ${returnValue.javaClass.simpleName}",
            )
        }
        return JvmMethodCompletion.Normal(method = this, returnValue = null)
    }

    val value = returnValue ?: throw JvmMethodCompletionException(
        "Method $ownerClassName.$name$descriptor must complete with $returnDescriptor value",
    )
    if (!value.matchesRuntimeFieldDescriptor(returnDescriptor)) {
        throw JvmMethodCompletionException(
            "Method $ownerClassName.$name$descriptor must complete with $returnDescriptor value, got " +
                value.javaClass.simpleName,
        )
    }
    return JvmMethodCompletion.Normal(method = this, returnValue = value)
}

fun JvmResolvedMethod.abruptCompletion(throwable: JvmObjectReferenceValue): JvmMethodCompletion.Abrupt =
    JvmMethodCompletion.Abrupt(method = this, throwable = throwable)

class JvmMethodCompletionException(message: String) : IllegalStateException(message)

internal fun String.returnDescriptor(): String {
    require(startsWith("(")) { "method descriptor must start with '('" }
    val end = indexOf(')')
    require(end >= 0) { "method descriptor must contain ')'" }
    require(end < lastIndex) { "method descriptor must include a return descriptor" }
    return substring(end + 1)
}

internal fun JvmValue.matchesRuntimeFieldDescriptor(descriptor: String): Boolean =
    when {
        descriptor in intLikeRuntimeFieldDescriptors -> this is JvmIntValue
        descriptor == "F" -> this is JvmFloatValue
        descriptor == "J" -> this is JvmLongValue
        descriptor == "D" -> this is JvmDoubleValue
        descriptor.startsWith("L") || descriptor.startsWith("[") -> this is JvmReferenceValue
        else -> false
    }

private val intLikeRuntimeFieldDescriptors = setOf("Z", "B", "C", "S", "I")
