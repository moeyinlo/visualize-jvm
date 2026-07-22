package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BytecodeDecoderTest {
    @Test
    fun `reserved opcodes are rejected before execution`() {
        val reservedOpcodes = listOf(
            0xCA to "breakpoint",
            0xFE to "impdep1",
            0xFF to "impdep2",
        )

        for ((opcode, mnemonic) in reservedOpcodes) {
            val exception = assertFailsWith<BytecodeDecodingException> {
                BytecodeDecoder.decode(byteArrayOf(opcode.toByte()))
            }

            assertEquals(
                "Reserved opcode $mnemonic (0x${opcode.toString(16)}) at offset 0 cannot be decoded for execution",
                exception.message,
            )
        }
    }
}
