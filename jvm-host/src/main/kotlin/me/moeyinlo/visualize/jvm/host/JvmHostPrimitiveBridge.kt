package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmPrimitiveValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue

object JvmHostPrimitiveBridge {
    fun toHost(
        value: JvmPrimitiveValue,
        targetType: Class<*>,
    ): Any =
        when (value) {
            is JvmBooleanValue -> {
                requireHostType(value, targetType, Boolean::class.javaPrimitiveType!!)
                value.value
            }
            is JvmByteValue -> {
                requireHostType(value, targetType, Byte::class.javaPrimitiveType!!)
                value.value.toByte()
            }
            is JvmCharValue -> {
                requireHostType(value, targetType, Char::class.javaPrimitiveType!!)
                value.value.toChar()
            }
            is JvmShortValue -> {
                requireHostType(value, targetType, Short::class.javaPrimitiveType!!)
                value.value.toShort()
            }
            is JvmIntValue -> {
                requireHostType(value, targetType, Int::class.javaPrimitiveType!!)
                value.value
            }
            is JvmLongValue -> {
                requireHostType(value, targetType, Long::class.javaPrimitiveType!!)
                value.value
            }
            is JvmFloatValue -> {
                requireHostType(value, targetType, Float::class.javaPrimitiveType!!)
                value.value
            }
            is JvmDoubleValue -> {
                requireHostType(value, targetType, Double::class.javaPrimitiveType!!)
                value.value
            }
        }

    fun fromHost(
        value: Any?,
        sourceType: Class<*>,
    ): JvmPrimitiveValue {
        if (value == null) {
            throw JvmHostPrimitiveBridgeException("Host primitive ${sourceType.name} returned null")
        }
        return when (sourceType) {
            Boolean::class.javaPrimitiveType!! -> {
                if (value is Boolean) {
                    JvmBooleanValue(value)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Byte::class.javaPrimitiveType!! -> {
                if (value is Byte) {
                    JvmByteValue(value.toInt())
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Char::class.javaPrimitiveType!! -> {
                if (value is Char) {
                    JvmCharValue(value.code)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Short::class.javaPrimitiveType!! -> {
                if (value is Short) {
                    JvmShortValue(value.toInt())
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Int::class.javaPrimitiveType!! -> {
                if (value is Int) {
                    JvmIntValue(value)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Long::class.javaPrimitiveType!! -> {
                if (value is Long) {
                    JvmLongValue(value)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Float::class.javaPrimitiveType!! -> {
                if (value is Float) {
                    JvmFloatValue(value)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            Double::class.javaPrimitiveType!! -> {
                if (value is Double) {
                    JvmDoubleValue(value)
                } else {
                    throwHostValueMismatch(value, sourceType)
                }
            }
            else -> throw JvmHostPrimitiveBridgeException(
                "Host type ${sourceType.name} is not a primitive bridge source",
            )
        }
    }

    private fun requireHostType(
        value: JvmPrimitiveValue,
        actualType: Class<*>,
        expectedType: Class<*>,
    ) {
        if (actualType != expectedType) {
            throw JvmHostPrimitiveBridgeException(
                "Cannot bridge guest ${value.guestTypeName()} to host ${actualType.name}",
            )
        }
    }

    private fun throwHostValueMismatch(
        value: Any,
        sourceType: Class<*>,
    ): Nothing =
        throw JvmHostPrimitiveBridgeException(
            "Host primitive ${sourceType.name} returned ${value::class.java.name}",
        )

    private fun JvmPrimitiveValue.guestTypeName(): String =
        when (this) {
            is JvmBooleanValue -> "boolean"
            is JvmByteValue -> "byte"
            is JvmCharValue -> "char"
            is JvmShortValue -> "short"
            is JvmIntValue -> "int"
            is JvmLongValue -> "long"
            is JvmFloatValue -> "float"
            is JvmDoubleValue -> "double"
        }
}

class JvmHostPrimitiveBridgeException(message: String) : IllegalStateException(message)
