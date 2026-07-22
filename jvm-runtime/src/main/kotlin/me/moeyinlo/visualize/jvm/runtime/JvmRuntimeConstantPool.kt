package me.moeyinlo.visualize.jvm.runtime

@JvmInline
value class JvmRuntimeConstantPoolIndex(val value: Int) {
    init {
        require(value >= 1) { "runtime constant pool indexes are one-based: $value" }
    }

    override fun toString(): String = "#$value"
}

sealed interface JvmRuntimeConstantPoolEntry

data class JvmRuntimeLiteralConstant(val value: JvmValue) : JvmRuntimeConstantPoolEntry

data class JvmRuntimeStringConstant(val value: String) : JvmRuntimeConstantPoolEntry

data class JvmRuntimeClassSymbolicReference(val internalName: String) : JvmRuntimeConstantPoolEntry {
    init {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
    }
}

data class JvmRuntimeFieldSymbolicReference(val field: JvmFieldReference) : JvmRuntimeConstantPoolEntry

data class JvmRuntimeMethodSymbolicReference(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isInterface: Boolean = false,
) : JvmRuntimeConstantPoolEntry {
    init {
        require(ownerClassName.isNotBlank()) { "method owner class name must not be blank" }
        require(name.isNotBlank()) { "method name must not be blank" }
        require(descriptor.isNotBlank()) { "method descriptor must not be blank" }
    }
}

sealed interface JvmRuntimeResolvedConstant {
    data class Value(val value: JvmValue) : JvmRuntimeResolvedConstant
    data class String(val value: kotlin.String) : JvmRuntimeResolvedConstant
    data class Class(val internalName: kotlin.String) : JvmRuntimeResolvedConstant
    data class Field(val field: JvmResolvedField) : JvmRuntimeResolvedConstant
    data class Method(val method: JvmResolvedMethod) : JvmRuntimeResolvedConstant
}

class JvmRuntimeConstantPool(
    val ownerClassName: String,
    entries: List<JvmRuntimeConstantPoolEntry>,
) {
    init {
        require(ownerClassName.isNotBlank()) { "constant pool owner class name must not be blank" }
    }

    private val entries = entries.toList()
    private val resolvedConstants = linkedMapOf<JvmRuntimeConstantPoolIndex, JvmRuntimeResolvedConstant>()

    val size: Int
        get() = entries.size

    operator fun get(index: JvmRuntimeConstantPoolIndex): JvmRuntimeConstantPoolEntry =
        entries.getOrNull(index.value - 1)
            ?: throw JvmRuntimeConstantPoolAccessException(
                "Runtime constant pool index $index is outside 1..$size for $ownerClassName",
            )

    fun cacheResolved(
        index: JvmRuntimeConstantPoolIndex,
        resolved: JvmRuntimeResolvedConstant,
    ): JvmRuntimeResolvedConstant {
        get(index)
        resolvedConstants[index] = resolved
        return resolved
    }

    fun resolved(index: JvmRuntimeConstantPoolIndex): JvmRuntimeResolvedConstant? {
        get(index)
        return resolvedConstants[index]
    }

    fun toList(): List<JvmRuntimeConstantPoolEntry> = entries.toList()
}

class JvmRuntimeConstantPoolAccessException(message: String) : IllegalStateException(message)
