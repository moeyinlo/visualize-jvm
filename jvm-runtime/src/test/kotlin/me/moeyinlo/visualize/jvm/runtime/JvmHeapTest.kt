package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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
    fun `heap allocates objects with prepared declared instance fields`() {
        val heap = JvmHeap()
        val classDefinition = JvmClassDefinition(
            internalName = "Example",
            fields = listOf(
                JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = false),
                JvmFieldDefinition(name = "name", descriptor = "Ljava/lang/String;", isStatic = false),
                JvmFieldDefinition(name = "global", descriptor = "J", isStatic = true),
            ),
        )
        val counter = JvmFieldReference("Example", "counter", "I")
        val name = JvmFieldReference("Example", "name", "Ljava/lang/String;")
        val global = JvmFieldReference("Example", "global", "J")

        val reference = heap.allocateObject(classDefinition)

        assertEquals(JvmHeapObject("Example"), heap.get(reference))
        assertTrue(heap.hasInstanceField(reference, counter))
        assertTrue(heap.hasInstanceField(reference, name))
        assertFalse(heap.hasInstanceField(reference, global))
        assertEquals(JvmIntValue(0), heap.getInstanceField(reference, counter))
        assertEquals(JvmNullValue, heap.getInstanceField(reference, name))
    }
    @Test
    fun `heap tracks uninitialized object state for constructor execution`() {
        val heap = JvmHeap()

        val initialized = heap.allocateObject("Example")
        val uninitialized = heap.allocateUninitializedObject("Example")

        assertTrue(heap.isInitialized(initialized))
        assertFalse(heap.isInitialized(uninitialized))
        heap.markInitialized(uninitialized)
        assertTrue(heap.isInitialized(uninitialized))
        assertEquals(JvmHeapObject("Example"), heap.get(uninitialized))
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

    @Test
    fun `heap allocates call site objects with target method handle payloads`() {
        val heap = JvmHeap()
        val target = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 9,
        )

        val callSite = heap.allocateCallSite(target)

        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/CallSite",
                payload = JvmCallSitePayload(target),
            ),
            heap.get(callSite),
        )
    }

    @Test
    fun `heap allocates direct byte buffer objects with native address payloads`() {
        val heap = JvmHeap()

        val buffer = heap.allocateDirectByteBuffer(address = 0x1000L, capacity = 64L)

        assertEquals(
            JvmHeapObject(
                className = "java/nio/DirectByteBuffer",
                payload = JvmDirectByteBufferPayload(address = 0x1000L, capacity = 64L),
            ),
            heap.get(buffer),
        )
    }

    @Test
    fun `heap records throwable causes in throwable payloads`() {
        val heap = JvmHeap()
        val throwable = heap.allocateObject("java/lang/ExceptionInInitializerError")
        val cause = heap.allocateObject("java/lang/ArithmeticException")

        heap.recordThrowableCause(throwable, cause)

        assertEquals(
            JvmThrowablePayload(
                stackTrace = emptyList(),
                cause = cause,
            ),
            heap.get(throwable).payload,
        )
    }

    @Test
    fun `heap rejects call site targets that are not method handles`() {
        val heap = JvmHeap()
        val notMethodHandle = heap.allocateObject("java/lang/String")

        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            heap.allocateCallSite(notMethodHandle)
        }

        assertEquals(
            "call site target must be a java/lang/invoke/MethodHandle object: java/lang/String",
            exception.message,
        )
    }

    @Test
    fun `heap shallow clones call site payloads while preserving target identity`() {
        val heap = JvmHeap()
        val target = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 9,
        )
        val callSite = heap.allocateCallSite(target)

        val cloned = heap.shallowClone(callSite)

        assertNotEquals(callSite, cloned)
        assertEquals(JvmCallSitePayload(target), heap.get(cloned).payload)
    }

    @Test
    fun `heap shallow clones objects with copied instance fields`() {
        val heap = JvmHeap()
        val original = heap.allocateObject("Example")
        val counter = JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I")
        val child = JvmFieldReference(ownerClassName = "Example", name = "child", descriptor = "LChild;")
        val childReference = heap.allocateObject("Child")
        heap.putInstanceField(original, counter, JvmIntValue(42))
        heap.putInstanceField(original, child, childReference)

        val cloned = heap.shallowClone(original)

        assertNotEquals(original, cloned)
        assertEquals(JvmHeapObject("Example"), heap.get(cloned))
        assertEquals(JvmIntValue(42), heap.getInstanceField(cloned, counter))
        assertEquals(childReference, heap.getInstanceField(cloned, child))
    }

    @Test
    fun `heap shallow clones arrays with independent element storage`() {
        val heap = JvmHeap()
        val original = heap.allocateIntArray(2)
        val originalPayload = heap.get(original).payload as JvmIntArrayPayload
        originalPayload.elements[0] = 3
        originalPayload.elements[1] = 4

        val cloned = heap.shallowClone(original)

        assertNotEquals(original, cloned)
        val clonedPayload = heap.get(cloned).payload as JvmIntArrayPayload
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
        originalPayload.elements[1] = 9
        assertEquals(mutableListOf(3, 4), clonedPayload.elements)
    }
}
