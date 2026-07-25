package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import me.moeyinlo.visualize.jvm.runtime.JvmValue

interface JvmJniUpcallDispatcher {
    fun callVoidMethod(
        receiver: JvmObjectReferenceValue,
        method: JvmResolvedMethod,
        arguments: List<JvmValue>,
    )

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
