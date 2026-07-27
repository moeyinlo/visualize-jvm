package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.jni.JvmNativeDowncallInvoker
import me.moeyinlo.visualize.jvm.jni.JvmNativeGuestMethodSignature
import me.moeyinlo.visualize.jvm.jni.JvmNativeLibraryRegistry
import me.moeyinlo.visualize.jvm.jni.JvmSimulatedJniEnvironment
import me.moeyinlo.visualize.jvm.jni.prepareInstanceInvocation
import me.moeyinlo.visualize.jvm.jni.prepareStaticInvocation
import me.moeyinlo.visualize.jvm.jni.toGuestValue
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmThreadScheduler
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import me.moeyinlo.visualize.jvm.runtime.JvmValue
import me.moeyinlo.visualize.jvm.runtime.JvmVmTerminationState

data class JvmNativeMethodKey(
    val ownerClassName: String,
    val name: String,
    val descriptor: String,
    val isStatic: Boolean,
) {
    companion object {
        fun from(method: JvmResolvedMethod): JvmNativeMethodKey =
            JvmNativeMethodKey(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.signaturePolymorphicDeclarationDescriptor ?: method.descriptor,
                isStatic = method.isStatic,
            )
    }
}

data class JvmNativeMethodInvocation(
    val receiver: JvmObjectReferenceValue?,
    val arguments: List<JvmValue>,
)

data class JvmNativeMethodContext(
    val heap: JvmHeap,
    val classHierarchy: JvmClassHierarchy,
    val staticFields: JvmStaticFields,
    val currentClassName: String?,
    val monitors: JvmMonitorState = JvmMonitorState(),
    val threadScheduler: JvmThreadScheduler? = null,
    val currentThreadId: String = "main",
    val terminationState: JvmVmTerminationState = JvmVmTerminationState(),
    val currentTimeMillisProvider: () -> Long = System::currentTimeMillis,
    val nanoTimeProvider: () -> Long = System::nanoTime,
    val stackTraceProvider: () -> List<JvmStackTraceFrame> = { emptyList() },
    val threadSleepHandler: (millis: Long, nanos: Int) -> Unit = { _, _ -> },
    val loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
        throw JvmUnsupportedInstructionException(
            "Native method context cannot load native library $logicalName",
        )
    },
    val unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
        throw JvmUnsupportedInstructionException(
            "Native method context cannot unload native library $logicalName",
        )
    },
    internal val callStaticMethodHandler: (
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
    ) -> JvmValue? = { ownerClassName, name, descriptor, _ ->
        throw JvmUnsupportedInstructionException(
            "Native method context cannot upcall static method $ownerClassName.$name:$descriptor",
        )
    },
    internal val callInstanceMethodHandler: (
        receiver: JvmObjectReferenceValue,
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
    ) -> JvmValue? = { _, ownerClassName, name, descriptor, _ ->
        throw JvmUnsupportedInstructionException(
            "Native method context cannot upcall instance method $ownerClassName.$name:$descriptor",
        )
    },
) {
    fun callStaticMethod(
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
    ): JvmValue? =
        callStaticMethodHandler(ownerClassName, name, descriptor, arguments)

    fun callInstanceMethod(
        receiver: JvmObjectReferenceValue,
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
    ): JvmValue? =
        callInstanceMethodHandler(receiver, ownerClassName, name, descriptor, arguments)
}

fun interface JvmNativeMethodIntrinsic {
    fun invoke(
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): JvmValue?
}

private typealias DynamicNativeMethodResolver = (JvmNativeMethodKey) -> JvmNativeMethodIntrinsic?

