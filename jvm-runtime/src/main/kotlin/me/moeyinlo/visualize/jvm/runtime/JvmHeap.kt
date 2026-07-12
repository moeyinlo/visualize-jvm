package me.moeyinlo.visualize.jvm.runtime

data class JvmHeapObject(
    val className: String,
)

class JvmHeap {
    private val objects = linkedMapOf<JvmReferenceId, JvmHeapObject>()
    private var nextReferenceId = 1

    fun allocateObject(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        val referenceId = JvmReferenceId(nextReferenceId)
        nextReferenceId += 1
        objects[referenceId] = JvmHeapObject(className)
        return JvmObjectReferenceValue(referenceId)
    }

    fun get(reference: JvmObjectReferenceValue): JvmHeapObject =
        objects[reference.referenceId]
            ?: throw JvmHeapAccessException("Unknown heap reference ${reference.referenceId}")
}

class JvmHeapAccessException(message: String) : IllegalStateException(message)
