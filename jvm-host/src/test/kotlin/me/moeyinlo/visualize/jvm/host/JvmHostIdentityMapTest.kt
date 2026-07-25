package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmHostIdentityMapTest {
    @Test
    fun `binds guest references and host objects bidirectionally`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("demo/GuestObject")
        val hostObject = EqualHostObject(1)
        val identityMap = JvmHostIdentityMap()

        identityMap.bind(reference, hostObject)

        assertSame(hostObject, identityMap.hostForGuest(reference))
        assertEquals(reference, identityMap.guestForHost(hostObject))
    }

    @Test
    fun `uses host reference identity rather than equals`() {
        val heap = JvmHeap()
        val firstReference = heap.allocateObject("demo/GuestObject")
        val secondReference = heap.allocateObject("demo/GuestObject")
        val firstHostObject = EqualHostObject(7)
        val secondHostObject = EqualHostObject(7)
        val identityMap = JvmHostIdentityMap()

        identityMap.bind(firstReference, firstHostObject)
        identityMap.bind(secondReference, secondHostObject)

        assertEquals(firstReference, identityMap.guestForHost(firstHostObject))
        assertEquals(secondReference, identityMap.guestForHost(secondHostObject))
    }

    @Test
    fun `returns null for unbound guest references and host objects`() {
        val heap = JvmHeap()
        val identityMap = JvmHostIdentityMap()

        assertEquals(null, identityMap.hostForGuest(heap.allocateObject("demo/GuestObject")))
        assertEquals(null, identityMap.guestForHost(EqualHostObject(1)))
    }

    @Test
    fun `rejects rebinding guest references to a different host object`() {
        val heap = JvmHeap()
        val reference = heap.allocateObject("demo/GuestObject")
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(reference, EqualHostObject(1))

        val exception = assertFailsWith<JvmHostIdentityMapException> {
            identityMap.bind(reference, EqualHostObject(2))
        }

        assertEquals("Guest reference ${reference.referenceId.value} is already bound to a different host object", exception.message)
    }

    @Test
    fun `rejects rebinding host objects to a different guest reference`() {
        val heap = JvmHeap()
        val firstReference = heap.allocateObject("demo/GuestObject")
        val secondReference = heap.allocateObject("demo/GuestObject")
        val hostObject = EqualHostObject(1)
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(firstReference, hostObject)

        val exception = assertFailsWith<JvmHostIdentityMapException> {
            identityMap.bind(secondReference, hostObject)
        }

        assertEquals("Host object is already bound to guest reference ${firstReference.referenceId.value}", exception.message)
    }

    data class EqualHostObject(val id: Int)
}
