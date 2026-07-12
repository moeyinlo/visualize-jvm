package me.moeyinlo.visualize.jvm.verifier

object NewArrayInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        atype: Int,
        maxStack: Int,
    ): VerificationFrameState {
        val componentType = atype.toPrimitiveArrayComponentType()
        val withoutCount = VerifierOperandStack
            .fromFrame(stack = frame.stack, maxStack = maxStack)
            .pop(VerificationType.Integer)
        val nextStack = withoutCount.stack.push(VerificationType.ArrayOf(componentType)).values
        return frame.copy(stack = nextStack)
    }

    private fun Int.toPrimitiveArrayComponentType(): VerificationType =
        when (this) {
            4 -> VerificationType.Boolean
            5 -> VerificationType.Char
            6 -> VerificationType.Float
            7 -> VerificationType.Double
            8 -> VerificationType.Byte
            9 -> VerificationType.Short
            10 -> VerificationType.Integer
            11 -> VerificationType.Long
            else -> throw MethodVerificationException(
                "newarray atype $this is not a valid primitive array type code",
            )
        }
}
