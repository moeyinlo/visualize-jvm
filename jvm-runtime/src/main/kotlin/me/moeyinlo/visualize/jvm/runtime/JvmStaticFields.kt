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
            ?: field.defaultFieldValue()
}

class JvmStaticFieldAccessException(message: String) : IllegalStateException(message)

internal fun JvmFieldReference.defaultFieldValue(): JvmValue =
    when {
        descriptor in intLikeFieldDescriptors -> JvmIntValue(0)
        descriptor == "F" -> JvmFloatValue(0.0f)
        descriptor == "J" -> JvmLongValue(0L)
        descriptor == "D" -> JvmDoubleValue(0.0)
        descriptor.startsWith("L") || descriptor.startsWith("[") -> JvmNullValue
        else -> throw JvmStaticFieldAccessException("Field $this has no supported default value")
    }

private val intLikeFieldDescriptors = setOf("Z", "B", "C", "S", "I")
