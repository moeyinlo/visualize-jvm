package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute

object MethodTypeCheckingVerifier {
    fun verify(
        code: CodeAttribute,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        verify(
            code = code,
            initialFrameState = null,
            frameStates = frameStates,
        )
    }

    fun verify(
        code: CodeAttribute,
        initialFrame: MethodInitialFrame,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        verify(
            code = code,
            initialFrameState = initialFrame.toVerificationFrameState(),
            frameStates = frameStates,
        )
    }

    private fun verify(
        code: CodeAttribute,
        initialFrameState: VerificationFrameState?,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        val frames = frameStates.toList()
        val framesWithInitial = listOfNotNull(initialFrameState) + frames
        MethodResourceLimitsVerifier.verify(code = code, frameStates = framesWithInitial)
        val controlFlowGraph = MethodControlFlowGraphBuilder.build(code)
        verifyFrameOffsets(frames = frames, instructionOffsets = controlFlowGraph.instructionOffsets)
        val frameOffsets = frames.mapTo(hashSetOf()) { it.bytecodeOffset }
        if (initialFrameState != null) {
            frameOffsets += initialFrameState.bytecodeOffset
        }
        verifyBranchTargetFrames(controlFlowGraph.edges, frameOffsets = frameOffsets)
        verifyExceptionHandlerTargetFrames(controlFlowGraph.edges, frameOffsets = frameOffsets)
        verifyInstructionTransfers(
            code = code,
            framesByOffset = framesWithInitial.associateBy { frame -> frame.bytecodeOffset },
        )
    }

    private fun MethodInitialFrame.toVerificationFrameState(): VerificationFrameState =
        VerificationFrameState(
            bytecodeOffset = 0,
            locals = locals,
            stack = stack,
        )

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

    private fun verifyExceptionHandlerTargetFrames(
        edges: Set<ControlFlowEdge>,
        frameOffsets: Set<Int>,
    ) {
        edges.asSequence()
            .filter { edge -> edge.kind == ControlFlowEdgeKind.ExceptionHandler }
            .forEach { edge ->
                if (edge.targetOffset !in frameOffsets) {
                    throw MethodVerificationException(
                        "Exception handler target ${edge.targetOffset} has no stack map frame",
                    )
                }
            }
    }

    private fun verifyInstructionTransfers(
        code: CodeAttribute,
        framesByOffset: Map<Int, VerificationFrameState>,
    ) {
        framesByOffset.forEach { (offset, frame) ->
            when (code.code.u1(offset)) {
                in 0x99..0x9E -> IntZeroBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
            }
        }
    }

    private fun ByteArray.u1(offset: Int): Int = this[offset].toInt() and 0xFF
}
