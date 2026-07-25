package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmHostFieldAccessorTest {
    @Test
    fun `gets and sets static primitive host fields`() {
        HostFieldFixture.staticCount = 3
        val heap = JvmHeap()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostFieldFixture::class.java)
        val field = JvmHostFieldResolver.resolveStaticField(
            owner = mirror,
            name = "staticCount",
            descriptor = "I",
        )

        assertEquals(JvmIntValue(3), JvmHostFieldAccessor.getStatic(field, heap))

        JvmHostFieldAccessor.setStatic(field, JvmIntValue(11), heap)

        assertEquals(11, HostFieldFixture.staticCount)
    }

    @Test
    fun `gets and sets instance string host fields through identity mapped receivers`() {
        val heap = JvmHeap()
        val hostReceiver = HostFieldFixture().also { it.label = "old" }
        val guestReceiver = heap.allocateObject("me/moeyinlo/visualize/jvm/host/JvmHostFieldAccessorTest\$HostFieldFixture")
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(guestReceiver, hostReceiver)
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostFieldFixture::class.java)
        val field = JvmHostFieldResolver.resolveInstanceField(
            owner = mirror,
            name = "label",
            descriptor = "Ljava/lang/String;",
        )

        val oldValue = JvmHostFieldAccessor.getInstance(field, guestReceiver, heap, identityMap)
        val oldReference = oldValue as JvmObjectReferenceValue
        assertEquals(JvmStringPayload("old"), heap.get(oldReference).payload)

        JvmHostFieldAccessor.setInstance(field, guestReceiver, heap.allocateString("new"), heap, identityMap)

        assertEquals("new", hostReceiver.label)
    }

    class HostFieldFixture {
        @JvmField
        var label: String? = null

        companion object {
            @JvmField
            var staticCount: Int = 0
        }
    }
}
