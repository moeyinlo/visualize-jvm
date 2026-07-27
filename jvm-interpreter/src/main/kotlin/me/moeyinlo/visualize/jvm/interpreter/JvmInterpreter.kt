package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInterfaceMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.jni.JvmJniUpcallDispatcher
import me.moeyinlo.visualize.jvm.jni.JvmJniUpcallException
import me.moeyinlo.visualize.jvm.jni.JvmNativeGuestException
import me.moeyinlo.visualize.jvm.jni.JvmNativeLibraryLoader
import me.moeyinlo.visualize.jvm.jni.JvmSimulatedJavaVm
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmBootstrapArgument
import me.moeyinlo.visualize.jvm.runtime.JvmBootstrapMethodAccessException
import me.moeyinlo.visualize.jvm.runtime.JvmBootstrapMethodTable
import me.moeyinlo.visualize.jvm.runtime.JvmByteArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationState
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmDynamicConstantBootstrapInvocation
import me.moeyinlo.visualize.jvm.runtime.JvmDynamicConstantLinkageException
import me.moeyinlo.visualize.jvm.runtime.JvmDynamicConstantRegistry
import me.moeyinlo.visualize.jvm.runtime.JvmDynamicConstantResolver
import me.moeyinlo.visualize.jvm.runtime.JvmExceptionHandler
import me.moeyinlo.visualize.jvm.runtime.JvmExceptionHandlerTable
import me.moeyinlo.visualize.jvm.runtime.JvmFloatArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmInvokeDynamicBootstrapInvocation
import me.moeyinlo.visualize.jvm.runtime.JvmInvokeDynamicCallSiteKey
import me.moeyinlo.visualize.jvm.runtime.JvmInvokeDynamicCallSiteResolver
import me.moeyinlo.visualize.jvm.runtime.JvmInvokeDynamicCallSiteRegistry
import me.moeyinlo.visualize.jvm.runtime.JvmInvokeDynamicLinkageException
import me.moeyinlo.visualize.jvm.runtime.JvmLocalVariables
import me.moeyinlo.visualize.jvm.runtime.JvmLongArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmLineNumberTableEntry
import me.moeyinlo.visualize.jvm.runtime.JvmLinkedInvokeDynamicCallSite
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandleReferenceKind
import me.moeyinlo.visualize.jvm.runtime.JvmMethodHandleTarget
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorEnterResult
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorOwnershipException
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmThreadScheduler
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmOperandStack
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmReturnAddressValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortArrayPayload
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmRuntimeConstantPoolIndex
import me.moeyinlo.visualize.jvm.runtime.JvmValue
import me.moeyinlo.visualize.jvm.runtime.JvmThreadSchedulingState

data class JvmExecutionResult(
    val operandStack: JvmOperandStack,
)

sealed interface JvmScheduledThreadExecutionResult {
    data class Completed(val result: JvmExecutionResult) : JvmScheduledThreadExecutionResult

    data class Suspended(val suspension: JvmThreadSuspendedException) : JvmScheduledThreadExecutionResult
}

data class JvmScheduledThreadFrame(
    val threadId: String,
    val code: ByteArray,
    val maxStack: Int,
    val constantPool: ConstantPool = ConstantPool.fromEntries(emptyList()),
    val localVariables: JvmLocalVariables = JvmLocalVariables(maxLocals = 0),
    val currentClassName: String? = null,
    val exceptionHandlers: List<JvmExceptionHandler> = emptyList(),
    val bootstrapMethods: JvmBootstrapMethodTable = JvmBootstrapMethodTable(),
    val invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry = JvmInvokeDynamicCallSiteRegistry(),
    val dynamicConstants: JvmDynamicConstantRegistry = JvmDynamicConstantRegistry(),
    val operandStackValues: List<JvmValue> = emptyList(),
    val startBytecodeOffset: Int = 0,
) {
    init {
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        require(maxStack >= 0) { "max_stack must not be negative: $maxStack" }
        require(startBytecodeOffset >= 0) { "start bytecode offset must not be negative: $startBytecodeOffset" }
        JvmOperandStack.fromValues(maxStack = maxStack, values = operandStackValues)
    }
}

data class JvmScheduledThreadsExecutionResult(
    val completedThreads: Map<String, JvmExecutionResult>,
    val suspendedThreads: Map<String, JvmThreadSuspendedException>,
    val executedThreadIds: List<String>,
    val stalledThreadIds: List<String> = emptyList(),
)

class JvmScheduledThreadSwitchLimitException(
    val maxThreadSwitches: Int,
    val executedThreadIds: List<String>,
    val remainingThreadIds: List<String>,
    message: String,
) : IllegalStateException(message)

private data class JvmFrameExecutionResult(
    val operandStack: JvmOperandStack,
    val hasReturned: Boolean = false,
    val returnValue: JvmValue? = null,
)

class JvmUnsupportedInstructionException(message: String) : IllegalStateException(message)

class JvmThrownException(
    val throwable: JvmObjectReferenceValue,
    val guestClassName: String,
    message: String,
) : RuntimeException(message)

class JvmArithmeticException(
    val guestClassName: String,
    message: String,
) : ArithmeticException(message)

class JvmNegativeArraySizeException(
    val guestClassName: String,
    message: String,
) : NegativeArraySizeException(message)

class JvmArrayIndexOutOfBoundsException(
    val guestClassName: String,
    message: String,
) : ArrayIndexOutOfBoundsException(message)

class JvmNullPointerException(
    val guestClassName: String,
    message: String,
) : NullPointerException(message)

class JvmArrayStoreException(
    val guestClassName: String,
    message: String,
) : ArrayStoreException(message)

class JvmClassCastException(
    val guestClassName: String,
    message: String,
) : ClassCastException(message)

class JvmIllegalMonitorStateException(
    val guestClassName: String,
    message: String,
) : IllegalMonitorStateException(message)

class JvmMonitorBlockedException(
    val objectReference: JvmObjectReferenceValue,
    val threadId: String,
    val ownerThreadId: String,
    val blockedThreadIds: List<String>,
    val bytecodeOffset: Int,
    message: String,
) : IllegalStateException(message)

class JvmThreadSuspendedException(
    val threadId: String,
    val state: JvmThreadSchedulingState,
    val suspendedAtBytecodeOffset: Int,
    val nextBytecodeOffset: Int?,
    val operandStackValues: List<JvmValue> = emptyList(),
    message: String,
) : IllegalStateException(message)

class JvmIncompatibleClassChangeError(
    val guestClassName: String,
    message: String,
) : IncompatibleClassChangeError(message)

class JvmIllegalAccessError(
    val guestClassName: String,
    message: String,
) : IllegalAccessError(message)

class JvmAbstractMethodError(
    val guestClassName: String,
    message: String,
) : AbstractMethodError(message)

class JvmUnsatisfiedLinkError(
    val guestClassName: String,
    message: String,
) : UnsatisfiedLinkError(message)

object JvmInterpreter {
    private val intLikeFieldDescriptors = setOf("Z", "B", "C", "S", "I")

    fun jniUpcallDispatcher(
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields = JvmStaticFields(),
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry = JvmNativeMethodRegistry.Empty,
        monitors: JvmMonitorState = JvmMonitorState(),
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String = "main",
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String? = null,
        dynamicConstants: JvmDynamicConstantRegistry = JvmDynamicConstantRegistry(),
    ): JvmJniUpcallDispatcher = object : JvmJniUpcallDispatcher {
        override fun callVoidMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ) {
            executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
        }

