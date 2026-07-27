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
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
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

class JvmUnsafeSyntheticMemory(
    staticLongSlots: Map<Long, Long> = emptyMap(),
    staticIntSlots: Map<Long, Int> = emptyMap(),
    staticReferenceSlots: Map<Long, JvmReferenceValue> = emptyMap(),
) {
    private val staticLongSlots = staticLongSlots.toMutableMap()
    private val staticIntSlots = staticIntSlots.toMutableMap()
    private val staticReferenceSlots = staticReferenceSlots.toMutableMap()

    fun getStaticLong(offset: Long): Long = staticLongSlots[offset] ?: 0L

    fun putStaticLong(offset: Long, value: Long) {
        staticLongSlots[offset] = value
    }

    fun compareAndSetStaticLong(
        offset: Long,
        expected: Long,
        replacement: Long,
    ): Boolean {
        val current = getStaticLong(offset)
        if (current != expected) {
            return false
        }
        staticLongSlots[offset] = replacement
        return true
    }

    fun compareAndExchangeStaticLong(
        offset: Long,
        expected: Long,
        replacement: Long,
    ): Long {
        val current = getStaticLong(offset)
        if (current == expected) {
            staticLongSlots[offset] = replacement
        }
        return current
    }

    fun getStaticInt(offset: Long): Int = staticIntSlots[offset] ?: 0

    fun putStaticInt(offset: Long, value: Int) {
        staticIntSlots[offset] = value
    }

    fun compareAndSetStaticInt(
        offset: Long,
        expected: Int,
        replacement: Int,
    ): Boolean {
        val current = getStaticInt(offset)
        if (current != expected) {
            return false
        }
        staticIntSlots[offset] = replacement
        return true
    }

    fun compareAndExchangeStaticInt(
        offset: Long,
        expected: Int,
        replacement: Int,
    ): Int {
        val current = getStaticInt(offset)
        if (current == expected) {
            staticIntSlots[offset] = replacement
        }
        return current
    }

    fun getStaticReference(offset: Long): JvmReferenceValue = staticReferenceSlots[offset] ?: JvmNullValue

    fun putStaticReference(offset: Long, value: JvmReferenceValue) {
        staticReferenceSlots[offset] = value
    }

    fun compareAndSetStaticReference(
        offset: Long,
        expected: JvmReferenceValue,
        replacement: JvmReferenceValue,
    ): Boolean {
        val current = getStaticReference(offset)
        if (current != expected) {
            return false
        }
        staticReferenceSlots[offset] = replacement
        return true
    }

    fun compareAndExchangeStaticReference(
        offset: Long,
        expected: JvmReferenceValue,
        replacement: JvmReferenceValue,
    ): JvmReferenceValue {
        val current = getStaticReference(offset)
        if (current == expected) {
            staticReferenceSlots[offset] = replacement
        }
        return current
    }
}

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
    val availableProcessorsProvider: () -> Int = { Runtime.getRuntime().availableProcessors() },
    val freeMemoryProvider: () -> Long = { Runtime.getRuntime().freeMemory() },
    val totalMemoryProvider: () -> Long = { Runtime.getRuntime().totalMemory() },
    val maxMemoryProvider: () -> Long = { Runtime.getRuntime().maxMemory() },
    val gcHandler: () -> Unit = { System.gc() },
    val stackTraceProvider: () -> List<JvmStackTraceFrame> = { emptyList() },
    val threadYieldHandler: () -> Unit = Thread::yield,
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
    val unsafeMemory: JvmUnsafeSyntheticMemory = JvmUnsafeSyntheticMemory(),
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
    private val SystemRegisterNativesKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
    )
    private val SystemSetIn0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "setIn0",
        descriptor = "(Ljava/io/InputStream;)V",
        isStatic = true,
    )
    private val SystemSetOut0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "setOut0",
        descriptor = "(Ljava/io/PrintStream;)V",
        isStatic = true,
    )
    private val SystemSetErr0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/System",
        name = "setErr0",
        descriptor = "(Ljava/io/PrintStream;)V",
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
    private val RuntimeAvailableProcessorsKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "availableProcessors",
        descriptor = "()I",
        isStatic = false,
    )
    private val RuntimeFreeMemoryKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "freeMemory",
        descriptor = "()J",
        isStatic = false,
    )
    private val RuntimeTotalMemoryKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "totalMemory",
        descriptor = "()J",
        isStatic = false,
    )
    private val RuntimeMaxMemoryKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "maxMemory",
        descriptor = "()J",
        isStatic = false,
    )
    private val RuntimeGcKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Runtime",
        name = "gc",
        descriptor = "()V",
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
    private val ClassRegisterNativesKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
    )
    private val ClassGetPrimitiveClassKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "getPrimitiveClass",
        descriptor = "(Ljava/lang/String;)Ljava/lang/Class;",
        isStatic = true,
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
    private val ClassIsInstanceKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isInstance",
        descriptor = "(Ljava/lang/Object;)Z",
        isStatic = false,
    )
    private val ClassIsAssignableFromKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isAssignableFrom",
        descriptor = "(Ljava/lang/Class;)Z",
        isStatic = false,
    )
    private val ClassGetSuperclassKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "getSuperclass",
        descriptor = "()Ljava/lang/Class;",
        isStatic = false,
    )
    private val ClassGetInterfaces0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "getInterfaces0",
        descriptor = "()[Ljava/lang/Class;",
        isStatic = false,
    )
    private val ClassDesiredAssertionStatus0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "desiredAssertionStatus0",
        descriptor = "(Ljava/lang/Class;)Z",
        isStatic = true,
    )
    private val ClassIsHiddenKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Class",
        name = "isHidden",
        descriptor = "()Z",
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
    private val ThreadCurrentCarrierThreadKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "currentCarrierThread",
        descriptor = "()Ljava/lang/Thread;",
        isStatic = true,
    )
    private val ThreadRegisterNativesKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
    )
    private val ThreadFindScopedValueBindingsKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "findScopedValueBindings",
        descriptor = "()Ljava/lang/Object;",
        isStatic = true,
    )
    private val ThreadScopedValueCacheKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "scopedValueCache",
        descriptor = "()[Ljava/lang/Object;",
        isStatic = true,
    )
    private val ThreadSetScopedValueCacheKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "setScopedValueCache",
        descriptor = "([Ljava/lang/Object;)V",
        isStatic = true,
    )
    private val ThreadGetThreadsKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "getThreads",
        descriptor = "()[Ljava/lang/Thread;",
        isStatic = true,
    )
    private val ThreadClearInterruptEventKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "clearInterruptEvent",
        descriptor = "()V",
        isStatic = true,
    )
    private val ThreadSetNativeNameKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "setNativeName",
        descriptor = "(Ljava/lang/String;)V",
        isStatic = false,
    )
    private val ThreadSetPriority0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "setPriority0",
        descriptor = "(I)V",
        isStatic = false,
    )
    private val ThreadInterrupt0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "interrupt0",
        descriptor = "()V",
        isStatic = false,
    )
    private val ThreadStart0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "start0",
        descriptor = "()V",
        isStatic = false,
    )
    private val ThreadSetCurrentThreadKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "setCurrentThread",
        descriptor = "(Ljava/lang/Thread;)V",
        isStatic = false,
    )
    private val ThreadGetStackTrace0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "getStackTrace0",
        descriptor = "()Ljava/lang/Object;",
        isStatic = false,
    )
    private val ThreadDumpThreadsKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "dumpThreads",
        descriptor = "([Ljava/lang/Thread;)[[Ljava/lang/StackTraceElement;",
        isStatic = true,
    )
    private val ThreadGetNextThreadIdOffsetKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "getNextThreadIdOffset",
        descriptor = "()J",
        isStatic = true,
    )
    private val ThreadSleepMillisKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "sleep",
        descriptor = "(J)V",
        isStatic = true,
    )
    private val ThreadYield0Key = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "yield0",
        descriptor = "()V",
        isStatic = true,
    )
    private val ThreadHoldsLockKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "holdsLock",
        descriptor = "(Ljava/lang/Object;)Z",
        isStatic = true,
    )
    private val ThreadEnsureMaterializedForStackWalkKey = JvmNativeMethodKey(
        ownerClassName = "java/lang/Thread",
        name = "ensureMaterializedForStackWalk",
        descriptor = "(Ljava/lang/Object;)V",
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
    private val UnsafeRegisterNativesKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "registerNatives",
        descriptor = "()V",
        isStatic = true,
    )
    private val UnsafeGetLongVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getLongVolatile",
        descriptor = "(Ljava/lang/Object;J)J",
        isStatic = false,
    )
    private val UnsafeGetLongKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getLong",
        descriptor = "(Ljava/lang/Object;J)J",
        isStatic = false,
    )
    private val UnsafeGetReferenceVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getReferenceVolatile",
        descriptor = "(Ljava/lang/Object;J)Ljava/lang/Object;",
        isStatic = false,
    )
    private val UnsafePutReferenceVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putReferenceVolatile",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;)V",
        isStatic = false,
    )
    private val UnsafeGetReferenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getReference",
        descriptor = "(Ljava/lang/Object;J)Ljava/lang/Object;",
        isStatic = false,
    )
    private val UnsafePutReferenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;)V",
        isStatic = false,
    )
    private val UnsafeCompareAndSetReferenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z",
        isStatic = false,
    )
    private val UnsafeCompareAndExchangeReferenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeReference",
        descriptor = "(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        isStatic = false,
    )
    private val UnsafeGetIntVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getIntVolatile",
        descriptor = "(Ljava/lang/Object;J)I",
        isStatic = false,
    )
    private val UnsafeGetIntKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "getInt",
        descriptor = "(Ljava/lang/Object;J)I",
        isStatic = false,
    )
    private val UnsafePutIntVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putIntVolatile",
        descriptor = "(Ljava/lang/Object;JI)V",
        isStatic = false,
    )
    private val UnsafePutIntKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putInt",
        descriptor = "(Ljava/lang/Object;JI)V",
        isStatic = false,
    )
    private val UnsafeCompareAndSetIntKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetInt",
        descriptor = "(Ljava/lang/Object;JII)Z",
        isStatic = false,
    )
    private val UnsafeCompareAndExchangeIntKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeInt",
        descriptor = "(Ljava/lang/Object;JII)I",
        isStatic = false,
    )
    private val UnsafePutLongVolatileKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putLongVolatile",
        descriptor = "(Ljava/lang/Object;JJ)V",
        isStatic = false,
    )
    private val UnsafePutLongKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "putLong",
        descriptor = "(Ljava/lang/Object;JJ)V",
        isStatic = false,
    )
    private val UnsafeCompareAndSetLongKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndSetLong",
        descriptor = "(Ljava/lang/Object;JJJ)Z",
        isStatic = false,
    )
    private val UnsafeCompareAndExchangeLongKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "compareAndExchangeLong",
        descriptor = "(Ljava/lang/Object;JJJ)J",
        isStatic = false,
    )
    private val UnsafeArrayBaseOffset0Key = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "arrayBaseOffset0",
        descriptor = "(Ljava/lang/Class;)I",
        isStatic = false,
    )
    private val UnsafeFullFenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "fullFence",
        descriptor = "()V",
        isStatic = false,
    )
    private val UnsafeLoadFenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "loadFence",
        descriptor = "()V",
        isStatic = false,
    )
    private val UnsafeStoreFenceKey = JvmNativeMethodKey(
        ownerClassName = "jdk/internal/misc/Unsafe",
        name = "storeFence",
        descriptor = "()V",
        isStatic = false,
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
    private val SystemSetIn0 = systemStreamSetter(
        name = "System.setIn0",
        field = JvmFieldReference("java/lang/System", "in", "Ljava/io/InputStream;"),
    )
    private val SystemSetOut0 = systemStreamSetter(
        name = "System.setOut0",
        field = JvmFieldReference("java/lang/System", "out", "Ljava/io/PrintStream;"),
    )
    private val SystemSetErr0 = systemStreamSetter(
        name = "System.setErr0",
        field = JvmFieldReference("java/lang/System", "err", "Ljava/io/PrintStream;"),
    )
    private val SystemRegisterNatives = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("System.registerNatives expects no receiver")
        }
        requireNoArguments("System.registerNatives", invocation)
        null
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
    private val RuntimeAvailableProcessors = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.availableProcessors intrinsic requires a receiver")
        requireNoArguments("Runtime.availableProcessors", invocation)
        context.heap.get(receiver)
        JvmIntValue(context.availableProcessorsProvider())
    }
    private val RuntimeFreeMemory = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.freeMemory intrinsic requires a receiver")
        requireNoArguments("Runtime.freeMemory", invocation)
        context.heap.get(receiver)
        JvmLongValue(context.freeMemoryProvider())
    }
    private val RuntimeTotalMemory = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.totalMemory intrinsic requires a receiver")
        requireNoArguments("Runtime.totalMemory", invocation)
        context.heap.get(receiver)
        JvmLongValue(context.totalMemoryProvider())
    }
    private val RuntimeMaxMemory = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.maxMemory intrinsic requires a receiver")
        requireNoArguments("Runtime.maxMemory", invocation)
        context.heap.get(receiver)
        JvmLongValue(context.maxMemoryProvider())
    }
    private val RuntimeGc = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Runtime.gc intrinsic requires a receiver")
        requireNoArguments("Runtime.gc", invocation)
        context.heap.get(receiver)
        context.gcHandler()
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
    private val ClassRegisterNatives = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Class.registerNatives expects no receiver")
        }
        requireNoArguments("Class.registerNatives", invocation)
        null
    }
    private val ClassGetPrimitiveClass = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Class.getPrimitiveClass expects no receiver")
        }
        val primitiveName = requireStringArgument("Class.getPrimitiveClass", context, invocation)
        if (primitiveName !in PrimitiveClassNames) {
            throw JvmUnsupportedInstructionException("Class.getPrimitiveClass expects a primitive class name")
        }
        context.heap.internClassMirror(primitiveName)
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
    private val ClassIsInstance = JvmNativeMethodIntrinsic { context, invocation ->
        val targetClassName = requireClassMirrorReceiverWithArguments("Class.isInstance", context, invocation)
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Class.isInstance expects one reference argument")
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> JvmIntValue(0)
            is JvmObjectReferenceValue -> {
                val sourceClassName = context.heap.get(value).className
                jvmBoolean(
                    targetClassName !in PrimitiveClassNames &&
                        context.classHierarchy.isAssignable(sourceClassName, targetClassName),
                )
            }
            else -> throw JvmUnsupportedInstructionException("Class.isInstance expects one reference argument")
        }
    }
    private val ClassIsAssignableFrom = JvmNativeMethodIntrinsic { context, invocation ->
        val targetClassName = requireClassMirrorReceiverWithArguments("Class.isAssignableFrom", context, invocation)
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Class.isAssignableFrom expects one Class mirror argument")
        }
        val sourceMirror = invocation.arguments.single() as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Class.isAssignableFrom expects one Class mirror argument")
        val sourceClassName = requireClassMirrorReference("Class.isAssignableFrom argument", context, sourceMirror)
        val assignable = if (sourceClassName in PrimitiveClassNames || targetClassName in PrimitiveClassNames) {
            sourceClassName == targetClassName
        } else {
            context.classHierarchy.isAssignable(sourceClassName, targetClassName)
        }
        jvmBoolean(assignable)
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
    private val ClassGetInterfaces0 = JvmNativeMethodIntrinsic { context, invocation ->
        val representedClassName = requireClassMirrorReceiver("Class.getInterfaces0", context, invocation)
        val interfaceNames = when {
            representedClassName in PrimitiveClassNames -> emptyList()
            representedClassName.startsWith("[") -> listOf("java/lang/Cloneable", "java/io/Serializable")
            else -> context.classHierarchy.directSuperinterfaceNames(representedClassName)
        }
        val interfaces = context.heap.allocateReferenceArray("java/lang/Class", interfaceNames.size)
        val payload = context.heap.get(interfaces).payload as JvmReferenceArrayPayload
        interfaceNames.forEachIndexed { index, interfaceName ->
            payload.elements[index] = context.heap.internClassMirror(interfaceName)
        }
        interfaces
    }
    private val ClassDesiredAssertionStatus0 = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Class.desiredAssertionStatus0 expects one Class mirror argument")
        }
        val mirror = invocation.arguments.single() as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Class.desiredAssertionStatus0 expects one Class mirror argument")
        requireClassMirrorReference("Class.desiredAssertionStatus0 argument", context, mirror)
        JvmIntValue(0)
    }
    private val ClassIsHidden = JvmNativeMethodIntrinsic { context, invocation ->
        requireClassMirrorReceiver("Class.isHidden", context, invocation)
        JvmIntValue(0)
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
    private val ThreadRegisterNatives = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.registerNatives expects no receiver")
        }
        requireNoArguments("Thread.registerNatives", invocation)
        null
    }
    private val ThreadCurrentThread = JvmNativeMethodIntrinsic { context, invocation ->
        requireNoArguments("Thread.currentThread", invocation)
        context.heap.internThread(context.currentThreadId)
    }
    private val ThreadCurrentCarrierThread = JvmNativeMethodIntrinsic { context, invocation ->
        requireNoArguments("Thread.currentCarrierThread", invocation)
        context.heap.internThread(context.currentThreadId)
    }
    private val ThreadFindScopedValueBindings = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.findScopedValueBindings expects no receiver")
        }
        requireNoArguments("Thread.findScopedValueBindings", invocation)
        JvmNullValue
    }
    private val ThreadScopedValueCache = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.scopedValueCache expects no receiver")
        }
        requireNoArguments("Thread.scopedValueCache", invocation)
        JvmNullValue
    }
    private val ThreadSetScopedValueCache = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException(
                "Thread.setScopedValueCache expects one nullable Object[] argument",
            )
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> Unit
            is JvmObjectReferenceValue -> {
                val payload = context.heap.get(value).payload
                if (payload !is JvmReferenceArrayPayload) {
                    throw JvmUnsupportedInstructionException(
                        "Thread.setScopedValueCache expects one nullable Object[] argument",
                    )
                }
            }
            else -> throw JvmUnsupportedInstructionException(
                "Thread.setScopedValueCache expects one nullable Object[] argument",
            )
        }
        null
    }
    private val ThreadGetThreads = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.getThreads expects no receiver")
        }
        requireNoArguments("Thread.getThreads", invocation)
        val threads = context.heap.allocateReferenceArray("java/lang/Thread", 1)
        val payload = context.heap.get(threads).payload as JvmReferenceArrayPayload
        payload.elements[0] = context.heap.internThread(context.currentThreadId)
        threads
    }
    private val ThreadClearInterruptEvent = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.clearInterruptEvent expects no receiver")
        }
        requireNoArguments("Thread.clearInterruptEvent", invocation)
        null
    }
    private val ThreadSetNativeName = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.setNativeName intrinsic requires a receiver")
        context.heap.get(receiver)
        requireStringArgument("Thread.setNativeName", context, invocation)
        null
    }
    private val ThreadSetPriority0 = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.setPriority0 intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 1 || invocation.arguments.single() !is JvmIntValue) {
            throw JvmUnsupportedInstructionException("Thread.setPriority0 expects one int priority argument")
        }
        null
    }
    private val ThreadInterrupt0 = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.interrupt0 intrinsic requires a receiver")
        requireNoArguments("Thread.interrupt0", invocation)
        context.heap.get(receiver)
        null
    }
    private val ThreadStart0 = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.start0 intrinsic requires a receiver")
        requireNoArguments("Thread.start0", invocation)
        context.heap.get(receiver)
        null
    }
    private val ThreadSetCurrentThread = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.setCurrentThread intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Thread.setCurrentThread expects one nullable Thread argument")
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> Unit
            is JvmObjectReferenceValue -> {
                val className = context.heap.get(value).className
                if (className != "java/lang/Thread" && !context.classHierarchy.isAssignable(className, "java/lang/Thread")) {
                    throw JvmUnsupportedInstructionException(
                        "Thread.setCurrentThread expects one nullable Thread argument",
                    )
                }
            }
            else -> throw JvmUnsupportedInstructionException(
                "Thread.setCurrentThread expects one nullable Thread argument",
            )
        }
        null
    }
    private val ThreadGetStackTrace0 = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Thread.getStackTrace0 intrinsic requires a receiver")
        requireNoArguments("Thread.getStackTrace0", invocation)
        context.heap.get(receiver)
        context.heap.allocateReferenceArray("java/lang/StackTraceElement", 0)
    }
    private val ThreadDumpThreads = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Thread.dumpThreads expects one Thread[] argument")
        }
        val threads = invocation.arguments.single() as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Thread.dumpThreads expects one Thread[] argument")
        val threadArrayPayload = context.heap.get(threads).payload as? JvmReferenceArrayPayload
            ?: throw JvmUnsupportedInstructionException("Thread.dumpThreads expects one Thread[] argument")
        val snapshots = context.heap.allocateReferenceArray(
            componentClassName = "[Ljava/lang/StackTraceElement;",
            length = threadArrayPayload.elements.size,
        )
        val snapshotsPayload = context.heap.get(snapshots).payload as JvmReferenceArrayPayload
        threadArrayPayload.elements.forEachIndexed { index, value ->
            when (value) {
                JvmNullValue -> Unit
                is JvmObjectReferenceValue -> {
                    val className = context.heap.get(value).className
                    if (className != "java/lang/Thread" && !context.classHierarchy.isAssignable(className, "java/lang/Thread")) {
                        throw JvmUnsupportedInstructionException("Thread.dumpThreads expects one Thread[] argument")
                    }
                    snapshotsPayload.elements[index] =
                        context.heap.allocateReferenceArray("java/lang/StackTraceElement", 0)
                }
            }
        }
        snapshots
    }
    private val ThreadGetNextThreadIdOffset = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.getNextThreadIdOffset expects no receiver")
        }
        requireNoArguments("Thread.getNextThreadIdOffset", invocation)
        JvmLongValue(ThreadNextThreadIdSyntheticOffset)
    }
    private val ThreadYield0 = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Thread.yield0 expects no receiver")
        }
        requireNoArguments("Thread.yield0", invocation)
        context.threadYieldHandler()
        null
    }
    private val ThreadHoldsLock = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Thread.holdsLock expects one reference argument")
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> JvmIntValue(0)
            is JvmObjectReferenceValue -> {
                context.heap.get(value)
                jvmBoolean(context.monitors.holdCount(value, context.currentThreadId) > 0)
            }
            else -> throw JvmUnsupportedInstructionException("Thread.holdsLock expects one reference argument")
        }
    }
    private val ThreadEnsureMaterializedForStackWalk = JvmNativeMethodIntrinsic { context, invocation ->
        if (invocation.receiver != null || invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException(
                "Thread.ensureMaterializedForStackWalk expects one reference argument",
            )
        }
        when (val value = invocation.arguments.single()) {
            JvmNullValue -> Unit
            is JvmObjectReferenceValue -> context.heap.get(value)
            else -> throw JvmUnsupportedInstructionException(
                "Thread.ensureMaterializedForStackWalk expects one reference argument",
            )
        }
        null
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
    private val UnsafeRegisterNatives = JvmNativeMethodIntrinsic { _, invocation ->
        if (invocation.receiver != null) {
            throw JvmUnsupportedInstructionException("Unsafe.registerNatives expects no receiver")
        }
        requireNoArguments("Unsafe.registerNatives", invocation)
        null
    }
    private val UnsafeGetLongVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getLongVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("Unsafe.getLongVolatile expects Object and long offset arguments")
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException("Unsafe.getLongVolatile expects Object and long offset arguments")
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getLongVolatile currently supports only synthetic static long slots",
            )
        }
        JvmLongValue(context.unsafeMemory.getStaticLong(offset.value))
    }
    private val UnsafeGetLong = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getLong intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("Unsafe.getLong expects Object and long offset arguments")
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException("Unsafe.getLong expects Object and long offset arguments")
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getLong currently supports only synthetic static long slots",
            )
        }
        JvmLongValue(context.unsafeMemory.getStaticLong(offset.value))
    }
    private val UnsafeGetReferenceVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getReferenceVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getReferenceVolatile expects Object and long offset arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.getReferenceVolatile expects Object and long offset arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getReferenceVolatile currently supports only synthetic static reference slots",
            )
        }
        context.unsafeMemory.getStaticReference(offset.value)
    }
    private val UnsafePutReferenceVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putReferenceVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putReferenceVolatile expects Object, long offset, and reference value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putReferenceVolatile expects Object, long offset, and reference value arguments",
            )
        val value = invocation.arguments[2] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putReferenceVolatile expects Object, long offset, and reference value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putReferenceVolatile currently supports only synthetic static reference slots",
            )
        }
        context.unsafeMemory.putStaticReference(offset = offset.value, value = value)
        null
    }
    private val UnsafeGetReference = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getReference intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getReference expects Object and long offset arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.getReference expects Object and long offset arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getReference currently supports only synthetic static reference slots",
            )
        }
        context.unsafeMemory.getStaticReference(offset.value)
    }
    private val UnsafePutReference = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putReference intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putReference expects Object, long offset, and reference value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putReference expects Object, long offset, and reference value arguments",
            )
        val value = invocation.arguments[2] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putReference expects Object, long offset, and reference value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putReference currently supports only synthetic static reference slots",
            )
        }
        context.unsafeMemory.putStaticReference(offset = offset.value, value = value)
        null
    }
    private val UnsafeCompareAndSetReference = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndSetReference intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetReference expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetReference expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetReference expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetReference expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetReference currently supports only synthetic static reference slots",
            )
        }
        jvmBoolean(
            context.unsafeMemory.compareAndSetStaticReference(
                offset = offset.value,
                expected = expected,
                replacement = replacement,
            ),
        )
    }
    private val UnsafeCompareAndExchangeReference = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndExchangeReference intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeReference expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeReference expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeReference expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmReferenceValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeReference expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeReference currently supports only synthetic static reference slots",
            )
        }
        context.unsafeMemory.compareAndExchangeStaticReference(
            offset = offset.value,
            expected = expected,
            replacement = replacement,
        )
    }
    private val UnsafeGetIntVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getIntVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("Unsafe.getIntVolatile expects Object and long offset arguments")
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException("Unsafe.getIntVolatile expects Object and long offset arguments")
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getIntVolatile currently supports only synthetic static int slots",
            )
        }
        JvmIntValue(context.unsafeMemory.getStaticInt(offset.value))
    }
    private val UnsafeGetInt = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.getInt intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 2) {
            throw JvmUnsupportedInstructionException("Unsafe.getInt expects Object and long offset arguments")
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException("Unsafe.getInt expects Object and long offset arguments")
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.getInt currently supports only synthetic static int slots",
            )
        }
        JvmIntValue(context.unsafeMemory.getStaticInt(offset.value))
    }
    private val UnsafePutIntVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putIntVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putIntVolatile expects Object, long offset, and int value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putIntVolatile expects Object, long offset, and int value arguments",
            )
        val value = invocation.arguments[2] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putIntVolatile expects Object, long offset, and int value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putIntVolatile currently supports only synthetic static int slots",
            )
        }
        context.unsafeMemory.putStaticInt(offset = offset.value, value = value.value)
        null
    }
    private val UnsafePutInt = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putInt intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putInt expects Object, long offset, and int value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putInt expects Object, long offset, and int value arguments",
            )
        val value = invocation.arguments[2] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putInt expects Object, long offset, and int value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putInt currently supports only synthetic static int slots",
            )
        }
        context.unsafeMemory.putStaticInt(offset = offset.value, value = value.value)
        null
    }
    private val UnsafeCompareAndSetInt = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndSetInt intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetInt expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetInt expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetInt expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetInt expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetInt currently supports only synthetic static int slots",
            )
        }
        jvmBoolean(
            context.unsafeMemory.compareAndSetStaticInt(
                offset = offset.value,
                expected = expected.value,
                replacement = replacement.value,
            ),
        )
    }
    private val UnsafeCompareAndExchangeInt = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndExchangeInt intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeInt expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeInt expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeInt expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmIntValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeInt expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeInt currently supports only synthetic static int slots",
            )
        }
        JvmIntValue(
            context.unsafeMemory.compareAndExchangeStaticInt(
                offset = offset.value,
                expected = expected.value,
                replacement = replacement.value,
            ),
        )
    }
    private val UnsafePutLongVolatile = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putLongVolatile intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putLongVolatile expects Object, long offset, and long value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putLongVolatile expects Object, long offset, and long value arguments",
            )
        val value = invocation.arguments[2] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putLongVolatile expects Object, long offset, and long value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putLongVolatile currently supports only synthetic static long slots",
            )
        }
        context.unsafeMemory.putStaticLong(offset = offset.value, value = value.value)
        null
    }
    private val UnsafePutLong = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.putLong intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 3) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putLong expects Object, long offset, and long value arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putLong expects Object, long offset, and long value arguments",
            )
        val value = invocation.arguments[2] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.putLong expects Object, long offset, and long value arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.putLong currently supports only synthetic static long slots",
            )
        }
        context.unsafeMemory.putStaticLong(offset = offset.value, value = value.value)
        null
    }
    private val UnsafeCompareAndSetLong = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndSetLong intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetLong expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetLong expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetLong expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetLong expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndSetLong currently supports only synthetic static long slots",
            )
        }
        jvmBoolean(
            context.unsafeMemory.compareAndSetStaticLong(
                offset = offset.value,
                expected = expected.value,
                replacement = replacement.value,
            ),
        )
    }
    private val UnsafeCompareAndExchangeLong = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.compareAndExchangeLong intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 4) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeLong expects Object, long offset, expected, and replacement arguments",
            )
        }
        val base = invocation.arguments[0]
        val offset = invocation.arguments[1] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeLong expects Object, long offset, expected, and replacement arguments",
            )
        val expected = invocation.arguments[2] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeLong expects Object, long offset, expected, and replacement arguments",
            )
        val replacement = invocation.arguments[3] as? JvmLongValue
            ?: throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeLong expects Object, long offset, expected, and replacement arguments",
            )
        if (base != JvmNullValue) {
            throw JvmUnsupportedInstructionException(
                "Unsafe.compareAndExchangeLong currently supports only synthetic static long slots",
            )
        }
        JvmLongValue(
            context.unsafeMemory.compareAndExchangeStaticLong(
                offset = offset.value,
                expected = expected.value,
                replacement = replacement.value,
            ),
        )
    }
    private val UnsafeArrayBaseOffset0 = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.arrayBaseOffset0 intrinsic requires a receiver")
        context.heap.get(receiver)
        if (invocation.arguments.size != 1) {
            throw JvmUnsupportedInstructionException("Unsafe.arrayBaseOffset0 expects one Class argument")
        }
        val arrayClass = invocation.arguments.single() as? JvmObjectReferenceValue
            ?: throw JvmUnsupportedInstructionException("Unsafe.arrayBaseOffset0 expects a non-null Class argument")
        val arrayClassName = requireClassMirrorReference("Unsafe.arrayBaseOffset0", context, arrayClass)
        if (!arrayClassName.startsWith("[")) {
            throw JvmUnsupportedInstructionException("Unsafe.arrayBaseOffset0 expects an array Class mirror")
        }
        JvmIntValue(UnsafeSyntheticArrayBaseOffset)
    }
    private val UnsafeFullFence = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.fullFence intrinsic requires a receiver")
        context.heap.get(receiver)
        requireNoArguments("Unsafe.fullFence", invocation)
        null
    }
    private val UnsafeLoadFence = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.loadFence intrinsic requires a receiver")
        context.heap.get(receiver)
        requireNoArguments("Unsafe.loadFence", invocation)
        null
    }
    private val UnsafeStoreFence = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Unsafe.storeFence intrinsic requires a receiver")
        context.heap.get(receiver)
        requireNoArguments("Unsafe.storeFence", invocation)
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
        SystemRegisterNativesKey to SystemRegisterNatives,
        SystemSetIn0Key to SystemSetIn0,
        SystemSetOut0Key to SystemSetOut0,
        SystemSetErr0Key to SystemSetErr0,
        SystemCurrentTimeMillisKey to SystemCurrentTimeMillis,
        SystemNanoTimeKey to SystemNanoTime,
        SystemExitKey to SystemExit,
        SystemMapLibraryNameKey to SystemMapLibraryName,
        SystemLoadLibraryKey to SystemLoadLibrary,
        RuntimeLoadLibrary0Key to RuntimeLoadLibrary0,
        RuntimeAvailableProcessorsKey to RuntimeAvailableProcessors,
        RuntimeFreeMemoryKey to RuntimeFreeMemory,
        RuntimeTotalMemoryKey to RuntimeTotalMemory,
        RuntimeMaxMemoryKey to RuntimeMaxMemory,
        RuntimeGcKey to RuntimeGc,
        RuntimeExitKey to RuntimeExit,
        ShutdownBeforeHaltKey to ShutdownBeforeHalt,
        ShutdownHalt0Key to ShutdownHalt0,
        NativeLibrariesLoadKey to NativeLibrariesLoad,
        NativeLibrariesFindBuiltinLibKey to NativeLibrariesFindBuiltinLib,
        NativeLibrariesUnloadKey to NativeLibrariesUnload,
        ClassInitClassNameKey to ClassInitClassName,
        ClassRegisterNativesKey to ClassRegisterNatives,
        ClassGetPrimitiveClassKey to ClassGetPrimitiveClass,
        ClassIsArrayKey to ClassIsArray,
        ClassIsPrimitiveKey to ClassIsPrimitive,
        ClassIsInterfaceKey to ClassIsInterface,
        ClassIsInstanceKey to ClassIsInstance,
        ClassIsAssignableFromKey to ClassIsAssignableFrom,
        ClassGetSuperclassKey to ClassGetSuperclass,
        ClassGetInterfaces0Key to ClassGetInterfaces0,
        ClassDesiredAssertionStatus0Key to ClassDesiredAssertionStatus0,
        ClassIsHiddenKey to ClassIsHidden,
        ThrowableFillInStackTraceKey to ThrowableFillInStackTrace,
        StringInternKey to StringIntern,
        ThreadRegisterNativesKey to ThreadRegisterNatives,
        ThreadCurrentThreadKey to ThreadCurrentThread,
        ThreadCurrentCarrierThreadKey to ThreadCurrentCarrierThread,
        ThreadFindScopedValueBindingsKey to ThreadFindScopedValueBindings,
        ThreadScopedValueCacheKey to ThreadScopedValueCache,
        ThreadSetScopedValueCacheKey to ThreadSetScopedValueCache,
        ThreadGetThreadsKey to ThreadGetThreads,
        ThreadClearInterruptEventKey to ThreadClearInterruptEvent,
        ThreadSetNativeNameKey to ThreadSetNativeName,
        ThreadSetPriority0Key to ThreadSetPriority0,
        ThreadInterrupt0Key to ThreadInterrupt0,
        ThreadStart0Key to ThreadStart0,
        ThreadSetCurrentThreadKey to ThreadSetCurrentThread,
        ThreadGetStackTrace0Key to ThreadGetStackTrace0,
        ThreadDumpThreadsKey to ThreadDumpThreads,
        ThreadGetNextThreadIdOffsetKey to ThreadGetNextThreadIdOffset,
        ThreadYield0Key to ThreadYield0,
        ThreadHoldsLockKey to ThreadHoldsLock,
        ThreadEnsureMaterializedForStackWalkKey to ThreadEnsureMaterializedForStackWalk,
        ThreadSleepMillisKey to ThreadSleepMillis,
        ThreadSleepMillisNanosKey to ThreadSleepMillisNanos,
        ThreadSleepNanos0Key to ThreadSleepNanos0,
        UnsafeRegisterNativesKey to UnsafeRegisterNatives,
        UnsafeGetLongVolatileKey to UnsafeGetLongVolatile,
        UnsafeGetLongKey to UnsafeGetLong,
        UnsafeGetReferenceVolatileKey to UnsafeGetReferenceVolatile,
        UnsafePutReferenceVolatileKey to UnsafePutReferenceVolatile,
        UnsafeGetReferenceKey to UnsafeGetReference,
        UnsafePutReferenceKey to UnsafePutReference,
        UnsafeCompareAndSetReferenceKey to UnsafeCompareAndSetReference,
        UnsafeCompareAndExchangeReferenceKey to UnsafeCompareAndExchangeReference,
        UnsafeGetIntVolatileKey to UnsafeGetIntVolatile,
        UnsafeGetIntKey to UnsafeGetInt,
        UnsafePutIntVolatileKey to UnsafePutIntVolatile,
        UnsafePutIntKey to UnsafePutInt,
        UnsafeCompareAndSetIntKey to UnsafeCompareAndSetInt,
        UnsafeCompareAndExchangeIntKey to UnsafeCompareAndExchangeInt,
        UnsafePutLongVolatileKey to UnsafePutLongVolatile,
        UnsafePutLongKey to UnsafePutLong,
        UnsafeCompareAndSetLongKey to UnsafeCompareAndSetLong,
        UnsafeCompareAndExchangeLongKey to UnsafeCompareAndExchangeLong,
        UnsafeArrayBaseOffset0Key to UnsafeArrayBaseOffset0,
        UnsafeFullFenceKey to UnsafeFullFence,
        UnsafeLoadFenceKey to UnsafeLoadFence,
        UnsafeStoreFenceKey to UnsafeStoreFence,
    )

    private const val NativeLibrariesNativeLibraryImplClassName =
        "jdk/internal/loader/NativeLibraries\$NativeLibraryImpl"
    private const val ThreadNextThreadIdSyntheticOffset = 1L
    private const val UnsafeSyntheticArrayBaseOffset = 0
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

    private fun systemStreamSetter(name: String, field: JvmFieldReference): JvmNativeMethodIntrinsic =
        JvmNativeMethodIntrinsic { context, invocation ->
            if (invocation.receiver != null || invocation.arguments.size != 1) {
                throw JvmUnsupportedInstructionException("$name expects one reference argument")
            }
            val value = invocation.arguments.single() as? JvmReferenceValue
                ?: throw JvmUnsupportedInstructionException("$name expects one reference argument")
            if (value is JvmObjectReferenceValue) {
                context.heap.get(value)
            }
            context.staticFields.put(field, value)
            null
        }

    private fun requireClassMirrorReceiver(
        name: String,
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        requireNoArguments(name, invocation)
        return requireClassMirrorReceiverWithArguments(name, context, invocation)
    }

    private fun requireClassMirrorReceiverWithArguments(
        name: String,
        context: JvmNativeMethodContext,
        invocation: JvmNativeMethodInvocation,
    ): String {
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("$name intrinsic requires a receiver")
        return requireClassMirrorReference(name, context, receiver)
    }

    private fun requireClassMirrorReference(
        name: String,
        context: JvmNativeMethodContext,
        reference: JvmObjectReferenceValue,
    ): String =
        when (val payload = context.heap.get(reference).payload) {
            is JvmClassPayload -> payload.representedClassName
            else -> throw JvmUnsupportedInstructionException(
                "$name intrinsic requires a java/lang/Class mirror receiver",
            )
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
