package me.moeyinlo.visualize.jvm.runtime

data class JvmHeapObject(
    val className: String,
    val payload: JvmHeapPayload = JvmHeapPayload.None,
)

sealed interface JvmHeapPayload {
    data object None : JvmHeapPayload
}

data class JvmStringPayload(val value: String) : JvmHeapPayload

data class JvmClassPayload(val representedClassName: String) : JvmHeapPayload

data class JvmBooleanArrayPayload(val elements: MutableList<Boolean>) : JvmHeapPayload

data class JvmDoubleArrayPayload(val elements: MutableList<Double>) : JvmHeapPayload

data class JvmByteArrayPayload(val elements: MutableList<Byte>) : JvmHeapPayload

data class JvmCharArrayPayload(val elements: MutableList<Char>) : JvmHeapPayload

data class JvmFloatArrayPayload(val elements: MutableList<Float>) : JvmHeapPayload

data class JvmIntArrayPayload(val elements: MutableList<Int>) : JvmHeapPayload

data class JvmLongArrayPayload(val elements: MutableList<Long>) : JvmHeapPayload

data class JvmShortArrayPayload(val elements: MutableList<Short>) : JvmHeapPayload

data class JvmMethodTypePayload(val descriptor: String) : JvmHeapPayload

enum class JvmMethodHandleReferenceKind {
    GetField,
    GetStatic,
    PutField,
    PutStatic,
    InvokeVirtual,
    InvokeStatic,
    InvokeSpecial,
    NewInvokeSpecial,
    InvokeInterface,
}

data class JvmMethodHandlePayload(
    val referenceKind: JvmMethodHandleReferenceKind,
    val referenceIndex: Int,
) : JvmHeapPayload

data class JvmReferenceArrayPayload(val elements: MutableList<JvmReferenceValue>) : JvmHeapPayload

private data class JvmMethodHandleKey(
    val referenceKind: JvmMethodHandleReferenceKind,
    val referenceIndex: Int,
)

class JvmHeap {
    private val objects = linkedMapOf<JvmReferenceId, JvmHeapObject>()
    private val internedStrings = linkedMapOf<String, JvmObjectReferenceValue>()
    private val classMirrors = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodTypes = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodHandles = linkedMapOf<JvmMethodHandleKey, JvmObjectReferenceValue>()
    private var nextReferenceId = 1

    fun allocateObject(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        return allocate(JvmHeapObject(className))
    }

    fun allocateBooleanArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[Z",
                payload = JvmBooleanArrayPayload(MutableList(length) { false }),
            ),
        )
    }

    fun allocateDoubleArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[D",
                payload = JvmDoubleArrayPayload(MutableList(length) { 0.0 }),
            ),
        )
    }

    fun allocateByteArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[B",
                payload = JvmByteArrayPayload(MutableList(length) { 0.toByte() }),
            ),
        )
    }

    fun allocateCharArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[C",
                payload = JvmCharArrayPayload(MutableList(length) { '\u0000' }),
            ),
        )
    }

    fun allocateFloatArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[F",
                payload = JvmFloatArrayPayload(MutableList(length) { 0.0f }),
            ),
        )
    }

    fun allocateIntArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[I",
                payload = JvmIntArrayPayload(MutableList(length) { 0 }),
            ),
        )
    }

    fun allocateLongArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[J",
                payload = JvmLongArrayPayload(MutableList(length) { 0L }),
            ),
        )
    }

    fun allocateReferenceArray(componentClassName: String, length: Int): JvmObjectReferenceValue {
        require(componentClassName.isNotBlank()) { "array component class name must not be blank" }
        require(length >= 0) { "array length must be non-negative: $length" }

        val arrayClassName = if (componentClassName.startsWith("[")) {
            "[$componentClassName"
        } else {
            "[L$componentClassName;"
        }
        return allocate(
            JvmHeapObject(
                className = arrayClassName,
                payload = JvmReferenceArrayPayload(MutableList(length) { JvmNullValue }),
            ),
        )
    }

    fun allocateShortArray(length: Int): JvmObjectReferenceValue {
        require(length >= 0) { "array length must be non-negative: $length" }

        return allocate(
            JvmHeapObject(
                className = "[S",
                payload = JvmShortArrayPayload(MutableList(length) { 0.toShort() }),
            ),
        )
    }

    fun allocateString(value: String): JvmObjectReferenceValue = allocate(
        JvmHeapObject(
            className = "java/lang/String",
            payload = JvmStringPayload(value),
        ),
    )

    fun internString(value: String): JvmObjectReferenceValue =
        internedStrings.getOrPut(value) { allocateString(value) }

    fun internClassMirror(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        return classMirrors.getOrPut(className) {
            allocate(
                JvmHeapObject(
                    className = "java/lang/Class",
                    payload = JvmClassPayload(className),
                ),
            )
        }
    }

    fun internMethodType(descriptor: String): JvmObjectReferenceValue {
        require(descriptor.isNotBlank()) { "method type descriptor must not be blank" }

        return methodTypes.getOrPut(descriptor) {
            allocate(
                JvmHeapObject(
                    className = "java/lang/invoke/MethodType",
                    payload = JvmMethodTypePayload(descriptor),
                ),
            )
        }
    }

    fun internMethodHandle(
        referenceKind: JvmMethodHandleReferenceKind,
        referenceIndex: Int,
    ): JvmObjectReferenceValue {
        require(referenceIndex > 0) { "method handle reference index must be positive" }

        val key = JvmMethodHandleKey(referenceKind = referenceKind, referenceIndex = referenceIndex)
        return methodHandles.getOrPut(key) {
            allocate(
                JvmHeapObject(
                    className = "java/lang/invoke/MethodHandle",
                    payload = JvmMethodHandlePayload(
                        referenceKind = referenceKind,
                        referenceIndex = referenceIndex,
                    ),
                ),
            )
        }
    }

    fun get(reference: JvmObjectReferenceValue): JvmHeapObject =
        objects[reference.referenceId]
            ?: throw JvmHeapAccessException("Unknown heap reference ${reference.referenceId}")

    private fun allocate(heapObject: JvmHeapObject): JvmObjectReferenceValue {
        val referenceId = JvmReferenceId(nextReferenceId)
        nextReferenceId += 1
        objects[referenceId] = heapObject
        return JvmObjectReferenceValue(referenceId)
    }
}

class JvmHeapAccessException(message: String) : IllegalStateException(message)
