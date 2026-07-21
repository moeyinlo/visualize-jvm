package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError

class JvmSimulatedJniEnvironment(
    private val classHierarchy: JvmClassHierarchy,
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
}
