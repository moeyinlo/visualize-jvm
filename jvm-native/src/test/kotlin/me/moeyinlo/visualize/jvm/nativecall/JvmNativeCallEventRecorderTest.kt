package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmNativeCallEventRecorderTest {
    @Test
    fun `native call recorder emits sequenced enter return and throw snapshots`() {
        val recorder = JvmNativeCallEventRecorder()
        val frame = JvmNativeMethodFrame(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
            entryPoint = "pkg.NativeApi.call",
            environment = JvmNativeExecutionEnvironment.VmIntrinsic,
        )

        recorder.record(JvmNativeCallAction.Entered, depth = 1, frame = frame, detail = "args=1")
        recorder.record(JvmNativeCallAction.Returned, depth = 1, frame = frame, detail = "return=long")
        recorder.record(JvmNativeCallAction.Threw, depth = 1, frame = frame, detail = "guest=java/lang/Throwable")

        assertEquals(
            listOf(
                JvmNativeCallEventSnapshot(
                    sequence = 1,
                    depth = 1,
                    action = JvmNativeCallAction.Entered,
                    signature = frame.signature,
                    environment = JvmNativeExecutionEnvironment.VmIntrinsic,
                    bindingName = "pkg.NativeApi.call",
                    detail = "args=1",
                ),
                JvmNativeCallEventSnapshot(
                    sequence = 2,
                    depth = 1,
                    action = JvmNativeCallAction.Returned,
                    signature = frame.signature,
                    environment = JvmNativeExecutionEnvironment.VmIntrinsic,
                    bindingName = "pkg.NativeApi.call",
                    detail = "return=long",
                ),
                JvmNativeCallEventSnapshot(
                    sequence = 3,
                    depth = 1,
                    action = JvmNativeCallAction.Threw,
                    signature = frame.signature,
                    environment = JvmNativeExecutionEnvironment.VmIntrinsic,
                    bindingName = "pkg.NativeApi.call",
                    detail = "guest=java/lang/Throwable",
                ),
            ),
            recorder.snapshots(),
        )
    }

    @Test
    fun `native call recorder preserves fallback events without host object details`() {
        val recorder = JvmNativeCallEventRecorder()
        val frame = JvmNativeMethodFrame(
            ownerClassName = "pkg/NativeApi",
            methodName = "jniCall",
            methodDescriptor = "()V",
            isStatic = false,
            entryPoint = "Java_pkg_NativeApi_jniCall",
            environment = JvmNativeExecutionEnvironment.SimulatedJni,
        )

        recorder.record(
            action = JvmNativeCallAction.FellBackToSimulatedJni,
            depth = 2,
            frame = frame,
            detail = "intrinsic miss",
        )

        assertEquals(
            listOf(
                JvmNativeCallEventSnapshot(
                    sequence = 1,
                    depth = 2,
                    action = JvmNativeCallAction.FellBackToSimulatedJni,
                    signature = frame.signature,
                    environment = JvmNativeExecutionEnvironment.SimulatedJni,
                    bindingName = "Java_pkg_NativeApi_jniCall",
                    detail = "intrinsic miss",
                ),
            ),
            recorder.snapshots(),
        )
    }
}