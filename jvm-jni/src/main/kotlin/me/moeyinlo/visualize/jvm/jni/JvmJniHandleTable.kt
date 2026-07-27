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
    private val localFrameCapacities = mutableListOf<Int>()
    private var nextHandleId = 1

    val liveHandleCount: Int
        get() = entries.size

    val localFrameDepth: Int
        get() = localFrameStarts.size

    val globalHandleCount: Int
        get() = countHandlesInScope(JvmJniHandleScope.Global)

    val weakGlobalHandleCount: Int
        get() = countHandlesInScope(JvmJniHandleScope.WeakGlobal)

    fun newObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference), JvmJniHandleScope.Local)

    fun newGlobalObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference), JvmJniHandleScope.Global)

    fun newWeakGlobalObjectHandle(reference: JvmObjectReferenceValue): JvmJniHandleId =
        allocate(JvmJniHandleEntry.ObjectHandle(reference), JvmJniHandleScope.WeakGlobal)

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

    fun resolveObjectOrNull(handle: JvmJniHandleId?): JvmObjectReferenceValue? =
        handle?.let(::resolveObject)

    fun resolveClass(handle: JvmJniHandleId): String =
        entry(handle).expect<JvmJniHandleEntry.ClassHandle>(handle).className

    fun resolveClassOrNull(handle: JvmJniHandleId?): String? =
        handle?.let(::resolveClass)

    fun resolveMethodId(handle: JvmJniHandleId): JvmResolvedMethod =
        entry(handle).expect<JvmJniHandleEntry.MethodIdHandle>(handle).method

    fun resolveMethodIdOrNull(handle: JvmJniHandleId?): JvmResolvedMethod? =
        handle?.let(::resolveMethodId)

    fun resolveFieldId(handle: JvmJniHandleId): JvmResolvedField =
        entry(handle).expect<JvmJniHandleEntry.FieldIdHandle>(handle).field

    fun resolveFieldIdOrNull(handle: JvmJniHandleId?): JvmResolvedField? =
        handle?.let(::resolveFieldId)

    fun deleteLocal(handle: JvmJniHandleId) {
        deleteScoped(handle = handle, expectedScope = JvmJniHandleScope.Local)
    }

    fun deleteGlobal(handle: JvmJniHandleId) {
        deleteScoped(handle = handle, expectedScope = JvmJniHandleScope.Global)
    }

    fun deleteWeakGlobal(handle: JvmJniHandleId) {
        deleteScoped(handle = handle, expectedScope = JvmJniHandleScope.WeakGlobal)
    }

    fun referenceType(handle: JvmJniHandleId?): JvmJniReferenceType =
        handle
            ?.let(entries::get)
            ?.scope
            ?.toReferenceType()
            ?: JvmJniReferenceType.Invalid

    fun pushLocalFrame(capacity: Int = Int.MAX_VALUE) {
        localFrameStarts += nextHandleId
        localFrameCapacities += capacity
    }

    fun deleteCurrentLocalFrameHandles() {
        val frameStart = localFrameStarts.removeLastOrNull()
            ?: throw JvmJniLocalFrameException("JNI local frame stack is empty")
        localFrameCapacities.removeLast()
        val scopedHandles = entries.filter { (handle, record) ->
            record.scope == JvmJniHandleScope.Local && handle.value >= frameStart
        }.keys
        scopedHandles.forEach(entries::remove)
    }

    fun snapshotLocalReference(handle: JvmJniHandleId): JvmJniLocalReferenceSnapshot =
        when (val handleEntry = entry(handle)) {
            is JvmJniHandleEntry.ObjectHandle -> JvmJniLocalReferenceSnapshot.ObjectReference(handleEntry.reference)
            is JvmJniHandleEntry.ClassHandle -> JvmJniLocalReferenceSnapshot.ClassReference(handleEntry.className)
            is JvmJniHandleEntry.MethodIdHandle,
            is JvmJniHandleEntry.FieldIdHandle,
            -> throw JvmJniHandleTypeException(
                "JNI handle ${handle.value} has kind ${handleEntry::class.simpleName}, expected local reference",
            )
        }

    fun newLocalReference(snapshot: JvmJniLocalReferenceSnapshot): JvmJniHandleId =
        when (snapshot) {
            is JvmJniLocalReferenceSnapshot.ObjectReference -> newObjectHandle(snapshot.reference)
            is JvmJniLocalReferenceSnapshot.ClassReference -> newClassHandle(snapshot.className)
        }

    fun newGlobalReference(snapshot: JvmJniLocalReferenceSnapshot): JvmJniHandleId =
        when (snapshot) {
            is JvmJniLocalReferenceSnapshot.ObjectReference -> newGlobalObjectHandle(snapshot.reference)
            is JvmJniLocalReferenceSnapshot.ClassReference ->
                allocate(JvmJniHandleEntry.ClassHandle(snapshot.className), JvmJniHandleScope.Global)
        }

    fun newWeakGlobalReference(snapshot: JvmJniLocalReferenceSnapshot): JvmJniHandleId =
        when (snapshot) {
            is JvmJniLocalReferenceSnapshot.ObjectReference -> newWeakGlobalObjectHandle(snapshot.reference)
            is JvmJniLocalReferenceSnapshot.ClassReference ->
                allocate(JvmJniHandleEntry.ClassHandle(snapshot.className), JvmJniHandleScope.WeakGlobal)
        }

    private fun allocate(entry: JvmJniHandleEntry, scope: JvmJniHandleScope): JvmJniHandleId {
        if (scope == JvmJniHandleScope.Local) {
            requireLocalFrameCapacity()
        }
        val handle = JvmJniHandleId(nextHandleId)
        nextHandleId += 1
        entries[handle] = JvmJniHandleRecord(entry = entry, scope = scope)
        return handle
    }

    private fun requireLocalFrameCapacity() {
        val frameStart = localFrameStarts.lastOrNull() ?: return
        val capacity = localFrameCapacities.last()
        val currentFrameLocalHandles = entries.count { (handle, record) ->
            record.scope == JvmJniHandleScope.Local && handle.value >= frameStart
        }
        if (currentFrameLocalHandles >= capacity) {
            throw JvmJniLocalFrameException("JNI local frame capacity $capacity exceeded")
        }
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

    private fun countHandlesInScope(scope: JvmJniHandleScope): Int =
        entries.count { (_, record) -> record.scope == scope }

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
    WeakGlobal,
}

enum class JvmJniReferenceType {
    Invalid,
    Local,
    Global,
    WeakGlobal,
}

sealed interface JvmJniLocalReferenceSnapshot {
    data class ObjectReference(val reference: JvmObjectReferenceValue) : JvmJniLocalReferenceSnapshot

    data class ClassReference(val className: String) : JvmJniLocalReferenceSnapshot
}

private fun JvmJniHandleScope.toReferenceType(): JvmJniReferenceType =
    when (this) {
        JvmJniHandleScope.Local -> JvmJniReferenceType.Local
        JvmJniHandleScope.Global -> JvmJniReferenceType.Global
        JvmJniHandleScope.WeakGlobal -> JvmJniReferenceType.WeakGlobal
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
