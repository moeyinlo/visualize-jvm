package me.moeyinlo.visualize.jvm.host

import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class JvmHostMethodMirror(
    val owner: JvmHostDelegatedClassMirror,
    val name: String,
    val descriptor: String,
    val hostMethod: Method,
    val isStatic: Boolean,
    val parameterTypes: List<Class<*>>,
    val returnType: Class<*>,
)

object JvmHostMethodResolver {
    fun resolveInstanceMethod(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
    ): JvmHostMethodMirror =
        resolveMethod(owner, name, descriptor, expectedStatic = false)

    fun resolveStaticMethod(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
    ): JvmHostMethodMirror =
        resolveMethod(owner, name, descriptor, expectedStatic = true)

    private fun resolveMethod(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
        expectedStatic: Boolean,
    ): JvmHostMethodMirror {
        require(name.isNotBlank()) { "host method name must not be blank" }
        val parsedDescriptor = JvmHostMethodDescriptorParser.parse(
            descriptor = descriptor,
            classLoader = owner.hostClass.classLoader,
        )
        val hostMethod = try {
            owner.hostClass.getMethod(name, *parsedDescriptor.parameterTypes.toTypedArray())
        } catch (exception: NoSuchMethodException) {
            throw JvmHostMethodResolutionException(
                "Host method ${owner.hostBinaryName}.$name:$descriptor was not found",
                exception,
            )
        }
        val actualStatic = Modifier.isStatic(hostMethod.modifiers)
        if (actualStatic != expectedStatic) {
            val staticText = if (actualStatic) "static" else "not static"
            throw JvmHostMethodResolutionException(
                "Host method ${owner.hostBinaryName}.$name:$descriptor is $staticText",
            )
        }
        if (hostMethod.returnType != parsedDescriptor.returnType) {
            throw JvmHostMethodResolutionException(
                "Host method ${owner.hostBinaryName}.$name:$descriptor returns " +
                    "${hostMethod.returnType.name}, expected ${parsedDescriptor.returnType.name}",
            )
        }
        return JvmHostMethodMirror(
            owner = owner,
            name = name,
            descriptor = descriptor,
            hostMethod = hostMethod,
            isStatic = actualStatic,
            parameterTypes = parsedDescriptor.parameterTypes,
            returnType = parsedDescriptor.returnType,
        )
    }
}

class JvmHostMethodResolutionException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

private data class JvmHostParsedMethodDescriptor(
    val parameterTypes: List<Class<*>>,
    val returnType: Class<*>,
)

private object JvmHostMethodDescriptorParser {
    fun parse(
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
