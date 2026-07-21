package me.moeyinlo.visualize.jvm.jni

object JvmNativeSymbolNameResolver {
    fun candidates(signature: JvmNativeGuestMethodSignature): List<String> =
        listOf(shortName(signature), longName(signature))

    fun shortName(signature: JvmNativeGuestMethodSignature): String =
        "Java_${mangle(signature.ownerClassName)}_${mangle(signature.methodName)}"

    fun longName(signature: JvmNativeGuestMethodSignature): String =
        shortName(signature) + "__" + mangle(parameterDescriptor(signature.methodDescriptor))

    private fun parameterDescriptor(methodDescriptor: String): String {
        require(methodDescriptor.startsWith("(")) { "JNI method descriptor must start with '('" }
        val end = methodDescriptor.indexOf(')')
        require(end >= 0) { "JNI method descriptor must contain ')'" }
        require(end < methodDescriptor.lastIndex) { "JNI method descriptor must include a return descriptor" }
        return methodDescriptor.substring(1, end)
    }

    private fun mangle(value: String): String = buildString {
        value.forEach { character ->
            append(character.mangled())
        }
    }

    private fun Char.mangled(): String =
        when (this) {
            '/', '.' -> "_"
            '_' -> "_1"
            ';' -> "_2"
            '[' -> "_3"
            in 'a'..'z',
            in 'A'..'Z',
            in '0'..'9',
            -> toString()
            else -> "_0" + code.toString(16).padStart(4, '0')
        }
}
