package me.moeyinlo.visualize.jvm.runtime

data class JvmHeapObject(
    val className: String,
    val payload: JvmHeapPayload = JvmHeapPayload.None,
    val isInitialized: Boolean = true,
)

sealed interface JvmHeapPayload {
    data object None : JvmHeapPayload
}

data class JvmStringPayload(val value: String) : JvmHeapPayload

data class JvmClassPayload(val representedClassName: String) : JvmHeapPayload

data class JvmStackTraceFrame(
    val declaringClass: String,
    val methodName: String,
    val fileName: String?,
    val lineNumber: Int?,
)

data class JvmThrowablePayload(
    val stackTrace: List<JvmStackTraceFrame>,
    val cause: JvmReferenceValue = JvmNullValue,
    val detailMessage: JvmReferenceValue = JvmNullValue,
) : JvmHeapPayload

data class JvmThreadPayload(val threadId: String) : JvmHeapPayload

data class JvmBooleanArrayPayload(val elements: MutableList<Boolean>) : JvmHeapPayload

data class JvmDoubleArrayPayload(val elements: MutableList<Double>) : JvmHeapPayload

data class JvmByteArrayPayload(val elements: MutableList<Byte>) : JvmHeapPayload

data class JvmCharArrayPayload(val elements: MutableList<Char>) : JvmHeapPayload

data class JvmFloatArrayPayload(val elements: MutableList<Float>) : JvmHeapPayload

data class JvmIntArrayPayload(val elements: MutableList<Int>) : JvmHeapPayload

data class JvmLongArrayPayload(val elements: MutableList<Long>) : JvmHeapPayload

data class JvmShortArrayPayload(val elements: MutableList<Short>) : JvmHeapPayload

data class JvmMethodTypePayload(val descriptor: String) : JvmHeapPayload

data class JvmMethodHandlesLookupPayload(val lookupClassName: String) : JvmHeapPayload

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

data class JvmCallSitePayload(val targetMethodHandle: JvmObjectReferenceValue) : JvmHeapPayload

data class JvmReferenceArrayPayload(val elements: MutableList<JvmReferenceValue>) : JvmHeapPayload

data class JvmDirectByteBufferPayload(val address: Long, val capacity: Long) : JvmHeapPayload

private data class JvmMethodHandleKey(
    val referenceKind: JvmMethodHandleReferenceKind,
    val referenceIndex: Int,
)

class JvmHeap {
    private val objects = linkedMapOf<JvmReferenceId, JvmHeapObject>()
    private val instanceFields = linkedMapOf<JvmReferenceId, MutableMap<JvmFieldReference, JvmValue>>()
    private val internedStrings = linkedMapOf<String, JvmObjectReferenceValue>()
    private val classMirrors = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodTypes = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodHandleLookups = linkedMapOf<String, JvmObjectReferenceValue>()
    private val methodHandles = linkedMapOf<JvmMethodHandleKey, JvmObjectReferenceValue>()
    private val threads = linkedMapOf<String, JvmObjectReferenceValue>()
    private var nextReferenceId = 1

    fun allocateObject(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        return allocate(JvmHeapObject(className))
    }

    fun allocateObject(classDefinition: JvmClassDefinition): JvmObjectReferenceValue {
        return allocateObject(classDefinition, superclasses = emptyList())
    }

    fun allocateObject(
        methodArea: JvmMethodArea,
        loadedClassKey: JvmLoadedClassKey,
    ): JvmObjectReferenceValue {
        val entry = methodArea.getClass(loadedClassKey)
            ?: throw JvmMethodAreaAccessException(
                "Class ${loadedClassKey.diagnosticName} is not defined in the method area",
            )
        return allocateObject(
            classDefinition = entry.definition,
            superclasses = methodArea.superclassDefinitionsFor(loadedClassKey),
        )
    }

    fun allocateObject(
        classDefinition: JvmClassDefinition,
        superclasses: List<JvmClassDefinition>,
    ): JvmObjectReferenceValue {
        val reference = allocateObject(classDefinition.internalName)
        (superclasses + classDefinition).forEach { definition ->
            prepareDeclaredInstanceFields(reference, definition)
        }
        return reference
    }

