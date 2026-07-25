package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassPayload
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue

object JvmHostClassMirrorBridge {
    fun toHost(
        value: JvmReferenceValue,
        targetType: Class<*>,
        heap: JvmHeap,
        classLoader: ClassLoader? = null,
    ): Class<*>? {
        requireClassType(targetType, role = "target")
        if (value == JvmNullValue) {
            return null
        }
        val reference = value as? JvmObjectReferenceValue
            ?: throw JvmHostClassMirrorBridgeException("Guest value $value is not an object reference")
        val heapObject = heap.get(reference)
        if (heapObject.className != "java/lang/Class") {
            throw JvmHostClassMirrorBridgeException(
                "Guest reference ${heapObject.className} is not java/lang/Class",
            )
        }
        val payload = heapObject.payload as? JvmClassPayload
            ?: throw JvmHostClassMirrorBridgeException("Guest java/lang/Class has no class payload")
        return payload.representedClassName.toHostClass(classLoader)
    }

    fun fromHost(
        value: Any?,
        sourceType: Class<*>,
        heap: JvmHeap,
    ): JvmReferenceValue {
        requireClassType(sourceType, role = "source")
        if (value == null) {
            return JvmNullValue
        }
        val hostClass = value as? Class<*>
            ?: throw JvmHostClassMirrorBridgeException(
                "Host class source returned ${value::class.java.name}",
            )
        return heap.internClassMirror(hostClass.toGuestClassName())
    }

    private fun requireClassType(
        type: Class<*>,
        role: String,
    ) {
        if (type != Class::class.java) {
            throw JvmHostClassMirrorBridgeException(
                "Host class mirror bridge $role type must be java.lang.Class: ${type.name}",
            )
        }
    }

    private fun String.toHostClass(classLoader: ClassLoader?): Class<*> =
        when (this) {
            "boolean" -> Boolean::class.javaPrimitiveType!!
            "byte" -> Byte::class.javaPrimitiveType!!
            "char" -> Char::class.javaPrimitiveType!!
            "short" -> Short::class.javaPrimitiveType!!
            "int" -> Int::class.javaPrimitiveType!!
            "long" -> Long::class.javaPrimitiveType!!
            "float" -> Float::class.javaPrimitiveType!!
            "double" -> Double::class.javaPrimitiveType!!
            "void" -> Void.TYPE
            else -> Class.forName(replace('/', '.'), false, classLoader)
        }

    private fun Class<*>.toGuestClassName(): String =
        when (this) {
            Boolean::class.javaPrimitiveType -> "boolean"
            Byte::class.javaPrimitiveType -> "byte"
            Char::class.javaPrimitiveType -> "char"
            Short::class.javaPrimitiveType -> "short"
            Int::class.javaPrimitiveType -> "int"
            Long::class.javaPrimitiveType -> "long"
            Float::class.javaPrimitiveType -> "float"
            Double::class.javaPrimitiveType -> "double"
            Void.TYPE -> "void"
            else -> name.replace('.', '/')
        }
}

class JvmHostClassMirrorBridgeException(message: String) : IllegalStateException(message)