class JvmNativeMethodRegistry(
    private val intrinsics: Map<JvmNativeMethodKey, JvmNativeMethodIntrinsic> = emptyMap(),
    private val simulatedJni: Map<JvmNativeMethodKey, JvmNativeMethodIntrinsic> = emptyMap(),
    private val dynamicSimulatedJniResolvers: List<DynamicNativeMethodResolver> = emptyList(),
    private val intrinsicOwnerWhitelist: Set<String>? = null,
) {
    fun resolve(method: JvmResolvedMethod): JvmNativeMethodIntrinsic? =
        JvmNativeMethodKey.from(method).let { key ->
            key.intrinsicWhenWhitelisted()
                ?: simulatedJni[key]
                ?: key.resolveDynamicSimulatedJni()
        }

    private fun JvmNativeMethodKey.intrinsicWhenWhitelisted(): JvmNativeMethodIntrinsic? {
        if (intrinsicOwnerWhitelist != null && ownerClassName !in intrinsicOwnerWhitelist) {
            return null
        }
        return intrinsics[this]
    }

    private fun JvmNativeMethodKey.resolveDynamicSimulatedJni(): JvmNativeMethodIntrinsic? =
        dynamicSimulatedJniResolvers.firstNotNullOfOrNull { resolver -> resolver(this) }

    companion object {
        val Empty: JvmNativeMethodRegistry = JvmNativeMethodRegistry()

        fun from(vararg entries: Pair<JvmNativeMethodKey, JvmNativeMethodIntrinsic>): JvmNativeMethodRegistry =
            JvmNativeMethodRegistry(entries.toMap())

        fun fromSimulatedJni(
            vararg entries: Pair<JvmNativeMethodKey, JvmNativeMethodIntrinsic>,
        ): JvmNativeMethodRegistry =
            JvmNativeMethodRegistry(simulatedJni = entries.toMap())

        fun fromLoadedNativeLibraries(
            loadedLibraries: JvmNativeLibraryRegistry,
            environment: JvmSimulatedJniEnvironment,
            invokeDowncall: JvmNativeDowncallInvoker,
        ): JvmNativeMethodRegistry {
            val loadedLibraryResolver: DynamicNativeMethodResolver = { key ->
                val signature = JvmNativeGuestMethodSignature(
                    ownerClassName = key.ownerClassName,
                    methodName = key.name,
                    methodDescriptor = key.descriptor,
                    isStatic = key.isStatic,
                )
                val target = loadedLibraries.resolveExport(signature)
                    ?: loadedLibraries.loadedLibraries()
                        .asSequence()
                        .mapNotNull { loaded ->
                            environment.registeredNativeMethods.resolveDowncallTarget(
                                library = loaded.library,
                                className = key.ownerClassName,
                                name = key.name,
                                descriptor = key.descriptor,
                                isStatic = key.isStatic,
                            )
                        }
                        .firstOrNull()
                target?.let { resolvedTarget ->
                    JvmNativeMethodIntrinsic { _, invocation ->
                        environment.pushLocalFrame(NativeInvocationLocalCapacity)
                        try {
                            val downcallInvocation = if (key.isStatic) {
                                val classHandle = environment.handles.newClassHandle(key.ownerClassName)
                                resolvedTarget.prepareStaticInvocation(
                                    environment = environment,
                                    classHandle = classHandle,
                                    guestArguments = invocation.arguments,
                                )
                            } else {
                                val receiver = invocation.receiver
                                    ?: throw JvmUnsupportedInstructionException(
                                        "Loaded native instance export ${key.ownerClassName}.${key.name}:" +
                                            "${key.descriptor} requires a receiver",
                                    )
                                resolvedTarget.prepareInstanceInvocation(
                                    environment = environment,
                                    receiver = receiver,
                                    guestArguments = invocation.arguments,
                                )
                            }
                            return@JvmNativeMethodIntrinsic invokeDowncall.invoke(downcallInvocation).toGuestValue(environment)
                        } finally {
                            environment.popLocalFrame(null)
                        }
                    }
                }
            }
            return JvmNativeMethodRegistry(
                dynamicSimulatedJniResolvers = listOf(loadedLibraryResolver),
            )
        }

        private const val NativeInvocationLocalCapacity: Int = 16
    }
}

