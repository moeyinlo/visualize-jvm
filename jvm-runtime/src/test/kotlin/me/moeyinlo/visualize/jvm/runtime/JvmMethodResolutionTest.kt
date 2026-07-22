package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmMethodResolutionTest {
    @Test
    fun `method resolution finds a static method declared directly by the referenced class`() {
        val method = JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(method),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution searches the superclass chain after the referenced class`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    superclassName = "Grandparent",
                ),
                JvmClassDefinition(
                    internalName = "Grandparent",
                    methods = listOf(JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Grandparent",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution does not inherit instance initialization methods from superclasses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(JvmMethodDefinition(name = "<init>", descriptor = "()V", isStatic = false)),
                ),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "<init>",
                descriptor = "()V",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.<init>:()V", exception.message)
    }

    @Test
    fun `virtual method resolution starts at the receiver class before superclasses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Sub",
                    superclassName = "Base",
                    methods = listOf(JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false)),
                ),
                JvmClassDefinition(
                    internalName = "Base",
                    methods = listOf(JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Sub",
                name = "value",
                descriptor = "()I",
                isStatic = false,
            ),
            hierarchy.resolveVirtualMethod(
                receiverClassName = "Sub",
                name = "value",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `class initialization method lookup returns declared static void clinit only`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(
                        JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = true, maxStack = 1),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "<clinit>",
                descriptor = "()V",
                isStatic = true,
                maxStack = 1,
            ),
            hierarchy.classInitializationMethod("Example"),
        )
    }

    @Test
    fun `class initialization method lookup does not inherit superclass clinit`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example", superclassName = "Parent"),
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = true)),
                ),
            ),
        )

        assertNull(hierarchy.classInitializationMethod("Example"))
    }

    @Test
    fun `class initialization method lookup ignores invalid clinit shapes already rejected by classfile validation`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(
                        JvmMethodDefinition(name = "<clinit>", descriptor = "(I)V", isStatic = true),
                        JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = false),
                    ),
                ),
            ),
        )

        assertNull(hierarchy.classInitializationMethod("Example"))
    }

    @Test
    fun `class hierarchy exposes only the direct superclass name`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    superclassName = "Grandparent",
                ),
                JvmClassDefinition(internalName = "Grandparent"),
            ),
        )

        assertEquals("Parent", hierarchy.directSuperclassName("Example"))
        assertEquals("Grandparent", hierarchy.directSuperclassName("Parent"))
        assertNull(hierarchy.directSuperclassName("Grandparent"))
        assertNull(hierarchy.directSuperclassName("Missing"))
    }

    @Test
    fun `method resolution throws guest NoSuchMethodError when lookup misses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example"),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "missing",
                descriptor = "()V",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.missing:()V", exception.message)
    }
}