        override fun callObjectMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmReferenceValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmReferenceValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallObjectMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                        "expected JvmReferenceValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callBooleanMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmBooleanValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallBooleanMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
            )
            return JvmBooleanValue(intValue.value != 0)
        }

        override fun callByteMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmByteValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallByteMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
            )
            return JvmByteValue(intValue.value)
        }

        override fun callCharMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmCharValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallCharMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
            return JvmCharValue(intValue.value)
        }

        override fun callShortMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmShortValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallShortMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
            return JvmShortValue(intValue.value)
        }

        override fun callIntMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmIntValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallIntMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callLongMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmLongValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmLongValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallLongMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmLongValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callFloatMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmFloatValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmFloatValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallFloatMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmFloatValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callDoubleMethod(
            receiver: JvmObjectReferenceValue,
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmDoubleValue {
            val returnValue = executeInstanceMethodUpcall(
                receiver = receiver,
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmDoubleValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallDoubleMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmDoubleValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callStaticVoidMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ) {
            executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
        }

        override fun callStaticObjectMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmReferenceValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmReferenceValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticObjectMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                        "expected JvmReferenceValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callStaticBooleanMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmBooleanValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticBooleanMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
            )
            return JvmBooleanValue(intValue.value != 0)
        }

        override fun callStaticByteMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmByteValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticByteMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
            return JvmByteValue(intValue.value)
        }

        override fun callStaticCharMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmCharValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticCharMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
            return JvmCharValue(intValue.value)
        }

        override fun callStaticShortMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmShortValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            val intValue = returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticShortMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
            return JvmShortValue(intValue.value)
        }

        override fun callStaticIntMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmIntValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmIntValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticIntMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmIntValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callStaticLongMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmLongValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmLongValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticLongMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmLongValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callStaticFloatMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmFloatValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmFloatValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticFloatMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmFloatValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }

        override fun callStaticDoubleMethod(
            method: JvmResolvedMethod,
            arguments: List<JvmValue>,
        ): JvmDoubleValue {
            val returnValue = executeStaticMethodUpcall(
                ownerClassName = method.ownerClassName,
                name = method.name,
                descriptor = method.descriptor,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                dynamicConstants = dynamicConstants,
            )
            return returnValue as? JvmDoubleValue
                ?: throw JvmJniUpcallException(
                    "Invalid interpreter-backed CallStaticDoubleMethod return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: expected JvmDoubleValue but was " +
                        (returnValue?.javaClass?.simpleName ?: "void"),
                )
        }
    }

    fun execute(
        code: ByteArray,
        maxStack: Int,
        constantPool: ConstantPool = ConstantPool.fromEntries(emptyList()),
        heap: JvmHeap = JvmHeap(),
        localVariables: JvmLocalVariables = JvmLocalVariables(maxLocals = 0),
        classHierarchy: JvmClassHierarchy = JvmClassHierarchy.Empty,
        staticFields: JvmStaticFields = JvmStaticFields(),
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry = JvmNativeMethodRegistry.Empty,
        monitors: JvmMonitorState = JvmMonitorState(),
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String = "main",
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String? = null,
        currentMethodName: String = "<main>",
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        exceptionHandlers: List<JvmExceptionHandler> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable = JvmBootstrapMethodTable(),
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry = JvmInvokeDynamicCallSiteRegistry(),
        dynamicConstants: JvmDynamicConstantRegistry = JvmDynamicConstantRegistry(),
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
        nativeLibraryLoader: JvmNativeLibraryLoader? = null,
        javaVm: JvmSimulatedJavaVm? = null,
        initialOperandStackValues: List<JvmValue> = emptyList(),
        startBytecodeOffset: Int = 0,
    ): JvmExecutionResult {
        val effectiveLoadNativeLibraryHandler = if (nativeLibraryLoader == null) {
            loadNativeLibraryHandler
        } else {
            nativeLibraryLoader.loadHook(
                javaVm ?: throw JvmUnsupportedInstructionException(
                    "Native library loader requires a simulated JavaVM",
                ),
            )
        }
        val effectiveUnloadNativeLibraryHandler = if (nativeLibraryLoader == null) {
            unloadNativeLibraryHandler
        } else {
            nativeLibraryLoader.unloadHook(
                javaVm ?: throw JvmUnsupportedInstructionException(
                    "Native library loader requires a simulated JavaVM",
                ),
            )
        }
        val frameResult = executeFrame(
            code = code,
            maxStack = maxStack,
            startBytecodeOffset = startBytecodeOffset,
            constantPool = constantPool,
            heap = heap,
            localVariables = localVariables,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = currentClassName,
            currentMethodName = currentMethodName,
            currentSourceFile = currentSourceFile,
            currentLineNumberTable = currentLineNumberTable,
            allowReturn = false,
            exceptionHandlers = exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = effectiveLoadNativeLibraryHandler,
            unloadNativeLibraryHandler = effectiveUnloadNativeLibraryHandler,
            initialOperandStackValues = initialOperandStackValues,
        )
        return JvmExecutionResult(operandStack = frameResult.operandStack)
    }

    fun executeScheduledThread(
        code: ByteArray,
        maxStack: Int,
        constantPool: ConstantPool = ConstantPool.fromEntries(emptyList()),
        heap: JvmHeap = JvmHeap(),
        localVariables: JvmLocalVariables = JvmLocalVariables(maxLocals = 0),
        classHierarchy: JvmClassHierarchy = JvmClassHierarchy.Empty,
        staticFields: JvmStaticFields = JvmStaticFields(),
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry = JvmNativeMethodRegistry.Empty,
        monitors: JvmMonitorState = JvmMonitorState(),
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String = "main",
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String? = null,
        exceptionHandlers: List<JvmExceptionHandler> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable = JvmBootstrapMethodTable(),
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry = JvmInvokeDynamicCallSiteRegistry(),
        dynamicConstants: JvmDynamicConstantRegistry = JvmDynamicConstantRegistry(),
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
        nativeLibraryLoader: JvmNativeLibraryLoader? = null,
        javaVm: JvmSimulatedJavaVm? = null,
        initialOperandStackValues: List<JvmValue> = emptyList(),
        startBytecodeOffset: Int = 0,
    ): JvmScheduledThreadExecutionResult =
        try {
            JvmScheduledThreadExecutionResult.Completed(
                execute(
                    code = code,
                    maxStack = maxStack,
                    constantPool = constantPool,
                    heap = heap,
                    localVariables = localVariables,
                    classHierarchy = classHierarchy,
                    staticFields = staticFields,
                    classInitializationStates = classInitializationStates,
                    nativeMethods = nativeMethods,
                    monitors = monitors,
                    threadScheduler = threadScheduler,
                    currentThreadId = currentThreadId,
                    monitorUnblockedHandler = monitorUnblockedHandler,
                    currentClassName = currentClassName,
                    exceptionHandlers = exceptionHandlers,
                    bootstrapMethods = bootstrapMethods,
                    invokeDynamicCallSites = invokeDynamicCallSites,
                    dynamicConstants = dynamicConstants,
                    loadNativeLibraryHandler = loadNativeLibraryHandler,
                    unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                    nativeLibraryLoader = nativeLibraryLoader,
                    javaVm = javaVm,
                    initialOperandStackValues = initialOperandStackValues,
                    startBytecodeOffset = startBytecodeOffset,
                ),
            )
        } catch (exception: JvmThreadSuspendedException) {
            threadScheduler?.suspendThread(exception.threadId, exception.state)
            JvmScheduledThreadExecutionResult.Suspended(exception)
        } catch (exception: JvmMonitorBlockedException) {
            val schedulerState = threadScheduler?.state(currentThreadId)
                ?.takeIf { state -> state != JvmThreadSchedulingState.Runnable }
                ?: JvmThreadSchedulingState.BlockedOnMonitor(
                    reference = exception.objectReference,
                    ownerThreadId = exception.ownerThreadId,
                )
            JvmScheduledThreadExecutionResult.Suspended(
                JvmThreadSuspendedException(
                    threadId = exception.threadId,
                    state = schedulerState,
                    suspendedAtBytecodeOffset = exception.bytecodeOffset,
                    nextBytecodeOffset = exception.bytecodeOffset,
                    operandStackValues = listOf(exception.objectReference),
                    message = exception.message ?: "Thread ${exception.threadId} is blocked entering a monitor",
                ),
            )
        }

    fun executeScheduledThreads(
        frames: List<JvmScheduledThreadFrame>,
        heap: JvmHeap = JvmHeap(),
        classHierarchy: JvmClassHierarchy = JvmClassHierarchy.Empty,
        staticFields: JvmStaticFields = JvmStaticFields(),
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry = JvmNativeMethodRegistry.Empty,
        monitors: JvmMonitorState = JvmMonitorState(),
        threadScheduler: JvmThreadScheduler = JvmThreadScheduler(),
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
        nativeLibraryLoader: JvmNativeLibraryLoader? = null,
        javaVm: JvmSimulatedJavaVm? = null,
        maxThreadSwitches: Int = 1024,
    ): JvmScheduledThreadsExecutionResult {
        require(maxThreadSwitches > 0) { "max thread switches must be positive: $maxThreadSwitches" }
        val threadOrder = frames.map { frame -> frame.threadId }
        require(threadOrder.toSet().size == threadOrder.size) { "scheduled thread ids must be unique" }

        val remainingFrames = linkedMapOf<String, JvmScheduledThreadFrame>()
        frames.forEach { frame -> remainingFrames[frame.threadId] = frame }
        val completedThreads = linkedMapOf<String, JvmExecutionResult>()
        val suspendedThreads = linkedMapOf<String, JvmThreadSuspendedException>()
        val executedThreadIds = mutableListOf<String>()
        var stalledThreadIds = emptyList<String>()
        var previousThreadId: String? = null
        var switchCount = 0

        while (remainingFrames.isNotEmpty()) {
            val remainingThreadIds = threadOrder.filter { threadId -> threadId in remainingFrames }
            if (switchCount >= maxThreadSwitches) {
                throw JvmScheduledThreadSwitchLimitException(
                    maxThreadSwitches = maxThreadSwitches,
                    executedThreadIds = executedThreadIds.toList(),
                    remainingThreadIds = remainingThreadIds,
                    message = "Scheduled execution exceeded max thread switches $maxThreadSwitches",
                )
            }
            switchCount += 1

            val threadId = threadScheduler.nextRunnableThreadId(
                threadIds = remainingThreadIds,
                afterThreadId = previousThreadId,
            ) ?: run {
                stalledThreadIds = remainingThreadIds
                break
            }
            previousThreadId = threadId

            when (threadScheduler.resumePendingMonitorReentry(monitors, threadId)) {
                is JvmMonitorEnterResult.Blocked -> continue
                is JvmMonitorEnterResult.Acquired,
                null,
                -> Unit
            }

            val frame = remainingFrames.getValue(threadId)
            executedThreadIds += threadId
            when (
                val result = executeScheduledThread(
                    code = frame.code,
                    maxStack = frame.maxStack,
                    constantPool = frame.constantPool,
                    heap = heap,
                    localVariables = frame.localVariables,
                    classHierarchy = classHierarchy,
                    staticFields = staticFields,
                    classInitializationStates = classInitializationStates,
                    nativeMethods = nativeMethods,
                    monitors = monitors,
                    threadScheduler = threadScheduler,
                    currentThreadId = threadId,
                    monitorUnblockedHandler = monitorUnblockedHandler,
                    currentClassName = frame.currentClassName,
                    exceptionHandlers = frame.exceptionHandlers,
                    bootstrapMethods = frame.bootstrapMethods,
                    invokeDynamicCallSites = frame.invokeDynamicCallSites,
                    dynamicConstants = frame.dynamicConstants,
                    loadNativeLibraryHandler = loadNativeLibraryHandler,
                    unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                    nativeLibraryLoader = nativeLibraryLoader,
                    javaVm = javaVm,
                    initialOperandStackValues = frame.operandStackValues,
                    startBytecodeOffset = frame.startBytecodeOffset,
                )
            ) {
                is JvmScheduledThreadExecutionResult.Completed -> {
                    remainingFrames.remove(threadId)
                    suspendedThreads.remove(threadId)
                    completedThreads[threadId] = result.result
                }
                is JvmScheduledThreadExecutionResult.Suspended -> {
                    suspendedThreads[threadId] = result.suspension
                    val nextBytecodeOffset = result.suspension.nextBytecodeOffset
                    if (nextBytecodeOffset != null) {
                        remainingFrames[threadId] = frame.copy(
                            startBytecodeOffset = nextBytecodeOffset,
                            operandStackValues = result.suspension.operandStackValues,
                        )
                    }
                }
            }
        }

        return JvmScheduledThreadsExecutionResult(
            completedThreads = completedThreads,
            suspendedThreads = suspendedThreads,
            executedThreadIds = executedThreadIds,
            stalledThreadIds = stalledThreadIds,
        )
    }

    private fun executeFrame(
        code: ByteArray,
        maxStack: Int,
        startBytecodeOffset: Int = 0,
        constantPool: ConstantPool,
        heap: JvmHeap,
        localVariables: JvmLocalVariables,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        currentMethodName: String = "<active-use>",
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        allowReturn: Boolean,
        exceptionHandlers: List<JvmExceptionHandler>,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
        initialOperandStackValues: List<JvmValue> = emptyList(),
    ): JvmFrameExecutionResult {
        val operandStack = JvmOperandStack.fromValues(maxStack = maxStack, values = initialOperandStackValues)
        val instructions = BytecodeDecoder.decode(code)
        val instructionIndexByOffset = instructions
            .mapIndexed { index, instruction -> instruction.offset to index }
            .toMap()
        var instructionIndex = if (instructions.isEmpty()) {
            0
        } else {
            instructionIndexByOffset[startBytecodeOffset]
                ?: throw JvmUnsupportedInstructionException(
                    "Invalid start bytecode offset $startBytecodeOffset: target is not an instruction offset",
                )
        }
        while (instructionIndex < instructions.size) {
            val instruction = instructions[instructionIndex]
            try {
                if (allowReturn && instruction.metadata.opcode in 0xAC..0xB1) {
                    return executeReturnInstruction(instruction, operandStack)
                }
                val branchTargetOffset = when (instruction.metadata.opcode) {
                    0x99 -> executeIntBranch(instruction, operandStack) { value -> value == 0 }
                    0x9A -> executeIntBranch(instruction, operandStack) { value -> value != 0 }
                    0x9B -> executeIntBranch(instruction, operandStack) { value -> value < 0 }
                    0x9C -> executeIntBranch(instruction, operandStack) { value -> value >= 0 }
                    0x9D -> executeIntBranch(instruction, operandStack) { value -> value > 0 }
                    0x9E -> executeIntBranch(instruction, operandStack) { value -> value <= 0 }
                    0x9F -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 == value2 }
                    0xA0 -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 != value2 }
                    0xA1 -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 < value2 }
                    0xA2 -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 >= value2 }
                    0xA3 -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 > value2 }
                    0xA4 -> executeIntCompareBranch(instruction, operandStack) { value1, value2 -> value1 <= value2 }
                    0xA5 -> executeReferenceCompareBranch(instruction, operandStack) { value1, value2 -> value1 == value2 }
                    0xA6 -> executeReferenceCompareBranch(instruction, operandStack) { value1, value2 -> value1 != value2 }
                    0xA7 -> instruction.branchTargetOffset()
                    0xA8 -> executeSubroutineBranch(instruction, operandStack)
                    0xA9 -> executeSubroutineReturn(instruction, localVariables)
                    0xAA -> executeTableSwitch(instruction, operandStack)
                    0xAB -> executeLookupSwitch(instruction, operandStack)
                    0xC4 -> executeWideOrSubroutineReturn(instruction, operandStack, localVariables)
                    0xC6 -> executeReferenceBranch(instruction, operandStack) { value -> value == JvmNullValue }
                    0xC7 -> executeReferenceBranch(instruction, operandStack) { value -> value != JvmNullValue }
                    0xC8 -> instruction.wideBranchTargetOffset()
                    0xC9 -> executeWideSubroutineBranch(instruction, operandStack)
                    else -> {
                        executeInstruction(
                            instruction,
                            operandStack,
                            constantPool,
                            heap,
                            localVariables,
                            classHierarchy,
                            staticFields,
                            classInitializationStates,
                            nativeMethods,
                            monitors,
                            threadScheduler,
                            currentThreadId,
                            monitorUnblockedHandler,
                            currentClassName,
                            currentMethodName,
                            currentSourceFile,
                            currentLineNumberTable,
                            bootstrapMethods,
                            invokeDynamicCallSites,
                            dynamicConstants,
                            loadNativeLibraryHandler,
                            unloadNativeLibraryHandler,
                        )
                        throwIfCurrentThreadSuspended(
                            threadScheduler = threadScheduler,
                            currentThreadId = currentThreadId,
                            suspendedAtBytecodeOffset = instruction.offset,
                            nextBytecodeOffset = instructions.getOrNull(instructionIndex + 1)?.offset,
                            operandStackValues = operandStack.toList(),
                        )
                        null
                    }
                }
                instructionIndex = if (branchTargetOffset == null) {
                    instructionIndex + 1
                } else {
                    instructionIndexByOffset[branchTargetOffset]
                        ?: throw JvmUnsupportedInstructionException(
                            "Invalid ${instruction.metadata.mnemonic} branch target $branchTargetOffset " +
                                "at offset ${instruction.offset}: target is not an instruction offset",
                        )
                }
            } catch (exception: JvmThrownException) {
                instructionIndex = dispatchExistingGuestThrowableToHandler(
                    instruction = instruction,
                    throwable = exception.throwable,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNullPointerException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmArithmeticException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNegativeArraySizeException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmArrayIndexOutOfBoundsException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmClassCastException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmArrayStoreException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmIllegalMonitorStateException) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmIncompatibleClassChangeError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmIllegalAccessError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmAbstractMethodError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmUnsatisfiedLinkError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNativeGuestException) {
                instructionIndex = dispatchExistingGuestThrowableToHandler(
                    instruction = instruction,
                    throwable = exception.throwable,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNoClassDefFoundError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNoSuchFieldError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            } catch (exception: JvmNoSuchMethodError) {
                instructionIndex = dispatchCreatedGuestThrowableToHandler(
                    instruction = instruction,
                    guestClassName = exception.guestClassName,
                    originalException = exception,
                    operandStack = operandStack,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    exceptionHandlers = exceptionHandlers,
                    instructionIndexByOffset = instructionIndexByOffset,
                )
            }
        }
        return JvmFrameExecutionResult(operandStack = operandStack)
    }

    private fun throwIfCurrentThreadSuspended(
        threadScheduler: JvmThreadScheduler?,
        currentThreadId: String,
        suspendedAtBytecodeOffset: Int,
        nextBytecodeOffset: Int?,
        operandStackValues: List<JvmValue> = emptyList(),
    ) {
        val state = threadScheduler?.state(currentThreadId) ?: return
        if (state != JvmThreadSchedulingState.Runnable) {
            throw JvmThreadSuspendedException(
                threadId = currentThreadId,
                state = state,
                suspendedAtBytecodeOffset = suspendedAtBytecodeOffset,
                nextBytecodeOffset = nextBytecodeOffset,
                operandStackValues = operandStackValues,
                message = "Thread $currentThreadId is suspended in scheduler state $state",
            )
        }
    }

    private fun dispatchExistingGuestThrowableToHandler(
        instruction: DecodedInstruction,
        throwable: JvmObjectReferenceValue,
        originalException: Throwable,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        exceptionHandlers: List<JvmExceptionHandler>,
        instructionIndexByOffset: Map<Int, Int>,
    ): Int {
        val throwableClassName = heap.get(throwable).className
        val handler = JvmExceptionHandlerTable.findHandler(
            handlers = exceptionHandlers,
            thrownAtPc = instruction.offset,
            throwableClassName = throwableClassName,
            classHierarchy = classHierarchy,
        ) ?: throw originalException
        resetOperandStackForExceptionHandler(operandStack, throwable)
        return exceptionHandlerInstructionIndex(instruction, handler, instructionIndexByOffset)
    }

    private fun dispatchCreatedGuestThrowableToHandler(
        instruction: DecodedInstruction,
        guestClassName: String,
        originalException: Throwable,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        exceptionHandlers: List<JvmExceptionHandler>,
        instructionIndexByOffset: Map<Int, Int>,
    ): Int {
        val handler = JvmExceptionHandlerTable.findHandler(
            handlers = exceptionHandlers,
            thrownAtPc = instruction.offset,
            throwableClassName = guestClassName,
            classHierarchy = classHierarchy,
        ) ?: throw originalException
        val throwable = heap.allocateObject(guestClassName)
        resetOperandStackForExceptionHandler(operandStack, throwable)
        return exceptionHandlerInstructionIndex(instruction, handler, instructionIndexByOffset)
    }

    private fun exceptionHandlerInstructionIndex(
        instruction: DecodedInstruction,
        handler: JvmExceptionHandler,
        instructionIndexByOffset: Map<Int, Int>,
    ): Int {
        return instructionIndexByOffset[handler.handlerPc]
            ?: throw JvmUnsupportedInstructionException(
                "Invalid exception handler target ${handler.handlerPc} for " +
                    "${instruction.metadata.mnemonic} at offset ${instruction.offset}: " +
                    "target is not an instruction offset",
            )
    }

    private fun resetOperandStackForExceptionHandler(
        operandStack: JvmOperandStack,
        throwable: JvmObjectReferenceValue,
    ) {
        while (operandStack.valueCount > 0) {
            operandStack.pop()
        }
        operandStack.push(throwable)
    }

    private fun executeInstruction(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        localVariables: JvmLocalVariables,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        currentMethodName: String,
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        when (instruction.metadata.opcode) {
            0x00 -> Unit
            0x01 -> operandStack.push(JvmNullValue)
            in 0x02..0x08 -> operandStack.push(JvmIntValue(instruction.metadata.opcode - 0x03))
            in 0x09..0x0A -> operandStack.push(JvmLongValue((instruction.metadata.opcode - 0x09).toLong()))
            in 0x0B..0x0D -> operandStack.push(JvmFloatValue((instruction.metadata.opcode - 0x0B).toFloat()))
            in 0x0E..0x0F -> operandStack.push(JvmDoubleValue((instruction.metadata.opcode - 0x0E).toDouble()))
            0x10 -> operandStack.push(JvmIntValue(instruction.operands[0].toByte().toInt()))
            0x11 -> operandStack.push(
                JvmIntValue(((instruction.operands[0] shl 8) or instruction.operands[1]).toShort().toInt()),
            )
            0x12,
            0x13,
            -> executeLdc(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0x14 -> executeLdc2(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0x15,
            in 0x1A..0x1D,
            -> executeIntLoad(instruction, operandStack, localVariables)
            0x16,
            in 0x1E..0x21,
            -> executeLongLoad(instruction, operandStack, localVariables)
            0x17,
            in 0x22..0x25,
            -> executeFloatLoad(instruction, operandStack, localVariables)
            0x18,
            in 0x26..0x29,
            -> executeDoubleLoad(instruction, operandStack, localVariables)
            0x19,
            in 0x2A..0x2D,
            -> executeReferenceLoad(instruction, operandStack, localVariables)
            0x2E -> executeIntArrayLoad(instruction, operandStack, heap)
            0x2F -> executeLongArrayLoad(instruction, operandStack, heap)
            0x30 -> executeFloatArrayLoad(instruction, operandStack, heap)
            0x31 -> executeDoubleArrayLoad(instruction, operandStack, heap)
            0x32 -> executeReferenceArrayLoad(instruction, operandStack, heap)
            0x33 -> executeByteArrayLoad(instruction, operandStack, heap)
            0x34 -> executeCharArrayLoad(instruction, operandStack, heap)
            0x35 -> executeShortArrayLoad(instruction, operandStack, heap)
            0x36,
            in 0x3B..0x3E,
            -> executeIntStore(instruction, operandStack, localVariables)
            0x37,
            in 0x3F..0x42,
            -> executeLongStore(instruction, operandStack, localVariables)
            0x38,
            in 0x43..0x46,
            -> executeFloatStore(instruction, operandStack, localVariables)
            0x39,
            in 0x47..0x4A,
            -> executeDoubleStore(instruction, operandStack, localVariables)
            0x3A,
            in 0x4B..0x4E,
            -> executeReferenceStore(instruction, operandStack, localVariables)
            0x4F -> executeIntArrayStore(instruction, operandStack, heap)
            0x50 -> executeLongArrayStore(instruction, operandStack, heap)
            0x51 -> executeFloatArrayStore(instruction, operandStack, heap)
            0x52 -> executeDoubleArrayStore(instruction, operandStack, heap)
            0x53 -> executeReferenceArrayStore(instruction, operandStack, heap, classHierarchy)
            0x54 -> executeByteArrayStore(instruction, operandStack, heap)
            0x55 -> executeCharArrayStore(instruction, operandStack, heap)
            0x56 -> executeShortArrayStore(instruction, operandStack, heap)
            0x57 -> executePop(instruction, operandStack)
            0x58 -> executePop2(instruction, operandStack)
            0x59 -> executeDup(instruction, operandStack)
            0x5A -> executeDupX1(instruction, operandStack)
            0x5B -> executeDupX2(instruction, operandStack)
            0x5C -> executeDup2(instruction, operandStack)
            0x5D -> executeDup2X1(instruction, operandStack)
            0x5E -> executeDup2X2(instruction, operandStack)
            0x5F -> executeSwap(instruction, operandStack)
            0x60 -> executeIntAdd(instruction, operandStack)
            0x61 -> executeLongAdd(instruction, operandStack)
            0x62 -> executeFloatAdd(instruction, operandStack)
            0x63 -> executeDoubleAdd(instruction, operandStack)
            0x64 -> executeIntSub(instruction, operandStack)
            0x65 -> executeLongSub(instruction, operandStack)
            0x66 -> executeFloatSub(instruction, operandStack)
            0x67 -> executeDoubleSub(instruction, operandStack)
            0x68 -> executeIntMul(instruction, operandStack)
            0x69 -> executeLongMul(instruction, operandStack)
            0x6A -> executeFloatMul(instruction, operandStack)
            0x6B -> executeDoubleMul(instruction, operandStack)
            0x6C -> executeIntDiv(instruction, operandStack)
            0x6D -> executeLongDiv(instruction, operandStack)
            0x6E -> executeFloatDiv(instruction, operandStack)
            0x6F -> executeDoubleDiv(instruction, operandStack)
            0x70 -> executeIntRem(instruction, operandStack)
            0x71 -> executeLongRem(instruction, operandStack)
            0x72 -> executeFloatRem(instruction, operandStack)
            0x73 -> executeDoubleRem(instruction, operandStack)
            0x74 -> executeIntNeg(instruction, operandStack)
            0x75 -> executeLongNeg(instruction, operandStack)
            0x76 -> executeFloatNeg(instruction, operandStack)
            0x77 -> executeDoubleNeg(instruction, operandStack)
            0x78 -> executeIntShiftLeft(instruction, operandStack)
            0x79 -> executeLongShiftLeft(instruction, operandStack)
            0x7A -> executeIntArithmeticShiftRight(instruction, operandStack)
            0x7B -> executeLongArithmeticShiftRight(instruction, operandStack)
            0x7C -> executeIntLogicalShiftRight(instruction, operandStack)
            0x7D -> executeLongLogicalShiftRight(instruction, operandStack)
            0x7E -> executeIntAnd(instruction, operandStack)
            0x7F -> executeLongAnd(instruction, operandStack)
            0x80 -> executeIntOr(instruction, operandStack)
            0x81 -> executeLongOr(instruction, operandStack)
            0x82 -> executeIntXor(instruction, operandStack)
            0x83 -> executeLongXor(instruction, operandStack)
            0x84 -> executeIncrement(instruction, localVariables)
            0x85 -> executeIntToLong(instruction, operandStack)
            0x86 -> executeIntToFloat(instruction, operandStack)
            0x87 -> executeIntToDouble(instruction, operandStack)
            0x88 -> executeLongToInt(instruction, operandStack)
            0x89 -> executeLongToFloat(instruction, operandStack)
            0x8A -> executeLongToDouble(instruction, operandStack)
            0x8B -> executeFloatToInt(instruction, operandStack)
            0x8C -> executeFloatToLong(instruction, operandStack)
            0x8D -> executeFloatToDouble(instruction, operandStack)
            0x8E -> executeDoubleToInt(instruction, operandStack)
            0x8F -> executeDoubleToLong(instruction, operandStack)
            0x90 -> executeDoubleToFloat(instruction, operandStack)
            0x91 -> executeIntToByte(instruction, operandStack)
            0x92 -> executeIntToChar(instruction, operandStack)
            0x93 -> executeIntToShort(instruction, operandStack)
            0x94 -> executeLongCompare(instruction, operandStack)
            0x95 -> executeFloatCompareLess(instruction, operandStack)
            0x96 -> executeFloatCompareGreater(instruction, operandStack)
            0x97 -> executeDoubleCompareLess(instruction, operandStack)
            0x98 -> executeDoubleCompareGreater(instruction, operandStack)
            0xB2 -> executeGetStatic(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                staticFields = staticFields,
                heap = heap,
                classHierarchy = classHierarchy,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                currentMethodName = currentMethodName,
                currentSourceFile = currentSourceFile,
                currentLineNumberTable = currentLineNumberTable,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0xB3 -> executePutStatic(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                staticFields = staticFields,
                heap = heap,
                classHierarchy = classHierarchy,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                currentMethodName = currentMethodName,
                currentSourceFile = currentSourceFile,
                currentLineNumberTable = currentLineNumberTable,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0xB4 -> executeGetField(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                currentClassName,
            )
            0xB5 -> executePutField(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                currentClassName,
            )
            0xB6 -> executeInvokeVirtual(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                staticFields,
                nativeMethods,
                monitors,
                threadScheduler,
                currentThreadId,
                monitorUnblockedHandler,
                currentClassName,
                bootstrapMethods,
                invokeDynamicCallSites,
                dynamicConstants,
                loadNativeLibraryHandler,
                unloadNativeLibraryHandler,
            )
            0xB7 -> executeInvokeSpecial(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                staticFields,
                nativeMethods,
                monitors,
                threadScheduler,
                currentThreadId,
                monitorUnblockedHandler,
                currentClassName,
                bootstrapMethods,
                invokeDynamicCallSites,
                dynamicConstants,
                loadNativeLibraryHandler,
                unloadNativeLibraryHandler,
            )
            0xB8 -> executeInvokeStatic(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                staticFields,
                classInitializationStates,
                nativeMethods,
                monitors,
                threadScheduler,
                currentThreadId,
                monitorUnblockedHandler,
                currentClassName,
                currentMethodName,
                currentSourceFile,
                currentLineNumberTable,
                bootstrapMethods,
                invokeDynamicCallSites,
                dynamicConstants,
                loadNativeLibraryHandler,
                unloadNativeLibraryHandler,
            )
            0xB9 -> executeInvokeInterface(
                instruction,
                operandStack,
                constantPool,
                heap,
                classHierarchy,
                staticFields,
                nativeMethods,
                monitors,
                threadScheduler,
                currentThreadId,
                monitorUnblockedHandler,
                currentClassName,
                bootstrapMethods,
                invokeDynamicCallSites,
                dynamicConstants,
                loadNativeLibraryHandler,
                unloadNativeLibraryHandler,
            )
            0xBA -> executeInvokeDynamic(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0xBC -> executeNewArray(instruction, operandStack, heap)
            0xBB -> executeNew(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            0xBD -> executeANewArray(instruction, operandStack, constantPool, heap)
            0xBE -> executeArrayLength(instruction, operandStack, heap)
            0xBF -> executeAThrow(instruction, operandStack, heap)
            0xC0 -> executeCheckCast(instruction, operandStack, constantPool, heap, classHierarchy)
            0xC1 -> executeInstanceOf(instruction, operandStack, constantPool, heap, classHierarchy)
            0xC2 -> executeMonitorEnter(instruction, operandStack, heap, monitors, threadScheduler, currentThreadId)
            0xC3 -> executeMonitorExit(
                instruction,
                operandStack,
                heap,
                monitors,
                threadScheduler,
                currentThreadId,
                monitorUnblockedHandler,
            )
            0xC5 -> executeMultiANewArray(instruction, operandStack, constantPool, heap)
            0xC4 -> executeWide(instruction, operandStack, localVariables)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported instruction ${instruction.metadata.mnemonic} " +
                    "(${instruction.metadata.opcode.hexByte()}) at offset ${instruction.offset}",
            )
        }
    }

    private fun executeIntLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeLongLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeFloatLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeDoubleLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeReferenceLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        operandStack.push(value)
    }

    private fun executeIntArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmIntArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmIntArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmIntValue(payload.elements[index.value]))
    }

    private fun executeLongArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmLongArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmLongArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmLongValue(payload.elements[index.value]))
    }

    private fun executeFloatArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmFloatArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmFloatArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmFloatValue(payload.elements[index.value]))
    }

    private fun executeDoubleArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmDoubleArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmDoubleArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmDoubleValue(payload.elements[index.value]))
    }

    private fun executeReferenceArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmReferenceArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmReferenceArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(payload.elements[index.value])
    }

    private fun executeByteArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        val value = when (payload) {
            is JvmByteArrayPayload -> {
                if (index.value !in payload.elements.indices) {
                    throw JvmArrayIndexOutOfBoundsException(
                        guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                        message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                            payload.elements.size,
                    )
                }
                payload.elements[index.value].toInt()
            }
            is JvmBooleanArrayPayload -> {
                if (index.value !in payload.elements.indices) {
                    throw JvmArrayIndexOutOfBoundsException(
                        guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                        message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                            payload.elements.size,
                    )
                }
                if (payload.elements[index.value]) 1 else 0
            }
            else -> throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmByteArrayPayload or JvmBooleanArrayPayload but was " +
                    payload.javaClass.simpleName,
            )
        }
        operandStack.push(JvmIntValue(value))
    }

    private fun executeCharArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmCharArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmCharArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmIntValue(payload.elements[index.value].code))
    }

    private fun executeShortArrayLoad(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmShortArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmShortArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        operandStack.push(JvmIntValue(payload.elements[index.value].toInt()))
    }

    private fun executeIntStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeLongStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeFloatStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeDoubleStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeReferenceStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, value)
    }

    private fun executeIntArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmIntArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmIntArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = value.value
    }

    private fun executeLongArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmLongArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmLongArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = value.value
    }

    private fun executeFloatArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmFloatArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmFloatArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = value.value
    }

    private fun executeDoubleArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmDoubleArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmDoubleArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = value.value
    }

    private fun executeReferenceArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val arrayObject = heap.get(arrayReference)
        val payload = arrayObject.payload
        if (payload !is JvmReferenceArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmReferenceArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        if (!isReferenceAssignableToArrayComponent(value, arrayObject.className, heap, classHierarchy)) {
            val valueClassName = heap.get(value as JvmObjectReferenceValue).className
            throw JvmArrayStoreException(
                guestClassName = "java/lang/ArrayStoreException",
                message = "${instruction.metadata.mnemonic} cannot store $valueClassName into ${arrayObject.className}",
            )
        }
        payload.elements[index.value] = value
    }

    private fun isReferenceAssignableToArrayComponent(
        value: JvmReferenceValue,
        arrayClassName: String,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ): Boolean {
        if (value == JvmNullValue) {
            return true
        }

        val componentClassName = referenceArrayComponentClassName(arrayClassName)
        val valueClassName = heap.get(value as JvmObjectReferenceValue).className
        return classHierarchy.isAssignable(sourceClassName = valueClassName, targetClassName = componentClassName)
    }

    private fun referenceArrayComponentClassName(arrayClassName: String): String =
        if (arrayClassName.startsWith("[L") && arrayClassName.endsWith(";")) {
            arrayClassName.substring(2, arrayClassName.length - 1)
        } else {
            arrayClassName.substring(1)
        }

    private fun executeByteArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        when (payload) {
            is JvmByteArrayPayload -> {
                if (index.value !in payload.elements.indices) {
                    throw JvmArrayIndexOutOfBoundsException(
                        guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                        message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                            payload.elements.size,
                    )
                }
                payload.elements[index.value] = value.value.toByte()
            }
            is JvmBooleanArrayPayload -> {
                if (index.value !in payload.elements.indices) {
                    throw JvmArrayIndexOutOfBoundsException(
                        guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                        message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                            payload.elements.size,
                    )
                }
                payload.elements[index.value] = value.value != 0
            }
            else -> throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmByteArrayPayload or JvmBooleanArrayPayload but was " +
                    payload.javaClass.simpleName,
            )
        }
    }

    private fun executeCharArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmCharArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmCharArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = (value.value and 0xFFFF).toChar()
    }

    private fun executeShortArrayStore(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        val index = operandStack.pop()
        val arrayReference = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} value at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        if (index !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${index.javaClass.simpleName}",
            )
        }
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val payload = heap.get(arrayReference).payload
        if (payload !is JvmShortArrayPayload) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmShortArrayPayload but was ${payload.javaClass.simpleName}",
            )
        }
        if (index.value !in payload.elements.indices) {
            throw JvmArrayIndexOutOfBoundsException(
                guestClassName = "java/lang/ArrayIndexOutOfBoundsException",
                message = "${instruction.metadata.mnemonic} index ${index.value} out of bounds for length " +
                    payload.elements.size,
            )
        }
        payload.elements[index.value] = value.value.toShort()
    }

    private fun executePop(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.peek()
        if (value.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value but was category ${value.category.slotWidth}",
            )
        }
        operandStack.pop()
    }

    private fun executePop2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val top = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (top.category.slotWidth == 2) {
            operandStack.pop()
            return
        }

        val next = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values but found one",
            )
        if (next.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        }
        operandStack.pop()
        operandStack.pop()
    }

    private fun executeDup(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.peek()
        if (value.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value but was category ${value.category.slotWidth}",
            )
        }
        operandStack.push(value)
    }

    private fun executeDupX1(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values but found one",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDupX2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (value1.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected top category 1 value",
            )
        }

        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value over category 2 value " +
                    "or three category 1 values",
            )
        if (value2.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected three category 1 values",
            )
        if (value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 1 value over category 2 value " +
                    "or three category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        if (value1.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value1)
            return
        }

        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values or one category 2 value",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2X1(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 2 value over category 1 value " +
                    "or three category 1 values",
            )

        if (value1.category.slotWidth == 2) {
            if (value2.category.slotWidth != 1) {
                throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected category 2 value over category 1 value",
                )
            }
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected three category 1 values",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected category 2 value over category 1 value " +
                    "or three category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeDup2X2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected a valid dup2_x2 operand form",
            )

        if (value1.category.slotWidth == 2) {
            if (value2.category.slotWidth == 2) {
                operandStack.pop()
                operandStack.pop()
                operandStack.push(value1)
                operandStack.push(value2)
                operandStack.push(value1)
                return
            }

            val value3 = values.dropLast(2).lastOrNull()
                ?: throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected two category 1 values below a category 2 value",
                )
            if (value2.category.slotWidth != 1 || value3.category.slotWidth != 1) {
                throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                        "${instruction.offset}: expected two category 1 values below a category 2 value",
                )
            }

            operandStack.pop()
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value1)
            operandStack.push(value3)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected top two category 1 values or top category 2 value",
            )
        }

        val value3 = values.dropLast(2).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected a third value below two category 1 values",
            )
        if (value3.category.slotWidth == 2) {
            operandStack.pop()
            operandStack.pop()
            operandStack.pop()
            operandStack.push(value2)
            operandStack.push(value1)
            operandStack.push(value3)
            operandStack.push(value2)
            operandStack.push(value1)
            return
        }

        val value4 = values.dropLast(3).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected four category 1 values",
            )
        if (value3.category.slotWidth != 1 || value4.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected four category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.pop()
        operandStack.push(value2)
        operandStack.push(value1)
        operandStack.push(value4)
        operandStack.push(value3)
        operandStack.push(value2)
        operandStack.push(value1)
    }

    private fun executeSwap(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val values = operandStack.toList()
        val value1 = values.lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: operand stack is empty",
            )
        val value2 = values.dropLast(1).lastOrNull()
            ?: throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        if (value1.category.slotWidth != 1 || value2.category.slotWidth != 1) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected two category 1 values",
            )
        }

        operandStack.pop()
        operandStack.pop()
        operandStack.push(value1)
        operandStack.push(value2)
    }

    private fun executeIntAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value + value2.value))
    }

    private fun executeIntSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value - value2.value))
    }

    private fun executeIntMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value * value2.value))
    }

    private fun executeIntDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmIntValue(value1.value / value2.value))
    }

    private fun executeIntRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmIntValue(value1.value % value2.value))
    }

    private fun executeIntNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(-value.value))
    }

    private fun executeIntShiftLeft(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value shl (value2.value and 0x1F)))
    }

    private fun executeIntArithmeticShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value shr (value2.value and 0x1F)))
    }

    private fun executeIntLogicalShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value ushr (value2.value and 0x1F)))
    }

    private fun executeIntAnd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value and value2.value))
    }

    private fun executeIntOr(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value or value2.value))
    }

    private fun executeIntXor(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value xor value2.value))
    }

    private fun executeLongAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value + value2.value))
    }

    private fun executeLongSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value - value2.value))
    }

    private fun executeLongMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value * value2.value))
    }

    private fun executeLongDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0L) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmLongValue(value1.value / value2.value))
    }

    private fun executeLongRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }
        if (value2.value == 0L) {
            throw JvmArithmeticException(
                guestClassName = "java/lang/ArithmeticException",
                message = "${instruction.metadata.mnemonic} at offset ${instruction.offset}: division by zero",
            )
        }

        operandStack.push(JvmLongValue(value1.value % value2.value))
    }

    private fun executeLongNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(-value.value))
    }

    private fun executeLongShiftLeft(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value shl (value2.value and 0x3F)))
    }

    private fun executeLongArithmeticShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value shr (value2.value and 0x3F)))
    }

    private fun executeLongLogicalShiftRight(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value ushr (value2.value and 0x3F)))
    }

    private fun executeLongAnd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value and value2.value))
    }

    private fun executeLongOr(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value or value2.value))
    }

    private fun executeLongXor(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value1.value xor value2.value))
    }

    private fun executeFloatAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value + value2.value))
    }

    private fun executeFloatSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value - value2.value))
    }

    private fun executeFloatMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value * value2.value))
    }

    private fun executeFloatDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value / value2.value))
    }

    private fun executeFloatRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value1.value % value2.value))
    }

    private fun executeFloatNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(-value.value))
    }

    private fun executeDoubleAdd(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value + value2.value))
    }

    private fun executeDoubleSub(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value - value2.value))
    }

    private fun executeDoubleMul(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value * value2.value))
    }

    private fun executeDoubleDiv(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value / value2.value))
    }

    private fun executeDoubleRem(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value1.value % value2.value))
    }

    private fun executeDoubleNeg(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(-value.value))
    }

    private fun executeIncrement(
        instruction: DecodedInstruction,
        localVariables: JvmLocalVariables,
    ) {
        val index = instruction.localVariableIndex()
        val value = localVariables.load(index)
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }
        localVariables.store(index, JvmIntValue(value.value + instruction.incrementConstant()))
    }

    private fun executeIntToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeIntToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
    }

    private fun executeIntToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeLongToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeLongToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
    }

    private fun executeLongToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeFloatToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeFloatToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeFloatToDouble(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmDoubleValue(value.value.toDouble()))
    }

    private fun executeDoubleToInt(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toInt()))
    }

    private fun executeDoubleToLong(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmLongValue(value.value.toLong()))
    }

    private fun executeDoubleToFloat(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmFloatValue(value.value.toFloat()))
    }

    private fun executeIntToByte(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toByte().toInt()))
    }

    private fun executeIntToChar(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value and 0xFFFF))
    }

    private fun executeIntToShort(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value.value.toShort().toInt()))
    }

    private fun executeLongCompare(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmLongValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmLongValue but was ${value1.javaClass.simpleName}",
            )
        }

        operandStack.push(JvmIntValue(value1.value.compareTo(value2.value)))
    }

    private fun executeFloatCompareLess(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        val result = when {
            value1.value.isNaN() || value2.value.isNaN() -> -1
            value1.value > value2.value -> 1
            value1.value == value2.value -> 0
            else -> -1
        }
        operandStack.push(JvmIntValue(result))
    }

    private fun executeFloatCompareGreater(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmFloatValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmFloatValue but was ${value1.javaClass.simpleName}",
            )
        }

        val result = when {
            value1.value.isNaN() || value2.value.isNaN() -> 1
            value1.value > value2.value -> 1
            value1.value == value2.value -> 0
            else -> -1
        }
        operandStack.push(JvmIntValue(result))
    }

    private fun executeDoubleCompareLess(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        val result = when {
            value1.value.isNaN() || value2.value.isNaN() -> -1
            value1.value > value2.value -> 1
            value1.value == value2.value -> 0
            else -> -1
        }
        operandStack.push(JvmIntValue(result))
    }

    private fun executeDoubleCompareGreater(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ) {
        val value2 = operandStack.pop()
        if (value2 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value2.javaClass.simpleName}",
            )
        }
        val value1 = operandStack.pop()
        if (value1 !is JvmDoubleValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmDoubleValue but was ${value1.javaClass.simpleName}",
            )
        }

        val result = when {
            value1.value.isNaN() || value2.value.isNaN() -> 1
            value1.value > value2.value -> 1
            value1.value == value2.value -> 0
            else -> -1
        }
        operandStack.push(JvmIntValue(result))
    }

    private fun executeIntBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        shouldBranch: (Int) -> Boolean,
    ): Int? {
        val value = operandStack.pop()
        if (value !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${value.javaClass.simpleName}",
            )
        }

        return if (shouldBranch(value.value)) {
            instruction.branchTargetOffset()
        } else {
            null
        }
    }

    private fun executeIntCompareBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        shouldBranch: (Int, Int) -> Boolean,
    ): Int? {
        val value2 = operandStack.pop()
        val value1 = operandStack.pop()
        if (value1 !is JvmIntValue || value2 !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operands at offset " +
                    "${instruction.offset}: expected two JvmIntValue operands but found " +
                    "${value1.javaClass.simpleName} and ${value2.javaClass.simpleName}",
            )
        }

        return if (shouldBranch(value1.value, value2.value)) {
            instruction.branchTargetOffset()
        } else {
            null
        }
    }

    private fun executeReferenceCompareBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        shouldBranch: (JvmReferenceValue, JvmReferenceValue) -> Boolean,
    ): Int? {
        val value2 = operandStack.pop()
        val value1 = operandStack.pop()
        if (value1 !is JvmReferenceValue || value2 !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operands at offset " +
                    "${instruction.offset}: expected two JvmReferenceValue operands but found " +
                    "${value1.javaClass.simpleName} and ${value2.javaClass.simpleName}",
            )
        }

        return if (shouldBranch(value1, value2)) {
            instruction.branchTargetOffset()
        } else {
            null
        }
    }

    private fun executeReferenceBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        shouldBranch: (JvmReferenceValue) -> Boolean,
    ): Int? {
        val value = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }

        return if (shouldBranch(value)) {
            instruction.branchTargetOffset()
        } else {
            null
        }
    }

    private fun executeTableSwitch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ): Int {
        val key = operandStack.pop()
        if (key !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${key.javaClass.simpleName}",
            )
        }

        val padding = instruction.switchPadding()
        val defaultOffset = instruction.readSignedOperandInt(padding)
        val low = instruction.readSignedOperandInt(padding + Int.SIZE_BYTES)
        val high = instruction.readSignedOperandInt(padding + Int.SIZE_BYTES * 2)
        val jumpOffset = if (key.value in low..high) {
            instruction.readSignedOperandInt(
                padding + TABLESWITCH_HEADER_BYTES + (key.value - low) * Int.SIZE_BYTES,
            )
        } else {
            defaultOffset
        }
        return instruction.offset + jumpOffset
    }

    private fun executeLookupSwitch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ): Int {
        val key = operandStack.pop()
        if (key !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} operand at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${key.javaClass.simpleName}",
            )
        }

        val padding = instruction.switchPadding()
        val defaultOffset = instruction.readSignedOperandInt(padding)
        val pairCount = instruction.readSignedOperandInt(padding + Int.SIZE_BYTES)
        var pairOffset = padding + LOOKUPSWITCH_HEADER_BYTES
        repeat(pairCount) {
            val match = instruction.readSignedOperandInt(pairOffset)
            val jumpOffset = instruction.readSignedOperandInt(pairOffset + Int.SIZE_BYTES)
            if (key.value == match) {
                return instruction.offset + jumpOffset
            }
            pairOffset += LOOKUPSWITCH_PAIR_BYTES
        }
        return instruction.offset + defaultOffset
    }

    private fun executeSubroutineBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ): Int {
        operandStack.push(JvmReturnAddressValue(instruction.nextInstructionOffset()))
        return instruction.branchTargetOffset()
    }

    private fun executeWideSubroutineBranch(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ): Int {
        operandStack.push(JvmReturnAddressValue(instruction.nextInstructionOffset()))
        return instruction.wideBranchTargetOffset()
    }

    private fun executeSubroutineReturn(
        instruction: DecodedInstruction,
        localVariables: JvmLocalVariables,
    ): Int {
        val index = instruction.localVariableIndex()
        val returnAddress = localVariables.load(index)
        if (returnAddress !is JvmReturnAddressValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} local variable $index at offset " +
                    "${instruction.offset}: expected JvmReturnAddressValue but was ${returnAddress.javaClass.simpleName}",
            )
        }
        return returnAddress.address
    }

    private fun executeWideOrSubroutineReturn(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ): Int? {
        if (instruction.modifiedWideOpcode() == 0xA9) {
            return executeSubroutineReturn(instruction, localVariables)
        }
        executeWide(instruction, operandStack, localVariables)
        return null
    }

    private fun executeWide(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        localVariables: JvmLocalVariables,
    ) {
        when (val modifiedOpcode = instruction.modifiedWideOpcode()) {
            0x15 -> executeIntLoad(instruction, operandStack, localVariables)
            0x16 -> executeLongLoad(instruction, operandStack, localVariables)
            0x17 -> executeFloatLoad(instruction, operandStack, localVariables)
            0x18 -> executeDoubleLoad(instruction, operandStack, localVariables)
            0x19 -> executeReferenceLoad(instruction, operandStack, localVariables)
            0x36 -> executeIntStore(instruction, operandStack, localVariables)
            0x37 -> executeLongStore(instruction, operandStack, localVariables)
            0x38 -> executeFloatStore(instruction, operandStack, localVariables)
            0x39 -> executeDoubleStore(instruction, operandStack, localVariables)
            0x3A -> executeReferenceStore(instruction, operandStack, localVariables)
            0x84 -> executeIncrement(instruction, localVariables)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported wide-modified instruction ${OpcodeTable.metadata(modifiedOpcode).mnemonic} " +
                    "(${modifiedOpcode.hexByte()}) at offset ${instruction.offset}",
            )
        }
    }

    private fun executeLdc(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val index = instruction.constantPoolIndex()
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid ldc constant_pool index $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        when (entry) {
            is ConstantClassEntry -> {
                val nameEntry = try {
                    constantPool[entry.nameIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_Class name_index ${entry.nameIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (nameEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_Class name_index ${entry.nameIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            nameEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internClassMirror(nameEntry.value))
            }
            is ConstantDynamicEntry -> {
                val value = resolveDynamicConstant(
                    instruction = instruction,
                    index = index,
                    constantPool = constantPool,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    staticFields = staticFields,
                    nativeMethods = nativeMethods,
                    monitors = monitors,
                    currentThreadId = currentThreadId,
                    currentClassName = currentClassName,
                    bootstrapMethods = bootstrapMethods,
                    invokeDynamicCallSites = invokeDynamicCallSites,
                    dynamicConstants = dynamicConstants,
                    loadNativeLibraryHandler = loadNativeLibraryHandler,
                    unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                )
                if (value.category.slotWidth != 1) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_Dynamic $index at offset ${instruction.offset}: " +
                            "expected category 1 value but was category ${value.category.slotWidth}",
                    )
                }
                operandStack.push(value)
            }
            is ConstantFloatEntry -> operandStack.push(JvmFloatValue(entry.value))
            is ConstantIntegerEntry -> operandStack.push(JvmIntValue(entry.value))
            is ConstantMethodHandleEntry -> {
                val referencedEntry = try {
                    constantPool[entry.referenceIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodHandle reference_index ${entry.referenceIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (!entry.referenceKind.matches(referencedEntry)) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodHandle reference_index ${entry.referenceIndex} at offset " +
                            "${instruction.offset}: reference_kind ${entry.referenceKind} cannot target " +
                            referencedEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(
                    heap.internMethodHandle(
                        referenceKind = entry.referenceKind.toRuntimeReferenceKind(),
                        referenceIndex = entry.referenceIndex.value,
                    ),
                )
            }
            is ConstantMethodTypeEntry -> {
                val descriptorEntry = try {
                    constantPool[entry.descriptorIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodType descriptor_index ${entry.descriptorIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (descriptorEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_MethodType descriptor_index ${entry.descriptorIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            descriptorEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internMethodType(descriptorEntry.value))
            }
            is ConstantStringEntry -> {
                val stringEntry = try {
                    constantPool[entry.stringIndex]
                } catch (exception: ConstantPoolFormatException) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_String string_index ${entry.stringIndex} " +
                            "at offset ${instruction.offset}: ${exception.message}",
                    )
                }
                if (stringEntry !is ConstantUtf8Entry) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc CONSTANT_String string_index ${entry.stringIndex} at offset " +
                            "${instruction.offset}: expected ConstantUtf8Entry but was " +
                            stringEntry.javaClass.simpleName,
                    )
                }
                operandStack.push(heap.internString(stringEntry.value))
            }
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ldc constant ${entry.javaClass.simpleName} at offset ${instruction.offset}",
            )
        }
    }

    private fun executeLdc2(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val index = instruction.constantPoolIndex()
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid ldc2_w constant_pool index $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        when (entry) {
            is ConstantDoubleEntry -> operandStack.push(JvmDoubleValue(entry.value))
            is ConstantDynamicEntry -> {
                val value = resolveDynamicConstant(
                    instruction = instruction,
                    index = index,
                    constantPool = constantPool,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    staticFields = staticFields,
                    nativeMethods = nativeMethods,
                    monitors = monitors,
                    currentThreadId = currentThreadId,
                    currentClassName = currentClassName,
                    bootstrapMethods = bootstrapMethods,
                    invokeDynamicCallSites = invokeDynamicCallSites,
                    dynamicConstants = dynamicConstants,
                    loadNativeLibraryHandler = loadNativeLibraryHandler,
                    unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                )
                if (value.category.slotWidth != 2) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ldc2_w CONSTANT_Dynamic $index at offset ${instruction.offset}: " +
                            "expected category 2 value but was category ${value.category.slotWidth}",
                    )
                }
                operandStack.push(value)
            }
            is ConstantLongEntry -> operandStack.push(JvmLongValue(entry.value))
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ldc2_w constant ${entry.javaClass.simpleName} at offset ${instruction.offset}",
            )
        }
    }

    private fun resolveDynamicConstant(
        instruction: DecodedInstruction,
        index: ConstantPoolIndex,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue {
        val runtimeIndex = JvmRuntimeConstantPoolIndex(index.value)
        dynamicConstants.resolved(runtimeIndex)?.let { value -> return value }

        val invocation = try {
            JvmDynamicConstantResolver.resolveBootstrapInvocation(
                constantPool = constantPool,
                index = index,
                bootstrapMethods = bootstrapMethods,
            )
        } catch (exception: JvmBootstrapMethodAccessException) {
            throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: ${exception.message}",
            )
        } catch (exception: JvmDynamicConstantLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: ${exception.message}",
            )
        }

        val ownerClassName = currentClassName
            ?: throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: " +
                    "current class is required for MethodHandles.Lookup",
            )
        val bootstrapArguments = materializeDynamicConstantBootstrapArguments(
            instruction = instruction,
            invocation = invocation,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            currentClassName = ownerClassName,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        if (invocation.bootstrapMethodHandle.referenceKind != JvmMethodHandleReferenceKind.InvokeStatic) {
            throw JvmUnsupportedInstructionException(
                "Unsupported CONSTANT_Dynamic $index ${invocation.constant.name}:${invocation.constant.descriptor} " +
                    "bootstrap #${invocation.constant.bootstrapMethodIndex} with ${bootstrapArguments.size} " +
                    "bootstrap method argument(s) at offset ${instruction.offset}: bootstrap method handle " +
                    "${invocation.bootstrapMethodHandle.referenceKind} execution is not implemented yet",
            )
        }
        val bootstrapMethod = try {
            JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
                constantPool = constantPool,
                classHierarchy = classHierarchy,
                methodHandle = invocation.bootstrapMethodHandle,
            )
        } catch (exception: JvmInvokeDynamicLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        val bootstrapReturnValue = executeStaticMethodWithArguments(
            instruction = instruction,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = bootstrapMethod,
            arguments = bootstrapArguments,
            opcodeMnemonic = "CONSTANT_Dynamic bootstrap",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        ) ?: throw JvmUnsupportedInstructionException(
            "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: bootstrap method " +
                "${bootstrapMethod.ownerClassName}.${bootstrapMethod.name}:${bootstrapMethod.descriptor} " +
                "completed without returning a value",
        )
        requireDynamicConstantValue(
            instruction = instruction,
            index = index,
            descriptor = invocation.constant.descriptor,
            value = bootstrapReturnValue,
            heap = heap,
            classHierarchy = classHierarchy,
        )
        return try {
            dynamicConstants.bind(runtimeIndex, bootstrapReturnValue)
        } catch (exception: JvmDynamicConstantLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
    }

    private fun materializeDynamicConstantBootstrapArguments(
        instruction: DecodedInstruction,
        invocation: JvmDynamicConstantBootstrapInvocation,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): List<JvmValue> =
        buildList {
            add(heap.internMethodHandlesLookup(currentClassName))
            add(heap.internString(invocation.constant.name))
            add(heap.internClassMirror(invocation.constant.descriptor.dynamicConstantClassMirrorName()))
            invocation.staticArguments.forEach { argument ->
                add(
                    materializeDynamicConstantBootstrapStaticArgument(
                        instruction = instruction,
                        argument = argument,
                        constantPool = constantPool,
                        heap = heap,
                        classHierarchy = classHierarchy,
                        staticFields = staticFields,
                        nativeMethods = nativeMethods,
                        monitors = monitors,
                        threadScheduler = threadScheduler,
                        currentThreadId = currentThreadId,
                        currentClassName = currentClassName,
                        bootstrapMethods = bootstrapMethods,
                        invokeDynamicCallSites = invokeDynamicCallSites,
                        dynamicConstants = dynamicConstants,
                        loadNativeLibraryHandler = loadNativeLibraryHandler,
                        unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                    ),
                )
            }
        }

    private fun materializeDynamicConstantBootstrapStaticArgument(
        instruction: DecodedInstruction,
        argument: JvmBootstrapArgument,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue =
        when (argument) {
            is JvmBootstrapArgument.ClassConstant -> heap.internClassMirror(argument.internalName)
            is JvmBootstrapArgument.DoubleConstant -> argument.value
            is JvmBootstrapArgument.DynamicConstant -> resolveDynamicConstant(
                instruction = instruction,
                index = ConstantPoolIndex(argument.constantPoolIndex.value),
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            is JvmBootstrapArgument.FloatConstant -> argument.value
            is JvmBootstrapArgument.IntegerConstant -> argument.value
            is JvmBootstrapArgument.LongConstant -> argument.value
            is JvmBootstrapArgument.MethodHandleConstant -> heap.internMethodHandle(
                referenceKind = argument.payload.referenceKind,
                referenceIndex = argument.payload.referenceIndex,
            )
            is JvmBootstrapArgument.MethodTypeConstant -> heap.internMethodType(argument.descriptor)
            is JvmBootstrapArgument.StringConstant -> heap.internString(argument.value)
        }

    private fun String.dynamicConstantClassMirrorName(): String =
        when (this) {
            "Z" -> "boolean"
            "B" -> "byte"
            "C" -> "char"
            "S" -> "short"
            "I" -> "int"
            "J" -> "long"
            "F" -> "float"
            "D" -> "double"
            else -> when {
                startsWith("L") && endsWith(";") -> substring(1, lastIndex)
                startsWith("[") -> this
                else -> this
            }
        }

    private fun requireDynamicConstantValue(
        instruction: DecodedInstruction,
        index: ConstantPoolIndex,
        descriptor: String,
        value: JvmValue,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (!value.matchesFieldDescriptor(descriptor)) {
            throw JvmUnsupportedInstructionException(
                "Invalid CONSTANT_Dynamic $index value at offset ${instruction.offset}: " +
                    "expected $descriptor but was ${value.javaClass.simpleName}",
            )
        }
        if (!descriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = descriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid CONSTANT_Dynamic $index value at offset ${instruction.offset}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun executeAThrow(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val value = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        if (value == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "athrow of null objectref at offset ${instruction.offset}",
            )
        }

        val throwable = value as JvmObjectReferenceValue
        val throwableClassName = heap.get(throwable).className
        throw JvmThrownException(
            throwable = throwable,
            guestClassName = throwableClassName,
            message = "Unhandled guest exception $throwableClassName thrown by athrow at offset ${instruction.offset}",
        )
    }

    private fun executeMonitorEnter(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
    ) {
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "monitorenter on null objectref at offset ${instruction.offset}",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    objectref.javaClass.simpleName,
            )
        }

        heap.get(objectref)
        val enterResult = threadScheduler?.tryEnterMonitor(monitors, objectref, currentThreadId)
            ?: monitors.tryEnter(objectref, currentThreadId)
        when (val result = enterResult) {
            is JvmMonitorEnterResult.Acquired -> Unit
            is JvmMonitorEnterResult.Blocked -> throw JvmMonitorBlockedException(
                objectReference = objectref,
                threadId = currentThreadId,
                ownerThreadId = result.ownerThreadId,
                blockedThreadIds = result.blockedThreadIds,
                bytecodeOffset = instruction.offset,
                message = "Thread $currentThreadId is blocked entering monitor ${objectref.referenceId.value} " +
                    "owned by ${result.ownerThreadId} at offset ${instruction.offset}",
            )
        }
    }

    private fun executeMonitorExit(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
    ) {
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "monitorexit on null objectref at offset ${instruction.offset}",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    objectref.javaClass.simpleName,
            )
        }

        heap.get(objectref)
        try {
            val result = threadScheduler?.exitMonitor(monitors, objectref, currentThreadId)
                ?: monitors.exitAndSelectUnblocked(objectref, currentThreadId)
            result.unblockedThreadId?.let { threadId ->
                monitorUnblockedHandler(objectref, threadId)
            }
        } catch (exception: JvmMonitorOwnershipException) {
            throw JvmIllegalMonitorStateException(
                guestClassName = "java/lang/IllegalMonitorStateException",
                message = exception.message ?: "Current thread does not own monitor",
            )
        }
    }

    private fun executeCheckCast(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        val targetClassName = resolveConstantClassName(instruction, constantPool)
        val value = operandStack.peek()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        if (value == JvmNullValue) {
            return
        }
        val valueClassName = heap.get(value as JvmObjectReferenceValue).className
        if (classHierarchy.isAssignable(sourceClassName = valueClassName, targetClassName = targetClassName)) {
            return
        }
        throw JvmClassCastException(
            guestClassName = "java/lang/ClassCastException",
            message = "$valueClassName cannot be cast to $targetClassName",
        )
    }

    private fun executeInstanceOf(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        val targetClassName = resolveConstantClassName(instruction, constantPool)
        val value = operandStack.pop()
        if (value !is JvmReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmReferenceValue but was ${value.javaClass.simpleName}",
            )
        }
        if (value == JvmNullValue) {
            operandStack.push(JvmIntValue(0))
            return
        }
        val valueClassName = heap.get(value as JvmObjectReferenceValue).className
        if (classHierarchy.isAssignable(sourceClassName = valueClassName, targetClassName = targetClassName)) {
            operandStack.push(JvmIntValue(1))
            return
        }
        operandStack.push(JvmIntValue(0))
    }

    private fun initializeClassForActiveUse(
        className: String,
        classHierarchy: JvmClassHierarchy,
        heap: JvmHeap,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        currentThreadId: String,
        activeUseBytecodeOffset: Int = -1,
        activeUseStackTrace: List<JvmStackTraceFrame> = emptyList(),
        threadScheduler: JvmThreadScheduler? = null,
        executeClassInitializer: ((JvmResolvedMethod) -> Unit)? = null,
    ) {
        if (!classHierarchy.hasClass(className)) {
            return
        }
        when (val state = classInitializationStates.get(className)) {
            JvmClassInitializationState.Prepared -> {
                classInitializationStates.startInitialization(className, currentThreadId)
                try {
                    if (!classHierarchy.isInterface(className)) {
                        classHierarchy.directSuperclassName(className)?.let { superclassName ->
                            initializeClassForActiveUse(
                                className = superclassName,
                                classHierarchy = classHierarchy,
                                heap = heap,
                                classInitializationStates = classInitializationStates,
                                currentThreadId = currentThreadId,
                                activeUseBytecodeOffset = activeUseBytecodeOffset,
                                activeUseStackTrace = activeUseStackTrace,
                                threadScheduler = threadScheduler,
                                executeClassInitializer = executeClassInitializer,
                            )
                        }
                        classHierarchy.defaultMethodSuperinterfaceNames(className).forEach { interfaceName ->
                            initializeClassForActiveUse(
                                className = interfaceName,
                                classHierarchy = classHierarchy,
                                heap = heap,
                                classInitializationStates = classInitializationStates,
                                currentThreadId = currentThreadId,
                                activeUseBytecodeOffset = activeUseBytecodeOffset,
                                activeUseStackTrace = activeUseStackTrace,
                                threadScheduler = threadScheduler,
                                executeClassInitializer = executeClassInitializer,
                            )
                        }
                    }
                    val classInitializer = classHierarchy.classInitializationMethod(className)
                    if (classInitializer == null) {
                        val waitingThreadIds = classInitializationStates.completeInitialization(className, currentThreadId)
                        threadScheduler?.resumeClassInitializationWaiters(waitingThreadIds)
                    } else if (executeClassInitializer != null) {
                        executeClassInitializer(classInitializer)
                        val waitingThreadIds = classInitializationStates.completeInitialization(className, currentThreadId)
                        threadScheduler?.resumeClassInitializationWaiters(waitingThreadIds)
                    }
                } catch (exception: Throwable) {
                    if (exception is JvmThreadSuspendedException || exception is JvmMonitorBlockedException) {
                        throw exception
                    }
                    val initializationFailure = exception.initializationFailureForActiveUse(
                        classHierarchy = classHierarchy,
                        heap = heap,
                        activeUseStackTrace = activeUseStackTrace,
                    )
                    val waitingThreadIds = classInitializationStates.failInitialization(
                        className = className,
                        threadId = currentThreadId,
                        errorClassName = initializationFailure.initializationErrorClassName(heap),
                    )
                    threadScheduler?.resumeClassInitializationWaiters(waitingThreadIds)
                    throw initializationFailure
                }
            }
            JvmClassInitializationState.Initialized -> Unit
            is JvmClassInitializationState.Initializing -> {
                if (state.threadId != currentThreadId) {
                    classInitializationStates.recordInitializationWaiter(className, currentThreadId)
                    val classMirror = heap.internClassMirror(className)
                    throw JvmThreadSuspendedException(
                        threadId = currentThreadId,
                        state = JvmThreadSchedulingState.BlockedOnMonitor(
                            reference = classMirror,
                            ownerThreadId = state.threadId,
                        ),
                        suspendedAtBytecodeOffset = activeUseBytecodeOffset,
                        nextBytecodeOffset = activeUseBytecodeOffset,
                        message = "Thread $currentThreadId is waiting for class $className initialization by ${state.threadId}",
                    )
                }
            }
            is JvmClassInitializationState.Erroneous -> throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = className,
            )
        }
    }

    private fun Throwable.initializationErrorClassName(heap: JvmHeap): String =
        when (this) {
            is JvmArithmeticException -> guestClassName
            is JvmNegativeArraySizeException -> guestClassName
            is JvmArrayIndexOutOfBoundsException -> guestClassName
            is JvmNullPointerException -> guestClassName
            is JvmArrayStoreException -> guestClassName
            is JvmClassCastException -> guestClassName
            is JvmIllegalMonitorStateException -> guestClassName
            is JvmIncompatibleClassChangeError -> guestClassName
            is JvmIllegalAccessError -> guestClassName
            is JvmAbstractMethodError -> guestClassName
            is JvmUnsatisfiedLinkError -> guestClassName
            is JvmNoClassDefFoundError -> guestClassName
            is JvmNoSuchFieldError -> guestClassName
            is JvmNoSuchMethodError -> guestClassName
            is JvmNativeGuestException -> heap.get(throwable).className
            is JvmThrownException -> guestClassName
            else -> "java/lang/ExceptionInInitializerError"
        }

    private fun Throwable.initializationFailureForActiveUse(
        classHierarchy: JvmClassHierarchy,
        heap: JvmHeap,
        activeUseStackTrace: List<JvmStackTraceFrame>,
    ): Throwable {
        val failureClassName = initializationErrorClassName(heap)
        if (classHierarchy.isAssignable(sourceClassName = failureClassName, targetClassName = "java/lang/Error")) {
            return this
        }
        val detailMessage = "Class initializer failed with $failureClassName"
        val cause = initializationCauseReference(heap)
        val wrapper = heap.allocateObject("java/lang/ExceptionInInitializerError")
        heap.recordThrowableStackTrace(wrapper, activeUseStackTrace)
        heap.recordThrowableDetailMessage(wrapper, heap.internString(detailMessage))
        heap.recordThrowableCause(wrapper, cause)
        return JvmThrownException(
            throwable = wrapper,
            guestClassName = "java/lang/ExceptionInInitializerError",
            message = detailMessage,
        )
    }

    private fun Throwable.initializationCauseReference(heap: JvmHeap): JvmReferenceValue =
        when (this) {
            is JvmNativeGuestException -> throwable
            is JvmThrownException -> throwable
            else -> heap.allocateObject(initializationErrorClassName(heap))
        }

    private fun activeUseStackTrace(
        currentClassName: String?,
        currentMethodName: String = "<active-use>",
        activeUseBytecodeOffset: Int = -1,
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
    ): List<JvmStackTraceFrame> =
        currentClassName?.let { className ->
            listOf(
                JvmStackTraceFrame(
                    declaringClass = className,
                    methodName = currentMethodName,
                    fileName = currentSourceFile,
                    lineNumber = currentLineNumberTable
                        .filter { entry -> entry.startPc <= activeUseBytecodeOffset }
                        .maxByOrNull { entry -> entry.startPc }
                        ?.lineNumber,
                ),
            )
        } ?: emptyList()

    private fun executeGetStatic(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        staticFields: JvmStaticFields,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        currentMethodName: String,
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedField = resolveRuntimeFieldReference(instruction, constantPool, classHierarchy)
        requireStaticField(instruction, resolvedField)
        requireAccessibleField(resolvedField, currentClassName, classHierarchy)
        initializeClassForActiveUse(
            resolvedField.reference.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(
                currentClassName = currentClassName,
                currentMethodName = currentMethodName,
                activeUseBytecodeOffset = instruction.offset,
                currentSourceFile = currentSourceFile,
                currentLineNumberTable = currentLineNumberTable,
            ),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        val field = resolvedField.reference
        val value = staticFields.get(field)
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        operandStack.push(value)
    }

    private fun executePutStatic(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        staticFields: JvmStaticFields,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        currentMethodName: String,
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedField = resolveRuntimeFieldReference(instruction, constantPool, classHierarchy)
        requireStaticField(instruction, resolvedField)
        requireAccessibleField(resolvedField, currentClassName, classHierarchy)
        initializeClassForActiveUse(
            resolvedField.reference.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(
                currentClassName = currentClassName,
                currentMethodName = currentMethodName,
                activeUseBytecodeOffset = instruction.offset,
                currentSourceFile = currentSourceFile,
                currentLineNumberTable = currentLineNumberTable,
            ),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        val field = resolvedField.reference
        val value = operandStack.pop()
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        staticFields.put(field, value)
    }

    private fun executeGetField(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        currentClassName: String?,
    ) {
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null objectref",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    objectref.javaClass.simpleName,
            )
        }
        val receiverClassName = heap.get(objectref).className
        val resolvedField = resolveRuntimeFieldReference(instruction, constantPool, classHierarchy)
        requireInstanceField(instruction, resolvedField)
        requireAccessibleField(resolvedField, currentClassName, classHierarchy, receiverClassName)
        val field = resolvedField.reference
        val value = heap.getInstanceField(objectref, field)
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        operandStack.push(value)
    }

    private fun executePutField(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        currentClassName: String?,
    ) {
        val value = operandStack.pop()
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "${instruction.metadata.mnemonic} on null objectref",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} objectref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    objectref.javaClass.simpleName,
            )
        }
        val receiverClassName = heap.get(objectref).className
        val resolvedField = resolveRuntimeFieldReference(instruction, constantPool, classHierarchy)
        requireInstanceField(instruction, resolvedField)
        requireAccessibleField(resolvedField, currentClassName, classHierarchy, receiverClassName)
        val field = resolvedField.reference
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        heap.putInstanceField(objectref, field, value)
    }

    private fun executeInvokeStatic(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        currentMethodName: String,
        currentSourceFile: String? = null,
        currentLineNumberTable: List<JvmLineNumberTableEntry> = emptyList(),
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedMethod = resolveRuntimeMethodReference(instruction, constantPool, classHierarchy)
        requireStaticMethod(instruction, resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        initializeClassForActiveUse(
            resolvedMethod.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(
                currentClassName = currentClassName,
                currentMethodName = currentMethodName,
                activeUseBytecodeOffset = instruction.offset,
                currentSourceFile = currentSourceFile,
                currentLineNumberTable = currentLineNumberTable,
            ),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        executeResolvedStaticMethod(
            instruction = instruction,
            operandStack = operandStack,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = resolvedMethod,
            opcodeMnemonic = "invokestatic",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
    }

    private fun executeResolvedStaticMethod(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        resolvedMethod: JvmResolvedMethod,
        opcodeMnemonic: String,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, resolvedMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    resolvedMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        executeStaticMethodWithArguments(
            instruction = instruction,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = resolvedMethod,
            arguments = arguments,
            opcodeMnemonic = opcodeMnemonic,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )?.let { returnValue ->
            operandStack.push(returnValue)
        }
    }

    private fun executeStaticMethodWithArguments(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        resolvedMethod: JvmResolvedMethod,
        arguments: List<JvmValue>,
        opcodeMnemonic: String,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue? {
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        if (arguments.size != argumentDescriptors.size) {
            throw JvmUnsupportedInstructionException(
                "Invalid $opcodeMnemonic arguments for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor}: expected ${argumentDescriptors.size} arguments but was " +
                    arguments.size,
            )
        }
        for ((argument, descriptor) in arguments.zip(argumentDescriptors)) {
            requireMethodArgumentValue(instruction, resolvedMethod, descriptor, argument)
            requireReferenceMethodArgumentAssignable(
                instruction,
                resolvedMethod,
                descriptor,
                argument,
                heap,
                classHierarchy,
            )
        }
        if (resolvedMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = resolvedMethod,
                receiver = null,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = resolvedMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = resolvedMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid $opcodeMnemonic native return for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return null
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                        "${resolvedMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, resolvedMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, resolvedMethod, returnDescriptor, returnValue, heap, classHierarchy)
            return returnValue
        }
        val methodCode = resolvedMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved static method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} has no Code attribute for $opcodeMnemonic",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = resolvedMethod.maxLocals)
        var localIndex = 0
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = resolvedMethod.maxStack,
            constantPool = resolvedMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = resolvedMethod.ownerClassName,
            currentMethodName = resolvedMethod.name,
            currentSourceFile = resolvedMethod.sourceFile,
            currentLineNumberTable = resolvedMethod.lineNumberTable,
            allowReturn = true,
            exceptionHandlers = resolvedMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = resolvedMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid $opcodeMnemonic return for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                        "${resolvedMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            return null
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Static method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, resolvedMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, resolvedMethod, returnDescriptor, returnValue, heap, classHierarchy)
        return returnValue
    }

    private fun executeInvokeVirtual(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedMethod = resolveRuntimeMethodReference(instruction, constantPool, classHierarchy)
        requireInstanceMethod(instruction, resolvedMethod)
        requireVirtualMethodName(resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, resolvedMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    resolvedMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke virtual method " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor} " +
                    "on null object reference",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokevirtual receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: expected reference but was " +
                    objectref.javaClass.simpleName,
            )
        }
        executeVirtualMethodWithArguments(
            instruction = instruction,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = currentClassName,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = resolvedMethod,
            receiver = objectref,
            arguments = arguments,
            opcodeMnemonic = "invokevirtual",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )?.let { returnValue ->
            operandStack.push(returnValue)
        }
    }

    private fun executeVirtualMethodWithArguments(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        resolvedMethod: JvmResolvedMethod,
        receiver: JvmObjectReferenceValue,
        arguments: List<JvmValue>,
        opcodeMnemonic: String,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue? {
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        if (arguments.size != argumentDescriptors.size) {
            throw JvmUnsupportedInstructionException(
                "Invalid $opcodeMnemonic arguments for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor}: expected ${argumentDescriptors.size} arguments but was " +
                    arguments.size,
            )
        }
        for ((argument, descriptor) in arguments.zip(argumentDescriptors)) {
            requireMethodArgumentValue(instruction, resolvedMethod, descriptor, argument)
            requireReferenceMethodArgumentAssignable(
                instruction,
                resolvedMethod,
                descriptor,
                argument,
                heap,
                classHierarchy,
            )
        }
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid $opcodeMnemonic receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedMethod.ownerClassName}",
            )
        }
        requireNonConstructorReceiverInitialized(resolvedMethod, receiver, heap)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy, receiverClassName)
        val targetMethod = classHierarchy.resolveVirtualMethod(
            receiverClassName = receiverClassName,
            name = resolvedMethod.name,
            descriptor = resolvedMethod.descriptor,
            resolvedMethod = resolvedMethod,
        )
        requireInstanceMethod(instruction, targetMethod)
        if (targetMethod.isAbstract) {
            throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}",
            )
        }
        if (targetMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = targetMethod,
                receiver = receiver,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = targetMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid $opcodeMnemonic native return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                            "${targetMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return null
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
            return returnValue
        }
        val methodCode = targetMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved instance method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} has no Code attribute for $opcodeMnemonic",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = targetMethod.maxLocals)

        calleeLocals.store(0, receiver)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = targetMethod.maxStack,
            constantPool = targetMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = targetMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = targetMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid $opcodeMnemonic return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            return null
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Instance method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
        return returnValue
    }

    private fun executeInvokeSpecial(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedMethod = resolveRuntimeMethodReference(instruction, constantPool, classHierarchy)
        requireInstanceMethod(instruction, resolvedMethod)
        requireVoidConstructorForInvokeSpecial(resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, resolvedMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    resolvedMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke special method " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor} " +
                    "on null object reference",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokespecial receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: expected reference but was " +
                    objectref.javaClass.simpleName,
            )
        }
        val receiverClassName = heap.get(objectref).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokespecial receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedMethod.ownerClassName}",
            )
        }
        requireConstructorReceiverUninitialized(resolvedMethod, objectref, heap)
        requireNonConstructorReceiverInitialized(resolvedMethod, objectref, heap)
        requireConstructorOwnerContext(resolvedMethod, receiverClassName, currentClassName, classHierarchy)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy, receiverClassName)

        if (resolvedMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = resolvedMethod,
                receiver = objectref,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = resolvedMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = resolvedMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid invokespecial native return for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                            "${resolvedMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                        "${resolvedMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, resolvedMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, resolvedMethod, returnDescriptor, returnValue, heap, classHierarchy)
            operandStack.push(returnValue)
            return
        }

        val methodCode = resolvedMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved instance method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} has no Code attribute for invokespecial",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = resolvedMethod.maxLocals)
        calleeLocals.store(0, objectref)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = resolvedMethod.maxStack,
            constantPool = resolvedMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = resolvedMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = resolvedMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = resolvedMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid invokespecial return for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                        "${resolvedMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            if (resolvedMethod.name == "<init>") {
                heap.markInitialized(objectref)
            }
            return
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Instance method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, resolvedMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, resolvedMethod, returnDescriptor, returnValue, heap, classHierarchy)
        operandStack.push(returnValue)
    }

    private fun executeInvokeDynamic(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val thirdOperand = instruction.operands[2]
        val fourthOperand = instruction.operands[3]
        if (thirdOperand != 0) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic third operand $thirdOperand at offset ${instruction.offset}: expected 0",
            )
        }
        if (fourthOperand != 0) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic fourth operand $fourthOperand at offset ${instruction.offset}: expected 0",
            )
        }
        val entry = constantPool[instruction.constantPoolIndex()]
        if (entry !is ConstantInvokeDynamicEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic constant pool entry ${instruction.constantPoolIndex()} at offset " +
                    "${instruction.offset}: expected CONSTANT_InvokeDynamic_info but was " +
                    entry.javaClass.simpleName,
            )
        }
        if (currentClassName != null) {
            val key = JvmInvokeDynamicCallSiteKey(
                ownerClassName = currentClassName,
                bytecodeOffset = instruction.offset,
            )
            val linkedCallSite = invokeDynamicCallSites.linked(key)
            if (linkedCallSite != null) {
                executeLinkedInvokeDynamicCallSite(
                    instruction = instruction,
                    operandStack = operandStack,
                    constantPool = constantPool,
                    heap = heap,
                    classHierarchy = classHierarchy,
                    staticFields = staticFields,
                    classInitializationStates = classInitializationStates,
                    nativeMethods = nativeMethods,
                    monitors = monitors,
                    currentThreadId = currentThreadId,
                    monitorUnblockedHandler = monitorUnblockedHandler,
                    currentClassName = currentClassName,
                    bootstrapMethods = bootstrapMethods,
                    invokeDynamicCallSites = invokeDynamicCallSites,
                    dynamicConstants = dynamicConstants,
                    linkedCallSite = linkedCallSite,
                    loadNativeLibraryHandler = loadNativeLibraryHandler,
                    unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                )
                return
            }
        }
        val invocation = try {
            JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
                constantPool = constantPool,
                index = instruction.constantPoolIndex(),
                bootstrapMethods = bootstrapMethods,
            )
        } catch (exception: JvmBootstrapMethodAccessException) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset ${instruction.offset}: " +
                    exception.message,
            )
        } catch (exception: JvmInvokeDynamicLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset ${instruction.offset}: " +
                    exception.message,
            )
        }
        val spec = invocation.callSite
        val ownerClassName = currentClassName
            ?: throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset " +
                    "${instruction.offset}: current class is required for MethodHandles.Lookup",
            )
        val bootstrapArguments = materializeInvokeDynamicBootstrapArguments(
            instruction = instruction,
            invocation = invocation,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            currentClassName = ownerClassName,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        if (invocation.bootstrapMethodHandle.referenceKind != JvmMethodHandleReferenceKind.InvokeStatic) {
            throw JvmUnsupportedInstructionException(
                "Unsupported invokedynamic call site ${instruction.constantPoolIndex()} " +
                    "${spec.name}:${spec.descriptor} bootstrap #${spec.bootstrapMethodIndex} " +
                    "with ${bootstrapArguments.size} bootstrap method argument(s) at offset ${instruction.offset}: " +
                    "bootstrap method handle ${invocation.bootstrapMethodHandle.referenceKind} execution is not " +
                    "implemented yet",
            )
        }
        val bootstrapMethod = try {
            JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
                constantPool = constantPool,
                classHierarchy = classHierarchy,
                methodHandle = invocation.bootstrapMethodHandle,
            )
        } catch (exception: JvmInvokeDynamicLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset ${instruction.offset}: " +
                    exception.message,
            )
        }
        val bootstrapReturnValue = executeStaticMethodWithArguments(
            instruction = instruction,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = bootstrapMethod,
            arguments = bootstrapArguments,
            opcodeMnemonic = "invokedynamic bootstrap",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        ) ?: throw JvmUnsupportedInstructionException(
            "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset ${instruction.offset}: " +
                "bootstrap method ${bootstrapMethod.ownerClassName}.${bootstrapMethod.name}:" +
                "${bootstrapMethod.descriptor} completed without returning a value",
        )
        val linkedCallSite = try {
            val bootstrapResult = invocation.extractBootstrapResult(heap, bootstrapReturnValue)
            JvmInvokeDynamicCallSiteResolver.bindBootstrapResult(
                key = JvmInvokeDynamicCallSiteKey(
                    ownerClassName = ownerClassName,
                    bytecodeOffset = instruction.offset,
                ),
                constantPool = constantPool,
                classHierarchy = classHierarchy,
                invocation = invocation,
                bootstrapResult = bootstrapResult,
                registry = invokeDynamicCallSites,
            )
        } catch (exception: JvmInvokeDynamicLinkageException) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset ${instruction.offset}: " +
                    exception.message,
            )
        }
        executeLinkedInvokeDynamicCallSite(
            instruction = instruction,
            operandStack = operandStack,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = currentClassName,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            linkedCallSite = linkedCallSite,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
    }

    private fun materializeInvokeDynamicBootstrapArguments(
        instruction: DecodedInstruction,
        invocation: JvmInvokeDynamicBootstrapInvocation,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        currentClassName: String,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): List<JvmValue> =
        buildList {
            add(heap.internMethodHandlesLookup(currentClassName))
            add(heap.internString(invocation.callSite.name))
            add(heap.internMethodType(invocation.callSite.descriptor))
            invocation.staticArguments.forEach { argument ->
                add(
                    try {
                        materializeDynamicConstantBootstrapStaticArgument(
                            instruction = instruction,
                            argument = argument,
                            constantPool = constantPool,
                            heap = heap,
                            classHierarchy = classHierarchy,
                            staticFields = staticFields,
                            nativeMethods = nativeMethods,
                            monitors = monitors,
                            currentThreadId = currentThreadId,
                            currentClassName = currentClassName,
                            bootstrapMethods = bootstrapMethods,
                            invokeDynamicCallSites = invokeDynamicCallSites,
                            dynamicConstants = dynamicConstants,
                            loadNativeLibraryHandler = loadNativeLibraryHandler,
                            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                        )
                    } catch (exception: JvmDynamicConstantLinkageException) {
                        throw JvmUnsupportedInstructionException(
                            "Invalid invokedynamic call site ${instruction.constantPoolIndex()} at offset " +
                                "${instruction.offset}: ${exception.message}",
                        )
                    },
                )
            }
        }

    private fun executeLinkedInvokeDynamicCallSite(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        when (linkedCallSite.targetMethodHandle.referenceKind) {
            JvmMethodHandleReferenceKind.GetField -> executeLinkedInvokeDynamicGetFieldTarget(
                instruction = instruction,
                operandStack = operandStack,
                heap = heap,
                classHierarchy = classHierarchy,
                linkedCallSite = linkedCallSite,
            )
            JvmMethodHandleReferenceKind.PutField -> executeLinkedInvokeDynamicPutFieldTarget(
                instruction = instruction,
                operandStack = operandStack,
                heap = heap,
                classHierarchy = classHierarchy,
                linkedCallSite = linkedCallSite,
            )
            JvmMethodHandleReferenceKind.GetStatic -> executeLinkedInvokeDynamicGetStaticTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                staticFields = staticFields,
                heap = heap,
                classHierarchy = classHierarchy,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.PutStatic -> executeLinkedInvokeDynamicPutStaticTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                staticFields = staticFields,
                heap = heap,
                classHierarchy = classHierarchy,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.InvokeStatic -> executeLinkedInvokeDynamicStaticTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.InvokeVirtual -> executeLinkedInvokeDynamicVirtualTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.InvokeSpecial -> executeLinkedInvokeDynamicSpecialTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.InvokeInterface -> executeLinkedInvokeDynamicInterfaceTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            JvmMethodHandleReferenceKind.NewInvokeSpecial -> executeLinkedInvokeDynamicConstructorTarget(
                instruction = instruction,
                operandStack = operandStack,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = currentClassName,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                linkedCallSite = linkedCallSite,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
    }

    private fun executeLinkedInvokeDynamicGetFieldTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
    ) {
        val target = linkedCallSite.target as? JvmMethodHandleTarget.Field
            ?: throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic getfield target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: linked target is not a field",
            )
        val resolvedField = target.field
        val expectedDescriptor = "(L${resolvedField.ownerClassName};)${resolvedField.descriptor}"
        if (linkedCallSite.spec.descriptor != expectedDescriptor) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                    "does not match call site descriptor",
            )
        }
        val receiver = operandStack.pop()
        if (receiver == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke dynamic getfield target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                    "on null object reference",
            )
        }
        if (receiver !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic getfield receiver for ${resolvedField.ownerClassName}." +
                    "${resolvedField.name}:${resolvedField.descriptor} at offset ${instruction.offset}: " +
                    "expected reference but was ${receiver.javaClass.simpleName}",
            )
        }
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedField.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic getfield receiver for ${resolvedField.ownerClassName}.${resolvedField.name}:" +
                    "${resolvedField.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedField.ownerClassName}",
            )
        }
        val field = JvmFieldReference(
            ownerClassName = resolvedField.ownerClassName,
            name = resolvedField.name,
            descriptor = resolvedField.descriptor,
        )
        val value = heap.getInstanceField(receiver, field)
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        operandStack.push(value)
    }

    private fun executeLinkedInvokeDynamicPutFieldTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
    ) {
        val target = linkedCallSite.target as? JvmMethodHandleTarget.Field
            ?: throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic putfield target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: linked target is not a field",
            )
        val resolvedField = target.field
        val expectedDescriptor = "(L${resolvedField.ownerClassName};${resolvedField.descriptor})V"
        if (linkedCallSite.spec.descriptor != expectedDescriptor) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                    "does not match call site descriptor",
            )
        }
        val value = operandStack.pop()
        val receiver = operandStack.pop()
        if (receiver == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke dynamic putfield target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                    "on null object reference",
            )
        }
        if (receiver !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic putfield receiver for ${resolvedField.ownerClassName}." +
                    "${resolvedField.name}:${resolvedField.descriptor} at offset ${instruction.offset}: " +
                    "expected reference but was ${receiver.javaClass.simpleName}",
            )
        }
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedField.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic putfield receiver for ${resolvedField.ownerClassName}.${resolvedField.name}:" +
                    "${resolvedField.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedField.ownerClassName}",
            )
        }
        val field = JvmFieldReference(
            ownerClassName = resolvedField.ownerClassName,
            name = resolvedField.name,
            descriptor = resolvedField.descriptor,
        )
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        heap.putInstanceField(receiver, field, value)
    }

    private fun executeLinkedInvokeDynamicGetStaticTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        staticFields: JvmStaticFields,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val target = linkedCallSite.target as? JvmMethodHandleTarget.Field
            ?: throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic getstatic target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: linked target is not a field",
            )
        val resolvedField = target.field
        val expectedDescriptor = "()${resolvedField.descriptor}"
        if (linkedCallSite.spec.descriptor != expectedDescriptor) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                "does not match call site descriptor",
            )
        }
        initializeClassForActiveUse(
            resolvedField.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(null),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        val field = JvmFieldReference(
            ownerClassName = resolvedField.ownerClassName,
            name = resolvedField.name,
            descriptor = resolvedField.descriptor,
        )
        val value = staticFields.get(field)
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        operandStack.push(value)
    }

    private fun executeLinkedInvokeDynamicPutStaticTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        staticFields: JvmStaticFields,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val target = linkedCallSite.target as? JvmMethodHandleTarget.Field
            ?: throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic putstatic target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: linked target is not a field",
            )
        val resolvedField = target.field
        val expectedDescriptor = "(${resolvedField.descriptor})V"
        if (linkedCallSite.spec.descriptor != expectedDescriptor) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: target " +
                    "${resolvedField.ownerClassName}.${resolvedField.name}:${resolvedField.descriptor} " +
                "does not match call site descriptor",
            )
        }
        initializeClassForActiveUse(
            resolvedField.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(null),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        val field = JvmFieldReference(
            ownerClassName = resolvedField.ownerClassName,
            name = resolvedField.name,
            descriptor = resolvedField.descriptor,
        )
        val value = operandStack.pop()
        requireFieldValue(instruction, field, value)
        requireReferenceFieldAssignable(instruction, field, value, heap, classHierarchy)
        staticFields.put(field, value)
    }

    private fun executeLinkedInvokeDynamicStaticTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        requireStaticMethod(instruction, linkedCallSite.targetMethod)
        requireLinkedInvokeDynamicDescriptor(instruction, linkedCallSite, linkedCallSite.targetMethod.descriptor)
        initializeClassForActiveUse(
            linkedCallSite.targetMethod.ownerClassName,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(null),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        executeResolvedStaticMethod(
            instruction = instruction,
            operandStack = operandStack,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            classInitializationStates = classInitializationStates,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = linkedCallSite.targetMethod,
            opcodeMnemonic = "invokedynamic",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
    }

    private fun executeLinkedInvokeDynamicVirtualTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        requireInstanceMethod(instruction, linkedCallSite.targetMethod)
        val expectedDescriptor = linkedCallSite.targetMethod.invokeVirtualMethodHandleDescriptor()
        requireLinkedInvokeDynamicDescriptor(instruction, linkedCallSite, expectedDescriptor)
        val argumentDescriptors = linkedCallSite.targetMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, linkedCallSite.targetMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    linkedCallSite.targetMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val receiver = operandStack.pop()
        if (receiver == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke dynamic virtual target " +
                    "${linkedCallSite.targetMethod.ownerClassName}.${linkedCallSite.targetMethod.name}:" +
                    "${linkedCallSite.targetMethod.descriptor} on null object reference",
            )
        }
        if (receiver !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic virtual receiver for ${linkedCallSite.targetMethod.ownerClassName}." +
                    "${linkedCallSite.targetMethod.name}:${linkedCallSite.targetMethod.descriptor} at offset " +
                    "${instruction.offset}: expected reference but was ${receiver.javaClass.simpleName}",
            )
        }
        executeVirtualMethodWithArguments(
            instruction = instruction,
            constantPool = constantPool,
            heap = heap,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            currentClassName = linkedCallSite.targetMethod.ownerClassName,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            resolvedMethod = linkedCallSite.targetMethod,
            receiver = receiver,
            arguments = arguments,
            opcodeMnemonic = "invokedynamic virtual",
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )?.let { returnValue ->
            operandStack.push(returnValue)
        }
    }

    private fun executeLinkedInvokeDynamicSpecialTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val targetMethod = linkedCallSite.targetMethod
        requireInstanceMethod(instruction, targetMethod)
        if (targetMethod.name == "<init>") {
            throw JvmUnsupportedInstructionException(
                "Unsupported invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: " +
                    "constructor InvokeSpecial target execution is not implemented yet",
            )
        }
        val expectedDescriptor = targetMethod.invokeVirtualMethodHandleDescriptor()
        requireLinkedInvokeDynamicDescriptor(instruction, linkedCallSite, expectedDescriptor)
        val argumentDescriptors = targetMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, targetMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    targetMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val receiver = operandStack.pop()
        if (receiver == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke dynamic special target " +
                    "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor} " +
                    "on null object reference",
            )
        }
        if (receiver !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic special receiver for ${targetMethod.ownerClassName}." +
                    "${targetMethod.name}:${targetMethod.descriptor} at offset ${instruction.offset}: " +
                    "expected reference but was ${receiver.javaClass.simpleName}",
            )
        }
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, targetMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic special receiver for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${targetMethod.ownerClassName}",
            )
        }
        requireNonConstructorReceiverInitialized(targetMethod, receiver, heap)
        requireAccessibleMethod(targetMethod, currentClassName, classHierarchy, receiverClassName)
        if (targetMethod.isAbstract) {
            throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}",
            )
        }
        if (targetMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = targetMethod,
                receiver = receiver,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = targetMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid invokedynamic special native return for ${targetMethod.ownerClassName}." +
                            "${targetMethod.name}:${targetMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
            operandStack.push(returnValue)
            return
        }
        val methodCode = targetMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved instance method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} has no Code attribute for invokedynamic special",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = targetMethod.maxLocals)
        calleeLocals.store(0, receiver)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }
        val frameResult = executeFrame(
            code = methodCode,
            maxStack = targetMethod.maxStack,
            constantPool = targetMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = targetMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = targetMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid invokedynamic special return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            return
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Instance method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
        operandStack.push(returnValue)
    }

    private fun executeLinkedInvokeDynamicInterfaceTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val resolvedMethod = linkedCallSite.targetMethod
        requireInstanceMethod(instruction, resolvedMethod)
        requireVirtualMethodName(resolvedMethod)
        val expectedDescriptor = resolvedMethod.invokeVirtualMethodHandleDescriptor()
        requireLinkedInvokeDynamicDescriptor(instruction, linkedCallSite, expectedDescriptor)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, resolvedMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    resolvedMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val receiver = operandStack.pop()
        if (receiver == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke dynamic interface target " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor} " +
                    "on null object reference",
            )
        }
        if (receiver !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic interface receiver for ${resolvedMethod.ownerClassName}." +
                    "${resolvedMethod.name}:${resolvedMethod.descriptor} at offset ${instruction.offset}: " +
                    "expected reference but was ${receiver.javaClass.simpleName}",
            )
        }
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic interface receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedMethod.ownerClassName}",
            )
        }
        requireNonConstructorReceiverInitialized(resolvedMethod, receiver, heap)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy, receiverClassName)
        val targetMethod = try {
            classHierarchy.resolveInterfaceMethodTarget(
                receiverClassName = receiverClassName,
                name = resolvedMethod.name,
                descriptor = resolvedMethod.descriptor,
                resolvedMethod = resolvedMethod,
            )
        } catch (exception: me.moeyinlo.visualize.jvm.runtime.JvmIncompatibleClassChangeError) {
            throw JvmIncompatibleClassChangeError(
                guestClassName = exception.guestClassName,
                message = exception.message ?: "$receiverClassName.${resolvedMethod.name}:${resolvedMethod.descriptor}",
            )
        } catch (exception: me.moeyinlo.visualize.jvm.runtime.JvmAbstractMethodError) {
            throw JvmAbstractMethodError(
                guestClassName = exception.guestClassName,
                message = exception.message ?: "$receiverClassName.${resolvedMethod.name}:${resolvedMethod.descriptor}",
            )
        }
        requireInstanceMethod(instruction, targetMethod)
        if (targetMethod.isAbstract) {
            throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}",
            )
        }
        if (targetMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = targetMethod,
                receiver = receiver,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = targetMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid invokedynamic interface native return for ${targetMethod.ownerClassName}." +
                            "${targetMethod.name}:${targetMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
            operandStack.push(returnValue)
            return
        }
        val methodCode = targetMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved interface target method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} has no Code attribute for invokedynamic interface",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = targetMethod.maxLocals)
        calleeLocals.store(0, receiver)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }
        val frameResult = executeFrame(
            code = methodCode,
            maxStack = targetMethod.maxStack,
            constantPool = targetMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = targetMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = targetMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid invokedynamic interface return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            return
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Interface target method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
        operandStack.push(returnValue)
    }

    private fun executeLinkedInvokeDynamicConstructorTarget(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val constructor = linkedCallSite.targetMethod
        if (constructor.name != "<init>") {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic constructor target for ${linkedCallSite.spec.name}:" +
                    "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: " +
                    "target ${constructor.ownerClassName}.${constructor.name}:${constructor.descriptor} is not <init>",
            )
        }
        requireInstanceMethod(instruction, constructor)
        val expectedDescriptor = constructor.newInvokeSpecialMethodHandleDescriptor()
        requireLinkedInvokeDynamicDescriptor(instruction, linkedCallSite, expectedDescriptor)
        requireAccessibleMethod(constructor, currentClassName, classHierarchy)
        val argumentDescriptors = constructor.descriptor.methodParameterDescriptors()
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, constructor, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    constructor,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val receiver = heap.allocateUninitializedObject(constructor.ownerClassName)
        if (constructor.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = constructor,
                receiver = receiver,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = constructor.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            if (nativeReturnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid invokedynamic constructor native return for ${constructor.ownerClassName}." +
                        "${constructor.name}:${constructor.descriptor}: expected void but returned " +
                        nativeReturnValue.javaClass.simpleName,
                )
            }
            heap.markInitialized(receiver)
            operandStack.push(receiver)
            return
        }
        val methodCode = constructor.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved constructor ${constructor.ownerClassName}.${constructor.name}:" +
                    "${constructor.descriptor} has no Code attribute for invokedynamic constructor",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = constructor.maxLocals)
        calleeLocals.store(0, receiver)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }
        val frameResult = executeFrame(
            code = methodCode,
            maxStack = constructor.maxStack,
            constantPool = constructor.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = constructor.ownerClassName,
            allowReturn = true,
            exceptionHandlers = constructor.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        if (frameResult.returnValue != null) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokedynamic constructor return for ${constructor.ownerClassName}.${constructor.name}:" +
                    "${constructor.descriptor}: expected void but returned " +
                    frameResult.returnValue.javaClass.simpleName,
            )
        }
        heap.markInitialized(receiver)
        operandStack.push(receiver)
    }

    private fun requireLinkedInvokeDynamicDescriptor(
        instruction: DecodedInstruction,
        linkedCallSite: JvmLinkedInvokeDynamicCallSite,
        expectedDescriptor: String,
    ) {
        if (linkedCallSite.spec.descriptor == expectedDescriptor) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid invokedynamic linked target for ${linkedCallSite.spec.name}:" +
                "${linkedCallSite.spec.descriptor} at offset ${instruction.offset}: " +
                "target ${linkedCallSite.targetMethod.ownerClassName}." +
                "${linkedCallSite.targetMethod.name}:${linkedCallSite.targetMethod.descriptor} " +
                "does not match call site descriptor",
        )
    }

    private fun executeInvokeInterface(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val symbolicMethod = resolveConstantMethodReference(instruction, constantPool)
        val resolvedMethod = classHierarchy.resolveInterfaceMethod(
            ownerClassName = symbolicMethod.ownerClassName,
            name = symbolicMethod.name,
            descriptor = symbolicMethod.descriptor,
        )
        val count = instruction.operands[2]
        val zero = instruction.operands[3]
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        val expectedCount = 1 + argumentDescriptors.sumOf { descriptor ->
            descriptor.parameterSlotWidth()
        }
        if (count != expectedCount) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokeinterface count $count at offset ${instruction.offset}: " +
                    "expected $expectedCount for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    resolvedMethod.descriptor,
            )
        }
        if (zero != 0) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokeinterface fourth operand $zero at offset ${instruction.offset}: expected 0",
            )
        }
        requireInstanceMethod(instruction, resolvedMethod)
        requireVirtualMethodName(resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val arguments = argumentDescriptors
            .asReversed()
            .map { descriptor ->
                val value = operandStack.pop()
                requireMethodArgumentValue(instruction, resolvedMethod, descriptor, value)
                requireReferenceMethodArgumentAssignable(
                    instruction,
                    resolvedMethod,
                    descriptor,
                    value,
                    heap,
                    classHierarchy,
                )
                value
            }
            .asReversed()
        val objectref = operandStack.pop()
        if (objectref == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "Cannot invoke interface method " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor} " +
                    "on null object reference",
            )
        }
        if (objectref !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokeinterface receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: expected reference but was " +
                    objectref.javaClass.simpleName,
            )
        }
        val receiverClassName = heap.get(objectref).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid invokeinterface receiver for ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} at offset ${instruction.offset}: " +
                    "$receiverClassName is not assignable to ${resolvedMethod.ownerClassName}",
            )
        }
        requireNonConstructorReceiverInitialized(resolvedMethod, objectref, heap)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy, receiverClassName)
        val targetMethod = try {
            classHierarchy.resolveInterfaceMethodTarget(
                receiverClassName = receiverClassName,
                name = resolvedMethod.name,
                descriptor = resolvedMethod.descriptor,
                resolvedMethod = resolvedMethod,
            )
        } catch (exception: me.moeyinlo.visualize.jvm.runtime.JvmIncompatibleClassChangeError) {
            throw JvmIncompatibleClassChangeError(
                guestClassName = exception.guestClassName,
                message = exception.message ?: "$receiverClassName.${resolvedMethod.name}:${resolvedMethod.descriptor}",
            )
        } catch (exception: me.moeyinlo.visualize.jvm.runtime.JvmAbstractMethodError) {
            throw JvmAbstractMethodError(
                guestClassName = exception.guestClassName,
                message = exception.message ?: "$receiverClassName.${resolvedMethod.name}:${resolvedMethod.descriptor}",
            )
        }
        requireInstanceMethod(instruction, targetMethod)
        if (targetMethod.isAbstract) {
            throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}",
            )
        }
        if (targetMethod.isNative) {
            val nativeReturnValue = executeNativeMethod(
                instruction = instruction,
                method = targetMethod,
                receiver = objectref,
                arguments = arguments,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                currentClassName = targetMethod.ownerClassName,
                dynamicConstants = dynamicConstants,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
            val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
            if (returnDescriptor == "V") {
                if (nativeReturnValue != null) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid invokeinterface native return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                            "${targetMethod.descriptor}: expected void but returned " +
                            nativeReturnValue.javaClass.simpleName,
                    )
                }
                return
            }
            val returnValue = nativeReturnValue
                ?: throw JvmUnsupportedInstructionException(
                    "Native method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor} completed without returning a value",
                )
            requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
            requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
            operandStack.push(returnValue)
            return
        }
        val methodCode = targetMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved interface target method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} has no Code attribute for invokeinterface",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = targetMethod.maxLocals)
        calleeLocals.store(0, objectref)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = targetMethod.maxStack,
            constantPool = targetMethod.constantPool ?: constantPool,
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = targetMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = targetMethod.exceptionHandlers,
            bootstrapMethods = bootstrapMethods,
            invokeDynamicCallSites = invokeDynamicCallSites,
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        val returnDescriptor = targetMethod.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (frameResult.returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid invokeinterface return for ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                        "${targetMethod.descriptor}: expected void but returned " +
                        frameResult.returnValue.javaClass.simpleName,
                )
            }
            return
        }
        val returnValue = frameResult.returnValue
            ?: throw JvmUnsupportedInstructionException(
                "Interface target method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} completed without returning a value",
            )
        requireMethodReturnValue(instruction, targetMethod, returnDescriptor, returnValue)
        requireReferenceMethodReturnAssignable(instruction, targetMethod, returnDescriptor, returnValue, heap, classHierarchy)
        operandStack.push(returnValue)
    }

    private fun executeNativeMethod(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
        receiver: JvmObjectReferenceValue?,
        arguments: List<JvmValue>,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue? {
        val intrinsic = nativeMethods.resolve(method)
            ?: throw JvmUnsatisfiedLinkError(
                guestClassName = "java/lang/UnsatisfiedLinkError",
                message = "Native method ${method.ownerClassName}.${method.name}:" +
                    "${method.descriptor} is not linked for ${instruction.metadata.mnemonic}",
            )
        return intrinsic.invoke(
            context = JvmNativeMethodContext(
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                currentClassName = currentClassName,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                callStaticMethodHandler = { ownerClassName, name, descriptor, upcallArguments ->
                    executeStaticMethodUpcall(
                        ownerClassName = ownerClassName,
                        name = name,
                        descriptor = descriptor,
                        arguments = upcallArguments,
                        heap = heap,
                        classHierarchy = classHierarchy,
                        staticFields = staticFields,
                        nativeMethods = nativeMethods,
                        monitors = monitors,
                        threadScheduler = threadScheduler,
                        currentThreadId = currentThreadId,
                        monitorUnblockedHandler = monitorUnblockedHandler,
                        currentClassName = currentClassName,
                        dynamicConstants = dynamicConstants,
                        loadNativeLibraryHandler = loadNativeLibraryHandler,
                        unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                    )
                },
                callInstanceMethodHandler = { upcallReceiver, ownerClassName, name, descriptor, upcallArguments ->
                    executeInstanceMethodUpcall(
                        receiver = upcallReceiver,
                        ownerClassName = ownerClassName,
                        name = name,
                        descriptor = descriptor,
                        arguments = upcallArguments,
                        heap = heap,
                        classHierarchy = classHierarchy,
                        staticFields = staticFields,
                        nativeMethods = nativeMethods,
                        monitors = monitors,
                        threadScheduler = threadScheduler,
                        currentThreadId = currentThreadId,
                        monitorUnblockedHandler = monitorUnblockedHandler,
                        currentClassName = currentClassName,
                        dynamicConstants = dynamicConstants,
                        loadNativeLibraryHandler = loadNativeLibraryHandler,
                        unloadNativeLibraryHandler = unloadNativeLibraryHandler,
                    )
                },
            ),
            invocation = JvmNativeMethodInvocation(
                receiver = receiver,
                arguments = arguments,
            ),
        )
    }

    private fun executeStaticMethodUpcall(
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue? {
        val resolvedMethod = classHierarchy.resolveMethod(
            ownerClassName = ownerClassName,
            name = name,
            descriptor = descriptor,
        )
        requireStaticUpcallMethod(resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val argumentDescriptors = resolvedMethod.descriptor.methodParameterDescriptors()
        if (arguments.size != argumentDescriptors.size) {
            throw JvmUnsupportedInstructionException(
                "Invalid simulated JNI static upcall arguments for " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor}: " +
                    "expected ${argumentDescriptors.size} arguments but was ${arguments.size}",
            )
        }
        for ((argument, argumentDescriptor) in arguments.zip(argumentDescriptors)) {
            requireUpcallArgumentValue("simulated JNI static upcall", resolvedMethod, argumentDescriptor, argument)
            requireUpcallReferenceArgumentAssignable(
                upcallKind = "simulated JNI static upcall",
                method = resolvedMethod,
                descriptor = argumentDescriptor,
                value = argument,
                heap = heap,
                classHierarchy = classHierarchy,
            )
        }
        val methodCode = resolvedMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved static method ${resolvedMethod.ownerClassName}.${resolvedMethod.name}:" +
                    "${resolvedMethod.descriptor} has no Code attribute for simulated JNI static upcall",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = resolvedMethod.maxLocals)
        var localIndex = 0
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = resolvedMethod.maxStack,
            constantPool = resolvedMethod.constantPool ?: ConstantPool.fromEntries(emptyList()),
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = resolvedMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = resolvedMethod.exceptionHandlers,
            bootstrapMethods = JvmBootstrapMethodTable(),
            invokeDynamicCallSites = JvmInvokeDynamicCallSiteRegistry(),
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        return requireUpcallReturnValue(
            upcallKind = "simulated JNI static upcall",
            method = resolvedMethod,
            returnValue = frameResult.returnValue,
            heap = heap,
            classHierarchy = classHierarchy,
        )
    }

    private fun executeInstanceMethodUpcall(
        receiver: JvmObjectReferenceValue,
        ownerClassName: String,
        name: String,
        descriptor: String,
        arguments: List<JvmValue>,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        currentClassName: String?,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ): JvmValue? {
        val resolvedMethod = classHierarchy.resolveMethod(
            ownerClassName = ownerClassName,
            name = name,
            descriptor = descriptor,
        )
        requireInstanceUpcallMethod(resolvedMethod)
        requireVirtualMethodName(resolvedMethod)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy)
        val receiverClassName = heap.get(receiver).className
        if (!classHierarchy.isAssignable(receiverClassName, resolvedMethod.ownerClassName)) {
            throw JvmUnsupportedInstructionException(
                "Invalid simulated JNI instance upcall receiver for " +
                    "${resolvedMethod.ownerClassName}.${resolvedMethod.name}:${resolvedMethod.descriptor}: " +
                    "$receiverClassName is not assignable to ${resolvedMethod.ownerClassName}",
            )
        }
        requireNonConstructorReceiverInitialized(resolvedMethod, receiver, heap)
        requireAccessibleMethod(resolvedMethod, currentClassName, classHierarchy, receiverClassName)
        val targetMethod = classHierarchy.resolveVirtualMethod(
            receiverClassName = receiverClassName,
            name = resolvedMethod.name,
            descriptor = resolvedMethod.descriptor,
            resolvedMethod = resolvedMethod,
        )
        requireInstanceUpcallMethod(targetMethod)
        if (targetMethod.isAbstract) {
            throw JvmAbstractMethodError(
                guestClassName = "java/lang/AbstractMethodError",
                message = "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}",
            )
        }
        val argumentDescriptors = targetMethod.descriptor.methodParameterDescriptors()
        if (arguments.size != argumentDescriptors.size) {
            throw JvmUnsupportedInstructionException(
                "Invalid simulated JNI instance upcall arguments for " +
                    "${targetMethod.ownerClassName}.${targetMethod.name}:${targetMethod.descriptor}: " +
                    "expected ${argumentDescriptors.size} arguments but was ${arguments.size}",
            )
        }
        for ((argument, argumentDescriptor) in arguments.zip(argumentDescriptors)) {
            requireUpcallArgumentValue("simulated JNI instance upcall", targetMethod, argumentDescriptor, argument)
            requireUpcallReferenceArgumentAssignable(
                upcallKind = "simulated JNI instance upcall",
                method = targetMethod,
                descriptor = argumentDescriptor,
                value = argument,
                heap = heap,
                classHierarchy = classHierarchy,
            )
        }
        val methodCode = targetMethod.code
            ?: throw JvmUnsupportedInstructionException(
                "Resolved instance method ${targetMethod.ownerClassName}.${targetMethod.name}:" +
                    "${targetMethod.descriptor} has no Code attribute for simulated JNI instance upcall",
            )
        val calleeLocals = JvmLocalVariables(maxLocals = targetMethod.maxLocals)
        calleeLocals.store(0, receiver)
        var localIndex = 1
        for (argument in arguments) {
            calleeLocals.store(localIndex, argument)
            localIndex += argument.category.slotWidth
        }

        val frameResult = executeFrame(
            code = methodCode,
            maxStack = targetMethod.maxStack,
            constantPool = targetMethod.constantPool ?: ConstantPool.fromEntries(emptyList()),
            heap = heap,
            localVariables = calleeLocals,
            classHierarchy = classHierarchy,
            staticFields = staticFields,
            nativeMethods = nativeMethods,
            monitors = monitors,
            threadScheduler = threadScheduler,
            currentThreadId = currentThreadId,
            monitorUnblockedHandler = monitorUnblockedHandler,
            currentClassName = targetMethod.ownerClassName,
            allowReturn = true,
            exceptionHandlers = targetMethod.exceptionHandlers,
            bootstrapMethods = JvmBootstrapMethodTable(),
            invokeDynamicCallSites = JvmInvokeDynamicCallSiteRegistry(),
            dynamicConstants = dynamicConstants,
            loadNativeLibraryHandler = loadNativeLibraryHandler,
            unloadNativeLibraryHandler = unloadNativeLibraryHandler,
        )
        return requireUpcallReturnValue(
            upcallKind = "simulated JNI instance upcall",
            method = targetMethod,
            returnValue = frameResult.returnValue,
            heap = heap,
            classHierarchy = classHierarchy,
        )
    }

    private fun requireStaticUpcallMethod(method: JvmResolvedMethod) {
        if (method.isStatic) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected static method ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "for simulated JNI static upcall",
        )
    }

    private fun requireInstanceUpcallMethod(method: JvmResolvedMethod) {
        if (!method.isStatic) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected instance method ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "for simulated JNI instance upcall",
        )
    }

    private fun requireUpcallArgumentValue(
        upcallKind: String,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
    ) {
        if (value.matchesFieldDescriptor(descriptor)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid $upcallKind argument for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                "expected $descriptor but was ${value.javaClass.simpleName}",
        )
    }

    private fun requireUpcallReferenceArgumentAssignable(
        upcallKind: String,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (!descriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = descriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid $upcallKind argument for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun requireUpcallReturnValue(
        upcallKind: String,
        method: JvmResolvedMethod,
        returnValue: JvmValue?,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ): JvmValue? {
        val returnDescriptor = method.descriptor.methodReturnDescriptor()
        if (returnDescriptor == "V") {
            if (returnValue != null) {
                throw JvmUnsupportedInstructionException(
                    "Invalid $upcallKind return for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                        "expected void but returned ${returnValue.javaClass.simpleName}",
                )
            }
            return null
        }
        val value = returnValue
            ?: throw JvmUnsupportedInstructionException(
                "$upcallKind ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                    "completed without returning a value",
            )
        if (!value.matchesFieldDescriptor(returnDescriptor)) {
            throw JvmUnsupportedInstructionException(
                "Invalid $upcallKind return for " +
                    "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                    "expected $returnDescriptor but was ${value.javaClass.simpleName}",
            )
        }
        if (!returnDescriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return value
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = returnDescriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return value
        }
        throw JvmUnsupportedInstructionException(
            "Invalid $upcallKind return for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun executeReturnInstruction(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
    ): JvmFrameExecutionResult =
        when (instruction.metadata.opcode) {
            0xAC -> operandStack.pop().also { value ->
                requireReturnOpcodeValue(instruction, "I", value)
            }.let { value ->
                JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = value)
            }
            0xAD -> operandStack.pop().also { value ->
                requireReturnOpcodeValue(instruction, "J", value)
            }.let { value ->
                JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = value)
            }
            0xAE -> operandStack.pop().also { value ->
                requireReturnOpcodeValue(instruction, "F", value)
            }.let { value ->
                JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = value)
            }
            0xAF -> operandStack.pop().also { value ->
                requireReturnOpcodeValue(instruction, "D", value)
            }.let { value ->
                JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = value)
            }
            0xB0 -> operandStack.pop().also { value ->
                if (value !is JvmReferenceValue) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid areturn value at offset ${instruction.offset}: expected JvmReferenceValue but was " +
                            value.javaClass.simpleName,
                    )
                }
            }.let { value ->
                JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = value)
            }
            0xB1 -> JvmFrameExecutionResult(operandStack = operandStack, hasReturned = true, returnValue = null)
            else -> error("Instruction ${instruction.metadata.mnemonic} is not a return instruction")
        }

    private fun requireReturnOpcodeValue(
        instruction: DecodedInstruction,
        descriptor: String,
        value: JvmValue,
    ) {
        if (value.matchesFieldDescriptor(descriptor)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} value at offset ${instruction.offset}: " +
                "expected $descriptor but was ${value.javaClass.simpleName}",
        )
    }

    private fun requireMethodArgumentValue(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
    ) {
        if (value.matchesFieldDescriptor(descriptor)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} argument for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} at offset ${instruction.offset}: " +
                "expected $descriptor but was ${value.javaClass.simpleName}",
        )
    }

    private fun requireReferenceMethodArgumentAssignable(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (!descriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = descriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} argument for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} at offset ${instruction.offset}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun requireMethodReturnValue(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
    ) {
        if (value.matchesFieldDescriptor(descriptor)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} return for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} at offset ${instruction.offset}: " +
                "expected $descriptor but was ${value.javaClass.simpleName}",
        )
    }

    private fun requireReferenceMethodReturnAssignable(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
        descriptor: String,
        value: JvmValue,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (!descriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = descriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} return for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} at offset ${instruction.offset}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun requireFieldValue(
        instruction: DecodedInstruction,
        field: JvmFieldReference,
        value: JvmValue,
    ) {
        if (value.matchesFieldDescriptor(field.descriptor)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} value for " +
                "${field.ownerClassName}.${field.name}:${field.descriptor} at offset ${instruction.offset}: " +
                "expected ${field.descriptor} but was ${value.javaClass.simpleName}",
        )
    }

    private fun JvmValue.matchesFieldDescriptor(descriptor: String): Boolean =
        when {
            descriptor in intLikeFieldDescriptors -> this is JvmIntValue
            descriptor == "F" -> this is JvmFloatValue
            descriptor == "J" -> this is JvmLongValue
            descriptor == "D" -> this is JvmDoubleValue
            descriptor.startsWith("L") || descriptor.startsWith("[") -> this is JvmReferenceValue
            else -> false
        }

    private fun requireReferenceFieldAssignable(
        instruction: DecodedInstruction,
        field: JvmFieldReference,
        value: JvmValue,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (!field.descriptor.isReferenceDescriptor() || value == JvmNullValue) {
            return
        }
        val reference = value as JvmObjectReferenceValue
        val sourceClassName = heap.get(reference).className
        val targetClassName = field.descriptor.referenceDescriptorClassName()
        if (classHierarchy.isAssignable(sourceClassName, targetClassName)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Invalid ${instruction.metadata.mnemonic} value for " +
                "${field.ownerClassName}.${field.name}:${field.descriptor} at offset ${instruction.offset}: " +
                "$sourceClassName is not assignable to $targetClassName",
        )
    }

    private fun String.isReferenceDescriptor(): Boolean =
        startsWith("L") && endsWith(";") || startsWith("[")

    private fun String.referenceDescriptorClassName(): String =
        if (startsWith("L") && endsWith(";")) {
            substring(1, length - 1)
        } else {
            this
        }

    private fun executeNew(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
        classHierarchy: JvmClassHierarchy,
        staticFields: JvmStaticFields,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        nativeMethods: JvmNativeMethodRegistry,
        monitors: JvmMonitorState,
        threadScheduler: JvmThreadScheduler? = null,
        currentThreadId: String,
        monitorUnblockedHandler: (objectReference: JvmObjectReferenceValue, threadId: String) -> Unit = { _, _ -> },
        bootstrapMethods: JvmBootstrapMethodTable,
        invokeDynamicCallSites: JvmInvokeDynamicCallSiteRegistry,
        dynamicConstants: JvmDynamicConstantRegistry,
        loadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library loading is not configured for $logicalName")
        },
        unloadNativeLibraryHandler: (logicalName: String) -> Unit = { logicalName ->
            throw JvmUnsupportedInstructionException("Native library unloading is not configured for $logicalName")
        },
    ) {
        val className = resolveConstantClassName(instruction, constantPool)
        initializeClassForActiveUse(
            className,
            classHierarchy,
            heap,
            classInitializationStates,
            currentThreadId,
            instruction.offset,
            activeUseStackTrace(null),
            threadScheduler,
        ) { classInitializer ->
            executeStaticMethodWithArguments(
                instruction = instruction,
                constantPool = constantPool,
                heap = heap,
                classHierarchy = classHierarchy,
                staticFields = staticFields,
                classInitializationStates = classInitializationStates,
                nativeMethods = nativeMethods,
                monitors = monitors,
                threadScheduler = threadScheduler,
                currentThreadId = currentThreadId,
                monitorUnblockedHandler = monitorUnblockedHandler,
                bootstrapMethods = bootstrapMethods,
                invokeDynamicCallSites = invokeDynamicCallSites,
                dynamicConstants = dynamicConstants,
                resolvedMethod = classInitializer,
                arguments = emptyList(),
                opcodeMnemonic = "class initialization",
                loadNativeLibraryHandler = loadNativeLibraryHandler,
                unloadNativeLibraryHandler = unloadNativeLibraryHandler,
            )
        }
        operandStack.push(heap.allocateUninitializedObject(className))
    }

    private fun executeANewArray(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
    ) {
        val count = operandStack.pop()
        if (count !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} count at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${count.javaClass.simpleName}",
            )
        }
        if (count.value < 0) {
            throw JvmNegativeArraySizeException(
                guestClassName = "java/lang/NegativeArraySizeException",
                message = count.value.toString(),
            )
        }

        operandStack.push(
            heap.allocateReferenceArray(
                componentClassName = resolveConstantClassName(instruction, constantPool),
                length = count.value,
            ),
        )
    }

    private fun resolveConstantClassName(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
    ): String {
        val index = instruction.constantPoolIndex()
        val mnemonic = instruction.metadata.mnemonic
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid $mnemonic constant_pool index $index at offset ${instruction.offset}: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid $mnemonic constant $index at offset ${instruction.offset}: expected ConstantClassEntry but was " +
                    entry.javaClass.simpleName,
            )
        }

        val nameEntry = try {
            constantPool[entry.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid $mnemonic CONSTANT_Class name_index ${entry.nameIndex} " +
                    "at offset ${instruction.offset}: ${exception.message}",
            )
        }
        if (nameEntry !is ConstantUtf8Entry) {
            throw JvmUnsupportedInstructionException(
                "Invalid $mnemonic CONSTANT_Class name_index ${entry.nameIndex} at offset " +
                    "${instruction.offset}: expected ConstantUtf8Entry but was " +
                    nameEntry.javaClass.simpleName,
            )
        }

        return nameEntry.value
    }

    private data class RuntimeResolvedField(
        val reference: JvmFieldReference,
        val isStatic: Boolean?,
        val isPrivate: Boolean = false,
        val isPackagePrivate: Boolean = false,
        val isProtected: Boolean = false,
    )

    private fun requireAccessibleField(
        field: RuntimeResolvedField,
        currentClassName: String?,
        classHierarchy: JvmClassHierarchy,
        receiverClassName: String? = null,
    ) {
        if (field.isPrivate && currentClassName != null && currentClassName != field.reference.ownerClassName) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access private field " +
                    "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor}",
            )
        }
        if (
            field.isPackagePrivate &&
            currentClassName != null &&
            currentClassName.runtimePackageName() != field.reference.ownerClassName.runtimePackageName()
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access package-private field " +
                    "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor}",
            )
        }
        if (
            field.isProtected &&
            currentClassName != null &&
            currentClassName.runtimePackageName() != field.reference.ownerClassName.runtimePackageName() &&
            !classHierarchy.isAssignable(currentClassName, field.reference.ownerClassName)
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access protected field " +
                    "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor}",
            )
        }
        if (
            field.isProtected &&
            currentClassName != null &&
            receiverClassName != null &&
            currentClassName.runtimePackageName() != field.reference.ownerClassName.runtimePackageName() &&
            classHierarchy.isAssignable(currentClassName, field.reference.ownerClassName) &&
            !classHierarchy.isAssignable(receiverClassName, currentClassName)
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access protected field " +
                    "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor} " +
                    "on receiver $receiverClassName",
            )
        }
    }

    private fun String.runtimePackageName(): String {
        val packageSeparatorIndex = lastIndexOf('/')
        if (packageSeparatorIndex < 0) {
            return ""
        }
        return substring(0, packageSeparatorIndex)
    }

    private fun requireStaticField(
        instruction: DecodedInstruction,
        field: RuntimeResolvedField,
    ) {
        if (field.isStatic != false) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected static field " +
                "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor} " +
                "for ${instruction.metadata.mnemonic}",
        )
    }

    private fun requireInstanceField(
        instruction: DecodedInstruction,
        field: RuntimeResolvedField,
    ) {
        if (field.isStatic != true) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected instance field " +
                "${field.reference.ownerClassName}.${field.reference.name}:${field.reference.descriptor} " +
                "for ${instruction.metadata.mnemonic}",
        )
    }

    private fun resolveRuntimeFieldReference(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
    ): RuntimeResolvedField {
        val symbolicField = resolveConstantFieldReference(instruction, constantPool)
        if (!classHierarchy.requiresResolvedClasses() && !classHierarchy.hasClass(symbolicField.ownerClassName)) {
            return RuntimeResolvedField(reference = symbolicField, isStatic = null)
        }
        val resolvedField = classHierarchy.resolveField(
            ownerClassName = symbolicField.ownerClassName,
            name = symbolicField.name,
            descriptor = symbolicField.descriptor,
        )
        return RuntimeResolvedField(
            reference = JvmFieldReference(
                ownerClassName = resolvedField.ownerClassName,
                name = resolvedField.name,
                descriptor = resolvedField.descriptor,
            ),
            isStatic = resolvedField.isStatic,
            isPrivate = resolvedField.isPrivate,
            isPackagePrivate = resolvedField.isPackagePrivate,
            isProtected = resolvedField.isProtected,
        )
    }

    private fun requireStaticMethod(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
    ) {
        if (method.isStatic) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected static method " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "for ${instruction.metadata.mnemonic}",
        )
    }

    private fun requireInstanceMethod(
        instruction: DecodedInstruction,
        method: JvmResolvedMethod,
    ) {
        if (!method.isStatic) {
            return
        }
        throw JvmIncompatibleClassChangeError(
            guestClassName = "java/lang/IncompatibleClassChangeError",
            message = "Expected instance method " +
                "${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "for ${instruction.metadata.mnemonic}",
        )
    }

    private fun requireVoidConstructorForInvokeSpecial(method: JvmResolvedMethod) {
        if (method.name != "<init>" || method.descriptor.methodReturnDescriptor() == "V") {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Constructor ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "must have a void descriptor for invokespecial",
        )
    }

    private fun requireConstructorReceiverUninitialized(
        method: JvmResolvedMethod,
        objectref: JvmObjectReferenceValue,
        heap: JvmHeap,
    ) {
        if (method.name != "<init>" || !heap.isInitialized(objectref)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Constructor ${method.ownerClassName}.${method.name}:${method.descriptor} receiver is already initialized",
        )
    }

    private fun requireNonConstructorReceiverInitialized(
        method: JvmResolvedMethod,
        objectref: JvmObjectReferenceValue,
        heap: JvmHeap,
    ) {
        if (method.name == "<init>" || heap.isInitialized(objectref)) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Cannot invoke special method ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "on uninitialized receiver",
        )
    }

    private fun requireConstructorOwnerContext(
        method: JvmResolvedMethod,
        receiverClassName: String,
        currentClassName: String?,
        classHierarchy: JvmClassHierarchy,
    ) {
        if (method.name != "<init>" || method.ownerClassName == receiverClassName) {
            return
        }
        if (
            currentClassName == receiverClassName &&
            classHierarchy.directSuperclassName(receiverClassName) == method.ownerClassName
        ) {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Constructor ${method.ownerClassName}.${method.name}:${method.descriptor} " +
                "cannot initialize receiver $receiverClassName outside constructor context for $receiverClassName",
        )
    }

    private fun requireVirtualMethodName(method: JvmResolvedMethod) {
        if (method.name != "<init>" && method.name != "<clinit>") {
            return
        }
        throw JvmUnsupportedInstructionException(
            "Method ${method.ownerClassName}.${method.name}:${method.descriptor} cannot be invoked with invokevirtual",
        )
    }

    private fun requireAccessibleMethod(
        method: JvmResolvedMethod,
        currentClassName: String?,
        classHierarchy: JvmClassHierarchy,
        receiverClassName: String? = null,
    ) {
        if (method.isPrivate && currentClassName != null && currentClassName != method.ownerClassName) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access private method " +
                    "${method.ownerClassName}.${method.name}:${method.descriptor}",
            )
        }
        if (
            method.isPackagePrivate &&
            currentClassName != null &&
            currentClassName.runtimePackageName() != method.ownerClassName.runtimePackageName()
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access package-private method " +
                    "${method.ownerClassName}.${method.name}:${method.descriptor}",
            )
        }
        if (
            method.isProtected &&
            currentClassName != null &&
            currentClassName.runtimePackageName() != method.ownerClassName.runtimePackageName() &&
            !classHierarchy.isAssignable(currentClassName, method.ownerClassName)
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access protected method " +
                    "${method.ownerClassName}.${method.name}:${method.descriptor}",
            )
        }
        if (
            method.isProtected &&
            currentClassName != null &&
            receiverClassName != null &&
            currentClassName.runtimePackageName() != method.ownerClassName.runtimePackageName() &&
            classHierarchy.isAssignable(currentClassName, method.ownerClassName) &&
            !classHierarchy.isAssignable(receiverClassName, currentClassName)
        ) {
            throw JvmIllegalAccessError(
                guestClassName = "java/lang/IllegalAccessError",
                message = "Class $currentClassName cannot access protected method " +
                    "${method.ownerClassName}.${method.name}:${method.descriptor} on receiver $receiverClassName",
            )
        }
    }

    private fun resolveRuntimeMethodReference(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        classHierarchy: JvmClassHierarchy,
    ): JvmResolvedMethod {
        val symbolicMethod = resolveConstantMethodReference(instruction, constantPool)
        return classHierarchy.resolveMethod(
            ownerClassName = symbolicMethod.ownerClassName,
            name = symbolicMethod.name,
            descriptor = symbolicMethod.descriptor,
        )
    }

    private data class SymbolicMethodReference(
        val ownerClassName: String,
        val name: String,
        val descriptor: String,
    )

    private fun resolveConstantMethodReference(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
    ): SymbolicMethodReference {
        val index = instruction.constantPoolIndex()
        val entry = constantPoolEntry(instruction, constantPool, index, "constant")
        val classIndex = when (entry) {
            is ConstantMethodRefEntry -> entry.classIndex
            is ConstantInterfaceMethodRefEntry -> entry.classIndex
            else -> throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} constant $index at offset ${instruction.offset}: " +
                    "expected ConstantMethodRefEntry or ConstantInterfaceMethodRefEntry but was " +
                    entry.javaClass.simpleName,
            )
        }
        val nameAndTypeIndex = when (entry) {
            is ConstantMethodRefEntry -> entry.nameAndTypeIndex
            is ConstantInterfaceMethodRefEntry -> entry.nameAndTypeIndex
        }

        val classEntry = constantPoolEntry(
            instruction,
            constantPool,
            classIndex,
            "CONSTANT_Methodref class_index",
        )
        if (classEntry !is ConstantClassEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} CONSTANT_Methodref class_index $classIndex " +
                    "at offset ${instruction.offset}: expected ConstantClassEntry but was " +
                    classEntry.javaClass.simpleName,
            )
        }
        val ownerClassName = utf8ConstantValue(
            instruction,
            constantPool,
            classEntry.nameIndex,
            "CONSTANT_Class name_index",
        )

        val nameAndTypeEntry = constantPoolEntry(
            instruction,
            constantPool,
            nameAndTypeIndex,
            "CONSTANT_Methodref name_and_type_index",
        )
        if (nameAndTypeEntry !is ConstantNameAndTypeEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} CONSTANT_Methodref name_and_type_index " +
                    "$nameAndTypeIndex at offset ${instruction.offset}: " +
                    "expected ConstantNameAndTypeEntry but was ${nameAndTypeEntry.javaClass.simpleName}",
            )
        }

        return SymbolicMethodReference(
            ownerClassName = ownerClassName,
            name = utf8ConstantValue(
                instruction,
                constantPool,
                nameAndTypeEntry.nameIndex,
                "CONSTANT_NameAndType name_index",
            ),
            descriptor = utf8ConstantValue(
                instruction,
                constantPool,
                nameAndTypeEntry.descriptorIndex,
                "CONSTANT_NameAndType descriptor_index",
            ),
        )
    }

    private fun String.methodParameterDescriptors(): List<String> {
        if (!startsWith("(")) {
            throw JvmUnsupportedInstructionException("Invalid method descriptor $this: missing opening parenthesis")
        }
        val parameters = mutableListOf<String>()
        var index = 1
        while (index < length && this[index] != ')') {
            val endIndex = fieldDescriptorEndIndex(index)
            parameters += substring(index, endIndex)
            index = endIndex
        }
        if (index >= length || this[index] != ')') {
            throw JvmUnsupportedInstructionException("Invalid method descriptor $this: missing closing parenthesis")
        }
        return parameters
    }

    private fun String.methodReturnDescriptor(): String {
        val closeIndex = indexOf(')')
        if (!startsWith("(") || closeIndex < 0 || closeIndex == lastIndex) {
            throw JvmUnsupportedInstructionException("Invalid method descriptor $this: missing return descriptor")
        }
        val returnDescriptor = substring(closeIndex + 1)
        if (returnDescriptor == "V") {
            return returnDescriptor
        }
        if (fieldDescriptorEndIndex(closeIndex + 1) != length) {
            throw JvmUnsupportedInstructionException("Invalid method descriptor $this: malformed return descriptor")
        }
        return returnDescriptor
    }

    private fun JvmResolvedMethod.invokeVirtualMethodHandleDescriptor(): String {
        val parameters = descriptor.methodParameterDescriptors().joinToString(separator = "")
        return "(L$ownerClassName;$parameters)${descriptor.methodReturnDescriptor()}"
    }

    private fun JvmResolvedMethod.newInvokeSpecialMethodHandleDescriptor(): String {
        val parameters = descriptor.methodParameterDescriptors().joinToString(separator = "")
        return "($parameters)L$ownerClassName;"
    }

    private fun String.parameterSlotWidth(): Int =
        if (this == "J" || this == "D") {
            2
        } else {
            1
        }

    private fun String.fieldDescriptorEndIndex(startIndex: Int): Int {
        if (startIndex !in indices) {
            throw JvmUnsupportedInstructionException("Invalid descriptor $this at index $startIndex")
        }
        return when (this[startIndex]) {
            'Z', 'B', 'C', 'S', 'I', 'F', 'J', 'D' -> startIndex + 1
            'L' -> {
                val endIndex = indexOf(';', startIndex)
                if (endIndex < 0) {
                    throw JvmUnsupportedInstructionException("Invalid descriptor $this: missing object terminator")
                }
                endIndex + 1
            }
            '[' -> {
                var componentIndex = startIndex
                while (componentIndex < length && this[componentIndex] == '[') {
                    componentIndex++
                }
                fieldDescriptorEndIndex(componentIndex)
            }
            else -> throw JvmUnsupportedInstructionException(
                "Invalid descriptor $this at index $startIndex: unsupported tag ${this[startIndex]}",
            )
        }
    }

    private fun resolveConstantFieldReference(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
    ): JvmFieldReference {
        val index = instruction.constantPoolIndex()
        val entry = constantPoolEntry(instruction, constantPool, index, "constant")
        if (entry !is ConstantFieldRefEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} constant $index at offset ${instruction.offset}: " +
                    "expected ConstantFieldRefEntry but was ${entry.javaClass.simpleName}",
            )
        }

        val classEntry = constantPoolEntry(
            instruction,
            constantPool,
            entry.classIndex,
            "CONSTANT_Fieldref class_index",
        )
        if (classEntry !is ConstantClassEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} CONSTANT_Fieldref class_index ${entry.classIndex} " +
                    "at offset ${instruction.offset}: expected ConstantClassEntry but was " +
                    classEntry.javaClass.simpleName,
            )
        }
        val ownerClassName = utf8ConstantValue(
            instruction,
            constantPool,
            classEntry.nameIndex,
            "CONSTANT_Class name_index",
        )

        val nameAndTypeEntry = constantPoolEntry(
            instruction,
            constantPool,
            entry.nameAndTypeIndex,
            "CONSTANT_Fieldref name_and_type_index",
        )
        if (nameAndTypeEntry !is ConstantNameAndTypeEntry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} CONSTANT_Fieldref name_and_type_index " +
                    "${entry.nameAndTypeIndex} at offset ${instruction.offset}: " +
                    "expected ConstantNameAndTypeEntry but was ${nameAndTypeEntry.javaClass.simpleName}",
            )
        }

        return JvmFieldReference(
            ownerClassName = ownerClassName,
            name = utf8ConstantValue(
                instruction,
                constantPool,
                nameAndTypeEntry.nameIndex,
                "CONSTANT_NameAndType name_index",
            ),
            descriptor = utf8ConstantValue(
                instruction,
                constantPool,
                nameAndTypeEntry.descriptorIndex,
                "CONSTANT_NameAndType descriptor_index",
            ),
        )
    }

    private fun utf8ConstantValue(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): String {
        val entry = constantPoolEntry(instruction, constantPool, index, role)
        if (entry !is ConstantUtf8Entry) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} $role $index at offset ${instruction.offset}: " +
                    "expected ConstantUtf8Entry but was ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }

    private fun constantPoolEntry(
        instruction: DecodedInstruction,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String,
    ): ConstantPoolEntry =
        try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} $role $index at offset ${instruction.offset}: " +
                    exception.message,
            )
        }

    private fun executeNewArray(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val count = operandStack.pop()
        if (count !is JvmIntValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} count at offset " +
                    "${instruction.offset}: expected JvmIntValue but was ${count.javaClass.simpleName}",
            )
        }
        if (count.value < 0) {
            throw JvmNegativeArraySizeException(
                guestClassName = "java/lang/NegativeArraySizeException",
                message = count.value.toString(),
            )
        }

        val reference = when (val atype = instruction.operands[0]) {
            4 -> heap.allocateBooleanArray(count.value)
            5 -> heap.allocateCharArray(count.value)
            6 -> heap.allocateFloatArray(count.value)
            7 -> heap.allocateDoubleArray(count.value)
            8 -> heap.allocateByteArray(count.value)
            9 -> heap.allocateShortArray(count.value)
            10 -> heap.allocateIntArray(count.value)
            11 -> heap.allocateLongArray(count.value)
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ${instruction.metadata.mnemonic} atype $atype at offset ${instruction.offset}",
            )
        }
        operandStack.push(reference)
    }

    private fun executeMultiANewArray(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        constantPool: ConstantPool,
        heap: JvmHeap,
    ) {
        val dimensions = instruction.operands[2]
        if (dimensions == 0) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} dimensions 0 at offset ${instruction.offset}: " +
                    "dimensions must be greater than zero",
            )
        }
        val counts = (0 until dimensions)
            .map {
                val count = operandStack.pop()
                if (count !is JvmIntValue) {
                    throw JvmUnsupportedInstructionException(
                        "Invalid ${instruction.metadata.mnemonic} count at offset ${instruction.offset}: " +
                            "expected JvmIntValue but was ${count.javaClass.simpleName}",
                    )
                }
                count.value
            }
            .asReversed()
        counts.firstOrNull { count -> count < 0 }?.let { count ->
            throw JvmNegativeArraySizeException(
                guestClassName = "java/lang/NegativeArraySizeException",
                message = count.toString(),
            )
        }
        val arrayClassName = resolveConstantClassName(instruction, constantPool)
        operandStack.push(allocateMultiDimensionalArray(arrayClassName, counts, heap, instruction))
    }

    private fun allocateMultiDimensionalArray(
        arrayClassName: String,
        counts: List<Int>,
        heap: JvmHeap,
        instruction: DecodedInstruction,
    ): JvmObjectReferenceValue {
        if (counts.size == 1) {
            return allocateOneDimensionalArray(arrayClassName, counts.single(), heap, instruction)
        }
        if (!arrayClassName.startsWith("[[")) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} dimensions ${counts.size} for $arrayClassName " +
                    "at offset ${instruction.offset}: dimensions exceed array class descriptor",
            )
        }

        val componentArrayClassName = arrayClassName.substring(1)
        val reference = heap.allocateReferenceArray(componentArrayClassName, counts.first())
        val payload = heap.get(reference).payload as JvmReferenceArrayPayload
        for (index in payload.elements.indices) {
            payload.elements[index] = allocateMultiDimensionalArray(
                arrayClassName = componentArrayClassName,
                counts = counts.drop(1),
                heap = heap,
                instruction = instruction,
            )
        }
        return reference
    }

    private fun allocateOneDimensionalArray(
        arrayClassName: String,
        count: Int,
        heap: JvmHeap,
        instruction: DecodedInstruction,
    ): JvmObjectReferenceValue {
        return when (arrayClassName) {
            "[Z" -> heap.allocateBooleanArray(count)
            "[B" -> heap.allocateByteArray(count)
            "[C" -> heap.allocateCharArray(count)
            "[D" -> heap.allocateDoubleArray(count)
            "[F" -> heap.allocateFloatArray(count)
            "[I" -> heap.allocateIntArray(count)
            "[J" -> heap.allocateLongArray(count)
            "[S" -> heap.allocateShortArray(count)
            else -> when {
                arrayClassName.startsWith("[L") && arrayClassName.endsWith(";") ->
                    heap.allocateReferenceArray(arrayClassName.substring(2, arrayClassName.length - 1), count)
                arrayClassName.startsWith("[[") ->
                    heap.allocateReferenceArray(arrayClassName.substring(1), count)
                else -> throw JvmUnsupportedInstructionException(
                    "Invalid ${instruction.metadata.mnemonic} array class $arrayClassName at offset " +
                        "${instruction.offset}: expected array class descriptor",
                )
            }
        }
    }

    private fun executeArrayLength(
        instruction: DecodedInstruction,
        operandStack: JvmOperandStack,
        heap: JvmHeap,
    ) {
        val arrayReference = operandStack.pop()
        if (arrayReference == JvmNullValue) {
            throw JvmNullPointerException(
                guestClassName = "java/lang/NullPointerException",
                message = "arraylength on null arrayref",
            )
        }
        if (arrayReference !is JvmObjectReferenceValue) {
            throw JvmUnsupportedInstructionException(
                "Invalid ${instruction.metadata.mnemonic} arrayref at offset " +
                    "${instruction.offset}: expected JvmObjectReferenceValue but was " +
                    arrayReference.javaClass.simpleName,
            )
        }

        val length = when (val payload = heap.get(arrayReference).payload) {
            is JvmBooleanArrayPayload -> payload.elements.size
            is JvmByteArrayPayload -> payload.elements.size
            is JvmCharArrayPayload -> payload.elements.size
            is JvmDoubleArrayPayload -> payload.elements.size
            is JvmFloatArrayPayload -> payload.elements.size
            is JvmIntArrayPayload -> payload.elements.size
            is JvmLongArrayPayload -> payload.elements.size
            is JvmReferenceArrayPayload -> payload.elements.size
            is JvmShortArrayPayload -> payload.elements.size
            else -> throw JvmUnsupportedInstructionException(
                "Unsupported ${instruction.metadata.mnemonic} payload ${payload.javaClass.simpleName} " +
                    "at offset ${instruction.offset}",
            )
        }
        operandStack.push(JvmIntValue(length))
    }

    private fun DecodedInstruction.constantPoolIndex(): ConstantPoolIndex =
        when (metadata.opcode) {
            0x12 -> ConstantPoolIndex(operands[0])
            0x13,
            0x14,
            0xB2,
            0xB3,
            0xB4,
            0xB5,
            0xB6,
            0xB7,
            0xB8,
            0xB9,
            0xBA,
            0xBB,
            0xBD,
            0xC5,
            0xC0,
            0xC1,
            -> ConstantPoolIndex((operands[0] shl 8) or operands[1])
            else -> error("Instruction ${metadata.mnemonic} does not use a constant_pool index")
        }

    private fun DecodedInstruction.localVariableIndex(): Int =
        when (metadata.opcode) {
            0x15,
            0x16,
            0x17,
            0x18,
            0x19,
            0x36,
            0x37,
            0x38,
            0x39,
            0x3A,
            0x84,
            0xA9,
            -> operands[0]
            0xC4 -> (operands[1] shl 8) or operands[2]
            in 0x1A..0x1D -> metadata.opcode - 0x1A
            in 0x1E..0x21 -> metadata.opcode - 0x1E
            in 0x22..0x25 -> metadata.opcode - 0x22
            in 0x26..0x29 -> metadata.opcode - 0x26
            in 0x2A..0x2D -> metadata.opcode - 0x2A
            in 0x3B..0x3E -> metadata.opcode - 0x3B
            in 0x3F..0x42 -> metadata.opcode - 0x3F
            in 0x43..0x46 -> metadata.opcode - 0x43
            in 0x47..0x4A -> metadata.opcode - 0x47
            in 0x4B..0x4E -> metadata.opcode - 0x4B
            else -> error("Instruction ${metadata.mnemonic} does not use a local variable index")
        }

    private fun DecodedInstruction.incrementConstant(): Int =
        when (metadata.opcode) {
            0x84 -> operands[1].toByte().toInt()
            0xC4 -> ((operands[3] shl 8) or operands[4]).toShort().toInt()
            else -> error("Instruction ${metadata.mnemonic} does not use an increment constant")
        }

    private fun DecodedInstruction.modifiedWideOpcode(): Int = operands[0]

    private fun DecodedInstruction.nextInstructionOffset(): Int = offset + 1 + operands.size

    private fun DecodedInstruction.branchTargetOffset(): Int =
        offset + ((operands[0] shl 8) or operands[1]).toShort().toInt()

    private fun DecodedInstruction.wideBranchTargetOffset(): Int =
        offset + readSignedOperandInt(0)

    private fun DecodedInstruction.switchPadding(): Int = (4 - ((offset + 1) % 4)) % 4

    private fun DecodedInstruction.readSignedOperandInt(index: Int): Int =
        (operands[index] shl 24) or
            (operands[index + 1] shl 16) or
            (operands[index + 2] shl 8) or
            operands[index + 3]

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"

    private fun MethodHandleReferenceKind.matches(entry: ConstantPoolEntry): Boolean =
        when (this) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> entry is ConstantFieldRefEntry

            MethodHandleReferenceKind.InvokeVirtual,
            MethodHandleReferenceKind.NewInvokeSpecial,
            -> entry is ConstantMethodRefEntry

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> entry is ConstantMethodRefEntry || entry is ConstantInterfaceMethodRefEntry

            MethodHandleReferenceKind.InvokeInterface -> entry is ConstantInterfaceMethodRefEntry
        }

    private fun MethodHandleReferenceKind.toRuntimeReferenceKind(): JvmMethodHandleReferenceKind =
        when (this) {
            MethodHandleReferenceKind.GetField -> JvmMethodHandleReferenceKind.GetField
            MethodHandleReferenceKind.GetStatic -> JvmMethodHandleReferenceKind.GetStatic
            MethodHandleReferenceKind.PutField -> JvmMethodHandleReferenceKind.PutField
            MethodHandleReferenceKind.PutStatic -> JvmMethodHandleReferenceKind.PutStatic
            MethodHandleReferenceKind.InvokeVirtual -> JvmMethodHandleReferenceKind.InvokeVirtual
            MethodHandleReferenceKind.InvokeStatic -> JvmMethodHandleReferenceKind.InvokeStatic
            MethodHandleReferenceKind.InvokeSpecial -> JvmMethodHandleReferenceKind.InvokeSpecial
            MethodHandleReferenceKind.NewInvokeSpecial -> JvmMethodHandleReferenceKind.NewInvokeSpecial
            MethodHandleReferenceKind.InvokeInterface -> JvmMethodHandleReferenceKind.InvokeInterface
        }

    private const val TABLESWITCH_HEADER_BYTES = Int.SIZE_BYTES * 3
    private const val LOOKUPSWITCH_HEADER_BYTES = Int.SIZE_BYTES * 2
    private const val LOOKUPSWITCH_PAIR_BYTES = Int.SIZE_BYTES * 2
}
