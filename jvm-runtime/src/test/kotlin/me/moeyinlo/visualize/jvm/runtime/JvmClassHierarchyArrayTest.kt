package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmClassHierarchyArrayTest {
    @Test
    fun `directSuperclassName returns Object for primitive reference and multidimensional arrays`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "java/lang/Object", superclassName = null),
                JvmClassDefinition(internalName = "pkg/Example"),
            ),
        )

        assertEquals("java/lang/Object", hierarchy.directSuperclassName("[I"))
        assertEquals("java/lang/Object", hierarchy.directSuperclassName("[Lpkg/Example;"))
        assertEquals("java/lang/Object", hierarchy.directSuperclassName("[[Lpkg/Example;"))
    }

    @Test
    fun `directSuperinterfaceNames returns Cloneable and Serializable for arrays`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "java/lang/Cloneable", isInterface = true),
                JvmClassDefinition(internalName = "java/io/Serializable", isInterface = true),
                JvmClassDefinition(internalName = "pkg/Example"),
            ),
        )

        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            hierarchy.directSuperinterfaceNames("[I"),
        )
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            hierarchy.directSuperinterfaceNames("[Lpkg/Example;"),
        )
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            hierarchy.directSuperinterfaceNames("[[Lpkg/Example;"),
        )
    }
}
