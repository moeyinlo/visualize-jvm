package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue

class JvmSimulatedJniEnvironment(
    private val classHierarchy: JvmClassHierarchy,
    private val heap: JvmHeap = JvmHeap(),
    val handles: JvmJniHandleTable = JvmJniHandleTable(),
) {
    fun findClass(className: String): JvmJniHandleId {
        if (!classHierarchy.hasClass(className)) {
            throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = className,
            )
        }
        return handles.newClassHandle(className)
    }

    fun getStaticMethodId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
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

    fun getObjectClass(objectHandle: JvmJniHandleId): JvmJniHandleId {
        val reference = handles.resolveObject(objectHandle)
        val className = heap.get(reference).className
        return handles.newClassHandle(className)
    }

    fun isInstanceOf(objectHandle: JvmJniHandleId?, classHandle: JvmJniHandleId): Boolean {
        if (objectHandle == null) {
            return true
        }
        val reference = handles.resolveObject(objectHandle)
        val sourceClassName = heap.get(reference).className
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
}

class JvmJniFieldAccessException(message: String) : IllegalStateException(message)

private fun String.isReferenceFieldDescriptor(): Boolean =
    startsWith("L") || startsWith("[")
