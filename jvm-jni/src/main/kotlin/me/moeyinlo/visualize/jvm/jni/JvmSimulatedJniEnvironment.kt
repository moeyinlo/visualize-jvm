package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmDirectByteBufferPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmValue

class JvmSimulatedJniEnvironment(
    private val classHierarchy: JvmClassHierarchy,
    private val heap: JvmHeap = JvmHeap(),
    private val staticFields: JvmStaticFields = JvmStaticFields(),
    val handles: JvmJniHandleTable = JvmJniHandleTable(),
    private val monitors: JvmMonitorState = JvmMonitorState(),
    private val currentThreadId: String = "main",
    private val exceptionReporter: (String) -> Unit = {},
    private val upcallDispatcher: JvmJniUpcallDispatcher = JvmJniUpcallDispatcher.Unbound,
    val registeredNativeMethods: JvmJniNativeMethodRegistry = JvmJniNativeMethodRegistry(),
) {
    private val throwableDetailMessageField = JvmFieldReference(
        ownerClassName = "java/lang/Throwable",
        name = "detailMessage",
        descriptor = "Ljava/lang/String;",
    )
    private var pendingException: JvmObjectReferenceValue? = null
    val pendingExceptionReference: JvmObjectReferenceValue?
        get() = pendingException
    private val localFrameCapacities = mutableListOf<Int>()
    var ensuredLocalCapacity: Int = 0
        private set
    val localFrameDepth: Int
        get() = localFrameCapacities.size
    val functions: JvmSimulatedJniFunctionTable =
        JvmSimulatedJniFunctionTable.bind(this)
    var maxLocalFrameCapacity: Int = 0
        private set

    fun getVersion(): Int = JvmJniVersions.Version24

    fun registerNatives(
        classHandle: JvmJniHandleId,
        methods: List<JvmJniNativeMethodDescriptor>,
    ): Int {
        val className = requireLoadedClass(handles.resolveClass(classHandle))
        return registeredNativeMethods.register(className, methods)
    }

    fun unregisterNatives(classHandle: JvmJniHandleId): Int {
        val className = requireLoadedClass(handles.resolveClass(classHandle))
        return registeredNativeMethods.unregister(className)
    }

    fun throwObject(throwableHandle: JvmJniHandleId): Int {
        val throwableReference = handles.resolveObject(throwableHandle)
        requireThrowableClass(heap.get(throwableReference).className)
        pendingException = throwableReference
        return 0
    }

    fun throwNew(throwableClassHandle: JvmJniHandleId, message: String?): Int {
        val throwableClassName = requireThrowableClass(handles.resolveClass(throwableClassHandle))
        val throwableReference = heap.allocateObject(throwableClassName)
        val detailMessageValue = message
            ?.let { value -> heap.allocateString(value) }
            ?: JvmNullValue
        heap.putInstanceField(throwableReference, throwableDetailMessageField, detailMessageValue)
        pendingException = throwableReference
        return 0
    }

    fun exceptionOccurred(): JvmJniHandleId? =
        pendingException?.let(handles::newObjectHandle)

    fun exceptionCheck(): Boolean =
        pendingException != null

    fun exceptionClear() {
        pendingException = null
    }

    fun exceptionDescribe() {
        pendingException?.let { throwable ->
            exceptionReporter(describeThrowable(throwable))
            pendingException = null
        }
    }

    fun fatalError(message: String?): Nothing {
        throw JvmJniFatalError(message.orEmpty())
    }

    fun newLocalRef(handle: JvmJniHandleId?): JvmJniHandleId? =
        handle?.let { localHandle -> handles.newLocalReference(handles.snapshotLocalReference(localHandle)) }

    fun isSameObject(left: JvmJniHandleId?, right: JvmJniHandleId?): Boolean {
        if (left == null || right == null) {
            return left == right
        }
        return handles.resolveObject(left) == handles.resolveObject(right)
    }

    fun newGlobalRef(handle: JvmJniHandleId?): JvmJniHandleId? =
        handle?.let { localHandle -> handles.newGlobalObjectHandle(handles.resolveObject(localHandle)) }

    fun deleteGlobalRef(handle: JvmJniHandleId?) {
        if (handle != null) {
            handles.deleteGlobal(handle)
        }
    }

    fun newWeakGlobalRef(handle: JvmJniHandleId?): JvmJniHandleId? =
        handle?.let { localHandle -> handles.newWeakGlobalObjectHandle(handles.resolveObject(localHandle)) }

    fun deleteWeakGlobalRef(handle: JvmJniHandleId?) {
        if (handle != null) {
            handles.deleteWeakGlobal(handle)
        }
    }

    fun getObjectRefType(handle: JvmJniHandleId?): JvmJniReferenceType =
        handles.referenceType(handle)

    fun deleteLocalRef(handle: JvmJniHandleId?) {
        if (handle != null) {
            handles.deleteLocal(handle)
        }
    }

    fun ensureLocalCapacity(capacity: Int): Int {
        require(capacity >= 0) { "JNI local capacity must be non-negative: $capacity" }
        ensuredLocalCapacity = maxOf(ensuredLocalCapacity, capacity)
        return 0
    }

    fun pushLocalFrame(capacity: Int): Int {
        require(capacity > 0) { "JNI local frame capacity must be positive: $capacity" }
        localFrameCapacities += capacity
        handles.pushLocalFrame()
        maxLocalFrameCapacity = maxOf(maxLocalFrameCapacity, capacity)
        return 0
    }

    fun popLocalFrame(result: JvmJniHandleId?): JvmJniHandleId? {
        val reboundResult = result?.let(handles::snapshotLocalReference)
        if (localFrameCapacities.removeLastOrNull() == null) {
            throw JvmJniLocalFrameException("JNI local frame stack is empty")
        }
        handles.deleteCurrentLocalFrameHandles()
        return reboundResult?.let(handles::newLocalReference)
    }

    internal fun takePendingException(): JvmObjectReferenceValue? {
        val throwable = pendingException
        pendingException = null
        return throwable
    }

    private fun describeThrowable(throwable: JvmObjectReferenceValue): String {
        val className = heap.get(throwable).className
        val detailMessage = (heap.getInstanceField(throwable, throwableDetailMessageField) as? JvmObjectReferenceValue)
            ?.let { messageReference -> heap.get(messageReference).payload as? JvmStringPayload }
            ?.value
        return if (detailMessage == null) {
            className
        } else {
            "$className: $detailMessage"
        }
    }

    private fun requireThrowableClass(className: String): String {
        if (!classHierarchy.isAssignable(className, "java/lang/Throwable")) {
            throw JvmJniExceptionAccessException(
                "JNI exception helper requires java/lang/Throwable, got $className",
            )
        }
        return className
    }

    private fun requireLoadedClass(className: String): String {
        if (!classHierarchy.hasClass(className)) {
            throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = className,
            )
        }
        return className
    }

    private fun requireReceiverAssignableToMethod(
        helperName: String,
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
    ) {
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, method.ownerClassName)) {
            throw JvmJniMethodAccessException(
                "$helperName requires receiver $receiverClassName to be assignable to ${method.ownerClassName}",
            )
        }
    }

    private fun requireClassAssignableToStaticMethod(
        helperName: String,
        className: String,
        method: JvmResolvedMethod,
    ) {
        if (!classHierarchy.isAssignable(className, method.ownerClassName)) {
            throw JvmJniMethodAccessException(
                "$helperName requires class $className to be assignable to ${method.ownerClassName}",
            )
        }
    }

    fun findClass(className: String): JvmJniHandleId {
        if (!classHierarchy.hasClass(className)) {
            throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = className,
            )
        }
        return handles.newClassHandle(className)
    }

    fun getSuperclass(classHandle: JvmJniHandleId): JvmJniHandleId? {
        val className = handles.resolveClass(classHandle)
        val superclassName = classHierarchy.directSuperclassName(className) ?: return null
        return handles.newClassHandle(superclassName)
    }

    fun isAssignableFrom(sourceClassHandle: JvmJniHandleId, targetClassHandle: JvmJniHandleId): Boolean {
        val sourceClassName = handles.resolveClass(sourceClassHandle)
        val targetClassName = handles.resolveClass(targetClassHandle)
        return classHierarchy.isAssignable(sourceClassName = sourceClassName, targetClassName = targetClassName)
    }

    fun getStaticMethodId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        if (name == "<clinit>") {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        val method = classHierarchy.resolveMethod(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (!method.isStatic) {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newMethodIdHandle(method)
    }

    fun getMethodId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        if (name == "<clinit>") {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        val method = classHierarchy.resolveMethod(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (method.isStatic) {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newMethodIdHandle(method)
    }

    fun callVoidMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ) {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceVoidMethod("CallVoidMethod")
        requireReceiverAssignableToMethod("CallVoidMethod", receiver, method)
        upcallDispatcher.callVoidMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        )
    }

    fun callNonvirtualVoidMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ) {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualVoidMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceVoidMethod("CallNonvirtualVoidMethod", className)
        upcallDispatcher.callVoidMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        )
    }

    fun callObjectMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): JvmJniHandleId? {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceObjectMethod("CallObjectMethod")
        requireReceiverAssignableToMethod("CallObjectMethod", receiver, method)
        return when (
            val result = upcallDispatcher.callObjectMethod(
                receiver = receiver,
                method = method,
                arguments = arguments,
            )
        ) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(result)
        }
    }

    fun callNonvirtualObjectMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): JvmJniHandleId? {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualObjectMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceObjectMethod("CallNonvirtualObjectMethod", className)
        return when (
            val result = upcallDispatcher.callObjectMethod(
                receiver = receiver,
                method = method,
                arguments = arguments,
            )
        ) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(result)
        }
    }

    fun callBooleanMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Boolean {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceBooleanMethod("CallBooleanMethod")
        requireReceiverAssignableToMethod("CallBooleanMethod", receiver, method)
        return upcallDispatcher.callBooleanMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualBooleanMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Boolean {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualBooleanMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceBooleanMethod("CallNonvirtualBooleanMethod", className)
        return upcallDispatcher.callBooleanMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callByteMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceByteMethod("CallByteMethod")
        requireReceiverAssignableToMethod("CallByteMethod", receiver, method)
        return upcallDispatcher.callByteMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualByteMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualByteMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceByteMethod("CallNonvirtualByteMethod", className)
        return upcallDispatcher.callByteMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callCharMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceCharMethod("CallCharMethod")
        requireReceiverAssignableToMethod("CallCharMethod", receiver, method)
        return upcallDispatcher.callCharMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualCharMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualCharMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceCharMethod("CallNonvirtualCharMethod", className)
        return upcallDispatcher.callCharMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callShortMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceShortMethod("CallShortMethod")
        requireReceiverAssignableToMethod("CallShortMethod", receiver, method)
        return upcallDispatcher.callShortMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualShortMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualShortMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceShortMethod("CallNonvirtualShortMethod", className)
        return upcallDispatcher.callShortMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callIntMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceIntMethod("CallIntMethod")
        requireReceiverAssignableToMethod("CallIntMethod", receiver, method)
        return upcallDispatcher.callIntMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualIntMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualIntMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceIntMethod("CallNonvirtualIntMethod", className)
        return upcallDispatcher.callIntMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callLongMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Long {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceLongMethod("CallLongMethod")
        requireReceiverAssignableToMethod("CallLongMethod", receiver, method)
        return upcallDispatcher.callLongMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualLongMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Long {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualLongMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceLongMethod("CallNonvirtualLongMethod", className)
        return upcallDispatcher.callLongMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callFloatMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Float {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceFloatMethod("CallFloatMethod")
        requireReceiverAssignableToMethod("CallFloatMethod", receiver, method)
        return upcallDispatcher.callFloatMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualFloatMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Float {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualFloatMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceFloatMethod("CallNonvirtualFloatMethod", className)
        return upcallDispatcher.callFloatMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callDoubleMethod(
        objectHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Double {
        val receiver = handles.resolveObject(objectHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireInstanceDoubleMethod("CallDoubleMethod")
        requireReceiverAssignableToMethod("CallDoubleMethod", receiver, method)
        return upcallDispatcher.callDoubleMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callNonvirtualDoubleMethod(
        objectHandle: JvmJniHandleId,
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Double {
        val receiver = handles.resolveObject(objectHandle)
        val className = handles.resolveClass(classHandle)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, className)) {
            throw JvmJniMethodAccessException(
                "CallNonvirtualDoubleMethod requires receiver $receiverClassName to be assignable to $className",
            )
        }
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireNonvirtualInstanceDoubleMethod("CallNonvirtualDoubleMethod", className)
        return upcallDispatcher.callDoubleMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticVoidMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ) {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticVoidMethod("CallStaticVoidMethod")
        requireClassAssignableToStaticMethod("CallStaticVoidMethod", className, method)
        upcallDispatcher.callStaticVoidMethod(
            method = method,
            arguments = arguments,
        )
    }

    fun callStaticObjectMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): JvmJniHandleId? {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticObjectMethod("CallStaticObjectMethod")
        requireClassAssignableToStaticMethod("CallStaticObjectMethod", className, method)
        return when (
            val result = upcallDispatcher.callStaticObjectMethod(
                method = method,
                arguments = arguments,
            )
        ) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(result)
        }
    }

    fun callStaticBooleanMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Boolean {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticBooleanMethod("CallStaticBooleanMethod")
        requireClassAssignableToStaticMethod("CallStaticBooleanMethod", className, method)
        return upcallDispatcher.callStaticBooleanMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticByteMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticByteMethod("CallStaticByteMethod")
        requireClassAssignableToStaticMethod("CallStaticByteMethod", className, method)
        return upcallDispatcher.callStaticByteMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticCharMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticCharMethod("CallStaticCharMethod")
        requireClassAssignableToStaticMethod("CallStaticCharMethod", className, method)
        return upcallDispatcher.callStaticCharMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticShortMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticShortMethod("CallStaticShortMethod")
        requireClassAssignableToStaticMethod("CallStaticShortMethod", className, method)
        return upcallDispatcher.callStaticShortMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticIntMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Int {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticIntMethod("CallStaticIntMethod")
        requireClassAssignableToStaticMethod("CallStaticIntMethod", className, method)
        return upcallDispatcher.callStaticIntMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticLongMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Long {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticLongMethod("CallStaticLongMethod")
        requireClassAssignableToStaticMethod("CallStaticLongMethod", className, method)
        return upcallDispatcher.callStaticLongMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticFloatMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Float {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticFloatMethod("CallStaticFloatMethod")
        requireClassAssignableToStaticMethod("CallStaticFloatMethod", className, method)
        return upcallDispatcher.callStaticFloatMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun callStaticDoubleMethod(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): Double {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireStaticDoubleMethod("CallStaticDoubleMethod")
        requireClassAssignableToStaticMethod("CallStaticDoubleMethod", className, method)
        return upcallDispatcher.callStaticDoubleMethod(
            method = method,
            arguments = arguments,
        ).value
    }

    fun allocObject(classHandle: JvmJniHandleId): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val receiver = heap.allocateUninitializedObject(className)
        return handles.newObjectHandle(receiver)
    }

    fun newObject(
        classHandle: JvmJniHandleId,
        methodIdHandle: JvmJniHandleId,
        arguments: List<JvmValue> = emptyList(),
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val method = handles.resolveMethodId(methodIdHandle)
        method.requireConstructorMethod("NewObject", className)
        val receiver = heap.allocateUninitializedObject(className)
        upcallDispatcher.callVoidMethod(
            receiver = receiver,
            method = method,
            arguments = arguments,
        )
        heap.markInitialized(receiver)
        return handles.newObjectHandle(receiver)
    }

    fun getObjectClass(objectHandle: JvmJniHandleId): JvmJniHandleId {
        val reference = handles.resolveObject(objectHandle)
        val className = requireLoadedClass(heap.get(reference).className)
        return handles.newClassHandle(className)
    }

    fun isInstanceOf(objectHandle: JvmJniHandleId?, classHandle: JvmJniHandleId): Boolean {
        if (objectHandle == null) {
            return true
        }
        val reference = handles.resolveObject(objectHandle)
        val sourceClassName = heap.get(reference).className
        requireLoadedClass(sourceClassName)
        val targetClassName = handles.resolveClass(classHandle)
        return classHierarchy.isAssignable(sourceClassName = sourceClassName, targetClassName = targetClassName)
    }

    fun getFieldId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val field = classHierarchy.resolveField(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (field.isStatic) {
            throw JvmNoSuchFieldError(
                guestClassName = "java/lang/NoSuchFieldError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newFieldIdHandle(field)
    }

    fun getStaticFieldId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val field = classHierarchy.resolveField(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (!field.isStatic) {
            throw JvmNoSuchFieldError(
                guestClassName = "java/lang/NoSuchFieldError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newFieldIdHandle(field)
    }

    fun newString(chars: CharArray, length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateString(chars.concatToString(startIndex = 0, endIndex = length)))

    fun newStringUtf(value: String): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateString(value))

    fun getStringLength(stringHandle: JvmJniHandleId): Int =
        resolveStringValue(stringHandle).length

    fun getStringChars(stringHandle: JvmJniHandleId): CharArray =
        resolveStringValue(stringHandle).toCharArray()

    fun getStringCritical(stringHandle: JvmJniHandleId): CharArray =
        getStringChars(stringHandle)

    fun releaseStringChars(stringHandle: JvmJniHandleId, chars: CharArray) {
        resolveStringValue(stringHandle)
    }

    fun releaseStringCritical(stringHandle: JvmJniHandleId, chars: CharArray) {
        resolveStringValue(stringHandle)
    }

    fun getStringRegion(stringHandle: JvmJniHandleId, start: Int, length: Int): CharArray {
        val value = resolveStringValue(stringHandle)
        requireStringRange("GetStringRegion", value.length, start, length)
        return value.toCharArray(startIndex = start, endIndex = start + length)
    }

    fun getStringUtfLength(stringHandle: JvmJniHandleId): Int =
        getStringUtfChars(stringHandle).size

    fun getStringUtfChars(stringHandle: JvmJniHandleId): ByteArray =
        encodeModifiedUtf8(resolveStringValue(stringHandle))

    fun getStringUtfRegion(stringHandle: JvmJniHandleId, start: Int, length: Int): ByteArray {
        val value = resolveStringValue(stringHandle)
        requireStringRange("GetStringUTFRegion", value.length, start, length)
        return encodeModifiedUtf8(value.substring(startIndex = start, endIndex = start + length))
    }

    fun releaseStringUtfChars(stringHandle: JvmJniHandleId, chars: ByteArray) {
        resolveStringValue(stringHandle)
    }

    fun monitorEnter(objectHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        return monitors.enter(reference, currentThreadId)
    }

    fun monitorExit(objectHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        return monitors.exit(reference, currentThreadId)
    }

    fun getArrayLength(arrayHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return when (val payload = heapObject.payload) {
            is JvmBooleanArrayPayload -> payload.elements.size
            is JvmByteArrayPayload -> payload.elements.size
            is JvmCharArrayPayload -> payload.elements.size
            is JvmDoubleArrayPayload -> payload.elements.size
            is JvmFloatArrayPayload -> payload.elements.size
            is JvmIntArrayPayload -> payload.elements.size
            is JvmLongArrayPayload -> payload.elements.size
            is JvmReferenceArrayPayload -> payload.elements.size
            is JvmShortArrayPayload -> payload.elements.size
            else -> throw JvmJniArrayAccessException(
                "JNI array helper requires array payload, got ${heapObject.className}",
            )
        }
    }

    fun getPrimitiveArrayCritical(arrayHandle: JvmJniHandleId): JvmJniPrimitiveArrayCritical {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return when (val payload = heapObject.payload) {
            is JvmBooleanArrayPayload -> JvmJniPrimitiveArrayCritical.Booleans(payload.elements.toBooleanArray())
            is JvmByteArrayPayload -> JvmJniPrimitiveArrayCritical.Bytes(payload.elements.toByteArray())
            is JvmCharArrayPayload -> JvmJniPrimitiveArrayCritical.Chars(payload.elements.toCharArray())
            is JvmDoubleArrayPayload -> JvmJniPrimitiveArrayCritical.Doubles(payload.elements.toDoubleArray())
            is JvmFloatArrayPayload -> JvmJniPrimitiveArrayCritical.Floats(payload.elements.toFloatArray())
            is JvmIntArrayPayload -> JvmJniPrimitiveArrayCritical.Ints(payload.elements.toIntArray())
            is JvmLongArrayPayload -> JvmJniPrimitiveArrayCritical.Longs(payload.elements.toLongArray())
            is JvmShortArrayPayload -> JvmJniPrimitiveArrayCritical.Shorts(payload.elements.toShortArray())
            else -> throw JvmJniArrayAccessException(
                "JNI primitive array critical helper requires primitive array payload, got ${heapObject.className}",
            )
        }
    }

    fun releasePrimitiveArrayCritical(
        arrayHandle: JvmJniHandleId,
        critical: JvmJniPrimitiveArrayCritical,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        when (critical) {
            is JvmJniPrimitiveArrayCritical.Booleans ->
                releaseBooleanArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Bytes ->
                releaseByteArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Chars ->
                releaseCharArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Doubles ->
                releaseDoubleArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Floats ->
                releaseFloatArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Ints ->
                releaseIntArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Longs ->
                releaseLongArrayElements(arrayHandle, critical.elements, mode)
            is JvmJniPrimitiveArrayCritical.Shorts ->
                releaseShortArrayElements(arrayHandle, critical.elements, mode)
        }
    }

    fun newDirectByteBuffer(address: Long, capacity: Long): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateDirectByteBuffer(address = address, capacity = capacity))

    fun getDirectBufferAddress(bufferHandle: JvmJniHandleId): Long =
        resolveDirectByteBuffer(bufferHandle).address

    fun getDirectBufferCapacity(bufferHandle: JvmJniHandleId): Long =
        resolveDirectByteBuffer(bufferHandle).capacity

    fun newBooleanArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateBooleanArray(length))

    fun getBooleanArrayElements(arrayHandle: JvmJniHandleId): BooleanArray =
        resolveBooleanArray(arrayHandle).elements.toBooleanArray()

    fun releaseBooleanArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: BooleanArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveBooleanArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getBooleanArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): BooleanArray {
        val array = resolveBooleanArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toBooleanArray()
    }

    fun setBooleanArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: BooleanArray) {
        val array = resolveBooleanArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newByteArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateByteArray(length))

    fun getByteArrayElements(arrayHandle: JvmJniHandleId): ByteArray =
        resolveByteArray(arrayHandle).elements.toByteArray()

    fun releaseByteArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: ByteArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveByteArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getByteArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): ByteArray {
        val array = resolveByteArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toByteArray()
    }

    fun setByteArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: ByteArray) {
        val array = resolveByteArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newCharArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateCharArray(length))

    fun getCharArrayElements(arrayHandle: JvmJniHandleId): CharArray =
        resolveCharArray(arrayHandle).elements.toCharArray()

    fun releaseCharArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: CharArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveCharArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getCharArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): CharArray {
        val array = resolveCharArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toCharArray()
    }

    fun setCharArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: CharArray) {
        val array = resolveCharArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newShortArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateShortArray(length))

    fun getShortArrayElements(arrayHandle: JvmJniHandleId): ShortArray =
        resolveShortArray(arrayHandle).elements.toShortArray()

    fun releaseShortArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: ShortArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveShortArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getShortArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): ShortArray {
        val array = resolveShortArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toShortArray()
    }

    fun setShortArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: ShortArray) {
        val array = resolveShortArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newIntArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateIntArray(length))

    fun getIntArrayElements(arrayHandle: JvmJniHandleId): IntArray =
        resolveIntArray(arrayHandle).elements.toIntArray()

    fun releaseIntArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: IntArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveIntArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getIntArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): IntArray {
        val array = resolveIntArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toIntArray()
    }

    fun setIntArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: IntArray) {
        val array = resolveIntArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newLongArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateLongArray(length))

    fun getLongArrayElements(arrayHandle: JvmJniHandleId): LongArray =
        resolveLongArray(arrayHandle).elements.toLongArray()

    fun releaseLongArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: LongArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveLongArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getLongArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): LongArray {
        val array = resolveLongArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toLongArray()
    }

    fun setLongArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: LongArray) {
        val array = resolveLongArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newFloatArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateFloatArray(length))

    fun getFloatArrayElements(arrayHandle: JvmJniHandleId): FloatArray =
        resolveFloatArray(arrayHandle).elements.toFloatArray()

    fun releaseFloatArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: FloatArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveFloatArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getFloatArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): FloatArray {
        val array = resolveFloatArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toFloatArray()
    }

    fun setFloatArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: FloatArray) {
        val array = resolveFloatArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newDoubleArray(length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateDoubleArray(length))

    fun getDoubleArrayElements(arrayHandle: JvmJniHandleId): DoubleArray =
        resolveDoubleArray(arrayHandle).elements.toDoubleArray()

    fun releaseDoubleArrayElements(
        arrayHandle: JvmJniHandleId,
        elements: DoubleArray,
        mode: JvmJniArrayReleaseMode = JvmJniArrayReleaseMode.CopyBackAndRelease,
    ) {
        val array = resolveDoubleArray(arrayHandle)
        requireArrayElementsBufferSize(array.elements.size, elements.size)
        if (mode != JvmJniArrayReleaseMode.Abort) {
            elements.forEachIndexed { index, value ->
                array.elements[index] = value
            }
        }
    }

    fun getDoubleArrayRegion(arrayHandle: JvmJniHandleId, start: Int, length: Int): DoubleArray {
        val array = resolveDoubleArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).toDoubleArray()
    }

    fun setDoubleArrayRegion(arrayHandle: JvmJniHandleId, start: Int, values: DoubleArray) {
        val array = resolveDoubleArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, value ->
            array.elements[start + offset] = value
        }
    }

    fun newObjectArray(
        length: Int,
        elementClassHandle: JvmJniHandleId,
        initialElementHandle: JvmJniHandleId?,
    ): JvmJniHandleId {
        val elementClassName = handles.resolveClass(elementClassHandle)
        val initialElement = initialElementHandle?.let { handle ->
            val reference = handles.resolveObject(handle)
            val elementClass = heap.get(reference).className
            if (!classHierarchy.isAssignable(sourceClassName = elementClass, targetClassName = elementClassName)) {
                throw JvmJniArrayAccessException(
                    "NewObjectArray initial element $elementClass is not assignable to $elementClassName",
                )
            }
            reference
        }
        val arrayReference = heap.allocateReferenceArray(elementClassName, length)
        if (initialElement != null) {
            val array = heap.get(arrayReference).payload as JvmReferenceArrayPayload
            array.elements.indices.forEach { index ->
                array.elements[index] = initialElement
            }
        }
        return handles.newObjectHandle(arrayReference)
    }

    fun getObjectArrayElement(arrayHandle: JvmJniHandleId, index: Int): JvmJniHandleId? {
        val array = resolveReferenceArray(arrayHandle)
        val element = array.elements.getOrNull(index)
            ?: throw JvmJniArrayAccessException("JNI array index $index is out of bounds")
        return when (element) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(element)
        }
    }

    fun setObjectArrayElement(arrayHandle: JvmJniHandleId, index: Int, valueHandle: JvmJniHandleId?) {
        val arrayReference = handles.resolveObject(arrayHandle)
        val arrayObject = heap.get(arrayReference)
        val array = arrayObject.payload as? JvmReferenceArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI object array helper requires reference array payload, got ${arrayObject.className}",
            )
        if (index !in array.elements.indices) {
            throw JvmJniArrayAccessException("JNI array index $index is out of bounds")
        }
        val value = valueHandle?.let { handle ->
            val reference = handles.resolveObject(handle)
            val valueClassName = heap.get(reference).className
            val componentClassName = arrayObject.className.referenceArrayComponentClassName()
            if (!classHierarchy.isAssignable(sourceClassName = valueClassName, targetClassName = componentClassName)) {
                throw JvmJniArrayAccessException(
                    "SetObjectArrayElement value $valueClassName is not assignable to $componentClassName",
                )
            }
            reference
        } ?: JvmNullValue
        array.elements[index] = value
    }

    fun getObjectArrayRegion(
        arrayHandle: JvmJniHandleId,
        start: Int,
        length: Int,
    ): List<JvmJniHandleId?> {
        val array = resolveReferenceArray(arrayHandle)
        requireArrayRange(array.elements.size, start, length)
        return array.elements.subList(start, start + length).map { element ->
            when (element) {
                JvmNullValue -> null
                is JvmObjectReferenceValue -> handles.newObjectHandle(element)
            }
        }
    }

    fun setObjectArrayRegion(
        arrayHandle: JvmJniHandleId,
        start: Int,
        values: List<JvmJniHandleId?>,
    ) {
        val array = resolveReferenceArray(arrayHandle)
        requireArrayRange(array.elements.size, start, values.size)
        values.forEachIndexed { offset, valueHandle ->
            setObjectArrayElement(arrayHandle, start + offset, valueHandle)
        }
    }

    fun getIntField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetIntField requires an instance int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmIntValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetIntField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setIntField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetIntField requires an instance int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmIntValue(value),
        )
    }

    fun getLongField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Long {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetLongField requires an instance long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmLongValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetLongField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setLongField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Long) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetLongField requires an instance long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmLongValue(value),
        )
    }

    fun getFloatField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Float {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetFloatField requires an instance float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmFloatValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetFloatField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setFloatField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Float) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetFloatField requires an instance float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmFloatValue(value),
        )
    }

    fun getDoubleField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Double {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetDoubleField requires an instance double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmDoubleValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetDoubleField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setDoubleField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Double) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetDoubleField requires an instance double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmDoubleValue(value),
        )
    }

    fun getBooleanField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Boolean {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetBooleanField requires an instance boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmBooleanValue -> value.value
            is JvmIntValue -> value.value != 0
            else -> throw JvmJniFieldAccessException(
                "GetBooleanField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setBooleanField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Boolean) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetBooleanField requires an instance boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmBooleanValue(value),
        )
    }

    fun getByteField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetByteField requires an instance byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmByteValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetByteField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setByteField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetByteField requires an instance byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmByteValue(value),
        )
    }

    fun getCharField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetCharField requires an instance char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmCharValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetCharField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setCharField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetCharField requires an instance char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmCharValue(value),
        )
    }

    fun getShortField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetShortField requires an instance short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmShortValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetShortField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun getStaticIntField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticIntField requires a static int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmIntValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticIntField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticIntField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticIntField requires a static int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmIntValue(value),
        )
    }

    fun getStaticLongField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Long {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticLongField requires a static long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmLongValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticLongField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticLongField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Long) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticLongField requires a static long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmLongValue(value),
        )
    }

    fun getStaticFloatField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Float {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticFloatField requires a static float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmFloatValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticFloatField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticFloatField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Float) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticFloatField requires a static float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmFloatValue(value),
        )
    }

    fun getStaticDoubleField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Double {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticDoubleField requires a static double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmDoubleValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticDoubleField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticDoubleField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Double) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticDoubleField requires a static double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmDoubleValue(value),
        )
    }

    fun getStaticBooleanField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Boolean {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticBooleanField requires a static boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmBooleanValue -> value.value
            is JvmIntValue -> value.value != 0
            else -> throw JvmJniFieldAccessException(
                "GetStaticBooleanField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticBooleanField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Boolean) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticBooleanField requires a static boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmBooleanValue(value),
        )
    }

    fun getStaticObjectField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): JvmJniHandleId? {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticObjectField requires a static reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(value)
            else -> throw JvmJniFieldAccessException(
                "GetStaticObjectField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticObjectField(
        classHandle: JvmJniHandleId,
        fieldIdHandle: JvmJniHandleId,
        valueHandle: JvmJniHandleId?,
    ) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticObjectField requires a static reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = if (valueHandle == null) {
            JvmNullValue
        } else {
            handles.resolveObject(valueHandle)
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            value,
        )
    }

    fun getStaticByteField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticByteField requires a static byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmByteValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticByteField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticByteField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticByteField requires a static byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmByteValue(value),
        )
    }

    fun getStaticCharField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticCharField requires a static char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmCharValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticCharField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticCharField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticCharField requires a static char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmCharValue(value),
        )
    }

    fun getStaticShortField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticShortField requires a static short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmShortValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticShortField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticShortField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticShortField requires a static short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmShortValue(value),
        )
    }

    fun setShortField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetShortField requires an instance short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmShortValue(value),
        )
    }

    fun getObjectField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): JvmJniHandleId? {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetObjectField requires an instance reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(value)
            else -> throw JvmJniFieldAccessException(
                "GetObjectField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setObjectField(
        objectHandle: JvmJniHandleId,
        fieldIdHandle: JvmJniHandleId,
        valueHandle: JvmJniHandleId?,
    ) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetObjectField requires an instance reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = if (valueHandle == null) {
            JvmNullValue
        } else {
            handles.resolveObject(valueHandle)
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            value,
        )
    }

    private fun requireArrayRange(arrayLength: Int, start: Int, length: Int) {
        if (start < 0 || length < 0 || start > arrayLength - length) {
            throw JvmJniArrayAccessException(
                "JNI array range start=$start length=$length is out of bounds for length $arrayLength",
            )
        }
    }

    private fun requireArrayElementsBufferSize(arrayLength: Int, bufferLength: Int) {
        if (bufferLength != arrayLength) {
            throw JvmJniArrayAccessException(
                "JNI array elements buffer length $bufferLength does not match array length $arrayLength",
            )
        }
    }

    private fun resolveReferenceArray(arrayHandle: JvmJniHandleId): JvmReferenceArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmReferenceArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI object array helper requires reference array payload, got ${heapObject.className}",
            )
    }

    private fun resolveBooleanArray(arrayHandle: JvmJniHandleId): JvmBooleanArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmBooleanArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI boolean array helper requires boolean array payload, got ${heapObject.className}",
            )
    }

    private fun resolveByteArray(arrayHandle: JvmJniHandleId): JvmByteArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmByteArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI byte array helper requires byte array payload, got ${heapObject.className}",
            )
    }

    private fun resolveCharArray(arrayHandle: JvmJniHandleId): JvmCharArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmCharArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI char array helper requires char array payload, got ${heapObject.className}",
            )
    }

    private fun resolveShortArray(arrayHandle: JvmJniHandleId): JvmShortArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmShortArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI short array helper requires short array payload, got ${heapObject.className}",
            )
    }

    private fun resolveIntArray(arrayHandle: JvmJniHandleId): JvmIntArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmIntArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI int array helper requires int array payload, got ${heapObject.className}",
            )
    }

    private fun resolveLongArray(arrayHandle: JvmJniHandleId): JvmLongArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmLongArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI long array helper requires long array payload, got ${heapObject.className}",
            )
    }

    private fun resolveFloatArray(arrayHandle: JvmJniHandleId): JvmFloatArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmFloatArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI float array helper requires float array payload, got ${heapObject.className}",
            )
    }

    private fun resolveDoubleArray(arrayHandle: JvmJniHandleId): JvmDoubleArrayPayload {
        val reference = handles.resolveObject(arrayHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmDoubleArrayPayload
            ?: throw JvmJniArrayAccessException(
                "JNI double array helper requires double array payload, got ${heapObject.className}",
            )
    }

    private fun encodeModifiedUtf8(value: String): ByteArray {
        val bytes = mutableListOf<Byte>()
        value.forEach { codeUnit ->
            val code = codeUnit.code
            when (code) {
                0 -> {
                    bytes += 0xc0.toByte()
                    bytes += 0x80.toByte()
                }
                in 1..0x7f -> bytes += code.toByte()
                in 0x80..0x7ff -> {
                    bytes += (0xc0 or (code shr 6)).toByte()
                    bytes += (0x80 or (code and 0x3f)).toByte()
                }
                else -> {
                    bytes += (0xe0 or (code shr 12)).toByte()
                    bytes += (0x80 or ((code shr 6) and 0x3f)).toByte()
                    bytes += (0x80 or (code and 0x3f)).toByte()
                }
            }
        }
        return bytes.toByteArray()
    }

    private fun resolveStringValue(stringHandle: JvmJniHandleId): String {
        val reference = handles.resolveObject(stringHandle)
        val heapObject = heap.get(reference)
        val payload = heapObject.payload as? JvmStringPayload
            ?: throw JvmJniStringAccessException(
                "JNI string helper requires java/lang/String payload, got ${heapObject.className}",
            )
        return payload.value
    }

    private fun requireStringRange(helperName: String, stringLength: Int, start: Int, length: Int) {
        if (start < 0 || length < 0 || start > stringLength - length) {
            throw JvmJniStringAccessException(
                "$helperName range $start..${start + length} is outside string length $stringLength",
            )
        }
    }

    private fun resolveDirectByteBuffer(bufferHandle: JvmJniHandleId): JvmDirectByteBufferPayload {
        val reference = handles.resolveObject(bufferHandle)
        val heapObject = heap.get(reference)
        return heapObject.payload as? JvmDirectByteBufferPayload
            ?: throw JvmJniDirectBufferAccessException(
                "JNI direct buffer helper requires java/nio/DirectByteBuffer payload, got ${heapObject.className}",
            )
    }
}

