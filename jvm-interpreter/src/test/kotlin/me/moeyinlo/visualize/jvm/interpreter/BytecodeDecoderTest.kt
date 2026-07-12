package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BytecodeDecoderTest {
    @Test
    fun `decoder decodes fixed length instructions with offsets and unsigned operand bytes`() {
        val instructions = BytecodeDecoder.decode(
            byteArrayOf(
                0x03.toByte(),
                0x10.toByte(), 0xFF.toByte(),
                0x11.toByte(), 0x01.toByte(), 0x02.toByte(),
                0xB1.toByte(),
            ),
        )

        assertEquals(
            listOf(
                DecodedInstruction(offset = 0, metadata = OpcodeTable.metadata(0x03), operands = emptyList()),
                DecodedInstruction(offset = 1, metadata = OpcodeTable.metadata(0x10), operands = listOf(0xFF)),
                DecodedInstruction(offset = 3, metadata = OpcodeTable.metadata(0x11), operands = listOf(0x01, 0x02)),
                DecodedInstruction(offset = 6, metadata = OpcodeTable.metadata(0xB1), operands = emptyList()),
            ),
            instructions,
        )
    }

    @Test
    fun `decoder treats opcodes as unsigned bytes`() {
        val instructions = BytecodeDecoder.decode(
            byteArrayOf(
                0xC8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            ),
        )

        assertEquals(
            listOf(
                DecodedInstruction(offset = 0, metadata = OpcodeTable.metadata(0xC8), operands = listOf(0x00, 0x00, 0x00, 0x01)),
            ),
            instructions,
        )
    }

    @Test
    fun `decoder rejects truncated fixed length instructions`() {
        val exception = assertFailsWith<BytecodeDecodingException> {
            BytecodeDecoder.decode(byteArrayOf(0x11.toByte(), 0x00.toByte()))
        }

        assertEquals(
            "Instruction sipush at offset 0 requires 3 bytes, code length is 2",
            exception.message,
        )
    }

    @Test
    fun `decoder rejects variable length instructions until their dedicated decoders run`() {
        val exception = assertFailsWith<BytecodeDecodingException> {
            BytecodeDecoder.decode(byteArrayOf(0xAA.toByte()))
        }

        assertEquals(
            "Instruction tableswitch at offset 0 requires variable-length decoding",
            exception.message,
        )
    }

    @Test
    fun `decoder rejects reserved opcodes`() {
        val exception = assertFailsWith<BytecodeDecodingException> {
            BytecodeDecoder.decode(byteArrayOf(0xCA.toByte()))
        }

        assertEquals(
            "Reserved opcode breakpoint (0xca) at offset 0 cannot be decoded for execution",
            exception.message,
        )
    }
}
