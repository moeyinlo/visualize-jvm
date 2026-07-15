package me.moeyinlo.visualize.jvm.runtime

sealed interface JvmValue {
    val category: JvmValueCategory
}

enum class JvmValueCategory(val slotWidth: Int) {
    Category1(slotWidth = 1),
    Category2(slotWidth = 2),
}

sealed interface JvmPrimitiveValue : JvmValue

sealed interface JvmReferenceValue : JvmValue

data object JvmNullValue : JvmReferenceValue {
    override val category: JvmValueCategory = JvmValueCategory.Category1
}

@JvmInline
value class JvmReferenceId(val value: Int) {
    init {
        require(value > 0) { "reference id must be positive: $value" }
    }
}

data class JvmObjectReferenceValue(val referenceId: JvmReferenceId) : JvmReferenceValue {
    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmReturnAddressValue(val address: Int) : JvmValue {
    init {
        require(address >= 0) { "return address must be non-negative: $address" }
    }

    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmBooleanValue(val value: Boolean) : JvmPrimitiveValue {
    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmByteValue(val value: Int) : JvmPrimitiveValue {
    init {
        require(value in Byte.MIN_VALUE..Byte.MAX_VALUE) { "byte value out of range: $value" }
    }

    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmCharValue(val value: Int) : JvmPrimitiveValue {
    init {
        require(value in Char.MIN_VALUE.code..Char.MAX_VALUE.code) { "char value out of range: $value" }
    }

    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmShortValue(val value: Int) : JvmPrimitiveValue {
    init {
        require(value in Short.MIN_VALUE..Short.MAX_VALUE) { "short value out of range: $value" }
    }

    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmIntValue(val value: Int) : JvmPrimitiveValue {
    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmLongValue(val value: Long) : JvmPrimitiveValue {
    override val category: JvmValueCategory = JvmValueCategory.Category2
}

data class JvmFloatValue(val value: Float) : JvmPrimitiveValue {
    override val category: JvmValueCategory = JvmValueCategory.Category1
}

data class JvmDoubleValue(val value: Double) : JvmPrimitiveValue {
    override val category: JvmValueCategory = JvmValueCategory.Category2
}
