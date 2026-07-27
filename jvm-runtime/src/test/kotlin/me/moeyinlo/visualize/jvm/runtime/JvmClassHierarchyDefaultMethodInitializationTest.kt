package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmClassHierarchyDefaultMethodInitializationTest {
    @Test
    fun `default method superinterface enumeration visits shared ancestors once before children`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "pkg/BaseIface",
                    isInterface = true,
                    methods = listOf(defaultMethod("baseDefault")),
                ),
                JvmClassDefinition(
                    internalName = "pkg/LeftIface",
                    interfaceNames = listOf("pkg/BaseIface"),
                    isInterface = true,
                    methods = listOf(defaultMethod("leftDefault")),
                ),
                JvmClassDefinition(
                    internalName = "pkg/RightIface",
                    interfaceNames = listOf("pkg/BaseIface"),
                    isInterface = true,
                    methods = listOf(defaultMethod("rightDefault")),
                ),
                JvmClassDefinition(
                    internalName = "pkg/Child",
                    interfaceNames = listOf("pkg/LeftIface", "pkg/RightIface"),
                ),
            ),
        )

        assertEquals(
            listOf("pkg/BaseIface", "pkg/LeftIface", "pkg/RightIface"),
            hierarchy.defaultMethodSuperinterfaceNames("pkg/Child"),
        )
    }

    private fun defaultMethod(name: String): JvmMethodDefinition = JvmMethodDefinition(
        name = name,
        descriptor = "()V",
        isStatic = false,
        code = byteArrayOf(0xB1.toByte()),
        maxStack = 0,
        maxLocals = 1,
    )
}