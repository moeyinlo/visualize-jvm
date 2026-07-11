package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

class MethodControlFlowGraphTest {
    @Test
    fun `builds fallthrough and branch edges for fixed-size bytecode`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0x03,
                0x99.toByte(), 0x00, 0x07,
                0x04,
                0xA7.toByte(), 0x00, 0x04,
                0x05,
                0xAC.toByte(),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 1, 4, 5, 8, 9), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 1, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(1, 4, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(1, 8, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(4, 5, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(5, 9, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(8, 9, ControlFlowEdgeKind.FallThrough),
            ),
            graph.edges,
        )
    }

    @Test
    fun `adds exception handler edges from protected instruction offsets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0x03,
                0x3B,
                0xB1.toByte(),
            ),
            exceptionTable = listOf(
                CodeExceptionHandler(
                    startPc = 0,
                    endPc = 2,
                    handlerPc = 2,
                    catchType = null,
                ),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(
            setOf(
                ControlFlowEdge(0, 1, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(1, 2, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(0, 2, ControlFlowEdgeKind.ExceptionHandler),
                ControlFlowEdge(1, 2, ControlFlowEdgeKind.ExceptionHandler),
            ),
            graph.edges,
        )
    }

    @Test
    fun `treats monitor instructions as one byte instructions`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xC2.toByte(),
                0xC3.toByte(),
                0xB1.toByte(),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 1, 2), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 1, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(1, 2, ControlFlowEdgeKind.FallThrough),
            ),
            graph.edges,
        )
    }

    @Test
    fun `rejects execution falling off the end of code`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0x00,
            ),
        )

        val exception = assertFailsWith<ControlFlowGraphException> {
            MethodControlFlowGraphBuilder.build(code)
        }

        assertEquals(
            "Execution can fall off the end of code after instruction 0",
            exception.message,
        )
    }

    @Test
    fun `does not add fallthrough edges for ret`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xA9.toByte(), 0x00,
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0), graph.instructionOffsets)
        assertEquals(emptySet(), graph.edges)
    }

    @Test
    fun `does not add fallthrough edges for wide ret`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 257,
            code = byteArrayOf(
                0xC4.toByte(), 0xA9.toByte(), 0x01, 0x00,
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0), graph.instructionOffsets)
        assertEquals(emptySet(), graph.edges)
    }

    @Test
    fun `builds branch edges for jsr subroutine targets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xA8.toByte(), 0x00, 0x04,
                0xB1.toByte(),
                0xA9.toByte(), 0x00,
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 3, 4), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 4, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 3, ControlFlowEdgeKind.FallThrough),
            ),
            graph.edges,
        )
    }

    @Test
    fun `builds branch edges for jsr_w subroutine targets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xC9.toByte(), 0x00, 0x00, 0x00, 0x06,
                0xB1.toByte(),
                0xA9.toByte(), 0x00,
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 5, 6), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 6, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 5, ControlFlowEdgeKind.FallThrough),
            ),
            graph.edges,
        )
    }

    @Test
    fun `builds fallthrough edges for wide local variable instructions`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 257,
            code = byteArrayOf(
                0xC4.toByte(), 0x15, 0x01, 0x00,
                0xC4.toByte(), 0x84.toByte(), 0x01, 0x00, 0x00, 0x01,
                0xB1.toByte(),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 4, 10), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 4, ControlFlowEdgeKind.FallThrough),
                ControlFlowEdge(4, 10, ControlFlowEdgeKind.FallThrough),
            ),
            graph.edges,
        )
    }

    @Test
    fun `rejects wide modifying an unsupported opcode`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xC4.toByte(), 0x03, 0x00, 0x00,
                0xB1.toByte(),
            ),
        )

        val exception = assertFailsWith<ControlFlowGraphException> {
            MethodControlFlowGraphBuilder.build(code)
        }

        assertEquals(
            "wide at 0 cannot modify opcode 0x3",
            exception.message,
        )
    }

    @Test
    fun `builds branch edges for tableswitch jump table targets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xAA.toByte(),
                0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x18,
                0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x02,
                0x00, 0x00, 0x00, 0x19,
                0x00, 0x00, 0x00, 0x1A,
                0xB1.toByte(),
                0xB1.toByte(),
                0xB1.toByte(),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 24, 25, 26), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 24, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 25, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 26, ControlFlowEdgeKind.Branch),
            ),
            graph.edges,
        )
    }

    @Test
    fun `builds branch edges for lookupswitch match offset targets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xAB.toByte(),
                0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x1C,
                0x00, 0x00, 0x00, 0x02,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x00, 0x00, 0x00, 0x1D,
                0x00, 0x00, 0x00, 0x05,
                0x00, 0x00, 0x00, 0x1E,
                0xB1.toByte(),
                0xB1.toByte(),
                0xB1.toByte(),
            ),
        )

        val graph = MethodControlFlowGraphBuilder.build(code)

        assertEquals(setOf(0, 28, 29, 30), graph.instructionOffsets)
        assertEquals(
            setOf(
                ControlFlowEdge(0, 28, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 29, ControlFlowEdgeKind.Branch),
                ControlFlowEdge(0, 30, ControlFlowEdgeKind.Branch),
            ),
            graph.edges,
        )
    }

    @Test
    fun `rejects lookupswitch match values that are not increasing`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xAB.toByte(),
                0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x1C,
                0x00, 0x00, 0x00, 0x02,
                0x00, 0x00, 0x00, 0x05,
                0x00, 0x00, 0x00, 0x1D,
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                0x00, 0x00, 0x00, 0x1E,
                0xB1.toByte(),
                0xB1.toByte(),
                0xB1.toByte(),
            ),
        )

        val exception = assertFailsWith<ControlFlowGraphException> {
            MethodControlFlowGraphBuilder.build(code)
        }

        assertEquals(
            "lookupswitch at 0 match value -1 is not greater than previous match value 5",
            exception.message,
        )
    }

    @Test
    fun `rejects branch targets that are not instruction offsets`() {
        val code = CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = 1,
            maxLocals = 1,
            code = byteArrayOf(
                0xA7.toByte(), 0x00, 0x01,
                0xB1.toByte(),
            ),
        )

        assertFailsWith<ControlFlowGraphException> {
            MethodControlFlowGraphBuilder.build(code)
        }
    }
}
