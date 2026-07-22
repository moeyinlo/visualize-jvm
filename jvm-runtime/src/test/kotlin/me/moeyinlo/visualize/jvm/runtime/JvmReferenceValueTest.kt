package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JvmReferenceValueTest {
    @Test
    fun `null and non null references are category one values`() {
        assertEquals(JvmValueCategory.Category1, JvmNullValue.category)
        assertEquals(JvmValueCategory.Category1, JvmObjectReferenceValue(JvmReferenceId(1)).category)
    }

    @Test
    fun `non null references preserve opaque guest identity`() {
        val first = JvmObjectReferenceValue(JvmReferenceId(7))
        val same = JvmObjectReferenceValue(JvmReferenceId(7))
        val second = JvmObjectReferenceValue(JvmReferenceId(8))

        assertEquals(first, same)
        assertNotEquals(first, second)
    }

    @Test
    fun `reference ids are positive opaque guest ids`() {
        assertFailsWith<IllegalArgumentException> { JvmReferenceId(0) }
        assertFailsWith<IllegalArgumentException> { JvmReferenceId(-1) }
    }

    @Test
    fun `object array and class mirror references are opaque category one reference values`() {
        val heap = JvmHeap()
        val objectReference = heap.allocateObject("pkg/Example")
        val primitiveArrayReference = heap.allocateIntArray(length = 2)
        val referenceArrayReference = heap.allocateReferenceArray(componentClassName = "java/lang/String", length = 1)
        val nestedArrayReference = heap.allocateReferenceArray(componentClassName = "[I", length = 1)
        val classMirrorReference = heap.internClassMirror("pkg/Example")

        listOf(
            JvmNullValue,
            objectReference,
            primitiveArrayReference,
            referenceArrayReference,
            nestedArrayReference,
            classMirrorReference,
        ).forEach { value ->
            assertIs<JvmReferenceValue>(value)
            assertEquals(JvmValueCategory.Category1, value.category)
        }

        assertEquals("pkg/Example", heap.get(objectReference).className)
        assertEquals("[I", heap.get(primitiveArrayReference).className)
        assertEquals("[Ljava/lang/String;", heap.get(referenceArrayReference).className)
        assertEquals("[[I", heap.get(nestedArrayReference).className)
        assertEquals("java/lang/Class", heap.get(classMirrorReference).className)
        assertEquals(JvmClassPayload("pkg/Example"), heap.get(classMirrorReference).payload)
    }
}