    fun allocateUninitializedObject(className: String): JvmObjectReferenceValue {
        require(className.isNotBlank()) { "class name must not be blank" }

        return allocate(
            JvmHeapObject(
                className = className,
                isInitialized = false,
            ),
        )
    }

    fun allocateUninitializedObject(
        methodArea: JvmMethodArea,
        loadedClassKey: JvmLoadedClassKey,
    ): JvmObjectReferenceValue {
        val entry = methodArea.getClass(loadedClassKey)
            ?: throw JvmMethodAreaAccessException(
                "Class ${loadedClassKey.diagnosticName} is not defined in the method area",
            )
        return allocateUninitializedObject(
            classDefinition = entry.definition,
            superclasses = methodArea.superclassDefinitionsFor(loadedClassKey),
        )
    }

    fun allocateUninitializedObject(
        classDefinition: JvmClassDefinition,
        superclasses: List<JvmClassDefinition> = emptyList(),
    ): JvmObjectReferenceValue {
        val reference = allocateUninitializedObject(classDefinition.internalName)
        (superclasses + classDefinition).forEach { definition ->
            prepareDeclaredInstanceFields(reference, definition)
        }
        return reference
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

    fun allocateCallSite(targetMethodHandle: JvmObjectReferenceValue): JvmObjectReferenceValue {
        val target = get(targetMethodHandle)
        require(target.className == "java/lang/invoke/MethodHandle") {
            "call site target must be a java/lang/invoke/MethodHandle object: ${target.className}"
        }
        return allocate(
            JvmHeapObject(
                className = "java/lang/invoke/CallSite",
                payload = JvmCallSitePayload(targetMethodHandle),
            ),
        )
    }

    fun allocateDirectByteBuffer(address: Long, capacity: Long): JvmObjectReferenceValue {
        require(capacity >= 0) { "direct byte buffer capacity must be non-negative: $capacity" }

        return allocate(
            JvmHeapObject(
                className = "java/nio/DirectByteBuffer",
                payload = JvmDirectByteBufferPayload(address = address, capacity = capacity),
            ),
        )
    }

    fun internString(value: String): JvmObjectReferenceValue =
        internedStrings.getOrPut(value) { allocateString(value) }

    fun internThread(threadId: String): JvmObjectReferenceValue {
        require(threadId.isNotBlank()) { "thread id must not be blank" }

        return threads.getOrPut(threadId) {
            allocate(
                JvmHeapObject(
                    className = "java/lang/Thread",
                    payload = JvmThreadPayload(threadId),
                ),
            )
        }
    }

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

    fun internMethodHandlesLookup(lookupClassName: String): JvmObjectReferenceValue {
        require(lookupClassName.isNotBlank()) { "lookup class name must not be blank" }

        return methodHandleLookups.getOrPut(lookupClassName) {
            allocate(
                JvmHeapObject(
                    className = "java/lang/invoke/MethodHandles\$Lookup",
                    payload = JvmMethodHandlesLookupPayload(lookupClassName),
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

    fun shallowClone(reference: JvmObjectReferenceValue): JvmObjectReferenceValue {
        val heapObject = get(reference)
        val clonedReference = allocate(
            heapObject.copy(payload = heapObject.payload.shallowClonePayload()),
        )
        instanceFields[reference.referenceId]?.let { fields ->
            instanceFields[clonedReference.referenceId] = fields.toMap(linkedMapOf())
        }
        return clonedReference
    }

    fun recordThrowableStackTrace(
        reference: JvmObjectReferenceValue,
        stackTrace: List<JvmStackTraceFrame>,
    ): JvmObjectReferenceValue {
        val heapObject = get(reference)
        val payload = heapObject.throwablePayloadOrDefault()
        objects[reference.referenceId] = heapObject.copy(payload = payload.copy(stackTrace = stackTrace.toList()))
        return reference
    }

    fun recordThrowableCause(
        reference: JvmObjectReferenceValue,
        cause: JvmReferenceValue,
    ): JvmObjectReferenceValue {
        val heapObject = get(reference)
        if (cause is JvmObjectReferenceValue) {
            get(cause)
        }
        val payload = heapObject.throwablePayloadOrDefault()
        objects[reference.referenceId] = heapObject.copy(payload = payload.copy(cause = cause))
        return reference
    }

    fun recordThrowableDetailMessage(
        reference: JvmObjectReferenceValue,
        detailMessage: JvmReferenceValue,
    ): JvmObjectReferenceValue {
        val heapObject = get(reference)
        if (detailMessage is JvmObjectReferenceValue) {
            val messageObject = get(detailMessage)
            require(messageObject.className == "java/lang/String") {
                "throwable detailMessage must be a java/lang/String object: ${messageObject.className}"
            }
        }
        val payload = heapObject.throwablePayloadOrDefault()
        objects[reference.referenceId] = heapObject.copy(payload = payload.copy(detailMessage = detailMessage))
        return reference
    }

    fun get(reference: JvmObjectReferenceValue): JvmHeapObject =
        objects[reference.referenceId]
            ?: throw JvmHeapAccessException("Unknown heap reference ${reference.referenceId}")

    fun isInitialized(reference: JvmObjectReferenceValue): Boolean =
        get(reference).isInitialized

    fun markInitialized(reference: JvmObjectReferenceValue) {
        val heapObject = get(reference)
        objects[reference.referenceId] = heapObject.copy(isInitialized = true)
    }

    fun putInstanceField(reference: JvmObjectReferenceValue, field: JvmFieldReference, value: JvmValue) {
        get(reference)
        instanceFields.getOrPut(reference.referenceId) { linkedMapOf() }[field] = value
    }

    fun hasInstanceField(reference: JvmObjectReferenceValue, field: JvmFieldReference): Boolean {
        get(reference)
        return instanceFields[reference.referenceId]?.containsKey(field) == true
    }

    fun getInstanceField(reference: JvmObjectReferenceValue, field: JvmFieldReference): JvmValue {
        get(reference)
        return instanceFields[reference.referenceId]?.get(field)
            ?: field.defaultFieldValue()
    }

    private fun prepareDeclaredInstanceFields(
        reference: JvmObjectReferenceValue,
        classDefinition: JvmClassDefinition,
    ) {
        classDefinition.fields
            .filterNot(JvmFieldDefinition::isStatic)
            .forEach { field ->
                val fieldReference = JvmFieldReference(
                    ownerClassName = classDefinition.internalName,
                    name = field.name,
                    descriptor = field.descriptor,
                )
                putInstanceField(reference, fieldReference, fieldReference.defaultFieldValue())
            }
    }

    private fun allocate(heapObject: JvmHeapObject): JvmObjectReferenceValue {
        val referenceId = JvmReferenceId(nextReferenceId)
        nextReferenceId += 1
        objects[referenceId] = heapObject
        return JvmObjectReferenceValue(referenceId)
    }
}

class JvmHeapAccessException(message: String) : IllegalStateException(message)

private fun JvmHeapObject.throwablePayloadOrDefault(): JvmThrowablePayload =
    payload as? JvmThrowablePayload ?: JvmThrowablePayload(stackTrace = emptyList())

private fun JvmHeapPayload.shallowClonePayload(): JvmHeapPayload =
    when (this) {
        is JvmBooleanArrayPayload -> JvmBooleanArrayPayload(elements.toMutableList())
        is JvmByteArrayPayload -> JvmByteArrayPayload(elements.toMutableList())
        is JvmCallSitePayload -> copy()
        is JvmCharArrayPayload -> JvmCharArrayPayload(elements.toMutableList())
        is JvmClassPayload -> copy()
        is JvmDirectByteBufferPayload -> copy()
        is JvmDoubleArrayPayload -> JvmDoubleArrayPayload(elements.toMutableList())
        is JvmFloatArrayPayload -> JvmFloatArrayPayload(elements.toMutableList())
        is JvmIntArrayPayload -> JvmIntArrayPayload(elements.toMutableList())
        is JvmLongArrayPayload -> JvmLongArrayPayload(elements.toMutableList())
        is JvmMethodHandlePayload -> copy()
        is JvmMethodHandlesLookupPayload -> copy()
        is JvmMethodTypePayload -> copy()
        JvmHeapPayload.None -> JvmHeapPayload.None
        is JvmReferenceArrayPayload -> JvmReferenceArrayPayload(elements.toMutableList())
        is JvmShortArrayPayload -> JvmShortArrayPayload(elements.toMutableList())
        is JvmStringPayload -> copy()
        is JvmThreadPayload -> copy()
        is JvmThrowablePayload -> copy(stackTrace = stackTrace.toList())
    }
