package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmDynamicConstantTest {
    @Test
    fun `dynamic constant resolver reads bootstrap index name and descriptor`() {
        val spec = JvmDynamicConstantResolver.resolveSpec(
            constantPool = dynamicConstantPool(),
            index = ConstantPoolIndex(1),
        )

        assertEquals(
            JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 2,
                name = "answer",
                descriptor = "I",
            ),
            spec,
        )
    }

    @Test
    fun `dynamic constant resolver links bootstrap methods by zero based index`() {
        val linkageSpec = JvmDynamicConstantResolver.resolveLinkageSpec(
            constantPool = dynamicConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(JvmRuntimeConstantPoolIndex(7), emptyList()),
                    JvmBootstrapMethod(JvmRuntimeConstantPoolIndex(8), emptyList()),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(9),
                        bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(10)),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmDynamicConstantLinkageSpec(
                constant = JvmDynamicConstantSpec(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                    bootstrapMethodIndex = 2,
                    name = "answer",
                    descriptor = "I",
                ),
                bootstrapMethod = JvmBootstrapMethod(
                    bootstrapMethodRef = JvmRuntimeConstantPoolIndex(9),
                    bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(10)),
                ),
            ),
            linkageSpec,
        )
    }

    @Test
    fun `dynamic constant resolver rejects non dynamic constant entries`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(listOf(ConstantIntegerEntry(1))),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "dynamic constant pool index #1 expected CONSTANT_Dynamic_info but found ConstantIntegerEntry",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant resolver reports malformed name and type references`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantDynamicEntry(
                            bootstrapMethodIndex = BootstrapMethodIndex(0),
                            nameAndTypeIndex = ConstantPoolIndex(2),
                        ),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/NotNameAndType", "pkg/NotNameAndType".encodeToByteArray()),
                    ),
                ),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "dynamic constant name_and_type_index #2 expected CONSTANT_NameAndType_info but found ConstantClassEntry",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant registry caches resolved values by constant pool index`() {
        val registry = JvmDynamicConstantRegistry()
        val index = JvmRuntimeConstantPoolIndex(4)
        val value = JvmIntValue(42)

        assertNull(registry.resolved(index))
        assertEquals(value, registry.bind(index, value))
        assertEquals(value, registry.resolved(index))
        assertEquals(value, registry.bind(index, value))
    }

    @Test
    fun `dynamic constant registry rejects rebinding a resolved constant`() {
        val registry = JvmDynamicConstantRegistry()
        val index = JvmRuntimeConstantPoolIndex(4)
        registry.bind(index, JvmIntValue(42))

        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            registry.bind(index, JvmIntValue(43))
        }

        assertEquals("dynamic constant #4 is already resolved", exception.message)
    }

    private fun dynamicConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(2),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
            ),
        )
}
