package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class JvmRuntimeDynamicLinkerTest {
    @Test
    fun `dynamic linker resolves and caches class and literal constants`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(
                JvmRuntimeClassSymbolicReference("java/lang/Object"),
                JvmRuntimeLiteralConstant(JvmIntValue(7)),
                JvmRuntimeStringConstant("hello"),
            ),
        )

        val classConstant = JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(1), JvmClassHierarchy.Empty)
        val literalConstant = JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(2), JvmClassHierarchy.Empty)
        val stringConstant = JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(3), JvmClassHierarchy.Empty)

        assertEquals(JvmRuntimeResolvedConstant.Class("java/lang/Object"), classConstant)
        assertEquals(JvmRuntimeResolvedConstant.Value(JvmIntValue(7)), literalConstant)
        assertEquals(JvmRuntimeResolvedConstant.String("hello"), stringConstant)
        assertSame(classConstant, pool.resolved(JvmRuntimeConstantPoolIndex(1)))
    }

    @Test
    fun `dynamic linker resolves field symbolic references through the class hierarchy`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(
                JvmRuntimeFieldSymbolicReference(
                    JvmFieldReference(ownerClassName = "Child", name = "answer", descriptor = "I"),
                ),
            ),
        )
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Child", superclassName = "Parent"),
                JvmClassDefinition(
                    internalName = "Parent",
                    fields = listOf(JvmFieldDefinition(name = "answer", descriptor = "I", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmRuntimeResolvedConstant.Field(
                JvmResolvedField(
                    ownerClassName = "Parent",
                    name = "answer",
                    descriptor = "I",
                    isStatic = true,
                ),
            ),
            JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(1), hierarchy),
        )
    }

    @Test
    fun `dynamic linker resolves method symbolic references through the class hierarchy`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(
                JvmRuntimeMethodSymbolicReference(
                    ownerClassName = "Child",
                    name = "run",
                    descriptor = "()V",
                ),
            ),
        )
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Child", superclassName = "Parent"),
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(JvmMethodDefinition(name = "run", descriptor = "()V", isStatic = false)),
                ),
            ),
        )

        assertEquals(
            JvmRuntimeResolvedConstant.Method(
                JvmResolvedMethod(
                    ownerClassName = "Parent",
                    name = "run",
                    descriptor = "()V",
                    isStatic = false,
                ),
            ),
            JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(1), hierarchy),
        )
    }

    @Test
    fun `dynamic linker returns cached resolutions without re-resolving`() {
        val pool = JvmRuntimeConstantPool(
            ownerClassName = "Example",
            entries = listOf(JvmRuntimeClassSymbolicReference("java/lang/Object")),
        )
        val cached = JvmRuntimeResolvedConstant.Class("CachedObject")
        pool.cacheResolved(JvmRuntimeConstantPoolIndex(1), cached)

        assertSame(cached, JvmRuntimeDynamicLinker.resolve(pool, JvmRuntimeConstantPoolIndex(1), JvmClassHierarchy.Empty))
    }
}
