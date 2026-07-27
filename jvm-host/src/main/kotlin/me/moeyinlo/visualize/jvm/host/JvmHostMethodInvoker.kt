package me.moeyinlo.visualize.jvm.host

import java.lang.reflect.InvocationTargetException
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionPolicy
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmPrimitiveValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmValue

object JvmHostMethodInvoker {
    fun invokeStatic(
        method: JvmHostMethodMirror,
        arguments: List<JvmValue>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        classLoader: ClassLoader? = method.owner.hostClass.classLoader,
        executionPolicy: JvmClassExecutionPolicy = JvmClassExecutionPolicy.Default,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        boundaryEvents: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink.None,
    ): JvmValue? {
        if (!method.isStatic) {
            throw JvmHostMethodInvocationException("Host method ${method.name} is not static")
        }
        if (arguments.size != method.parameterTypes.size) {
            throw JvmHostMethodInvocationException(
                "Host method ${method.name} expects ${method.parameterTypes.size} arguments but received ${arguments.size}",
            )
        }
        JvmHostInitializationBoundary.recordActiveUse(
            className = method.owner.guestInternalName,
            executionPolicy = executionPolicy,
            classInitializationStates = classInitializationStates,
            boundaryEvents = boundaryEvents,
        )
        boundaryEvents.recordMethod(
            action = JvmHostBoundaryAction.Delegated,
            method = method,
            detail = "static args=${arguments.size}",
        )
        try {
            val hostArguments = arguments.zip(method.parameterTypes).map { (argument, parameterType) ->
                argument.toHostArgument(parameterType, heap, identityMap, classLoader)
            }
            val hostResult = try {
                method.hostMethod.invoke(null, *hostArguments.toTypedArray())
            } catch (exception: IllegalAccessException) {
                throw JvmHostMethodInvocationException(
                    "Host method ${method.owner.hostBinaryName}.${method.name}:${method.descriptor} is not accessible",
                    exception,
                )
            } catch (exception: InvocationTargetException) {
                throw exception.targetException.toTranslatedGuestThrowable(heap)
            }
            return hostResult.toGuestReturn(method.returnType, heap, identityMap).also {
                boundaryEvents.recordMethod(
                    action = JvmHostBoundaryAction.Returned,
                    method = method,
                    detail = "return=${method.returnType.name}",
                )
            }
        } catch (exception: JvmHostTranslatedException) {
            boundaryEvents.recordMethod(
                action = JvmHostBoundaryAction.Failed,
                method = method,
                detail = "translated=${exception.hostThrowable::class.java.name}",
            )
            throw exception
        } catch (exception: RuntimeException) {
            boundaryEvents.recordMethod(
                action = JvmHostBoundaryAction.Failed,
                method = method,
                detail = "exception=${exception::class.java.name}",
            )
            throw exception
        }
    }

    fun invokeInstance(
        method: JvmHostMethodMirror,
        receiver: JvmReferenceValue,
        arguments: List<JvmValue>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        classLoader: ClassLoader? = method.owner.hostClass.classLoader,
        boundaryEvents: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink.None,
    ): JvmValue? {
        if (method.isStatic) {
            throw JvmHostMethodInvocationException("Host method ${method.name} is static")
        }
        if (arguments.size != method.parameterTypes.size) {
            throw JvmHostMethodInvocationException(
                "Host method ${method.name} expects ${method.parameterTypes.size} arguments but received ${arguments.size}",
            )
        }
        boundaryEvents.recordMethod(
            action = JvmHostBoundaryAction.Delegated,
            method = method,
            detail = "instance args=${arguments.size}",
        )
        try {
            val hostReceiver = receiver.toHostArgument(method.owner.hostClass, heap, identityMap, classLoader)
                ?: throw JvmHostMethodInvocationException("Host method ${method.name} receiver is null")
            val hostArguments = arguments.zip(method.parameterTypes).map { (argument, parameterType) ->
                argument.toHostArgument(parameterType, heap, identityMap, classLoader)
            }
            val hostResult = try {
                method.hostMethod.invoke(hostReceiver, *hostArguments.toTypedArray())
            } catch (exception: IllegalAccessException) {
                throw JvmHostMethodInvocationException(
                    "Host method ${method.owner.hostBinaryName}.${method.name}:${method.descriptor} is not accessible",
                    exception,
                )
            } catch (exception: InvocationTargetException) {
                throw exception.targetException.toTranslatedGuestThrowable(heap)
            }
            return hostResult.toGuestReturn(method.returnType, heap, identityMap).also {
                boundaryEvents.recordMethod(
                    action = JvmHostBoundaryAction.Returned,
                    method = method,
                    detail = "return=${method.returnType.name}",
                )
            }
        } catch (exception: JvmHostTranslatedException) {
            boundaryEvents.recordMethod(
                action = JvmHostBoundaryAction.Failed,
                method = method,
                detail = "translated=${exception.hostThrowable::class.java.name}",
            )
            throw exception
        } catch (exception: RuntimeException) {
            boundaryEvents.recordMethod(
                action = JvmHostBoundaryAction.Failed,
                method = method,
                detail = "exception=${exception::class.java.name}",
            )
            throw exception
        }
    }

