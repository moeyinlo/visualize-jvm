package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodsAttribute

data class JvmBootstrapMethod(
    val bootstrapMethodRef: JvmRuntimeConstantPoolIndex,
    val bootstrapArguments: List<JvmRuntimeConstantPoolIndex>,
)

class JvmBootstrapMethodTable(
    bootstrapMethods: List<JvmBootstrapMethod> = emptyList(),
) {
    private val bootstrapMethods = bootstrapMethods.toList()

    val size: Int
        get() = bootstrapMethods.size

    operator fun get(index: Int): JvmBootstrapMethod =
        bootstrapMethods.getOrNull(index)
            ?: throw JvmBootstrapMethodAccessException(
                "Bootstrap method index #$index is outside 0..${bootstrapMethods.lastIndex}",
            )

    fun toList(): List<JvmBootstrapMethod> = bootstrapMethods.toList()

    companion object {
        fun fromAttribute(attribute: BootstrapMethodsAttribute?): JvmBootstrapMethodTable =
            JvmBootstrapMethodTable(
                attribute?.bootstrapMethods.orEmpty().map { bootstrapMethod ->
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(bootstrapMethod.bootstrapMethodRef.value),
                        bootstrapArguments = bootstrapMethod.bootstrapArguments.map { argumentIndex ->
                            JvmRuntimeConstantPoolIndex(argumentIndex.value)
                        },
                    )
                },
            )
    }
}

class JvmBootstrapMethodAccessException(message: String) : IllegalStateException(message)