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

data class JvmMethodTypePayload(val descriptor: String) : JvmHeapPayload

class JvmHeap {
    private val objects = linkedMapOf<JvmReferenceId, JvmHeapObject>()
    private val internedStrings = linkedMapOf<String, JvmObjectReferenceValue>()
    private val classMirrors = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodTypes = linkedMapOf<String, JvmObjectReferenceValue>()
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
