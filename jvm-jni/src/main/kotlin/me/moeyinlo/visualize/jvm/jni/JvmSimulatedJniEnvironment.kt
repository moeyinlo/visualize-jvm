package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError

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
}