private fun String.referenceArrayComponentClassName(): String =
    when {
        startsWith("[L") && endsWith(";") -> substring(startIndex = 2, endIndex = length - 1)
        startsWith("[[") -> substring(startIndex = 1)
        else -> throw JvmJniArrayAccessException("Not a reference array class name: $this")
    }

class JvmJniFieldAccessException(message: String) : IllegalStateException(message)

class JvmJniMethodAccessException(message: String) : IllegalStateException(message)

class JvmJniStringAccessException(message: String) : IllegalStateException(message)

class JvmJniArrayAccessException(message: String) : IllegalStateException(message)

class JvmJniDirectBufferAccessException(message: String) : IllegalStateException(message)

class JvmJniExceptionAccessException(message: String) : IllegalStateException(message)

class JvmJniLocalFrameException(message: String) : IllegalStateException(message)

class JvmJniFatalError(message: String) : Error(message)

enum class JvmJniArrayReleaseMode {
    CopyBackAndRelease,
    Commit,
    Abort,
}

sealed interface JvmJniPrimitiveArrayCritical {
    data class Booleans(val elements: BooleanArray) : JvmJniPrimitiveArrayCritical
    data class Bytes(val elements: ByteArray) : JvmJniPrimitiveArrayCritical
    data class Chars(val elements: CharArray) : JvmJniPrimitiveArrayCritical
    data class Shorts(val elements: ShortArray) : JvmJniPrimitiveArrayCritical
    data class Ints(val elements: IntArray) : JvmJniPrimitiveArrayCritical
    data class Longs(val elements: LongArray) : JvmJniPrimitiveArrayCritical
    data class Floats(val elements: FloatArray) : JvmJniPrimitiveArrayCritical
    data class Doubles(val elements: DoubleArray) : JvmJniPrimitiveArrayCritical
}

