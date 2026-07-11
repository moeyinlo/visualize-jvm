package me.moeyinlo.visualize.jvm.verifier

object ReturnInstructionVerifier {
    fun verify(
        frame: VerificationFrameState,
        declaredReturnType: VerificationReturnType,
        maxStack: Int,
    ) {
        VerifierOperandStack.fromFrame(stack = frame.stack, maxStack = maxStack)
        if (declaredReturnType != VerificationReturnType.Void) {
            throw MethodVerificationException("Method return type is $declaredReturnType, expected void")
        }
        UninitializedThisRules.requireInitializedThisForReturn(frame)
    }
}

sealed interface VerificationReturnType {
    data object Void : VerificationReturnType {
        override fun toString(): String = "void"
    }

    data class Value(val type: VerificationType) : VerificationReturnType {
        override fun toString(): String = type.toString()
    }
}
