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
            OpcodeFormat.LookupSwitch -> lookupswitchLengthAt(code = code, metadata = metadata, offset = offset)
            OpcodeFormat.Wide -> wideLengthAt(code = code, metadata = metadata, offset = offset)
            OpcodeFormat.Reserved -> throw BytecodeDecodingException(
                "Reserved opcode ${metadata.mnemonic} (${metadata.opcode.hexByte()}) at offset $offset cannot be decoded for execution",
            )
        }

    private fun tableswitchLengthAt(code: ByteArray, metadata: OpcodeMetadata, offset: Int): Int {
        val padding = paddingAfterOpcode(offset)
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

    private fun lookupswitchLengthAt(code: ByteArray, metadata: OpcodeMetadata, offset: Int): Int {
        val padding = paddingAfterOpcode(offset)
        val alignedOperandOffset = offset + 1 + padding
        val headerLength = 1 + padding + LOOKUPSWITCH_HEADER_BYTES
        val headerEnd = offset + headerLength
        if (headerEnd > code.size) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires $headerLength bytes, code length is ${code.size}",
            )
        }

        val pairCount = code.readSignedInt(alignedOperandOffset + Int.SIZE_BYTES)
        if (pairCount < 0) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset has negative npairs $pairCount",
            )
        }

        val requiredLength = 1L + padding + LOOKUPSWITCH_HEADER_BYTES + pairCount.toLong() * LOOKUPSWITCH_PAIR_BYTES
        if (requiredLength > code.size - offset) {
            throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires $requiredLength bytes, code length is ${code.size}",
            )
        }
        return requiredLength.toInt()
    }

    private fun wideLengthAt(code: ByteArray, metadata: OpcodeMetadata, offset: Int): Int {
        if (offset + WIDE_MIN_BYTES > code.size) {
            return WIDE_MIN_BYTES
        }

        val modifiedOpcode = code[offset + 1].toInt() and 0xFF
        return when (modifiedOpcode) {
            0x84 -> WIDE_IINC_BYTES
            in 0x15..0x19,
            in 0x36..0x3A,
            0xA9,
            -> WIDE_LOCAL_BYTES
            else -> throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset cannot modify opcode " +
                    "${OpcodeTable.metadata(modifiedOpcode).mnemonic}",
            )
        }
    }

    private fun paddingAfterOpcode(offset: Int): Int = (4 - ((offset + 1) % 4)) % 4

    private fun ByteArray.sliceUnsignedBytes(fromIndex: Int, toIndex: Int): List<Int> =
        (fromIndex until toIndex).map { index -> this[index].toInt() and 0xFF }

    private fun ByteArray.readSignedInt(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"

    private const val TABLESWITCH_HEADER_BYTES = Int.SIZE_BYTES * 3
    private const val LOOKUPSWITCH_HEADER_BYTES = Int.SIZE_BYTES * 2
    private const val LOOKUPSWITCH_PAIR_BYTES = Int.SIZE_BYTES * 2
    private const val WIDE_MIN_BYTES = 2
    private const val WIDE_LOCAL_BYTES = 4
    private const val WIDE_IINC_BYTES = 6
}