private fun String.isReferenceFieldDescriptor(): Boolean =
    startsWith("L") || startsWith("[")

private fun JvmResolvedMethod.requireInstanceVoidMethod(helperName: String) {
    if (isStatic || !descriptor.endsWith("V")) {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance void method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceVoidMethod(helperName: String, className: String) {
    requireInstanceVoidMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireConstructorMethod(helperName: String, className: String) {
    if (ownerClassName != className || name != "<init>" || isStatic || returnDescriptor != "V") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance constructor for $className, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceObjectMethod(helperName: String) {
    if (isStatic || !returnDescriptor.isReferenceFieldDescriptor()) {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance object method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceObjectMethod(helperName: String, className: String) {
    requireInstanceObjectMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceBooleanMethod(helperName: String) {
    if (isStatic || returnDescriptor != "Z") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance boolean method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceBooleanMethod(helperName: String, className: String) {
    requireInstanceBooleanMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceByteMethod(helperName: String) {
    if (isStatic || returnDescriptor != "B") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance byte method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceByteMethod(helperName: String, className: String) {
    requireInstanceByteMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceCharMethod(helperName: String) {
    if (isStatic || returnDescriptor != "C") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance char method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceCharMethod(helperName: String, className: String) {
    requireInstanceCharMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceShortMethod(helperName: String) {
    if (isStatic || returnDescriptor != "S") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance short method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceShortMethod(helperName: String, className: String) {
    requireInstanceShortMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceIntMethod(helperName: String) {
    if (isStatic || returnDescriptor != "I") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance int method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceIntMethod(helperName: String, className: String) {
    requireInstanceIntMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceLongMethod(helperName: String) {
    if (isStatic || returnDescriptor != "J") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance long method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceLongMethod(helperName: String, className: String) {
    requireInstanceLongMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceFloatMethod(helperName: String) {
    if (isStatic || returnDescriptor != "F") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance float method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceFloatMethod(helperName: String, className: String) {
    requireInstanceFloatMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireInstanceDoubleMethod(helperName: String) {
    if (isStatic || returnDescriptor != "D") {
        throw JvmJniMethodAccessException(
            "$helperName requires an instance double method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireNonvirtualInstanceDoubleMethod(helperName: String, className: String) {
    requireInstanceDoubleMethod(helperName)
    if (ownerClassName != className) {
        throw JvmJniMethodAccessException(
            "$helperName requires method owner $ownerClassName to match declaring class $className",
        )
    }
}

private fun JvmResolvedMethod.requireStaticVoidMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "V") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static void method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticObjectMethod(helperName: String) {
    if (!isStatic || !returnDescriptor.isReferenceFieldDescriptor()) {
        throw JvmJniMethodAccessException(
            "$helperName requires a static reference-returning method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticBooleanMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "Z") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static boolean method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticByteMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "B") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static byte method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticCharMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "C") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static char method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticShortMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "S") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static short method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticIntMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "I") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static int method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticLongMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "J") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static long method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticFloatMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "F") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static float method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private fun JvmResolvedMethod.requireStaticDoubleMethod(helperName: String) {
    if (!isStatic || returnDescriptor != "D") {
        throw JvmJniMethodAccessException(
            "$helperName requires a static double method, got $ownerClassName.$name:$descriptor",
        )
    }
}

private val JvmResolvedMethod.returnDescriptor: String
    get() = descriptor.substringAfter(')')
