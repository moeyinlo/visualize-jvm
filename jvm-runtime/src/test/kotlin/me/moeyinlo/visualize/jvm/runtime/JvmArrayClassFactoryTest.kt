package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class JvmArrayClassFactoryTest {
    @Test
    fun `creates primitive array class metadata with JVMS superclass and interfaces`() {
        val methodArea = JvmMethodArea()
        val factory = JvmArrayClassFactory(methodArea)

        val arrayClass = factory.createPrimitiveArrayClass(JvmPrimitiveArrayComponent.Int)

        assertEquals("[I", arrayClass.definition.internalName)
        assertEquals("java/lang/Object", arrayClass.definition.superclassName)
        assertEquals(
            listOf("java/lang/Cloneable", "java/io/Serializable"),
            arrayClass.definition.interfaceNames,
        )
        assertEquals(
            JvmLoadedClassKey("[I", JvmClassLoaderIdentity.Bootstrap),
            arrayClass.loadedClassKey,
        )
        assertEquals(setOf(JvmClassLoaderIdentity.Bootstrap), arrayClass.initiatingLoaders)
        assertEquals(JvmPrimitiveArrayComponent.Int, factory.metadataFor("[I")?.component)
        assertSame(arrayClass, methodArea.getClass("[I"))
    }
}
