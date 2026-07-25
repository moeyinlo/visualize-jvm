package me.moeyinlo.visualize.jvm.host

import java.lang.reflect.Field
import java.lang.reflect.Modifier

data class JvmHostFieldMirror(
    val owner: JvmHostDelegatedClassMirror,
    val name: String,
    val descriptor: String,
    val hostField: Field,
    val isStatic: Boolean,
    val fieldType: Class<*>,
)

object JvmHostFieldResolver {
    fun resolveInstanceField(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
    ): JvmHostFieldMirror =
        resolveField(owner, name, descriptor, expectedStatic = false)

    fun resolveStaticField(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
    ): JvmHostFieldMirror =
        resolveField(owner, name, descriptor, expectedStatic = true)

    private fun resolveField(
        owner: JvmHostDelegatedClassMirror,
        name: String,
        descriptor: String,
        expectedStatic: Boolean,
    ): JvmHostFieldMirror {
        require(name.isNotBlank()) { "host field name must not be blank" }
        val fieldType = JvmHostDescriptorParser.parseFieldDescriptor(
            descriptor = descriptor,
            classLoader = owner.hostClass.classLoader,
        )
        val hostField = try {
            owner.hostClass.getField(name)
        } catch (exception: NoSuchFieldException) {
            throw JvmHostFieldResolutionException(
                "Host field ${owner.hostBinaryName}.$name:$descriptor was not found",
                exception,
            )
        }
        val actualStatic = Modifier.isStatic(hostField.modifiers)
        if (actualStatic != expectedStatic) {
            val staticText = if (actualStatic) "static" else "not static"
            throw JvmHostFieldResolutionException(
                "Host field ${owner.hostBinaryName}.$name:$descriptor is $staticText",
            )
        }
        if (hostField.type != fieldType) {
            throw JvmHostFieldResolutionException(
                "Host field ${owner.hostBinaryName}.$name:$descriptor has type " +
                    "${hostField.type.name}, expected ${fieldType.name}",
            )
        }
        return JvmHostFieldMirror(
            owner = owner,
            name = name,
            descriptor = descriptor,
            hostField = hostField,
            isStatic = actualStatic,
            fieldType = fieldType,
        )
    }
}

class JvmHostFieldResolutionException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