    private fun JvmHostBoundaryEventSink.recordMethod(
        action: JvmHostBoundaryAction,
        method: JvmHostMethodMirror,
        detail: String,
    ) {
        record(
            action = action,
            className = method.owner.guestInternalName,
            methodName = method.name,
            descriptor = method.descriptor,
            detail = detail,
        )
    }

    private fun JvmValue.toHostArgument(
        targetType: Class<*>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap,
        classLoader: ClassLoader?,
    ): Any? =
        when {
            targetType.isPrimitive -> {
                val primitive = this as? JvmPrimitiveValue
                    ?: throw JvmHostMethodInvocationException(
                        "Host primitive argument ${targetType.name} requires a guest primitive value",
                    )
                JvmHostPrimitiveBridge.toHost(primitive, targetType)
            }
            targetType == String::class.java -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostMethodInvocationException("Host String argument requires a guest reference value")
                JvmHostStringBridge.toHost(reference, targetType, heap)
            }
            targetType.isArray -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostMethodInvocationException("Host array argument requires a guest reference value")
                JvmHostArrayBridge.toHost(reference, targetType, heap)
            }
            targetType == Class::class.java -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostMethodInvocationException("Host Class argument requires a guest reference value")
                JvmHostClassMirrorBridge.toHost(reference, targetType, heap, classLoader)
            }
            Throwable::class.java.isAssignableFrom(targetType) -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostMethodInvocationException("Host Throwable argument requires a guest reference value")
                JvmHostThrowableBridge.toHost(reference, targetType, heap, classLoader)
            }
            this == JvmNullValue -> null
            this is JvmObjectReferenceValue -> identityMap.hostForGuest(this)
                ?: throw JvmHostMethodInvocationException(
                    "Guest reference ${referenceId.value} has no bound host object for ${targetType.name}",
                )
            else -> throw JvmHostMethodInvocationException(
                "Host argument ${targetType.name} cannot be bridged from $this",
            )
        }

    private fun Any?.toGuestReturn(
        sourceType: Class<*>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap,
    ): JvmValue? =
        when {
            sourceType == Void.TYPE -> null
            sourceType.isPrimitive -> JvmHostPrimitiveBridge.fromHost(this, sourceType)
            sourceType == String::class.java -> JvmHostStringBridge.fromHost(this, sourceType, heap)
            sourceType.isArray -> JvmHostArrayBridge.fromHost(this, sourceType, heap)
            sourceType == Class::class.java -> JvmHostClassMirrorBridge.fromHost(this, sourceType, heap)
            Throwable::class.java.isAssignableFrom(sourceType) -> JvmHostThrowableBridge.fromHost(this, sourceType, heap)
            this == null -> JvmNullValue
            else -> {
                identityMap.guestForHost(this) ?: heap.allocateObject(this::class.java.name.replace('.', '/')).also {
                    identityMap.bind(it, this)
                }
            }
        }

    private fun Throwable.toTranslatedGuestThrowable(heap: JvmHeap): JvmHostTranslatedException {
        val guestThrowable = JvmHostThrowableBridge.fromHost(this, Throwable::class.java, heap)
        val guestThrowableReference = guestThrowable as? JvmObjectReferenceValue
            ?: throw JvmHostMethodInvocationException("Host throwable ${this::class.java.name} translated to null")
        return JvmHostTranslatedException(
            guestThrowable = guestThrowableReference,
            hostThrowable = this,
        )
    }
}

class JvmHostMethodInvocationException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class JvmHostTranslatedException(
    val guestThrowable: JvmObjectReferenceValue,
    val hostThrowable: Throwable,
) : IllegalStateException(
    "Host throwable ${hostThrowable::class.java.name} was translated to guest reference ${guestThrowable.referenceId.value}",
    hostThrowable,
)
