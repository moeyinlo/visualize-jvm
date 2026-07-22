package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodSpecifier
import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodsAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmBootstrapMethodTableTest {
    @Test
    fun `bootstrap method table maps classfile bootstrap method specifiers to runtime indexes`() {
        val table = JvmBootstrapMethodTable.fromAttribute(
            BootstrapMethodsAttribute(
                nameIndex = ConstantPoolIndex(1),
                bootstrapMethods = listOf(
                    BootstrapMethodSpecifier(
                        bootstrapMethodRef = ConstantPoolIndex(7),
                        bootstrapArguments = listOf(ConstantPoolIndex(3), ConstantPoolIndex(5)),
                    ),
                ),
            ),
        )

        assertEquals(1, table.size)
        assertEquals(
            JvmBootstrapMethod(
                bootstrapMethodRef = JvmRuntimeConstantPoolIndex(7),
                bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(3), JvmRuntimeConstantPoolIndex(5)),
            ),
            table[0],
        )
        assertEquals(listOf(table[0]), table.toList())
    }

    @Test
    fun `bootstrap method table treats missing attribute as empty table`() {
        val table = JvmBootstrapMethodTable.fromAttribute(null)

        assertEquals(0, table.size)
    }

    @Test
    fun `bootstrap method table uses zero based bootstrap method indexes`() {
        val table = JvmBootstrapMethodTable(
            listOf(
                JvmBootstrapMethod(
                    bootstrapMethodRef = JvmRuntimeConstantPoolIndex(1),
                    bootstrapArguments = emptyList(),
                ),
            ),
        )

        val exception = assertFailsWith<JvmBootstrapMethodAccessException> {
            table[1]
        }

        assertEquals("Bootstrap method index #1 is outside 0..0", exception.message)
    }
}