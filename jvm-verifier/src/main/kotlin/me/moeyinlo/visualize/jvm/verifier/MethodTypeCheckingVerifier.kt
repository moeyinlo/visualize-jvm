package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex

object MethodTypeCheckingVerifier {
    fun verify(
        code: CodeAttribute,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        verify(
            code = code,
            constantPool = null,
            initialFrameState = null,
            declaredReturnType = null,
            frameStates = frameStates,
        )
    }

    fun verify(
        code: CodeAttribute,
        constantPool: ConstantPool,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        verify(
            code = code,
            constantPool = constantPool,
            initialFrameState = null,
            declaredReturnType = null,
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
            constantPool = null,
            initialFrameState = initialFrame.toVerificationFrameState(),
            declaredReturnType = initialFrame.returnType.toVerificationReturnType(),
            frameStates = frameStates,
        )
    }

    fun verify(
        code: CodeAttribute,
        constantPool: ConstantPool,
        initialFrame: MethodInitialFrame,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        verify(
            code = code,
            constantPool = constantPool,
            initialFrameState = initialFrame.toVerificationFrameState(),
            declaredReturnType = initialFrame.returnType.toVerificationReturnType(),
            frameStates = frameStates,
        )
    }

    private fun verify(
        code: CodeAttribute,
        constantPool: ConstantPool?,
        initialFrameState: VerificationFrameState?,
        declaredReturnType: VerificationReturnType?,
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
            constantPool = constantPool,
            declaredReturnType = declaredReturnType,
            framesByOffset = framesWithInitial.associateBy { frame -> frame.bytecodeOffset },
        )
    }

    private fun MethodInitialFrame.toVerificationFrameState(): VerificationFrameState =
        VerificationFrameState(
            bytecodeOffset = 0,
            locals = locals,
            stack = stack,
        )

    private fun VerificationType?.toVerificationReturnType(): VerificationReturnType =
        if (this == null) {
            VerificationReturnType.Void
        } else {
            VerificationReturnType.Value(this)
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
        constantPool: ConstantPool?,
        declaredReturnType: VerificationReturnType?,
        framesByOffset: Map<Int, VerificationFrameState>,
    ) {
        framesByOffset.forEach { (offset, frame) ->
            val opcode = code.code.u1(offset)
            when (opcode) {
                0x01 -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Null,
                    maxStack = code.maxStack,
                )
                in 0x02..0x08 -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Int,
                    maxStack = code.maxStack,
                )
                in 0x09..0x0A -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Long,
                    maxStack = code.maxStack,
                )
                in 0x0B..0x0D -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Float,
                    maxStack = code.maxStack,
                )
                in 0x0E..0x0F -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Double,
                    maxStack = code.maxStack,
                )
                0x10 -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Int,
                    maxStack = code.maxStack,
                )
                0x11 -> ConstantInstructionVerifier.verify(
                    frame = frame,
                    kind = ConstantPushKind.Int,
                    maxStack = code.maxStack,
                )
                0x12 -> LdcInstructionVerifier.verify(
                    frame = frame,
                    index = ConstantPoolIndex(code.code.u1(offset + 1)),
                    constantPool = requireConstantPool(mnemonic = "ldc", constantPool = constantPool),
                    maxStack = code.maxStack,
                )
                0x13 -> LdcInstructionVerifier.verify(
                    frame = frame,
                    index = ConstantPoolIndex(code.code.u2(offset + 1)),
                    constantPool = requireConstantPool(mnemonic = "ldc_w", constantPool = constantPool),
                    maxStack = code.maxStack,
                )
                0x14 -> LdcInstructionVerifier.verifyCategory2(
                    frame = frame,
                    index = ConstantPoolIndex(code.code.u2(offset + 1)),
                    constantPool = requireConstantPool(mnemonic = "ldc2_w", constantPool = constantPool),
                    maxStack = code.maxStack,
                )
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
                    0x84 -> WideIncrementInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        maxLocals = code.maxLocals,
                    )
                    0xA9 -> WideRetInstructionVerifier.verify(
                        frame = frame,
                        index = code.code.u2(offset + 2),
                        maxLocals = code.maxLocals,
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
                0x32 -> ReferenceArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x33 -> ByteArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x34 -> CharArrayLoadInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x35 -> ShortArrayLoadInstructionVerifier.verify(
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
                0x4F -> IntArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x50 -> LongArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x51 -> FloatArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x52 -> DoubleArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x53 -> ReferenceArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x54 -> ByteArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x55 -> CharArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x56 -> ShortArrayStoreInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x57 -> PopInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x58 -> Pop2InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x59 -> DupInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5A -> DupX1InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5B -> DupX2InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5C -> Dup2InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5D -> Dup2X1InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5E -> Dup2X2InstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x5F -> SwapInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x60 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Add,
                    maxStack = code.maxStack,
                )
                0x61 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Add,
                    maxStack = code.maxStack,
                )
                0x62 -> FloatBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = FloatBinaryArithmeticKind.Add,
                    maxStack = code.maxStack,
                )
                0x63 -> DoubleBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = DoubleBinaryArithmeticKind.Add,
                    maxStack = code.maxStack,
                )
                0x64 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Subtract,
                    maxStack = code.maxStack,
                )
                0x65 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Subtract,
                    maxStack = code.maxStack,
                )
                0x66 -> FloatBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = FloatBinaryArithmeticKind.Subtract,
                    maxStack = code.maxStack,
                )
                0x67 -> DoubleBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = DoubleBinaryArithmeticKind.Subtract,
                    maxStack = code.maxStack,
                )
                0x68 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Multiply,
                    maxStack = code.maxStack,
                )
                0x69 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Multiply,
                    maxStack = code.maxStack,
                )
                0x6A -> FloatBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = FloatBinaryArithmeticKind.Multiply,
                    maxStack = code.maxStack,
                )
                0x6B -> DoubleBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = DoubleBinaryArithmeticKind.Multiply,
                    maxStack = code.maxStack,
                )
                0x6C -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Divide,
                    maxStack = code.maxStack,
                )
                0x6D -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Divide,
                    maxStack = code.maxStack,
                )
                0x6E -> FloatBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = FloatBinaryArithmeticKind.Divide,
                    maxStack = code.maxStack,
                )
                0x6F -> DoubleBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = DoubleBinaryArithmeticKind.Divide,
                    maxStack = code.maxStack,
                )
                0x70 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Remainder,
                    maxStack = code.maxStack,
                )
                0x71 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Remainder,
                    maxStack = code.maxStack,
                )
                0x72 -> FloatBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = FloatBinaryArithmeticKind.Remainder,
                    maxStack = code.maxStack,
                )
                0x73 -> DoubleBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = DoubleBinaryArithmeticKind.Remainder,
                    maxStack = code.maxStack,
                )
                0x74 -> IntNegationInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x75 -> LongNegationInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x76 -> FloatNegationInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x77 -> DoubleNegationInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x78 -> IntShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = IntShiftKind.Left,
                    maxStack = code.maxStack,
                )
                0x79 -> LongShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = LongShiftKind.Left,
                    maxStack = code.maxStack,
                )
                0x7A -> IntShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = IntShiftKind.Right,
                    maxStack = code.maxStack,
                )
                0x7B -> LongShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = LongShiftKind.Right,
                    maxStack = code.maxStack,
                )
                0x7C -> IntShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = IntShiftKind.UnsignedRight,
                    maxStack = code.maxStack,
                )
                0x7D -> LongShiftInstructionVerifier.verify(
                    frame = frame,
                    kind = LongShiftKind.UnsignedRight,
                    maxStack = code.maxStack,
                )
                0x7E -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.And,
                    maxStack = code.maxStack,
                )
                0x7F -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.And,
                    maxStack = code.maxStack,
                )
                0x80 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Or,
                    maxStack = code.maxStack,
                )
                0x81 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Or,
                    maxStack = code.maxStack,
                )
                0x82 -> IntBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = IntBinaryArithmeticKind.Xor,
                    maxStack = code.maxStack,
                )
                0x83 -> LongBinaryArithmeticInstructionVerifier.verify(
                    frame = frame,
                    kind = LongBinaryArithmeticKind.Xor,
                    maxStack = code.maxStack,
                )
                0x84 -> IncrementInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    maxLocals = code.maxLocals,
                )
                0x85 -> IntToLongConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x86 -> IntToFloatConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x87 -> IntToDoubleConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x88 -> LongToIntConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x89 -> LongToFloatConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8A -> LongToDoubleConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8B -> FloatToIntConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8C -> FloatToLongConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8D -> FloatToDoubleConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8E -> DoubleToIntConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x8F -> DoubleToLongConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x90 -> DoubleToFloatConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x91 -> IntToByteConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x92 -> IntToCharConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x93 -> IntToShortConversionInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x94 -> LongCompareInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x95 -> FloatCompareLessInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x96 -> FloatCompareGreaterInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x97 -> DoubleCompareLessInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0x98 -> DoubleCompareGreaterInstructionVerifier.verify(
                    frame = frame,
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
                0xA8 -> JsrInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0xA9 -> RetInstructionVerifier.verify(
                    frame = frame,
                    index = code.code.u1(offset + 1),
                    maxLocals = code.maxLocals,
                )
                0xAC -> if (declaredReturnType != null) {
                    IntReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xAD -> if (declaredReturnType != null) {
                    LongReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xAE -> if (declaredReturnType != null) {
                    FloatReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xAF -> if (declaredReturnType != null) {
                    DoubleReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xB0 -> if (declaredReturnType != null) {
                    ReferenceReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xB1 -> if (declaredReturnType != null) {
                    ReturnInstructionVerifier.verify(
                        frame = frame,
                        declaredReturnType = declaredReturnType,
                        maxStack = code.maxStack,
                    )
                }
                0xBE -> ArrayLengthInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0xC2 -> MonitorInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0xC3 -> MonitorInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                in 0xC6..0xC7 -> ReferenceNullBranchInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
                0xC9 -> JsrWideInstructionVerifier.verify(
                    frame = frame,
                    maxStack = code.maxStack,
                )
            }
        }
    }

    private fun requireConstantPool(
        mnemonic: String,
        constantPool: ConstantPool?,
    ): ConstantPool =
        constantPool ?: throw MethodVerificationException("$mnemonic requires constant pool context")

    private fun ByteArray.u1(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.u2(offset: Int): Int = (u1(offset) shl 8) or u1(offset + 1)
}
