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
        val parsedDescriptor = JvmHostDescriptorParser.parseMethodDescriptor(
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
