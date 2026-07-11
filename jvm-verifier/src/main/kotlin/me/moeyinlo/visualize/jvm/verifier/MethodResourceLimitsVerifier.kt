package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute

class MethodVerificationException(message: String) : RuntimeException(message)

object MethodResourceLimitsVerifier {
    fun verify(
        code: CodeAttribute,
        frameStates: Iterable<VerificationFrameState>,
    ) {
        frameStates.forEach { frame ->
            val localUnits = frame.locals.locationUnits()
            if (localUnits > code.maxLocals) {
                throw MethodVerificationException(
                    "Frame at bytecode offset ${frame.bytecodeOffset} uses $localUnits local variable unit(s), " +
                        "exceeding max_locals=${code.maxLocals}",
                )
            }

            val stackUnits = frame.stack.locationUnits()
            if (stackUnits > code.maxStack) {
                throw MethodVerificationException(
                    "Frame at bytecode offset ${frame.bytecodeOffset} uses $stackUnits operand stack unit(s), " +
                        "exceeding max_stack=${code.maxStack}",
                )
            }
        }
    }

    private fun List<VerificationType>.locationUnits(): Int =
        sumOf { type -> type.locationCount }
}
