package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.AppendStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.ChopStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.FullStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.SameLocalsOneStackItemFrame
import me.moeyinlo.visualize.jvm.classfile.SameLocalsOneStackItemFrameExtended
import me.moeyinlo.visualize.jvm.classfile.SameStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.SameStackMapFrameExtended
import me.moeyinlo.visualize.jvm.classfile.StackMapFrame
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

data class VerificationFrameState(
    val bytecodeOffset: Int,
    val locals: List<VerificationType>,
    val stack: List<VerificationType>,
)

class StackMapFrameExpansionException(message: String) : RuntimeException(message)

object StackMapFrameExpander {
    fun expand(
        initialLocals: List<VerificationType>,
        frames: List<StackMapFrame>,
    ): List<VerificationFrameState> {
        var previousOffset: Int? = null
        var locals = initialLocals.toList()
        return frames.map { frame ->
            val bytecodeOffset = bytecodeOffset(previousOffset, frame.offsetDelta)
            val stack: List<VerificationType>
            when (frame) {
                is SameStackMapFrame,
                is SameStackMapFrameExtended,
                -> stack = emptyList()
                is SameLocalsOneStackItemFrame -> stack = listOf(frame.stack.toVerifierType())
                is SameLocalsOneStackItemFrameExtended -> stack = listOf(frame.stack.toVerifierType())
                is ChopStackMapFrame -> {
                    locals = chopLocals(locals, frame)
                    stack = emptyList()
                }
                is AppendStackMapFrame -> {
                    locals = locals + frame.locals.map { it.toVerifierType() }
                    stack = emptyList()
                }
                is FullStackMapFrame -> {
                    locals = frame.locals.map { it.toVerifierType() }
                    stack = frame.stack.map { it.toVerifierType() }
                }
            }
            previousOffset = bytecodeOffset
            VerificationFrameState(
                bytecodeOffset = bytecodeOffset,
                locals = locals,
                stack = stack,
            )
        }
    }

    private fun bytecodeOffset(previousOffset: Int?, offsetDelta: Int): Int =
        if (previousOffset == null) offsetDelta else previousOffset + offsetDelta + 1

    private fun chopLocals(
        locals: List<VerificationType>,
        frame: ChopStackMapFrame,
    ): List<VerificationType> {
        val choppedLocals = frame.choppedLocals
        if (choppedLocals > locals.size) {
            throw StackMapFrameExpansionException(
                "chop_frame removes $choppedLocals local(s) from ${locals.size} previous local(s)",
            )
        }
        return locals.dropLast(choppedLocals)
    }

    private fun ClassfileVerificationTypeInfo.toVerifierType(): VerificationType =
        VerificationType.fromClassfile(this)
}
