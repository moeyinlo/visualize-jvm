package me.moeyinlo.visualize.jvm.verifier

object MultiANewArrayInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        arrayType: VerificationType,
        dimensions: Int,
        maxStack: Int,
    ): VerificationFrameState {
        if (dimensions < 1) {
            throw MethodVerificationException("multianewarray dimensions $dimensions must be at least 1")
        }
        val arrayDimensionality = arrayType.arrayDimensionality()
        if (arrayDimensionality == 0) {
            throw MethodVerificationException("multianewarray target $arrayType is not an array type")
        }
        if (dimensions > arrayDimensionality) {
            throw MethodVerificationException(
                "multianewarray dimensions $dimensions exceed target array dimensionality $arrayDimensionality",
            )
        }

        var stack = VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        repeat(dimensions) {
            stack = stack.pop(VerificationType.Integer).stack
        }
        return frame.copy(stack = stack.push(arrayType).values)
    }

    private fun VerificationType.arrayDimensionality(): Int {
        var dimensionality = 0
        var current = this
        while (current is VerificationType.ArrayOf) {
            dimensionality += 1
            current = current.component
        }
        return dimensionality
    }
}
