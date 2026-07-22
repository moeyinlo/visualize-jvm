package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmRuntimeConstantPoolTest {
    @Test
    fun `runtime constant pool uses one based indexes`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(
                JvmRuntimeLiteralConstant(JvmIntValue(7)),
                JvmRuntimeStringConstant("hello"),
            ),
        )

        assertEquals(2, pool.size)
        assertEquals(JvmRuntimeLiteralConstant(JvmIntValue(7)), pool[JvmRuntimeConstantPoolIndex(1)])
        assertEquals(JvmRuntimeStringConstant("hello"), pool[JvmRuntimeConstantPoolIndex(2)])
        assertEquals(pool.toList(), listOf(pool[JvmRuntimeConstantPoolIndex(1)], pool[JvmRuntimeConstantPoolIndex(2)]))
    }

    @Test
    fun `runtime constant pool rejects invalid indexes`() {
        assertFailsWith<IllegalArgumentException> { JvmRuntimeConstantPoolIndex(0) }

        val pool = JvmRuntimeConstantPool(ownerClassName = "Example", entries = emptyList())
        val exception = assertFailsWith<JvmRuntimeConstantPoolAccessException> {
            pool[JvmRuntimeConstantPoolIndex(1)]
        }

        assertEquals("Runtime constant pool index #1 is outside 1..0 for Example", exception.message)
    }

    @Test
    fun `runtime constant pool stores symbolic references before resolution`() {
        val field = JvmFieldReference(ownerClassName = "Example", name = "counter", descriptor = "I")
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(
                JvmRuntimeClassSymbolicReference("java/lang/Object"),
                JvmRuntimeFieldSymbolicReference(field),
                JvmRuntimeMethodSymbolicReference(
                    ownerClassName = "Example",
                    name = "run",
                    descriptor = "()V",
                ),
            ),
        )

        assertEquals(JvmRuntimeClassSymbolicReference("java/lang/Object"), pool[JvmRuntimeConstantPoolIndex(1)])
        assertEquals(JvmRuntimeFieldSymbolicReference(field), pool[JvmRuntimeConstantPoolIndex(2)])
        assertEquals(
            JvmRuntimeMethodSymbolicReference(ownerClassName = "Example", name = "run", descriptor = "()V"),
            pool[JvmRuntimeConstantPoolIndex(3)],
        )
        assertNull(pool.resolved(JvmRuntimeConstantPoolIndex(1)))
    }

    @Test
    fun `runtime constant pool caches resolved constants by index`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(JvmRuntimeClassSymbolicReference("java/lang/Object")),
        )
        val resolved = JvmRuntimeResolvedConstant.Class("java/lang/Object")

        assertEquals(resolved, pool.cacheResolved(JvmRuntimeConstantPoolIndex(1), resolved))

        assertEquals(resolved, pool.resolved(JvmRuntimeConstantPoolIndex(1)))
    }

    @Test
    fun `runtime constant pool validates owner and symbolic names`() {
        assertFailsWith<IllegalArgumentException> {
            JvmRuntimeConstantPool(ownerClassName = "", entries = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            JvmRuntimeClassSymbolicReference("")
        }
        assertFailsWith<IllegalArgumentException> {
            JvmRuntimeMethodSymbolicReference(ownerClassName = "", name = "run", descriptor = "()V")
        }
    }
}
