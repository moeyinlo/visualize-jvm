package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class JvmInvokeDynamicCallSiteRegistryTest {
    @Test
    fun `call site resolver reads bootstrap index name and descriptor from invoke dynamic constants`() {
        val spec = JvmInvokeDynamicCallSiteResolver.resolveSpec(
            constantPool = invokedynamicConstantPool(),
            index = ConstantPoolIndex(1),
        )

        assertEquals(
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 7,
                name = "run",
                descriptor = "(I)Ljava/lang/String;",
            ),
            spec,
        )
    }

    @Test
    fun `call site resolver rejects non invoke dynamic constant pool entries`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(listOf(ConstantIntegerEntry(1))),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "invokedynamic constant pool index #1 expected CONSTANT_InvokeDynamic_info but found ConstantIntegerEntry",
            exception.message,
        )
    }

    @Test
    fun `call site resolver reports malformed invoke dynamic name and type references`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantInvokeDynamicEntry(
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
            "invokedynamic name_and_type index #2 expected CONSTANT_NameAndType_info but found ConstantClassEntry",
            exception.message,
        )
    }

    @Test
    fun `call site registry caches linked invokedynamic targets by owner and bytecode offset`() {
        val registry = JvmInvokeDynamicCallSiteRegistry()
        val key = JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = 12)
        val callSite = JvmLinkedInvokeDynamicCallSite(
            spec = JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(3),
                bootstrapMethodIndex = 0,
                name = "run",
                descriptor = "()I",
            ),
            targetMethod = JvmResolvedMethod(
                ownerClassName = "pkg/BootstrapTarget",
                name = "run",
                descriptor = "()I",
                isStatic = true,
            ),
        )

        assertNull(registry.linked(key))

        assertSame(callSite, registry.bind(key, callSite))
        assertSame(callSite, registry.linked(key))
        assertSame(callSite, registry.bind(key, callSite))
    }

    @Test
    fun `call site registry rejects rebinding an already linked bytecode offset`() {
        val registry = JvmInvokeDynamicCallSiteRegistry()
        val key = JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = 12)
        val spec = JvmInvokeDynamicCallSiteSpec(
            constantPoolIndex = JvmRuntimeConstantPoolIndex(3),
            bootstrapMethodIndex = 0,
            name = "run",
            descriptor = "()I",
        )
        registry.bind(
            key,
            JvmLinkedInvokeDynamicCallSite(
                spec = spec,
                targetMethod = JvmResolvedMethod(
                    ownerClassName = "pkg/FirstTarget",
                    name = "run",
                    descriptor = "()I",
                    isStatic = true,
                ),
            ),
        )

        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            registry.bind(
                key,
                JvmLinkedInvokeDynamicCallSite(
                    spec = spec,
                    targetMethod = JvmResolvedMethod(
                        ownerClassName = "pkg/SecondTarget",
                        name = "run",
                        descriptor = "()I",
                        isStatic = true,
                    ),
                ),
            )
        }

        assertEquals("invokedynamic call site pkg/Caller@12 is already linked", exception.message)
    }

    @Test
    fun `call site model validates owner offset bootstrap index and descriptor identity`() {
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteKey(ownerClassName = "", bytecodeOffset = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = -1,
                name = "run",
                descriptor = "()V",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "",
                descriptor = "()V",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "run",
                descriptor = "",
            )
        }
    }

    private fun invokedynamicConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantInvokeDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(7),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("run", "run".encodeToByteArray()),
                ConstantUtf8Entry("(I)Ljava/lang/String;", "(I)Ljava/lang/String;".encodeToByteArray()),
            ),
        )
}