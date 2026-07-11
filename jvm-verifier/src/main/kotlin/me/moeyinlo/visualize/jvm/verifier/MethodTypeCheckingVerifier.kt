package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute

object MethodTypeCheckingVerifier {
    fun verify(
        code: CodeAttribute,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        val frames = frameStates.toList()
        MethodResourceLimitsVerifier.verify(code = code, frameStates = frames)
        val controlFlowGraph = MethodControlFlowGraphBuilder.build(code)
        verifyFrameOffsets(frames = frames, instructionOffsets = controlFlowGraph.instructionOffsets)
        verifyBranchTargetFrames(controlFlowGraph.edges, frameOffsets = frames.mapTo(hashSetOf()) { it.bytecodeOffset })
    }

    private fun verifyFrameOffsets(
        frames: List<VerificationFrameState>,
        instructionOffsets: Set<Int>,
    ) {
        var previousOffset: Int? = null
        frames.forEach { frame ->
            val previous = previousOffset
            if (previous != null && frame.bytecodeOffset <= previous) {
                throw MethodVerificationException(
                    "Frame at bytecode offset ${frame.bytecodeOffset} is not after previous frame offset $previous",
                )
            }
            if (frame.bytecodeOffset !in instructionOffsets) {
                throw MethodVerificationException(
                    "Frame at bytecode offset ${frame.bytecodeOffset} does not correspond to an instruction offset",
                )
            }
            previousOffset = frame.bytecodeOffset
        }
    }

    private fun verifyBranchTargetFrames(
        edges: Set<ControlFlowEdge>,
        frameOffsets: Set<Int>,
    ) {
        edges.asSequence()
            .filter { edge -> edge.kind == ControlFlowEdgeKind.Branch }
            .forEach { edge ->
                if (edge.targetOffset !in frameOffsets) {
                    throw MethodVerificationException(
                        "Branch target ${edge.targetOffset} has no stack map frame",
                    )
                }
            }
    }
}
