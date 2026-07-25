package me.moeyinlo.visualize.jvm.jni

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmSimulatedJniFunctionTableMetadataTest {
    @Test
    fun `function table exposes the current simulated JNIEnv slot count`() {
        val constructorSlotCount = JvmSimulatedJniFunctionTable::class.java.declaredConstructors.single().parameterCount

        assertEquals(153, JvmSimulatedJniFunctionTable.SlotCount)
        assertEquals(constructorSlotCount, JvmSimulatedJniFunctionTable.SlotCount)
    }
}
