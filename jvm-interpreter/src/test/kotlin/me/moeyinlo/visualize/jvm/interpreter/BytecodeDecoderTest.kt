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
    fun `decoder decodes tableswitch with four byte aligned operands`() {
        val instructions = BytecodeDecoder.decode(
            byteArrayOf(
                0x00.toByte(),
                0xAA.toByte(),
                0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x10.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFC.toByte(),
                0xB1.toByte(),
            ),
        )

        assertEquals(
            listOf(
                DecodedInstruction(offset = 0, metadata = OpcodeTable.metadata(0x00), operands = emptyList()),
                DecodedInstruction(
                    offset = 1,
                    metadata = OpcodeTable.metadata(0xAA),
                    operands = listOf(
                        0x00, 0x00,
                        0x00, 0x00, 0x00, 0x10,
                        0x00, 0x00, 0x00, 0x01,
                        0x00, 0x00, 0x00, 0x02,
                        0x00, 0x00, 0x00, 0x20,
                        0xFF, 0xFF, 0xFF, 0xFC,
                    ),
                ),
                DecodedInstruction(offset = 24, metadata = OpcodeTable.metadata(0xB1), operands = emptyList()),
            ),
            instructions,
        )
    }

    @Test
    fun `decoder decodes lookupswitch with four byte aligned operands`() {
        val instructions = BytecodeDecoder.decode(
            byteArrayOf(
                0x00.toByte(),
                0xAB.toByte(),
                0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x10.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x20.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFC.toByte(),
                0xB1.toByte(),
            ),
        )

        assertEquals(
            listOf(
                DecodedInstruction(offset = 0, metadata = OpcodeTable.metadata(0x00), operands = emptyList()),
                DecodedInstruction(
                    offset = 1,
                    metadata = OpcodeTable.metadata(0xAB),
                    operands = listOf(
                        0x00, 0x00,
                        0x00, 0x00, 0x00, 0x10,
                        0x00, 0x00, 0x00, 0x02,
                        0xFF, 0xFF, 0xFF, 0xFF,
                        0x00, 0x00, 0x00, 0x20,
                        0x00, 0x00, 0x00, 0x03,
                        0xFF, 0xFF, 0xFF, 0xFC,
                    ),
                ),
                DecodedInstruction(offset = 28, metadata = OpcodeTable.metadata(0xB1), operands = emptyList()),
            ),
            instructions,
        )
    }

    @Test
    fun `decoder decodes wide local variable and iinc forms`() {
        val instructions = BytecodeDecoder.decode(
            byteArrayOf(
                0xC4.toByte(), 0x15.toByte(), 0x01.toByte(), 0x02.toByte(),
                0xC4.toByte(), 0x84.toByte(), 0x01.toByte(), 0x02.toByte(), 0xFF.toByte(), 0xFE.toByte(),
                0xB1.toByte(),
            ),
        )

        assertEquals(
            listOf(
                DecodedInstruction(offset = 0, metadata = OpcodeTable.metadata(0xC4), operands = listOf(0x15, 0x01, 0x02)),
                DecodedInstruction(
                    offset = 4,
                    metadata = OpcodeTable.metadata(0xC4),
                    operands = listOf(0x84, 0x01, 0x02, 0xFF, 0xFE),
                ),
                DecodedInstruction(offset = 10, metadata = OpcodeTable.metadata(0xB1), operands = emptyList()),
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
    fun `decoder rejects truncated wide instructions`() {
        val exception = assertFailsWith<BytecodeDecodingException> {
            BytecodeDecoder.decode(byteArrayOf(0xC4.toByte()))
        }

        assertEquals(
            "Instruction wide at offset 0 requires 2 bytes, code length is 1",
            exception.message,
        )
    }

    @Test
    fun `decoder rejects reserved opcodes`() {
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
