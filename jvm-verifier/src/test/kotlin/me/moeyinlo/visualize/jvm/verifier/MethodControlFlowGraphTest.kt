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