object JvmVmIntrinsics {
    private val ObjectGetClassKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "getClass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
    )
    private val ObjectHashCodeKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "hashCode",
        descriptor = "()I",
        isStatic = false,
    )
    private val ObjectCloneKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "clone",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
    )
    private val ObjectWaitKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "()V",
        isStatic = false,
    )
    private val ObjectWaitLongKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "(J)V",
        isStatic = false,
    )
    private val ObjectWaitLongIntKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "wait",
        descriptor = "(JI)V",
        isStatic = false,
    )
    private val ObjectNotifyKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "notify",
        descriptor = "()V",
        isStatic = false,
    )
    private val ObjectNotifyAllKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Object",
        name = "notifyAll",
        descriptor = "()V",
        isStatic = false,
    )
    private val SystemArraycopyKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "arraycopy",
        descriptor = "(Ljava/lang/Object;ILjava/lang/Object;II)V",
        isStatic = true,
    )
    private val SystemIdentityHashCodeKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "identityHashCode",
        descriptor = "(Ljava/lang/Object;)I",
        isStatic = true,
    )
    private val SystemCurrentTimeMillisKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "currentTimeMillis",
        descriptor = "()J",
        isStatic = true,
    )
    private val SystemNanoTimeKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "nanoTime",
        descriptor = "()J",
        isStatic = true,
    )
    private val SystemExitKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "exit",
        descriptor = "(I)V",
        isStatic = true,
    )
    private val SystemMapLibraryNameKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "mapLibraryName",
        descriptor = "(Ljava/lang/String;)Ljava/lang/String;",
        isStatic = true,
    )
    private val SystemLoadLibraryKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "loadLibrary",
        descriptor = "(Ljava/lang/String;)V",
        isStatic = true,
    )
    private val RuntimeLoadLibrary0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "loadLibrary0",
        descriptor = "(Ljava/lang/Class;Ljava/lang/String;)V",
        isStatic = false,
    )
    private val RuntimeExitKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "exit",
        descriptor = "(I)V",
        isStatic = false,
    )
    private val ShutdownBeforeHaltKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Shutdown",
        name = "beforeHalt",
        descriptor = "()V",
        isStatic = true,
    )
    private val ShutdownHalt0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Shutdown",
        name = "halt0",
        descriptor = "(I)V",
        isStatic = true,
    )
    private val NativeLibrariesLoadKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/loader/NativeLibraries",
        name = "load",
        descriptor = "(Ljdk/internal/loader/NativeLibraries\$NativeLibraryImpl;Ljava/lang/String;ZZ)Z",
        isStatic = true,
    )
    private val NativeLibrariesFindBuiltinLibKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/loader/NativeLibraries",
        name = "findBuiltinLib",
        descriptor = "(Ljava/lang/String;)Ljava/lang/String;",
        isStatic = true,
    )
    private val NativeLibrariesUnloadKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/loader/NativeLibraries",
        name = "unload",
        descriptor = "(Ljava/lang/String;ZJ)V",
        isStatic = true,
    )
    private val ClassInitClassNameKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "initClassName",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
    )
    private val ClassIsArrayKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isArray",
        descriptor = "()Z",
        isStatic = false,
    )
    private val ClassIsPrimitiveKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isPrimitive",
        descriptor = "()Z",
        isStatic = false,
    )
    private val ClassIsInterfaceKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isInterface",
        descriptor = "()Z",
        isStatic = false,
    )
    private val ClassGetSuperclassKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "getSuperclass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
    )
    private val ThrowableFillInStackTraceKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Throwable",
        name = "fillInStackTrace",
        descriptor = "(I)Ljava/lang/Throwable;",
        isStatic = false,
    )
    private val StringInternKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/String",
        name = "intern",
        descriptor = "()Ljava/lang/String;",
        isStatic = false,
    )
    private val ThreadCurrentThreadKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "currentThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
    )
    private val ThreadSleepMillisKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(J)V",
        isStatic = true,
    )
    private val ThreadSleepMillisNanosKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(JI)V",
        isStatic = true,
    )
    private val ThreadSleepNanos0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "sleepNanos0",
        descriptor = "(J)V",
        isStatic = true,
    )

    private val ObjectGetClass = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.getClass intrinsic requires a receiver")
        val receiverClassName = context.heap.get(receiver).className
        context.heap.internClassMirror(receiverClassName)
    }
    private val ObjectHashCode = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.hashCode intrinsic requires a receiver")
        context.heap.get(receiver)
        JvmIntValue(receiver.referenceId.value)
    }
    private val ObjectClone = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.clone intrinsic requires a receiver")
        val receiverClassName = context.heap.get(receiver).className
        if (!receiverClassName.startsWith("[") &&
            !context.classHierarchy.isAssignable(receiverClassName, "java/lang/Cloneable")
        ) {
            throw JvmUnsupportedInstructionException(
                "Object.clone intrinsic requires Cloneable receiver, got $receiverClassName",
            )
        }
        context.heap.shallowClone(receiver)
    }
    private val ObjectWait = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.wait intrinsic requires a receiver")
        validateWaitArguments(invocation.arguments)
        context.heap.get(receiver)
        context.threadScheduler?.waitForMonitorNotification(context.monitors, receiver, context.currentThreadId)
            ?: context.monitors.waitForNotification(receiver, context.currentThreadId)
        null
    }
    private val ObjectNotify = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.notify intrinsic requires a receiver")
        require(invocation.arguments.isEmpty()) { "Object.notify intrinsic expects no arguments" }
        context.heap.get(receiver)
        context.threadScheduler?.notifyOneMonitor(context.monitors, receiver, context.currentThreadId)
            ?: context.monitors.notifyOne(receiver, context.currentThreadId)
        null
    }
    private val ObjectNotifyAll = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.notifyAll intrinsic requires a receiver")
        require(invocation.arguments.isEmpty()) { "Object.notifyAll intrinsic expects no arguments" }
        context.heap.get(receiver)
        context.threadScheduler?.notifyAllMonitor(context.monitors, receiver, context.currentThreadId)
            ?: context.monitors.notifyAll(receiver, context.currentThreadId)
        null
    }
    private val SystemArraycopy = JvmNativeMethodIntrinsic { context, invocation ->
        val arguments = parseArraycopyArguments(invocation.arguments)
        val sourceObject = context.heap.get(arguments.source)
        val targetObject = context.heap.get(arguments.target)
        copyArrayPayload(
            classHierarchy = context.classHierarchy,
            sourceClassName = sourceObject.className,
            sourcePayload = sourceObject.payload,
            sourcePosition = arguments.sourcePosition,
            targetClassName = targetObject.className,
            targetPayload = targetObject.payload,
            targetPosition = arguments.targetPosition,
            length = arguments.length,
            heap = context.heap,
        )
        null
    }
    private val SystemIdentityHashCode = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("System.identityHashCode expects one argument")
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> JvmIntValue(0)
            is JvmObjectReferenceValue -> {
                context.heap.get(value)
                JvmIntValue(value.referenceId.value)
            }
            else -> throw JvmUnsupportedInstructionException(
                "System.identityHashCode expects a reference argument, got ${value.javaClass.simpleName}",
            )
        }
    }
    private val SystemCurrentTimeMillis = JvmNativeMethodIntrinsic { context, invocation ->
        requireNoArguments("System.currentTimeMillis", invocation)
        JvmLongValue(context.currentTimeMillisProvider())
    }
    private val SystemNanoTime = JvmNativeMethodIntrinsic { context, invocation ->
        requireNoArguments("System.nanoTime", invocation)
        JvmLongValue(context.nanoTimeProvider())
    }
    private val SystemExit = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("System.exit expects one int status argument")
        }
        val status = invocation.arguments.single() as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException("System.exit expects one int status argument")
        context.terminationState.terminateNormally(status.value)
        null
    }
    private val SystemMapLibraryName = JvmNativeMethodIntrinsic { context, invocation ->
        val logicalName = requireStringArgument("System.mapLibraryName", context, invocation)
        context.heap.internString(java.lang.System.mapLibraryName(logicalName))
    }
    private val SystemLoadLibrary = JvmNativeMethodIntrinsic { context, invocation ->
        context.loadNativeLibraryHandler(requireStringArgument("System.loadLibrary", context, invocation))
        null
    }
    private val RuntimeLoadLibrary0 = JvmNativeMethodIntrinsic { context, invocation ->
        context.loadNativeLibraryHandler(requireRuntimeLoadLibrary0Name(context, invocation))
        null
    }
    private val RuntimeExit = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.exit intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Runtime.exit expects one int status argument")
        }
        val status = invocation.arguments.single() as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException("Runtime.exit expects one int status argument")
        context.terminationState.terminateNormally(status.value)
        null
    }
    private val ShutdownBeforeHalt = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Shutdown.beforeHalt expects no receiver")
        }
        requireNoArguments("Shutdown.beforeHalt", invocation)
        null
    }
    private val ShutdownHalt0 = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Shutdown.halt0 expects one int status argument")
        }
        val status = invocation.arguments.single() as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException("Shutdown.halt0 expects one int status argument")
        context.terminationState.terminateNormally(status.value)
        null
    }
    private val NativeLibrariesLoad = JvmNativeMethodIntrinsic { context, invocation ->
        context.loadNativeLibraryHandler(requireNativeLibrariesLoadName(context, invocation))
        JvmIntValue(1)
    }
    private val NativeLibrariesFindBuiltinLib = JvmNativeMethodIntrinsic { context, invocation ->
        requireStringArgument("NativeLibraries.findBuiltinLib", context, invocation)
        JvmNullValue
    }
    private val NativeLibrariesUnload = JvmNativeMethodIntrinsic { context, invocation ->
        context.unloadNativeLibraryHandler(requireNativeLibrariesUnloadName(context, invocation))
        null
    }
    private val ClassInitClassName = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.initClassName", context, invocation)
        context.heap.internString(representedClassName.toBinaryClassName())
    }
    private val ClassIsArray = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.isArray", context, invocation)
        jvmBoolean(representedClassName.startsWith("["))
    }
    private val ClassIsPrimitive = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.isPrimitive", context, invocation)
        jvmBoolean(representedClassName in PrimitiveClassNames)
    }
    private val ClassIsInterface = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.isInterface", context, invocation)
        jvmBoolean(context.classHierarchy.isInterface(representedClassName))
    }
    private val ClassGetSuperclass = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.getSuperclass", context, invocation)
        when {
            representedClassName in PrimitiveClassNames -> JvmNullValue
            representedClassName == "java/lang/Object" -> JvmNullValue
            context.classHierarchy.isInterface(representedClassName) -> JvmNullValue
            representedClassName.startsWith("[") -> context.heap.internClassMirror("java/lang/Object")
            else -> context.classHierarchy.directSuperclassName(representedClassName)
                ?.let(context.heap::internClassMirror)
                ?: JvmNullValue
        }
    }
    private val ThrowableFillInStackTrace = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Throwable.fillInStackTrace intrinsic requires a receiver")
        if (invocation.arguments.size != 1 || invocation.arguments.single() !is JvmIntValue) {
            throw JvmUnsupportedInstructionException("Throwable.fillInStackTrace expects one int argument")
        }
        val receiverClassName = context.heap.get(receiver).className
        if (receiverClassName != "java/lang/Throwable" &&
            !context.classHierarchy.isAssignable(receiverClassName, "java/lang/Throwable")
        ) {
            throw JvmUnsupportedInstructionException(
                "Throwable.fillInStackTrace requires Throwable receiver, got $receiverClassName",
            )
        }
        context.heap.recordThrowableStackTrace(receiver, context.stackTraceProvider())
    }
    private val StringIntern = JvmNativeMethodIntrinsic { context, invocation ->
        val value = requireStringReceiver("String.intern", context, invocation)
        context.heap.internString(value)
    }
    private val ThreadCurrentThread = JvmNativeMethodIntrinsic { context, invocation ->
        requireNoArguments("Thread.currentThread", invocation)
        context.heap.internThread(context.currentThreadId)
    }
    private val ThreadSleepMillis = JvmNativeMethodIntrinsic { context, invocation ->
        val millis = requireSleepMillisArgument("Thread.sleep(J)", invocation)
        context.threadSleepHandler(millis, 0)
        null
    }
    private val ThreadSleepMillisNanos = JvmNativeMethodIntrinsic { context, invocation ->
        val (millis, nanos) = requireSleepMillisNanosArguments("Thread.sleep(JI)", invocation)
        context.threadSleepHandler(millis, nanos)
        null
    }
    private val ThreadSleepNanos0 = JvmNativeMethodIntrinsic { context, invocation ->
        val totalNanos = requireSleepMillisArgument("Thread.sleepNanos0(J)", invocation)
        val millis = totalNanos / 1_000_000L
        val nanos = (totalNanos % 1_000_000L).toInt()
        context.threadSleepHandler(millis, nanos)
        null
    }

    val Registry: JvmNativeMethodRegistry = JvmNativeMethodRegistry.from(
        ObjectGetClassKey to ObjectGetClass,
        ObjectHashCodeKey to ObjectHashCode,
        ObjectCloneKey to ObjectClone,
        ObjectWaitKey to ObjectWait,
        ObjectWaitLongKey to ObjectWait,
        ObjectWaitLongIntKey to ObjectWait,
        ObjectNotifyKey to ObjectNotify,
        ObjectNotifyAllKey to ObjectNotifyAll,
        SystemArraycopyKey to SystemArraycopy,
        SystemIdentityHashCodeKey to SystemIdentityHashCode,
        SystemCurrentTimeMillisKey to SystemCurrentTimeMillis,
        SystemNanoTimeKey to SystemNanoTime,
        SystemExitKey to SystemExit,
        SystemMapLibraryNameKey to SystemMapLibraryName,
        SystemLoadLibraryKey to SystemLoadLibrary,
        RuntimeLoadLibrary0Key to RuntimeLoadLibrary0,
        RuntimeExitKey to RuntimeExit,
        ShutdownBeforeHaltKey to ShutdownBeforeHalt,
        ShutdownHalt0Key to ShutdownHalt0,
        NativeLibrariesLoadKey to NativeLibrariesLoad,
        NativeLibrariesFindBuiltinLibKey to NativeLibrariesFindBuiltinLib,
        NativeLibrariesUnloadKey to NativeLibrariesUnload,
        ClassInitClassNameKey to ClassInitClassName,
        ClassIsArrayKey to ClassIsArray,
        ClassIsPrimitiveKey to ClassIsPrimitive,
        ClassIsInterfaceKey to ClassIsInterface,
        ClassGetSuperclassKey to ClassGetSuperclass,
        ThrowableFillInStackTraceKey to ThrowableFillInStackTrace,
        StringInternKey to StringIntern,
        ThreadCurrentThreadKey to ThreadCurrentThread,
        ThreadSleepMillisKey to ThreadSleepMillis,
        ThreadSleepMillisNanosKey to ThreadSleepMillisNanos,
        ThreadSleepNanos0Key to ThreadSleepNanos0,
    )

    private const val NativeLibrariesNativeLibraryImplClassName =
        "jdk/internal/loader/NativeLibraries\$NativeLibraryImpl"
    private val PrimitiveClassNames = setOf(
        "boolean",
        "byte",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "void",
    )

    private fun requireNoArguments(name: String, invocation: JvmNativeMethodInvocation) {
        if (invocation.arguments.isNotEmpty()) {
            throw JvmUnsupportedInstructionException("$name expects no arguments")
        }
    }

    private fun requireClassMirrorReceiver(
        name: String,
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        requireNoArguments(name, invocation)
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("$name intrinsic requires a receiver")
        return when (val payload = context.heap.get(receiver).payload) {
            is JvmClassPayload -> payload.representedClassName
            else -> throw JvmUnsupportedInstructionException(
                "$name intrinsic requires a java/lang/Class mirror receiver",
            )
        }
    }

    private fun requireStringReceiver(
        name: String,
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        requireNoArguments(name, invocation)
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("$name intrinsic requires a receiver")
        return stringPayload(name, context, receiver, "receiver")
    }

    private fun requireStringArgument(
        name: String,
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("$name expects one java/lang/String argument")
        }
        val argument = invocation.arguments.single() as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("$name expects a non-null java/lang/String argument")
        return stringPayload(name, context, argument, "argument")
    }

    private fun requireRuntimeLoadLibrary0Name(
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.loadLibrary0 intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("Runtime.loadLibrary0 expects Class and String arguments")
        }
        val fromClass = invocation.arguments[0] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Runtime.loadLibrary0 expects a non-null Class argument")
        if (context.heap.get(fromClass).payload !is JvmClassPayload) {
            throw JvmUnsupportedInstructionException("Runtime.loadLibrary0 first argument must be a java/lang/Class mirror")
        }
        val libraryName = invocation.arguments[1] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Runtime.loadLibrary0 expects a non-null java/lang/String argument")
        return stringPayload("Runtime.loadLibrary0", context, libraryName, "argument")
    }

    private fun requireNativeLibrariesLoadName(
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("NativeLibraries.load intrinsic is static")
        }
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException("NativeLibraries.load expects NativeLibraryImpl, String, boolean, boolean arguments")
        }
        val nativeLibrary = invocation.arguments[0] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("NativeLibraries.load expects a non-null NativeLibraryImpl argument")
        val nativeLibraryObject = context.heap.get(nativeLibrary)
        if (!context.classHierarchy.isAssignable(nativeLibraryObject.className, NativeLibrariesNativeLibraryImplClassName)) {
            throw JvmUnsupportedInstructionException(
                "NativeLibraries.load first argument must be a $NativeLibrariesNativeLibraryImplClassName object",
            )
        }
        val libraryName = invocation.arguments[1] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("NativeLibraries.load expects a non-null java/lang/String argument")
        invocation.arguments.drop(2).forEach { argument ->
            if (argument !is JvmIntValue || argument.value !in 0..1) {
                throw JvmUnsupportedInstructionException("NativeLibraries.load boolean flags must be 0 or 1")
            }
        }
        return stringPayload("NativeLibraries.load", context, libraryName, "argument")
    }

    private fun requireNativeLibrariesUnloadName(
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("NativeLibraries.unload intrinsic is static")
        }
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException("NativeLibraries.unload expects String, boolean, long arguments")
        }
        val libraryName = invocation.arguments[0] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("NativeLibraries.unload expects a non-null java/lang/String argument")
        val isBuiltin = invocation.arguments[1] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException("NativeLibraries.unload expects a boolean builtin flag")
        if (isBuiltin.value !in 0..1) {
            throw JvmUnsupportedInstructionException("NativeLibraries.unload boolean flag must be 0 or 1")
        }
        if (invocation.arguments[2] !is JvmLongValue) {
            throw JvmUnsupportedInstructionException("NativeLibraries.unload expects a long native handle")
        }
        return stringPayload("NativeLibraries.unload", context, libraryName, "argument")
    }

    private fun stringPayload(
        name: String,
        context: JvmNativeMethodContext,
        reference: JvmObjectReferenceValue,
        role: String,
    ): String =
        when (val payload = context.heap.get(reference).payload) {
            is JvmStringPayload -> payload.value
            else -> throw JvmUnsupportedInstructionException(
                "$name intrinsic requires a java/lang/String $role",
            )
        }

    private fun requireSleepMillisArgument(
        name: String,
        invocation: JvmNativeMethodInvocation,
    ): Long {
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("$name expects one long argument")
        }
        val millis = (invocation.arguments.single() as? JvmLongValue)?.value
            ?: throw JvmUnsupportedInstructionException("$name expects one long argument")
        if (millis < 0L) {
            throw JvmUnsupportedInstructionException("$name timeout must be non-negative")
        }
        return millis
    }

    private fun requireSleepMillisNanosArguments(
        name: String,
        invocation: JvmNativeMethodInvocation,
    ): Pair<Long, Int> {
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("$name expects long millis and int nanos")
        }
        val millis = (invocation.arguments[0] as? JvmLongValue)?.value
            ?: throw JvmUnsupportedInstructionException("$name expects long millis")
        val nanos = (invocation.arguments[1] as? JvmIntValue)?.value
            ?: throw JvmUnsupportedInstructionException("$name expects int nanos")
        if (millis < 0L || nanos !in 0..999_999) {
            throw JvmUnsupportedInstructionException("$name timeout or nanos is out of range")
        }
        return millis to nanos
    }

    private fun jvmBoolean(value: Boolean): JvmIntValue =
        JvmIntValue(if (value) 1 else 0)

    private fun String.toBinaryClassName(): String =
        replace('/', '.')

    private fun validateWaitArguments(arguments: List<JvmValue>) {
        when (arguments.size) {
            0 -> Unit
            1 -> {
                val timeoutMillis = (arguments[0] as? JvmLongValue)?.value
                    ?: throw JvmUnsupportedInstructionException("Object.wait(J)V expects a long timeout")
                if (timeoutMillis < 0L) {
                    throw JvmUnsupportedInstructionException("Object.wait timeout must be non-negative")
                }
            }
            2 -> {
                val timeoutMillis = (arguments[0] as? JvmLongValue)?.value
                    ?: throw JvmUnsupportedInstructionException("Object.wait(JI)V expects a long timeout")
                val nanos = (arguments[1] as? JvmIntValue)?.value
                    ?: throw JvmUnsupportedInstructionException("Object.wait(JI)V expects int nanos")
                if (timeoutMillis < 0L || nanos !in 0..999_999) {
                    throw JvmUnsupportedInstructionException("Object.wait timeout or nanos is out of range")
                }
            }
            else -> throw JvmUnsupportedInstructionException("Object.wait intrinsic received too many arguments")
        }
    }

    private fun parseArraycopyArguments(arguments: List<JvmValue>): ArraycopyArguments {
        if (arguments.size != 5) {
            throw JvmUnsupportedInstructionException("System.arraycopy expects five arguments")
        }
        val source = arguments[0] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("System.arraycopy source must be a non-null object reference")
        val sourcePosition = (arguments[1] as? JvmIntValue)?.value
            ?: throw JvmUnsupportedInstructionException("System.arraycopy source position must be int")
        val target = arguments[2] as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("System.arraycopy target must be a non-null object reference")
        val targetPosition = (arguments[3] as? JvmIntValue)?.value
            ?: throw JvmUnsupportedInstructionException("System.arraycopy target position must be int")
        val length = (arguments[4] as? JvmIntValue)?.value
            ?: throw JvmUnsupportedInstructionException("System.arraycopy length must be int")
        if (sourcePosition < 0 || targetPosition < 0 || length < 0) {
            throw JvmUnsupportedInstructionException("System.arraycopy positions and length must be non-negative")
        }
        return ArraycopyArguments(
            source = source,
            sourcePosition = sourcePosition,
            target = target,
            targetPosition = targetPosition,
            length = length,
        )
    }

    private fun copyArrayPayload(
        classHierarchy: JvmClassHierarchy,
        sourceClassName: String,
        sourcePayload: Any,
        sourcePosition: Int,
        targetClassName: String,
        targetPayload: Any,
        targetPosition: Int,
        length: Int,
        heap: JvmHeap,
    ) {
        when (sourcePayload) {
            is JvmBooleanArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmBooleanArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmByteArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmByteArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmCharArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmCharArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmShortArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmShortArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmIntArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmIntArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmLongArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmLongArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmFloatArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmFloatArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmDoubleArrayPayload -> copyMatchingPrimitiveArray(
                sourcePayload.elements,
                (targetPayload as? JvmDoubleArrayPayload)?.elements,
                sourcePosition,
                targetPosition,
                length,
            )
            is JvmReferenceArrayPayload -> {
                val targetReferenceArray = targetPayload as? JvmReferenceArrayPayload
                    ?: throw JvmUnsupportedInstructionException("System.arraycopy cannot mix reference and primitive arrays")
                copyReferenceArray(
                    classHierarchy = classHierarchy,
                    sourceElements = sourcePayload.elements,
                    sourcePosition = sourcePosition,
                    targetClassName = targetClassName,
                    targetElements = targetReferenceArray.elements,
                    targetPosition = targetPosition,
                    length = length,
                    heap = heap,
                )
            }
            else -> throw JvmUnsupportedInstructionException(
                "System.arraycopy source must be an array, got $sourceClassName",
            )
        }
    }

    private fun <T> copyMatchingPrimitiveArray(
        sourceElements: MutableList<T>,
        targetElements: MutableList<T>?,
        sourcePosition: Int,
        targetPosition: Int,
        length: Int,
    ) {
        if (targetElements == null) {
            throw JvmUnsupportedInstructionException("System.arraycopy primitive array types must match")
        }
        copyElements(sourceElements, sourcePosition, targetElements, targetPosition, length)
    }

    private fun copyReferenceArray(
        classHierarchy: JvmClassHierarchy,
        sourceElements: MutableList<JvmReferenceValue>,
        sourcePosition: Int,
        targetClassName: String,
        targetElements: MutableList<JvmReferenceValue>,
        targetPosition: Int,
        length: Int,
        heap: JvmHeap,
    ) {
        requireArrayRange(sourceElements.size, sourcePosition, length)
        requireArrayRange(targetElements.size, targetPosition, length)
        val targetComponentClassName = targetClassName.referenceArrayComponentClassName()
        val snapshot = sourceElements.subList(sourcePosition, sourcePosition + length).toList()
        snapshot.forEach { value ->
            if (value is JvmObjectReferenceValue) {
                val valueClassName = heap.get(value).className
                if (!classHierarchy.isAssignable(valueClassName, targetComponentClassName)) {
                    throw JvmUnsupportedInstructionException(
                        "System.arraycopy value $valueClassName is not assignable to $targetComponentClassName",
                    )
                }
            }
        }
        snapshot.forEachIndexed { offset, value ->
            targetElements[targetPosition + offset] = value
        }
    }

    private fun <T> copyElements(
        sourceElements: MutableList<T>,
        sourcePosition: Int,
        targetElements: MutableList<T>,
        targetPosition: Int,
        length: Int,
    ) {
        requireArrayRange(sourceElements.size, sourcePosition, length)
        requireArrayRange(targetElements.size, targetPosition, length)
        val snapshot = sourceElements.subList(sourcePosition, sourcePosition + length).toList()
        snapshot.forEachIndexed { offset, value ->
            targetElements[targetPosition + offset] = value
        }
    }

    private fun requireArrayRange(arrayLength: Int, start: Int, length: Int) {
        if (start > arrayLength - length) {
            throw JvmUnsupportedInstructionException(
                "System.arraycopy range start=$start length=$length is out of bounds for array length $arrayLength",
            )
        }
    }

    private fun String.referenceArrayComponentClassName(): String =
        when {
            startsWith("[L") && endsWith(";") -> substring(startIndex = 2, endIndex = length - 1)
            startsWith("[[") -> substring(startIndex = 1)
            else -> throw JvmUnsupportedInstructionException("System.arraycopy target must be a reference array")
        }

    private data class ArraycopyArguments(
        val source: JvmObjectReferenceValue,
        val sourcePosition: Int,
        val target: JvmObjectReferenceValue,
        val targetPosition: Int,
        val length: Int,
    )
}
