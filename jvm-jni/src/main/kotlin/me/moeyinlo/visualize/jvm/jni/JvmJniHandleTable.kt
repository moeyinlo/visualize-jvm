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
    private val entries = linkedMapOf<JvmJniHandleId, JvmJniHandleRecord>()
    private val localFrameStarts = mutableListOf<Int>()
    private var nextHandleId = 1

    fun newObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference), JvmJniHandleScope.Local)

    fun newGlobalObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference), JvmJniHandleScope.Global)

    fun newClassHandle(className: String): JvmJniHandleId {
        require(className.isNotBlank()) { "JNI class handle name must not be blank" }
        return allocate(JvmJniHandleEntry.ClassHandle(className), JvmJniHandleScope.Local)
    }

    fun newMethodIdHandle(method: JvmResolvedMethod): JvmJniHandleId =
        allocate(JvmJniHandleEntry.MethodIdHandle(method), JvmJniHandleScope.Local)

    fun newFieldIdHandle(field: JvmResolvedField): JvmJniHandleId =
        allocate(JvmJniHandleEntry.FieldIdHandle(field), JvmJniHandleScope.Local)

    fun resolveObject(handle: JvmJniHandleId): JvmObjectReferenceValue =
        entry(handle).expect<JvmJniHandleEntry.ObjectHandle>(handle).reference

    fun resolveClass(handle: JvmJniHandleId): String =
        entry(handle).expect<JvmJniHandleEntry.ClassHandle>(handle).className

    fun resolveMethodId(handle: JvmJniHandleId): JvmResolvedMethod =
        entry(handle).expect<JvmJniHandleEntry.MethodIdHandle>(handle).method

    fun resolveFieldId(handle: JvmJniHandleId): JvmResolvedField =
        entry(handle).expect<JvmJniHandleEntry.FieldIdHandle>(handle).field

    fun deleteLocal(handle: JvmJniHandleId) {
        deleteScoped(handle = handle, expectedScope = JvmJniHandleScope.Local)
    }

    fun deleteGlobal(handle: JvmJniHandleId) {
        deleteScoped(handle = handle, expectedScope = JvmJniHandleScope.Global)
    }

    fun pushLocalFrame() {
        localFrameStarts += nextHandleId
    }

    fun deleteCurrentLocalFrameHandles() {
        val frameStart = localFrameStarts.removeLastOrNull()
            ?: throw JvmJniLocalFrameException("JNI local frame stack is empty")
        val scopedHandles = entries.filter { (handle, record) ->
            record.scope == JvmJniHandleScope.Local && handle.value >= frameStart
        }.keys
        scopedHandles.forEach(entries::remove)
    }

    private fun allocate(entry: JvmJniHandleEntry, scope: JvmJniHandleScope): JvmJniHandleId {
        val handle = JvmJniHandleId(nextHandleId)
        nextHandleId += 1
        entries[handle] = JvmJniHandleRecord(entry = entry, scope = scope)
        return handle
    }

    private fun deleteScoped(handle: JvmJniHandleId, expectedScope: JvmJniHandleScope) {
        val record = entries[handle]
            ?: throw JvmJniInvalidHandleException("JNI handle ${handle.value} is not live")
        if (record.scope != expectedScope) {
            throw JvmJniHandleScopeException(
                "JNI handle ${handle.value} has scope ${record.scope}, expected $expectedScope",
            )
        }
        entries.remove(handle)
    }

    private fun entry(handle: JvmJniHandleId): JvmJniHandleEntry =
        entries[handle]?.entry
            ?: throw JvmJniInvalidHandleException("JNI handle ${handle.value} is not live")
}

private data class JvmJniHandleRecord(
    val entry: JvmJniHandleEntry,
    val scope: JvmJniHandleScope,
)

private enum class JvmJniHandleScope {
    Local,
    Global,
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

class JvmJniHandleScopeException(message: String) : IllegalStateException(message)
