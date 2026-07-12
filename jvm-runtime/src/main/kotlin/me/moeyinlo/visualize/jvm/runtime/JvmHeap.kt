package me.moeyinlo.visualize.jvm.runtime

data class JvmHeapObject(
    val className: String,
    val payload: JvmHeapPayload = JvmHeapPayload.None,
)

sealed interface JvmHeapPayload {
    data object None : JvmHeapPayload
}

data class JvmStringPayload(val value: String) : JvmHeapPayload

class JvmHeap {
    private val objects = linkedMapOf<JvmReferenceId, JvmHeapObject>()
    private var nextReferenceId = 1

    fun allocateObject(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        return allocate(JvmHeapObject(className))
    }

    fun allocateString(value: String): JvmObjectReferenceValue = allocate(
        JvmHeapObject(
            className = "java/lang/String",
            payload = JvmStringPayload(value),
        ),
    )

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
