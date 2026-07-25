package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmReferenceId
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedField
import me.moeyinlo.visualize.jvm.runtime.JvmResolvedMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmJniHandleTableTest {
    @Test
    fun `local handle table stores and resolves object class method and field handles`() {
        val table = JvmJniHandleTable()
        val objectReference = JvmObjectReferenceValue(JvmReferenceId(7))
        val method = JvmResolvedMethod(
            ownerClassName = "Example",
            name = "value",
            descriptor = "()I",
            isStatic = false,
        )
        val field = JvmResolvedField(
            ownerClassName = "Example",
            name = "counter",
            descriptor = "I",
            isStatic = false,
        )

        val objectHandle = table.newObjectHandle(objectReference)
        val classHandle = table.newClassHandle("Example")
        val methodHandle = table.newMethodIdHandle(method)
        val fieldHandle = table.newFieldIdHandle(field)

        assertEquals(objectReference, table.resolveObject(objectHandle))
        assertEquals("Example", table.resolveClass(classHandle))
        assertEquals(method, table.resolveMethodId(methodHandle))
        assertEquals(field, table.resolveFieldId(fieldHandle))
    }

    @Test
    fun `local handle table rejects cross kind handle resolution`() {
        val table = JvmJniHandleTable()
        val objectHandle = table.newObjectHandle(JvmObjectReferenceValue(JvmReferenceId(9)))

        assertFailsWith<JvmJniHandleTypeException> {
            table.resolveMethodId(objectHandle)
        }
    }

    @Test
    fun `deleted local handles cannot be resolved again`() {
        val table = JvmJniHandleTable()
        val fieldHandle = table.newFieldIdHandle(
            JvmResolvedField(
                ownerClassName = "Example",
                name = "counter",
                descriptor = "I",
                isStatic = false,
            ),
        )

        table.deleteLocal(fieldHandle)

        assertFailsWith<JvmJniInvalidHandleException> {
            table.resolveFieldId(fieldHandle)
        }
    }

    @Test
    fun `jobject handles resolve nullable guest object references`() {
        val table = JvmJniHandleTable()
        val objectReference = JvmObjectReferenceValue(JvmReferenceId(11))
        val localHandle = table.newObjectHandle(objectReference)
        val globalHandle = table.newGlobalObjectHandle(objectReference)
        val weakGlobalHandle = table.newWeakGlobalObjectHandle(objectReference)

        assertEquals(objectReference, table.resolveObjectOrNull(localHandle))
        assertEquals(objectReference, table.resolveObjectOrNull(globalHandle))
        assertEquals(objectReference, table.resolveObjectOrNull(weakGlobalHandle))
        assertEquals(null, table.resolveObjectOrNull(null))
    }

    @Test
    fun `jclass handles resolve nullable class names`() {
        val table = JvmJniHandleTable()
        val classHandle = table.newClassHandle("java/lang/String")

        assertEquals("java/lang/String", table.resolveClassOrNull(classHandle))
        assertEquals(null, table.resolveClassOrNull(null))
    }

    @Test
    fun `jmethodID handles resolve nullable resolved methods`() {
        val table = JvmJniHandleTable()
        val method = JvmResolvedMethod(
            ownerClassName = "Example",
            name = "run",
            descriptor = "()V",
            isStatic = false,
        )
        val methodHandle = table.newMethodIdHandle(method)

        assertEquals(method, table.resolveMethodIdOrNull(methodHandle))
        assertEquals(null, table.resolveMethodIdOrNull(null))
    }

    @Test
    fun `jfieldID handles resolve nullable resolved fields`() {
        val table = JvmJniHandleTable()
        val field = JvmResolvedField(
            ownerClassName = "Example",
            name = "value",
            descriptor = "I",
            isStatic = false,
        )
        val fieldHandle = table.newFieldIdHandle(field)

        assertEquals(field, table.resolveFieldIdOrNull(fieldHandle))
        assertEquals(null, table.resolveFieldIdOrNull(null))
    }
}
