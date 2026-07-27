package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionPolicy
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmPrimitiveValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmValue

object JvmHostFieldAccessor {
    fun getStatic(
        field: JvmHostFieldMirror,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        executionPolicy: JvmClassExecutionPolicy = JvmClassExecutionPolicy.Default,
        classInitializationStates: JvmClassInitializationStates = JvmClassInitializationStates(),
        boundaryEvents: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink.None,
    ): JvmValue {
        requireStatic(field, expectedStatic = true)
        JvmHostInitializationBoundary.recordActiveUse(
            className = field.owner.guestInternalName,
            executionPolicy = executionPolicy,
            classInitializationStates = classInitializationStates,
            boundaryEvents = boundaryEvents,
        )
        val hostValue = try {
            field.hostField.get(null)
        } catch (exception: IllegalAccessException) {
            throw JvmHostFieldAccessException(
                "Host field ${field.owner.hostBinaryName}.${field.name}:${field.descriptor} is not accessible",
                exception,
            )
        }
        return hostValue.toGuestFieldValue(field.fieldType, heap, identityMap)
    }

    fun setStatic(
        field: JvmHostFieldMirror,
        value: JvmValue,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        classLoader: ClassLoader? = field.owner.hostClass.classLoader,
    ) {
        requireStatic(field, expectedStatic = true)
        val hostValue = value.toHostFieldValue(field.fieldType, heap, identityMap, classLoader)
        try {
            field.hostField.set(null, hostValue)
        } catch (exception: IllegalAccessException) {
            throw JvmHostFieldAccessException(
                "Host field ${field.owner.hostBinaryName}.${field.name}:${field.descriptor} is not accessible",
                exception,
            )
        }
    }

    fun getInstance(
        field: JvmHostFieldMirror,
        receiver: JvmReferenceValue,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        classLoader: ClassLoader? = field.owner.hostClass.classLoader,
    ): JvmValue {
        requireStatic(field, expectedStatic = false)
        val hostReceiver = receiver.toHostFieldValue(field.owner.hostClass, heap, identityMap, classLoader)
            ?: throw JvmHostFieldAccessException("Host field ${field.name} receiver is null")
        val hostValue = try {
            field.hostField.get(hostReceiver)
        } catch (exception: IllegalAccessException) {
            throw JvmHostFieldAccessException(
                "Host field ${field.owner.hostBinaryName}.${field.name}:${field.descriptor} is not accessible",
                exception,
            )
        }
        return hostValue.toGuestFieldValue(field.fieldType, heap, identityMap)
    }

    fun setInstance(
        field: JvmHostFieldMirror,
        receiver: JvmReferenceValue,
        value: JvmValue,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap = JvmHostIdentityMap(),
        classLoader: ClassLoader? = field.owner.hostClass.classLoader,
    ) {
        requireStatic(field, expectedStatic = false)
        val hostReceiver = receiver.toHostFieldValue(field.owner.hostClass, heap, identityMap, classLoader)
            ?: throw JvmHostFieldAccessException("Host field ${field.name} receiver is null")
        val hostValue = value.toHostFieldValue(field.fieldType, heap, identityMap, classLoader)
        try {
            field.hostField.set(hostReceiver, hostValue)
        } catch (exception: IllegalAccessException) {
            throw JvmHostFieldAccessException(
                "Host field ${field.owner.hostBinaryName}.${field.name}:${field.descriptor} is not accessible",
                exception,
            )
        }
    }

    private fun requireStatic(field: JvmHostFieldMirror, expectedStatic: Boolean) {
        if (field.isStatic != expectedStatic) {
            val expectedText = if (expectedStatic) "static" else "instance"
            throw JvmHostFieldAccessException("Host field ${field.name} is not an $expectedText field")
        }
    }

    private fun JvmValue.toHostFieldValue(
        targetType: Class<*>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap,
        classLoader: ClassLoader?,
    ): Any? =
        when {
            targetType.isPrimitive -> {
                val primitive = this as? JvmPrimitiveValue
                    ?: throw JvmHostFieldAccessException(
                        "Host primitive field ${targetType.name} requires a guest primitive value",
                    )
                JvmHostPrimitiveBridge.toHost(primitive, targetType)
            }
            targetType == String::class.java -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostFieldAccessException("Host String field requires a guest reference value")
                JvmHostStringBridge.toHost(reference, targetType, heap)
            }
            targetType.isArray -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostFieldAccessException("Host array field requires a guest reference value")
                JvmHostArrayBridge.toHost(reference, targetType, heap)
            }
            targetType == Class::class.java -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostFieldAccessException("Host Class field requires a guest reference value")
                JvmHostClassMirrorBridge.toHost(reference, targetType, heap, classLoader)
            }
            Throwable::class.java.isAssignableFrom(targetType) -> {
                val reference = this as? JvmReferenceValue
                    ?: throw JvmHostFieldAccessException("Host Throwable field requires a guest reference value")
                JvmHostThrowableBridge.toHost(reference, targetType, heap, classLoader)
            }
            this == JvmNullValue -> null
            this is JvmObjectReferenceValue -> identityMap.hostForGuest(this)
                ?: throw JvmHostFieldAccessException(
                    "Guest reference ${referenceId.value} has no bound host object for ${targetType.name}",
                )
            else -> throw JvmHostFieldAccessException(
                "Host field ${targetType.name} cannot be bridged from $this",
            )
        }

    private fun Any?.toGuestFieldValue(
        sourceType: Class<*>,
        heap: JvmHeap,
        identityMap: JvmHostIdentityMap,
    ): JvmValue =
        when {
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
}

class JvmHostFieldAccessException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
