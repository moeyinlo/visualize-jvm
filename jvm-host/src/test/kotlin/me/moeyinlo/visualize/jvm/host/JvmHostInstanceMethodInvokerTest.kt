package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmHostInstanceMethodInvokerTest {
    @Test
    fun `invokes instance host methods on guest strings`() {
        val heap = JvmHeap()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(String::class.java)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "substring",
            descriptor = "(I)Ljava/lang/String;",
        )

        val result = JvmHostMethodInvoker.invokeInstance(
            method = method,
            receiver = heap.allocateString("guest"),
            arguments = listOf(JvmIntValue(2)),
            heap = heap,
        )

        val reference = result as JvmObjectReferenceValue
        assertEquals(JvmStringPayload("est"), heap.get(reference).payload)
    }

    @Test
    fun `invokes instance host methods on opaque identity mapped receivers`() {
        val heap = JvmHeap()
        val guestReceiver = heap.allocateObject("me/moeyinlo/visualize/jvm/host/JvmHostInstanceMethodInvokerTest\$Counter")
        val hostReceiver = Counter(5)
        val identityMap = JvmHostIdentityMap()
        identityMap.bind(guestReceiver, hostReceiver)
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(Counter::class.java)
        val method = JvmHostMethodResolver.resolveInstanceMethod(
            owner = mirror,
            name = "add",
            descriptor = "(I)I",
        )

        val result = JvmHostMethodInvoker.invokeInstance(
            method = method,
            receiver = guestReceiver,
            arguments = listOf(JvmIntValue(4)),
            heap = heap,
            identityMap = identityMap,
        )

        assertEquals(JvmIntValue(9), result)
        assertEquals(9, hostReceiver.value)
    }

    @Test
    fun `rejects instance invocation of static host methods`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(JvmHostStaticMethodInvokerTest.HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "add",
            descriptor = "(II)I",
        )

        val exception = assertFailsWith<JvmHostMethodInvocationException> {
            JvmHostMethodInvoker.invokeInstance(
                method = method,
                receiver = JvmHeap().allocateObject("demo/Receiver"),
                arguments = listOf(JvmIntValue(1), JvmIntValue(2)),
                heap = JvmHeap(),
            )
        }

        assertEquals("Host method add is static", exception.message)
    }

    class Counter(var value: Int) {
        fun add(delta: Int): Int {
            value += delta
            return value
        }
    }
}
