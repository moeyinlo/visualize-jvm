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
            val length = fixedLengthAt(metadata = metadata, offset = offset)
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

    private fun fixedLengthAt(metadata: OpcodeMetadata, offset: Int): Int =
        when (metadata.format) {
            OpcodeFormat.Fixed -> metadata.fixedLength ?: throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset has no fixed length metadata",
            )
            OpcodeFormat.TableSwitch,
            OpcodeFormat.LookupSwitch,
            OpcodeFormat.Wide,
            -> throw BytecodeDecodingException(
                "Instruction ${metadata.mnemonic} at offset $offset requires variable-length decoding",
            )
            OpcodeFormat.Reserved -> throw BytecodeDecodingException(
                "Reserved opcode ${metadata.mnemonic} (${metadata.opcode.hexByte()}) at offset $offset cannot be decoded for execution",
            )
        }

    private fun ByteArray.sliceUnsignedBytes(fromIndex: Int, toIndex: Int): List<Int> =
        (fromIndex until toIndex).map { index -> this[index].toInt() and 0xFF }

    private fun Int.hexByte(): String = "0x${toString(16).padStart(2, '0')}"
}
