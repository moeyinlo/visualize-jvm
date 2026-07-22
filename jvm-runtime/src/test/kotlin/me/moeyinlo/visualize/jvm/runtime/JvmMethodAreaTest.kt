package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JvmMethodAreaTest {
    @Test
    fun `method area defines and looks up class metadata by internal name`() {
        val methodArea = JvmMethodArea()
        val entry = JvmMethodAreaEntry(
            definition = JvmClassDefinition(
                internalName = "Example",
                superclassName = "java/lang/Object",
                fields = listOf(
                    JvmFieldDefinition(name = "counter", descriptor = "I", isStatic = true),
                ),
                methods = listOf(
                    JvmMethodDefinition(name = "run", descriptor = "()V", isStatic = false),
                ),
            ),
        )

        methodArea.defineClass(entry)

        assertEquals(1, methodArea.classCount)
        assertTrue(methodArea.hasClass("Example"))
        assertSame(entry, methodArea.getClass("Example"))
        assertEquals(listOf(entry), methodArea.toList())
    }

    @Test
    fun `method area rejects duplicate class definitions`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "Example")))

        val exception = assertFailsWith<JvmMethodAreaDefinitionException> {
            methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "Example")))
        }

        assertEquals("Class Example is already defined in the method area", exception.message)
    }

    @Test
    fun `method area rejects blank class names`() {
        val methodArea = JvmMethodArea()

        val defineException = assertFailsWith<IllegalArgumentException> {
            methodArea.defineClass(JvmMethodAreaEntry(JvmClassDefinition(internalName = "")))
        }
        val lookupException = assertFailsWith<IllegalArgumentException> {
            methodArea.getClass("")
        }

        assertEquals("class internal name must not be blank", defineException.message)
        assertEquals("class internal name must not be blank", lookupException.message)
    }

    @Test
    fun `method area reports missing classes`() {
        val methodArea = JvmMethodArea()

        val exception = assertFailsWith<JvmMethodAreaAccessException> {
            methodArea.getClass("Missing")
        }

        assertFalse(methodArea.hasClass("Missing"))
        assertEquals("Class Missing is not defined in the method area", exception.message)
    }

    @Test
    fun `method area exposes a class hierarchy view over defined classes`() {
        val methodArea = JvmMethodArea()
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(
                        JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false),
                    ),
                ),
            ),
        )
        methodArea.defineClass(
            JvmMethodAreaEntry(
                JvmClassDefinition(
                    internalName = "Child",
                    superclassName = "Parent",
                ),
            ),
        )

        val resolved = methodArea.classHierarchy().resolveVirtualMethod("Child", "value", "()I")

        assertEquals("Parent", resolved.ownerClassName)
        assertEquals("value", resolved.name)
        assertEquals("()I", resolved.descriptor)
    }
}
