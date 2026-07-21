package me.moeyinlo.visualize.jvm.interpreter

enum class OpcodeExecutionStatus {
    Implemented,
    MethodReturnOnly,
    NotYetImplemented,
    Reserved,
}

data class OpcodeExecutionCoverageEntry(
    val opcode: Int,
    val mnemonic: String,
    val format: OpcodeFormat,
    val status: OpcodeExecutionStatus,
)

object OpcodeExecutionCoverage {
    private val methodReturnOnlyOpcodes: Set<Int> = (0xAC..0xB1).toSet()

    private val implementedOpcodes: Set<Int> =
        (0x00..0xAB).toSet() +
            (0xB2..0xB8).toSet() +
            (0xBB..0xBE).toSet() +
            setOf(0xC0, 0xC1, 0xC4) +
            (0xC6..0xC9).toSet()

    val entries: List<OpcodeExecutionCoverageEntry> = OpcodeTable.entries.map { metadata ->
        OpcodeExecutionCoverageEntry(
            opcode = metadata.opcode,
            mnemonic = metadata.mnemonic,
            format = metadata.format,
            status = statusFor(metadata),
        )
    }

    private fun statusFor(metadata: OpcodeMetadata): OpcodeExecutionStatus =
        when {
            metadata.format == OpcodeFormat.Reserved -> OpcodeExecutionStatus.Reserved
            metadata.opcode in methodReturnOnlyOpcodes -> OpcodeExecutionStatus.MethodReturnOnly
            metadata.opcode in implementedOpcodes -> OpcodeExecutionStatus.Implemented
            else -> OpcodeExecutionStatus.NotYetImplemented
        }
}
