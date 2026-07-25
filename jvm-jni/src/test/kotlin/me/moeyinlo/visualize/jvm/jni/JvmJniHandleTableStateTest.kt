package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmJniHandleTableStateTest {
    @Test
    fun `handle table tracks live handle count across allocation deletion and local frames`() {
        val table = JvmJniHandleTable()
        val first = JvmObjectReferenceValue(JvmReferenceId(1))
        val second = JvmObjectReferenceValue(JvmReferenceId(2))

        assertEquals(0, table.liveHandleCount)
        val firstHandle = table.newObjectHandle(first)
        assertEquals(1, table.liveHandleCount)
        table.pushLocalFrame()
        table.newObjectHandle(second)
        assertEquals(2, table.liveHandleCount)
        table.deleteCurrentLocalFrameHandles()
        assertEquals(1, table.liveHandleCount)
        table.deleteLocal(firstHandle)
        assertEquals(0, table.liveHandleCount)
    }
}