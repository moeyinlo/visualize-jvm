package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpcodeExecutionCoverageTest {
    @Test
    fun `opcode execution coverage classifies every opcode table entry`() {
        val coverageByOpcode = OpcodeExecutionCoverage.entries.associateBy(OpcodeExecutionCoverageEntry::opcode)

        assertEquals(256, OpcodeExecutionCoverage.entries.size)
        assertEquals(OpcodeTable.entries.map(OpcodeMetadata::opcode), OpcodeExecutionCoverage.entries.map { it.opcode })
        assertEquals(OpcodeTable.entries.map(OpcodeMetadata::mnemonic), OpcodeExecutionCoverage.entries.map { it.mnemonic })
        assertEquals(OpcodeTable.entries.map(OpcodeMetadata::format), OpcodeExecutionCoverage.entries.map { it.format })
        assertEquals((0x00..0xFF).toSet(), coverageByOpcode.keys)
    }

    @Test
    fun `opcode execution coverage names current unsupported JVMS opcodes explicitly`() {
        val unsupported = OpcodeExecutionCoverage.entries
            .filter { entry -> entry.status == OpcodeExecutionStatus.NotYetImplemented }
            .map(OpcodeExecutionCoverageEntry::mnemonic)

        assertEquals(
            listOf(
                "invokeinterface",
                "invokedynamic",
                "multianewarray",
            ),
            unsupported,
        )
    }

    @Test
    fun `opcode execution coverage separates method return opcodes from top level execution`() {
        val returnOpcodes = OpcodeExecutionCoverage.entries
            .filter { entry -> entry.status == OpcodeExecutionStatus.MethodReturnOnly }
            .map(OpcodeExecutionCoverageEntry::mnemonic)

        assertEquals(
            listOf("ireturn", "lreturn", "freturn", "dreturn", "areturn", "return"),
            returnOpcodes,
        )
    }

    @Test
    fun `opcode execution coverage treats reserved opcodes as reserved instead of executable`() {
        val reservedOpcodes = OpcodeExecutionCoverage.entries
            .filter { entry -> entry.format == OpcodeFormat.Reserved }

        assertTrue(reservedOpcodes.isNotEmpty())
        assertTrue(reservedOpcodes.all { entry -> entry.status == OpcodeExecutionStatus.Reserved })
    }
}
