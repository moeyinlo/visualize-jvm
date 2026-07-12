package me.moeyinlo.visualize.jvm.interpreter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OpcodeMetadataTest {
    @Test
    fun `opcode metadata table contains one entry for every byte value`() {
        assertEquals(256, OpcodeTable.entries.size)
        OpcodeTable.entries.forEachIndexed { opcode, metadata ->
            assertEquals(opcode, metadata.opcode)
        }
    }

    @Test
    fun `opcode metadata records fixed length instructions`() {
        assertEquals(
            OpcodeMetadata(opcode = 0x00, mnemonic = "nop", format = OpcodeFormat.Fixed, fixedLength = 1),
            OpcodeTable.metadata(0x00),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0x15, mnemonic = "iload", format = OpcodeFormat.Fixed, fixedLength = 2),
            OpcodeTable.metadata(0x15),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0x13, mnemonic = "ldc_w", format = OpcodeFormat.Fixed, fixedLength = 3),
            OpcodeTable.metadata(0x13),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0xBA, mnemonic = "invokedynamic", format = OpcodeFormat.Fixed, fixedLength = 5),
            OpcodeTable.metadata(0xBA),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0xC8, mnemonic = "goto_w", format = OpcodeFormat.Fixed, fixedLength = 5),
            OpcodeTable.metadata(0xC8),
        )
    }

    @Test
    fun `opcode metadata marks alignment and wide dependent variable length instructions`() {
        assertEquals(OpcodeFormat.TableSwitch, OpcodeTable.metadata(0xAA).format)
        assertEquals("tableswitch", OpcodeTable.metadata(0xAA).mnemonic)
        assertNull(OpcodeTable.metadata(0xAA).fixedLength)

        assertEquals(OpcodeFormat.LookupSwitch, OpcodeTable.metadata(0xAB).format)
        assertEquals("lookupswitch", OpcodeTable.metadata(0xAB).mnemonic)
        assertNull(OpcodeTable.metadata(0xAB).fixedLength)

        assertEquals(OpcodeFormat.Wide, OpcodeTable.metadata(0xC4).format)
        assertEquals("wide", OpcodeTable.metadata(0xC4).mnemonic)
        assertNull(OpcodeTable.metadata(0xC4).fixedLength)
    }

    @Test
    fun `opcode metadata marks reserved opcodes`() {
        assertEquals(
            OpcodeMetadata(opcode = 0xCA, mnemonic = "breakpoint", format = OpcodeFormat.Reserved, fixedLength = 1),
            OpcodeTable.metadata(0xCA),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0xFE, mnemonic = "impdep1", format = OpcodeFormat.Reserved, fixedLength = 1),
            OpcodeTable.metadata(0xFE),
        )
        assertEquals(
            OpcodeMetadata(opcode = 0xFF, mnemonic = "impdep2", format = OpcodeFormat.Reserved, fixedLength = 1),
            OpcodeTable.metadata(0xFF),
        )
        assertEquals(OpcodeFormat.Reserved, OpcodeTable.metadata(0xCB).format)
        assertEquals("reserved_0xcb", OpcodeTable.metadata(0xCB).mnemonic)
    }

    @Test
    fun `opcode metadata rejects values outside unsigned byte range`() {
        assertFailsWith<IllegalArgumentException> { OpcodeTable.metadata(-1) }
        assertFailsWith<IllegalArgumentException> { OpcodeTable.metadata(256) }
    }
}
