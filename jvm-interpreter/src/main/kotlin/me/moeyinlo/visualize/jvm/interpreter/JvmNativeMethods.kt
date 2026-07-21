package me.moeyinlo.visualize.jvm.interpreter

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmMonitorState
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmValue

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
                descriptor = method.descriptor,
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
    val currentThreadId: String = "main",
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

class JvmNativeMethodRegistry(
    private val intrinsics: Map<JvmNativeMethodKey, JvmNativeMethodIntrinsic> = emptyMap(),
    private val simulatedJni: Map<JvmNativeMethodKey, JvmNativeMethodIntrinsic> = emptyMap(),
) {
    fun resolve(method: JvmResolvedMethod): JvmNativeMethodIntrinsic? =
        JvmNativeMethodKey.from(method).let { key ->
            intrinsics[key] ?: simulatedJni[key]
        }

    companion object {
        val Empty: JvmNativeMethodRegistry = JvmNativeMethodRegistry()

        fun from(vararg entries: Pair<JvmNativeMethodKey, JvmNativeMethodIntrinsic>): JvmNativeMethodRegistry =
            JvmNativeMethodRegistry(entries.toMap())

        fun fromSimulatedJni(
            vararg entries: Pair<JvmNativeMethodKey, JvmNativeMethodIntrinsic>,
        ): JvmNativeMethodRegistry =
            JvmNativeMethodRegistry(simulatedJni = entries.toMap())
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
        context.monitors.waitForNotification(receiver, context.currentThreadId)
        null
    }
    private val ObjectNotify = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.notify intrinsic requires a receiver")
        require(invocation.arguments.isEmpty()) { "Object.notify intrinsic expects no arguments" }
        context.heap.get(receiver)
        context.monitors.notifyOne(receiver, context.currentThreadId)
        null
    }
    private val ObjectNotifyAll = JvmNativeMethodIntrinsic { context, invocation ->
        val receiver = invocation.receiver
            ?: throw JvmUnsupportedInstructionException("Object.notifyAll intrinsic requires a receiver")
        require(invocation.arguments.isEmpty()) { "Object.notifyAll intrinsic expects no arguments" }
        context.heap.get(receiver)
        context.monitors.notifyAll(receiver, context.currentThreadId)
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
    )

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
}
