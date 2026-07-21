package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod

@JvmInline
value class JvmJniHandleId(val value: Int) {
    init {
        require(value > 0) { "JNI handle id must be positive: $value" }
    }
}

class JvmJniHandleTable {
    private val entries = linkedMapOf<JvmJniHandleId, JvmJniHandleEntry>()
    private var nextHandleId = 1

    fun newObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference))

    fun newClassHandle(className: String): JvmJniHandleId {
        require(className.isNotBlank()) { "JNI class handle name must not be blank" }
        return allocate(JvmJniHandleEntry.ClassHandle(className))
    }

    fun newMethodIdHandle(method: JvmResolvedMethod): JvmJniHandleId =
        allocate(JvmJniHandleEntry.MethodIdHandle(method))

    fun newFieldIdHandle(field: JvmResolvedField): JvmJniHandleId =
        allocate(JvmJniHandleEntry.FieldIdHandle(field))

    fun resolveObject(handle: JvmJniHandleId): JvmObjectReferenceValue =
        entry(handle).expect<JvmJniHandleEntry.ObjectHandle>(handle).reference

    fun resolveClass(handle: JvmJniHandleId): String =
        entry(handle).expect<JvmJniHandleEntry.ClassHandle>(handle).className

    fun resolveMethodId(handle: JvmJniHandleId): JvmResolvedMethod =
        entry(handle).expect<JvmJniHandleEntry.MethodIdHandle>(handle).method

    fun resolveFieldId(handle: JvmJniHandleId): JvmResolvedField =
        entry(handle).expect<JvmJniHandleEntry.FieldIdHandle>(handle).field

    fun deleteLocal(handle: JvmJniHandleId) {
        if (entries.remove(handle) == null) {
            throw JvmJniInvalidHandleException("JNI handle ${handle.value} is not live")
        }
    }

    private fun allocate(entry: JvmJniHandleEntry): JvmJniHandleId {
        val handle = JvmJniHandleId(nextHandleId)
        nextHandleId += 1
        entries[handle] = entry
        return handle
    }

    private fun entry(handle: JvmJniHandleId): JvmJniHandleEntry =
        entries[handle]
            ?: throw JvmJniInvalidHandleException("JNI handle ${handle.value} is not live")
}

private sealed interface JvmJniHandleEntry {
    data class ObjectHandle(val reference: JvmObjectReferenceValue) : JvmJniHandleEntry
    data class ClassHandle(val className: String) : JvmJniHandleEntry
    data class MethodIdHandle(val method: JvmResolvedMethod) : JvmJniHandleEntry
    data class FieldIdHandle(val field: JvmResolvedField) : JvmJniHandleEntry
}

private inline fun <reified T : JvmJniHandleEntry> JvmJniHandleEntry.expect(handle: JvmJniHandleId): T =
    this as? T
        ?: throw JvmJniHandleTypeException(
            "JNI handle ${handle.value} has kind ${this::class.simpleName}, expected ${T::class.simpleName}",
        )

class JvmJniInvalidHandleException(message: String) : IllegalStateException(message)

class JvmJniHandleTypeException(message: String) : IllegalStateException(message)
