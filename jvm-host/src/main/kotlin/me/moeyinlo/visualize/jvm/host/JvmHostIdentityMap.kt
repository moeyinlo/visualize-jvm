package me.moeyinlo.visualize.jvm.host

import java.util.IdentityHashMap
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId

class JvmHostIdentityMap {
    private val guestToHost = linkedMapOf<JvmReferenceId, Any>()
    private val hostToGuest = IdentityHashMap<Any, JvmObjectReferenceValue>()

    fun bind(
        guestReference: JvmObjectReferenceValue,
        hostObject: Any,
    ) {
        val existingHost = guestToHost[guestReference.referenceId]
        if (existingHost != null && existingHost !== hostObject) {
            throw JvmHostIdentityMapException(
                "Guest reference ${guestReference.referenceId.value} is already bound to a different host object",
            )
        }

        val existingGuest = hostToGuest[hostObject]
        if (existingGuest != null && existingGuest != guestReference) {
            throw JvmHostIdentityMapException(
                "Host object is already bound to guest reference ${existingGuest.referenceId.value}",
            )
        }

        guestToHost[guestReference.referenceId] = hostObject
        hostToGuest[hostObject] = guestReference
    }

    fun hostForGuest(guestReference: JvmObjectReferenceValue): Any? =
        guestToHost[guestReference.referenceId]

    fun guestForHost(hostObject: Any): JvmObjectReferenceValue? =
        hostToGuest[hostObject]
}

class JvmHostIdentityMapException(message: String) : IllegalStateException(message)
