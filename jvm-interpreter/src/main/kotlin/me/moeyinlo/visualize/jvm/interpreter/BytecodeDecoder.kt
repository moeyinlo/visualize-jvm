package me.moeyinlo.visualize.jvm.interpreter

class BytecodeDecodingException(message: String) : RuntimeException(message)

data class DecodedInstruction(
    val offset: Int,
    val metadata: OpcodeMetadata,
    val operands: List<Int>,
)

object BytecodeDecoder {
    fun decode(code: ByteArray): List<DecodedInstruction> {
        val instructions = mutableListOf<DecodedInstruction>()
        var offset = 0
        while (offset < code.size) {
            val metadata = OpcodeTable.metadata(code[offset])
            val length = instructionLengthAt(code = code, metadata = metadata, offset = offset)
            val nextOffset = offset + length
            if (nextOffset > code.size) {
                throw BytecodeDecodingException(
                    "Instruction ${metadata.mnemonic} at offset $offset requires $length bytes, code length is ${code.size}",
                )
            }

            instructions += DecodedInstruction(
                offset = offset,
                metadata = metadata,
                operands = code.sliceUnsignedBytes(offset + 1, nextOffset),
            )
            offset = nextOffset
        }
        return instructions
    }

    private fun instructionLengthAt(code: ByteArray, metadata: OpcodeMetadata, offset: Int): Int =
        when (metadata.format) {
            OpcodeFormat.Fixed -> metadata.fixedLength ?: throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset has no fixed length metadata",
            )
            OpcodeFormat.TableSwitch -> tableswitchLengthAt(code = code, metadata = metadata, offset = offset)
            OpcodeFormat.LookupSwitch,
            OpcodeFormat.Wide,
            -> throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires variable-length decoding",
            )
            OpcodeFormat.Reserved -> throw BytecodeDecodingException(
                "Reserved opcode ${metadata.mnemonic} (${metadata.opcode.hexByte()}) at offset $offset cannot be decoded for execution",
            )
        }

    private fun tableswitchLengthAt(code: ByteArray, metadata: OpcodeMetadata, offset: Int): Int {
        val padding = (4 - ((offset + 1) % 4)) % 4
        val alignedOperandOffset = offset + 1 + padding
        val headerLength = 1 + padding + TABLESWITCH_HEADER_BYTES
        val headerEnd = offset + headerLength
        if (headerEnd > code.size) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires $headerLength bytes, code length is ${code.size}",
            )
        }

        val low = code.readSignedInt(alignedOperandOffset + Int.SIZE_BYTES)
        val high = code.readSignedInt(alignedOperandOffset + Int.SIZE_BYTES * 2)
        if (high < low) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset has high $high lower than low $low",
            )
        }

        val jumpOffsetCount = high.toLong() - low.toLong() + 1L
        val requiredLength = 1L + padding + TABLESWITCH_HEADER_BYTES + jumpOffsetCount * Int.SIZE_BYTES
        if (requiredLength > code.size - offset) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires $requiredLength bytes, code length is ${code.size}",
            )
        }
        return requiredLength.toInt()
    }

    private fun ByteArray.sliceUnsignedBytes(fromIndex: Int, toIndex: Int): List<Int> =
        (fromIndex until toIndex).map { index -> this[index].toInt() and 0xFF }

    private fun ByteArray.readSignedInt(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"

    private const val TABLESWITCH_HEADER_BYTES = Int.SIZE_BYTES * 3
}
