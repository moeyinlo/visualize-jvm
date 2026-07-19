package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JvmHeapTest {
    @Test
    fun `heap allocates objects with opaque positive guest identities`() {
        val heap = JvmHeap()

        val first = heap.allocateObject("java/lang/String")
        val second = heap.allocateObject("java/lang/Class")

        assertEquals(JvmReferenceId(1), first.referenceId)
        assertEquals(JvmReferenceId(2), second.referenceId)
        assertNotEquals(first, second)
        assertEquals(JvmHeapObject("java/lang/String"), heap.get(first))
        assertEquals(JvmHeapObject("java/lang/Class"), heap.get(second))
    }

    @Test
    fun `heap stores instance fields per object reference`() {
        val heap = JvmHeap()
        val first = heap.allocateObject("Example")
        val second = heap.allocateObject("Example")
        val field = JvmFieldReference(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
        )

        heap.putInstanceField(first, field, JvmIntValue(3))
        heap.putInstanceField(second, field, JvmIntValue(5))

        assertEquals(JvmIntValue(3), heap.getInstanceField(first, field))
        assertEquals(JvmIntValue(5), heap.getInstanceField(second, field))
    }

    @Test
    fun `heap allocates string objects with guest payloads`() {
        val heap = JvmHeap()

        val reference = heap.allocateString("hello\u0000world")

        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("hello\u0000world"),
            ),
            heap.get(reference),
        )
    }

    @Test
    fun `heap interns string objects with identical code points`() {
        val heap = JvmHeap()

        val first = heap.internString("same")
        val second = heap.internString("same")
        val distinct = heap.internString("different")

        assertEquals(first, second)
        assertNotEquals(first, distinct)
        assertEquals(JvmReferenceId(1), first.referenceId)
        assertEquals(JvmReferenceId(2), distinct.referenceId)
    }

    @Test
    fun `heap interns class mirror objects by represented class name`() {
        val heap = JvmHeap()

        val first = heap.internClassMirror("java/lang/String")
        val second = heap.internClassMirror("java/lang/String")
        val distinct = heap.internClassMirror("java/lang/Object")

        assertEquals(first, second)
        assertNotEquals(first, distinct)
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("java/lang/String"),
            ),
            heap.get(first),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("java/lang/Object"),
            ),
            heap.get(distinct),
        )
    }

    @Test
    fun `heap interns method type objects by descriptor`() {
        val heap = JvmHeap()

        val first = heap.internMethodType("(Ljava/lang/String;)I")
        val second = heap.internMethodType("(Ljava/lang/String;)I")
        val distinct = heap.internMethodType("()V")

        assertEquals(first, second)
        assertNotEquals(first, distinct)
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodType",
                payload = JvmMethodTypePayload("(Ljava/lang/String;)I"),
            ),
            heap.get(first),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodType",
                payload = JvmMethodTypePayload("()V"),
            ),
            heap.get(distinct),
        )
    }

    @Test
    fun `heap interns method handle objects by symbolic reference`() {
        val heap = JvmHeap()

        val first = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 7,
        )
        val second = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 7,
        )
        val distinct = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeVirtual,
            referenceIndex = 7,
        )

        assertEquals(first, second)
        assertNotEquals(first, distinct)
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodHandle",
                payload = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = 7,
                ),
            ),
            heap.get(first),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodHandle",
                payload = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeVirtual,
                    referenceIndex = 7,
                ),
            ),
            heap.get(distinct),
        )
    }
}
