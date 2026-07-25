package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmValue

interface JvmJniUpcallDispatcher {
    fun callVoidMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    )

    fun callObjectMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmReferenceValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callBooleanMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmBooleanValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callByteMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmByteValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callCharMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmCharValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callShortMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmShortValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callIntMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmIntValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callLongMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmLongValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callFloatMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmFloatValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    fun callDoubleMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    ): JvmDoubleValue {
        throw JvmJniUpcallException(
            "No simulated JNI upcall dispatcher is configured for " +
                "${method.ownerClassName}.${method.name}:${method.descriptor}",
        )
    }

    companion object {
        val Unbound: JvmJniUpcallDispatcher = object : JvmJniUpcallDispatcher {
            override fun callVoidMethod(
                receiver: JvmObjectReferenceValue,
                method: JvmResolvedMethod,
                arguments: List<JvmValue>,
            ) {
                throw JvmJniUpcallException(
                    "No simulated JNI upcall dispatcher is configured for " +
                        "${method.ownerClassName}.${method.name}:${method.descriptor}",
                )
            }
        }
    }
}

class JvmJniUpcallException(message: String) : IllegalStateException(message)
