package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class JvmInvokeDynamicCallSiteRegistryTest {
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
}