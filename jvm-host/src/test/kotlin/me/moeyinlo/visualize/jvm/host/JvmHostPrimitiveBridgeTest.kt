package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmHostPrimitiveBridgeTest {
    @Test
    fun `converts guest primitive values to host reflection arguments`() {
        assertEquals(true, JvmHostPrimitiveBridge.toHost(JvmBooleanValue(true), Boolean::class.javaPrimitiveType!!))
        assertEquals(7.toByte(), JvmHostPrimitiveBridge.toHost(JvmByteValue(7), Byte::class.javaPrimitiveType!!))
        assertEquals('A', JvmHostPrimitiveBridge.toHost(JvmCharValue('A'.code), Char::class.javaPrimitiveType!!))
        assertEquals(8.toShort(), JvmHostPrimitiveBridge.toHost(JvmShortValue(8), Short::class.javaPrimitiveType!!))
        assertEquals(9, JvmHostPrimitiveBridge.toHost(JvmIntValue(9), Int::class.javaPrimitiveType!!))
        assertEquals(10L, JvmHostPrimitiveBridge.toHost(JvmLongValue(10L), Long::class.javaPrimitiveType!!))
        assertEquals(1.5f, JvmHostPrimitiveBridge.toHost(JvmFloatValue(1.5f), Float::class.javaPrimitiveType!!))
        assertEquals(2.5, JvmHostPrimitiveBridge.toHost(JvmDoubleValue(2.5), Double::class.javaPrimitiveType!!))
    }

    @Test
    fun `converts host primitive return values to guest primitive values`() {
        assertEquals(JvmBooleanValue(false), JvmHostPrimitiveBridge.fromHost(false, Boolean::class.javaPrimitiveType!!))
        assertEquals(JvmByteValue(-7), JvmHostPrimitiveBridge.fromHost((-7).toByte(), Byte::class.javaPrimitiveType!!))
        assertEquals(JvmCharValue('Z'.code), JvmHostPrimitiveBridge.fromHost('Z', Char::class.javaPrimitiveType!!))
        assertEquals(JvmShortValue(-8), JvmHostPrimitiveBridge.fromHost((-8).toShort(), Short::class.javaPrimitiveType!!))
        assertEquals(JvmIntValue(9), JvmHostPrimitiveBridge.fromHost(9, Int::class.javaPrimitiveType!!))
        assertEquals(JvmLongValue(10L), JvmHostPrimitiveBridge.fromHost(10L, Long::class.javaPrimitiveType!!))
        assertEquals(JvmFloatValue(1.5f), JvmHostPrimitiveBridge.fromHost(1.5f, Float::class.javaPrimitiveType!!))
        assertEquals(JvmDoubleValue(2.5), JvmHostPrimitiveBridge.fromHost(2.5, Double::class.javaPrimitiveType!!))
    }

    @Test
    fun `rejects mismatched guest primitive value and host target type`() {
        val exception = assertFailsWith<JvmHostPrimitiveBridgeException> {
            JvmHostPrimitiveBridge.toHost(JvmIntValue(1), Long::class.javaPrimitiveType!!)
        }

        assertEquals("Cannot bridge guest int to host long", exception.message)
    }

    @Test
    fun `rejects null host primitive return values`() {
        val exception = assertFailsWith<JvmHostPrimitiveBridgeException> {
            JvmHostPrimitiveBridge.fromHost(null, Int::class.javaPrimitiveType!!)
        }

        assertEquals("Host primitive int returned null", exception.message)
    }
}
