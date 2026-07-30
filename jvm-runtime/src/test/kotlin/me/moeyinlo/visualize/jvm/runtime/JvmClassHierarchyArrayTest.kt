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
}
