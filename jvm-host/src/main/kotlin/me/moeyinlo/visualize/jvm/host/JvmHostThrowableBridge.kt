package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStackTraceFrame
import me.moeyinlo.visualize.jvm.runtime.JvmThrowablePayload

object JvmHostThrowableBridge {
    fun toHost(
        value: JvmReferenceValue,
        targetType: Class<*>,
        heap: JvmHeap,
        classLoader: ClassLoader? = null,
    ): Throwable? {
        requireThrowableType(targetType, role = "target")
        if (value == JvmNullValue) {
            return null
        }
        val reference = value as? JvmObjectReferenceValue
            ?: throw JvmHostThrowableBridgeException("Guest value $value is not an object reference")
        val heapObject = heap.get(reference)
        val throwableClass = heapObject.className.toThrowableClass(classLoader)
        if (!targetType.isAssignableFrom(throwableClass)) {
            throw JvmHostThrowableBridgeException(
                "Guest throwable ${heapObject.className} is not assignable to host ${targetType.name}",
            )
        }
        val throwable = throwableClass.getConstructor().newInstance()
        val payload = heapObject.payload as? JvmThrowablePayload
        if (payload != null) {
            throwable.stackTrace = payload.stackTrace.map { frame -> frame.toHostStackTraceElement() }.toTypedArray()
        }
        return throwable
    }

    fun fromHost(
        value: Any?,
        sourceType: Class<*>,
        heap: JvmHeap,
    ): JvmReferenceValue {
        requireThrowableType(sourceType, role = "source")
        if (value == null) {
            return JvmNullValue
        }
        val throwable = value as? Throwable
            ?: throw JvmHostThrowableBridgeException(
                "Host throwable source returned ${value::class.java.name}",
            )
        if (!sourceType.isAssignableFrom(throwable::class.java)) {
            throw JvmHostThrowableBridgeException(
                "Host throwable ${throwable::class.java.name} is not assignable to source ${sourceType.name}",
            )
        }
        val reference = heap.allocateObject(throwable::class.java.toGuestThrowableClassName())
        return heap.recordThrowableStackTrace(
            reference = reference,
            stackTrace = throwable.stackTrace.map { element -> element.toGuestStackTraceFrame() },
        )
    }

    private fun requireThrowableType(
        type: Class<*>,
        role: String,
    ) {
        if (!Throwable::class.java.isAssignableFrom(type)) {
            throw JvmHostThrowableBridgeException(
                "Host throwable bridge $role type must be java.lang.Throwable assignable: ${type.name}",
            )
        }
    }

    private fun String.toThrowableClass(classLoader: ClassLoader?): Class<out Throwable> {
        val hostClass = try {
            Class.forName(replace('/', '.'), false, classLoader)
        } catch (exception: ClassNotFoundException) {
            throw JvmHostThrowableBridgeException("Guest reference $this is not a Throwable", exception)
        }
        return try {
            hostClass.asSubclass(Throwable::class.java)
        } catch (exception: ClassCastException) {
            throw JvmHostThrowableBridgeException("Guest reference $this is not a Throwable", exception)
        }
    }

    private fun Class<out Throwable>.toGuestThrowableClassName(): String =
        name.replace('.', '/')

    private fun JvmStackTraceFrame.toHostStackTraceElement(): StackTraceElement =
        StackTraceElement(
            declaringClass.replace('/', '.'),
            methodName,
            fileName,
            lineNumber ?: -1,
        )

    private fun StackTraceElement.toGuestStackTraceFrame(): JvmStackTraceFrame =
        JvmStackTraceFrame(
            declaringClass = className.replace('.', '/'),
            methodName = methodName,
            fileName = fileName,
            lineNumber = if (lineNumber >= 0) lineNumber else null,
        )
}

class JvmHostThrowableBridgeException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
