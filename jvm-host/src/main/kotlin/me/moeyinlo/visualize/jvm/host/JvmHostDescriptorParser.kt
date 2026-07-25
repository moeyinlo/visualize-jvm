package me.moeyinlo.visualize.jvm.host

internal data class JvmHostParsedMethodDescriptor(
    val parameterTypes: List<Class<*>>,
    val returnType: Class<*>,
)

internal object JvmHostDescriptorParser {
    fun parseMethodDescriptor(
        descriptor: String,
        classLoader: ClassLoader?,
    ): JvmHostParsedMethodDescriptor {
        require(descriptor.startsWith("(")) { "method descriptor must start with '('" }
        val parameterTypes = mutableListOf<Class<*>>()
        var index = 1
        while (index < descriptor.length && descriptor[index] != ')') {
            val parsed = parseType(descriptor, index, classLoader, allowVoid = false)
            parameterTypes += parsed.type
            index = parsed.nextIndex
        }
        require(index < descriptor.length && descriptor[index] == ')') {
            "method descriptor must contain ')'"
        }
        val returnType = parseType(descriptor, index + 1, classLoader, allowVoid = true)
        require(returnType.nextIndex == descriptor.length) {
            "method descriptor has trailing characters: $descriptor"
        }
        return JvmHostParsedMethodDescriptor(
            parameterTypes = parameterTypes.toList(),
            returnType = returnType.type,
        )
    }

    fun parseFieldDescriptor(
        descriptor: String,
        classLoader: ClassLoader?,
    ): Class<*> {
        val parsed = parseType(descriptor, startIndex = 0, classLoader = classLoader, allowVoid = false)
        require(parsed.nextIndex == descriptor.length) {
            "field descriptor has trailing characters: $descriptor"
        }
        return parsed.type
    }

    private fun parseType(
        descriptor: String,
        startIndex: Int,
        classLoader: ClassLoader?,
        allowVoid: Boolean,
    ): ParsedHostType {
        require(startIndex < descriptor.length) { "missing type descriptor in $descriptor" }
        return when (descriptor[startIndex]) {
            'Z' -> ParsedHostType(Boolean::class.javaPrimitiveType!!, startIndex + 1)
            'B' -> ParsedHostType(Byte::class.javaPrimitiveType!!, startIndex + 1)
            'C' -> ParsedHostType(Char::class.javaPrimitiveType!!, startIndex + 1)
            'S' -> ParsedHostType(Short::class.javaPrimitiveType!!, startIndex + 1)
            'I' -> ParsedHostType(Int::class.javaPrimitiveType!!, startIndex + 1)
            'J' -> ParsedHostType(Long::class.javaPrimitiveType!!, startIndex + 1)
            'F' -> ParsedHostType(Float::class.javaPrimitiveType!!, startIndex + 1)
            'D' -> ParsedHostType(Double::class.javaPrimitiveType!!, startIndex + 1)
            'V' -> {
                require(allowVoid) { "void is not a valid parameter type in $descriptor" }
                ParsedHostType(Void.TYPE, startIndex + 1)
            }
            'L' -> parseObjectType(descriptor, startIndex, classLoader)
            '[' -> parseArrayType(descriptor, startIndex, classLoader)
            else -> throw IllegalArgumentException("Unsupported type descriptor ${descriptor[startIndex]} in $descriptor")
        }
    }

    private fun parseObjectType(
        descriptor: String,
        startIndex: Int,
        classLoader: ClassLoader?,
    ): ParsedHostType {
        val endIndex = descriptor.indexOf(';', startIndex)
        require(endIndex > startIndex) { "object type descriptor must end with ';' in $descriptor" }
        val binaryName = descriptor.substring(startIndex + 1, endIndex).replace('/', '.')
        return ParsedHostType(Class.forName(binaryName, false, classLoader), endIndex + 1)
    }

    private fun parseArrayType(
        descriptor: String,
        startIndex: Int,
        classLoader: ClassLoader?,
    ): ParsedHostType {
        var index = startIndex
        while (index < descriptor.length && descriptor[index] == '[') {
            index++
        }
        val component = parseType(descriptor, index, classLoader, allowVoid = false)
        val arrayDescriptor = descriptor.substring(startIndex, component.nextIndex).replace('/', '.')
        return ParsedHostType(Class.forName(arrayDescriptor, false, classLoader), component.nextIndex)
    }
}

private data class ParsedHostType(
    val type: Class<*>,
    val nextIndex: Int,
)
