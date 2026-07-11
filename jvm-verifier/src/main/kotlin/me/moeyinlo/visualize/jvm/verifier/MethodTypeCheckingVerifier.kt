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
            val opcode = code.code.u1(offset)
            when (opcode) {
                0x15 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalLoadKind.Int,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x16 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalLoadKind.Long,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x17 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalLoadKind.Float,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x18 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalLoadKind.Double,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x19 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalLoadKind.Reference,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0xC4 -> when (code.code.u1(offset + 1)) {
                    0x15 -> WideLocalLoadInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalLoadKind.Int,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x16 -> WideLocalLoadInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalLoadKind.Long,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x17 -> WideLocalLoadInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalLoadKind.Float,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x18 -> WideLocalLoadInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalLoadKind.Double,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x19 -> WideLocalLoadInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalLoadKind.Reference,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x36 -> WideLocalStoreInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalStoreKind.Int,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x37 -> WideLocalStoreInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalStoreKind.Long,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x38 -> WideLocalStoreInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalStoreKind.Float,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x39 -> WideLocalStoreInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalStoreKind.Double,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                    0x3A -> WideLocalStoreInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        kind = LocalStoreKind.Reference,
                        maxLocals = code.maxLocals,
                        maxStack = code.maxStack,
                    )
                }
                in 0x1A..0x1D -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x1A,
                    kind = LocalLoadKind.Int,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x1E..0x21 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x1E,
                    kind = LocalLoadKind.Long,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x22..0x25 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x22,
                    kind = LocalLoadKind.Float,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x26..0x29 -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x26,
                    kind = LocalLoadKind.Double,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x2A..0x2D -> LocalLoadInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x2A,
                    kind = LocalLoadKind.Reference,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x2E -> IntArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x2F -> LongArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x30 -> FloatArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x31 -> DoubleArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x36 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalStoreKind.Int,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x37 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalStoreKind.Long,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x38 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalStoreKind.Float,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x39 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalStoreKind.Double,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                0x3A -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    kind = LocalStoreKind.Reference,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x3B..0x3E -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x3B,
                    kind = LocalStoreKind.Int,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x3F..0x42 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x3F,
                    kind = LocalStoreKind.Long,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x43..0x46 -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x43,
                    kind = LocalStoreKind.Float,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x47..0x4A -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x47,
                    kind = LocalStoreKind.Double,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x4B..0x4E -> LocalStoreInstructionVerifier.verify(
                    frame = frame,
                    index = opcode - 0x4B,
                    kind = LocalStoreKind.Reference,
                    maxLocals = code.maxLocals,
                    maxStack = code.maxStack,
                )
                in 0x99..0x9E -> IntZeroBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                in 0x9F..0xA4 -> IntCompareBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                in 0xA5..0xA6 -> ReferenceCompareBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                in 0xC6..0xC7 -> ReferenceNullBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
            }
        }
    }

    private fun ByteArray.u1(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.u2(offset: Int): Int = (u1(offset) shl 8) or u1(offset + 1)
}
