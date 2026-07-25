package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload

object JvmHostStringBridge {
    fun toHost(
        value: JvmReferenceValue,
        targetType: Class<*>,
        heap: JvmHeap,
    ): String? {
        requireStringType(targetType, role = "target")
        if (value == JvmNullValue) {
            return null
        }
        val reference = value as? JvmObjectReferenceValue
            ?: throw JvmHostStringBridgeException("Guest value $value is not an object reference")
        val heapObject = heap.get(reference)
        if (heapObject.className != "java/lang/String") {
            throw JvmHostStringBridgeException(
                "Guest reference ${heapObject.className} is not java/lang/String",
            )
        }
        val payload = heapObject.payload as? JvmStringPayload
            ?: throw JvmHostStringBridgeException("Guest java/lang/String has no string payload")
        return payload.value
    }

    fun fromHost(
        value: Any?,
        sourceType: Class<*>,
        heap: JvmHeap,
    ): JvmReferenceValue {
        requireStringType(sourceType, role = "source")
        if (value == null) {
            return JvmNullValue
        }
        val stringValue = value as? String
            ?: throw JvmHostStringBridgeException(
                "Host string source returned ${value::class.java.name}",
            )
        return heap.allocateString(stringValue)
    }

    private fun requireStringType(
        type: Class<*>,
        role: String,
    ) {
        if (type != String::class.java) {
            throw JvmHostStringBridgeException(
                "Host string bridge $role type must be java.lang.String: ${type.name}",
            )
        }
    }
}

class JvmHostStringBridgeException(message: String) : IllegalStateException(message)
