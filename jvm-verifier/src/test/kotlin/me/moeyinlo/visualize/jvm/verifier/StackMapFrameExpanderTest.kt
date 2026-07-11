package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import me.moeyinlo.visualize.jvm.classfile.AppendStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.ChopStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.FullStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.SameLocalsOneStackItemFrame
import me.moeyinlo.visualize.jvm.classfile.SameStackMapFrame
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

class StackMapFrameExpanderTest {
    @Test
    fun `expands stack map frames into verifier frame states`() {
        val initialLocals = listOf(
            VerificationType.ObjectType(ConstantPoolIndex(1)),
            VerificationType.Integer,
        )

        val states = StackMapFrameExpander.expand(
            initialLocals = initialLocals,
            frames = listOf(
                SameStackMapFrame(frameType = 0),
                SameLocalsOneStackItemFrame(
                    frameType = 65,
                    stack = ClassfileVerificationTypeInfo.Integer,
                ),
                AppendStackMapFrame(
                    frameType = 252,
                    offsetDelta = 3,
                    locals = listOf(ClassfileVerificationTypeInfo.Long),
                ),
                ChopStackMapFrame(
                    frameType = 250,
                    offsetDelta = 0,
                ),
                FullStackMapFrame(
                    offsetDelta = 2,
                    locals = listOf(ClassfileVerificationTypeInfo.Float),
                    stack = listOf(ClassfileVerificationTypeInfo.ObjectVariable(ConstantPoolIndex(2))),
                ),
            ),
        )

        assertEquals(
            listOf(
                VerificationFrameState(
                    bytecodeOffset = 0,
                    locals = initialLocals,
                    stack = emptyList(),
                ),
                VerificationFrameState(
                    bytecodeOffset = 2,
                    locals = initialLocals,
                    stack = listOf(VerificationType.Integer),
                ),
                VerificationFrameState(
                    bytecodeOffset = 6,
                    locals = initialLocals + VerificationType.Long,
                    stack = emptyList(),
                ),
                VerificationFrameState(
                    bytecodeOffset = 7,
                    locals = initialLocals,
                    stack = emptyList(),
                ),
                VerificationFrameState(
                    bytecodeOffset = 10,
                    locals = listOf(VerificationType.Float),
                    stack = listOf(VerificationType.ObjectType(ConstantPoolIndex(2))),
                ),
            ),
            states,
        )
    }

    @Test
    fun `rejects chop frames that remove more locals than the previous state has`() {
        assertFailsWith<StackMapFrameExpansionException> {
            StackMapFrameExpander.expand(
                initialLocals = emptyList(),
                frames = listOf(
                    ChopStackMapFrame(
                        frameType = 248,
                        offsetDelta = 0,
                    ),
                ),
            )
        }
    }
}
