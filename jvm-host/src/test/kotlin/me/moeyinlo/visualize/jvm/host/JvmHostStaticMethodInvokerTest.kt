package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmHostStaticMethodInvokerTest {
    @Test
    fun `invokes static host methods with primitive arguments and returns`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "add",
            descriptor = "(II)I",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(JvmIntValue(2), JvmIntValue(3)),
            heap = JvmHeap(),
        )

        assertEquals(JvmIntValue(5), result)
    }

    @Test
    fun `invokes static host methods with string arguments and returns`() {
        val heap = JvmHeap()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "prefix",
            descriptor = "(Ljava/lang/String;I)Ljava/lang/String;",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(heap.allocateString("v"), JvmIntValue(4)),
            heap = heap,
        )

        val reference = result as JvmObjectReferenceValue
        assertEquals(JvmStringPayload("v4"), heap.get(reference).payload)
    }

    @Test
    fun `invokes static void host methods with no guest return value`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "accept",
            descriptor = "(I)V",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = listOf(JvmIntValue(9)),
            heap = JvmHeap(),
        )

        assertEquals(null, result)
    }

    @Test
    fun `binds opaque host object returns in the guest host identity map`() {
        val heap = JvmHeap()
        val identityMap = JvmHostIdentityMap()
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "sharedObject",
            descriptor = "()Ljava/lang/Object;",
        )

        val result = JvmHostMethodInvoker.invokeStatic(
            method = method,
            arguments = emptyList(),
            heap = heap,
            identityMap = identityMap,
        )

        val reference = result as JvmObjectReferenceValue
        assertEquals(reference, identityMap.guestForHost(HostStaticFixture.shared))
        assertEquals(
            "me/moeyinlo/visualize/jvm/host/JvmHostStaticMethodInvokerTest\$SharedHostObject",
            heap.get(reference).className,
        )
    }

    @Test
    fun `rejects static invocation with wrong argument count`() {
        val mirror = JvmHostDelegatedClassMirror.fromHostClass(HostStaticFixture::class.java)
        val method = JvmHostMethodResolver.resolveStaticMethod(
            owner = mirror,
            name = "add",
            descriptor = "(II)I",
        )

        val exception = assertFailsWith<JvmHostMethodInvocationException> {
            JvmHostMethodInvoker.invokeStatic(
                method = method,
                arguments = listOf(JvmIntValue(1)),
                heap = JvmHeap(),
            )
        }

        assertEquals("Host method add expects 2 arguments but received 1", exception.message)
    }

    class HostStaticFixture {
        companion object {
            @JvmField
            val shared: Any = SharedHostObject()

            @JvmStatic
            fun add(left: Int, right: Int): Int = left + right

            @JvmStatic
            fun prefix(prefix: String?, value: Int): String? = "$prefix$value"

            @JvmStatic
            fun accept(value: Int) {
                require(value >= 0)
            }

            @JvmStatic
            fun sharedObject(): Any = shared
        }
    }

    class SharedHostObject
}
