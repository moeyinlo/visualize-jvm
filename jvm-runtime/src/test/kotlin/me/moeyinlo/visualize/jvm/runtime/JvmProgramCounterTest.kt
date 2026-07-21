package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmProgramCounterTest {
    @Test
    fun `bytecode pc stores the current non negative instruction offset`() {
        assertEquals(0, JvmProgramCounter.BytecodeOffset(0).offset)
        assertEquals(123, JvmProgramCounter.BytecodeOffset(123).offset)
        assertEquals(Int.MAX_VALUE, JvmProgramCounter.BytecodeOffset(Int.MAX_VALUE).offset)
    }

    @Test
    fun `bytecode pc rejects negative instruction offsets`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmProgramCounter.BytecodeOffset(-1)
        }

        assertEquals("program counter offset must be non-negative: -1", exception.message)
    }

    @Test
    fun `native method pc is explicitly undefined`() {
        assertSame(
            JvmProgramCounter.UndefinedForNativeMethod,
            JvmProgramCounter.UndefinedForNativeMethod,
        )
    }
}
