package me.moeyinlo.visualize.jvm.runtime

data class JvmFieldReference(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
) {
    init {
        require(ownerClassName.isNotBlank()) { "field owner class name must not be blank" }
        require(name.isNotBlank()) { "field name must not be blank" }
        require(descriptor.isNotBlank()) { "field descriptor must not be blank" }
    }
}

class JvmStaticFields {
    private val values = linkedMapOf<JvmFieldReference, JvmValue>()

    fun put(field: JvmFieldReference, value: JvmValue) {
        values[field] = value
    }

    fun get(field: JvmFieldReference): JvmValue =
        values[field]
            ?: throw JvmStaticFieldAccessException("Static field $field has no value")
}

class JvmStaticFieldAccessException(message: String) : IllegalStateException(message)
