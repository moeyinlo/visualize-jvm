package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload

object JvmHostArrayBridge {
    fun toHost(
        value: JvmReferenceValue,
        targetType: Class<*>,
        heap: JvmHeap,
    ): Any? {
        requireArrayType(targetType, role = "target")
        if (value == JvmNullValue) {
            return null
        }
        val reference = value as? JvmObjectReferenceValue
            ?: throw JvmHostArrayBridgeException("Guest value $value is not an object reference")
        val heapObject = heap.get(reference)
        return when (val payload = heapObject.payload) {
            is JvmBooleanArrayPayload -> {
                requireExactArrayType(targetType, BooleanArray::class.java)
                payload.elements.toBooleanArray()
            }
            is JvmByteArrayPayload -> {
                requireExactArrayType(targetType, ByteArray::class.java)
                payload.elements.toByteArray()
            }
            is JvmCharArrayPayload -> {
                requireExactArrayType(targetType, CharArray::class.java)
                payload.elements.toCharArray()
            }
            is JvmShortArrayPayload -> {
                requireExactArrayType(targetType, ShortArray::class.java)
                payload.elements.toShortArray()
            }
            is JvmIntArrayPayload -> {
                requireExactArrayType(targetType, IntArray::class.java)
                payload.elements.toIntArray()
            }
            is JvmLongArrayPayload -> {
                requireExactArrayType(targetType, LongArray::class.java)
                payload.elements.toLongArray()
            }
            is JvmFloatArrayPayload -> {
                requireExactArrayType(targetType, FloatArray::class.java)
                payload.elements.toFloatArray()
            }
            is JvmDoubleArrayPayload -> {
                requireExactArrayType(targetType, DoubleArray::class.java)
                payload.elements.toDoubleArray()
            }
            is JvmReferenceArrayPayload -> toHostReferenceArray(payload, targetType, heap)
            else -> throw JvmHostArrayBridgeException(
                "Guest reference ${heapObject.className} is not an array payload",
            )
        }
    }

    fun fromHost(
        value: Any?,
        sourceType: Class<*>,
        heap: JvmHeap,
    ): JvmReferenceValue {
        requireArrayType(sourceType, role = "source")
        if (value == null) {
            return JvmNullValue
        }
        return when (sourceType) {
            BooleanArray::class.java -> heap.allocateBooleanArrayFrom(expectHostArray<BooleanArray>(value, sourceType))
            ByteArray::class.java -> heap.allocateByteArrayFrom(expectHostArray<ByteArray>(value, sourceType))
            CharArray::class.java -> heap.allocateCharArrayFrom(expectHostArray<CharArray>(value, sourceType))
            ShortArray::class.java -> heap.allocateShortArrayFrom(expectHostArray<ShortArray>(value, sourceType))
            IntArray::class.java -> heap.allocateIntArrayFrom(expectHostArray<IntArray>(value, sourceType))
            LongArray::class.java -> heap.allocateLongArrayFrom(expectHostArray<LongArray>(value, sourceType))
            FloatArray::class.java -> heap.allocateFloatArrayFrom(expectHostArray<FloatArray>(value, sourceType))
            DoubleArray::class.java -> heap.allocateDoubleArrayFrom(expectHostArray<DoubleArray>(value, sourceType))
            else -> fromHostReferenceArray(value, sourceType, heap)
        }
    }

    private fun toHostReferenceArray(
        payload: JvmReferenceArrayPayload,
        targetType: Class<*>,
        heap: JvmHeap,
    ): Array<String?> {
        val componentType = targetType.componentType
        if (componentType != String::class.java) {
            throw JvmHostArrayBridgeException(
                "Host reference array component is not supported yet: ${componentType.name}",
            )
        }
        return payload.elements
            .map { element -> JvmHostStringBridge.toHost(element, String::class.java, heap) }
            .toTypedArray()
    }

    private fun fromHostReferenceArray(
        value: Any,
        sourceType: Class<*>,
        heap: JvmHeap,
    ): JvmReferenceValue {
        val componentType = sourceType.componentType
        if (componentType != String::class.java) {
            throw JvmHostArrayBridgeException(
                "Host reference array component is not supported yet: ${componentType.name}",
            )
        }
        val hostArray = expectHostArray<Array<*>>(value, sourceType)
        val reference = heap.allocateReferenceArray("java/lang/String", hostArray.size)
        val payload = heap.get(reference).payload as JvmReferenceArrayPayload
        hostArray.forEachIndexed { index, element ->
            payload.elements[index] = when (element) {
                null -> JvmNullValue
                is String -> heap.allocateString(element)
                else -> throw JvmHostArrayBridgeException(
                    "Host string array element $index is ${element::class.java.name}",
                )
            }
        }
        return reference
    }

    private fun requireArrayType(
        type: Class<*>,
        role: String,
    ) {
        if (!type.isArray) {
            throw JvmHostArrayBridgeException(
                "Host array bridge $role type must be an array: ${type.name}",
            )
        }
    }

    private fun requireExactArrayType(
        actualType: Class<*>,
        expectedType: Class<*>,
    ) {
        if (actualType != expectedType) {
            throw JvmHostArrayBridgeException(
                "Cannot bridge guest array to host ${actualType.name}; expected ${expectedType.name}",
            )
        }
    }

    private inline fun <reified T> expectHostArray(
        value: Any,
        sourceType: Class<*>,
    ): T =
        value as? T
            ?: throw JvmHostArrayBridgeException(
                "Host array source ${sourceType.name} returned ${value::class.java.name}",
            )

    private fun JvmHeap.allocateBooleanArrayFrom(values: BooleanArray): JvmObjectReferenceValue =
        allocateBooleanArray(values.size).also { reference ->
            (get(reference).payload as JvmBooleanArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateByteArrayFrom(values: ByteArray): JvmObjectReferenceValue =
        allocateByteArray(values.size).also { reference ->
            (get(reference).payload as JvmByteArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateCharArrayFrom(values: CharArray): JvmObjectReferenceValue =
        allocateCharArray(values.size).also { reference ->
            (get(reference).payload as JvmCharArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateShortArrayFrom(values: ShortArray): JvmObjectReferenceValue =
        allocateShortArray(values.size).also { reference ->
            (get(reference).payload as JvmShortArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateIntArrayFrom(values: IntArray): JvmObjectReferenceValue =
        allocateIntArray(values.size).also { reference ->
            (get(reference).payload as JvmIntArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateLongArrayFrom(values: LongArray): JvmObjectReferenceValue =
        allocateLongArray(values.size).also { reference ->
            (get(reference).payload as JvmLongArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateFloatArrayFrom(values: FloatArray): JvmObjectReferenceValue =
        allocateFloatArray(values.size).also { reference ->
            (get(reference).payload as JvmFloatArrayPayload).elements.replaceAll(values.asList())
        }

    private fun JvmHeap.allocateDoubleArrayFrom(values: DoubleArray): JvmObjectReferenceValue =
        allocateDoubleArray(values.size).also { reference ->
            (get(reference).payload as JvmDoubleArrayPayload).elements.replaceAll(values.asList())
        }

    private fun <T> MutableList<T>.replaceAll(values: List<T>) {
        values.forEachIndexed { index, value -> this[index] = value }
    }
}

class JvmHostArrayBridgeException(message: String) : IllegalStateException(message)
