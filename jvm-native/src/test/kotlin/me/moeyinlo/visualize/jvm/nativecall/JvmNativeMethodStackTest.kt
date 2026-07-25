package me.moeyinlo.visualize.jvm.nativecall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class JvmNativeMethodStackTest {
    @Test
    fun `native method stack pushes frames and exposes the current frame`() {
        val stack = JvmNativeMethodStack()
        val first = nativeFrame(methodName = "first")
        val second = nativeFrame(methodName = "second", environment = JvmNativeExecutionEnvironment.HostDowncall)

        stack.push(first)
        stack.push(second)

        assertEquals(2, stack.depth)
        assertSame(second, stack.currentFrame())
        assertEquals(listOf(first, second), stack.toList())
    }

    @Test
    fun `native method stack pops frames in last in first out order`() {
        val stack = JvmNativeMethodStack()
        val first = nativeFrame(methodName = "first")
        val second = nativeFrame(methodName = "second")
        stack.push(first)
        stack.push(second)

        assertSame(second, stack.pop())
        assertSame(first, stack.pop())
        assertEquals(0, stack.depth)
    }

    @Test
    fun `native method stack rejects negative maximum frame count`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            JvmNativeMethodStack(maxFrames = -1)
        }

        assertEquals("max native frame count must be non-negative: -1", exception.message)
    }

    @Test
    fun `fixed size native method stack rejects overflow`() {
        val stack = JvmNativeMethodStack(maxFrames = 1)
        stack.push(nativeFrame(methodName = "only"))

        val exception = assertFailsWith<JvmNativeStackOverflowException> {
            stack.push(nativeFrame(methodName = "overflow"))
        }

        assertEquals("Native method stack depth 2 exceeds max_native_frames=1", exception.message)
    }

    @Test
    fun `empty native method stack rejects pop and current frame access`() {
        val stack = JvmNativeMethodStack()

        assertFailsWith<JvmNativeStackUnderflowException> { stack.pop() }
        assertFailsWith<JvmNativeStackUnderflowException> { stack.currentFrame() }
    }

    @Test
    fun `native method frames validate guest method and host entry metadata`() {
        val frame = nativeFrame(
            libraryName = "native-api",
            entryPoint = "Java_pkg_NativeApi_call",
            environment = JvmNativeExecutionEnvironment.SimulatedJni,
        )

        assertEquals("pkg/NativeApi", frame.ownerClassName)
        assertEquals("native-api", frame.libraryName)
        assertEquals("Java_pkg_NativeApi_call", frame.entryPoint)
        assertEquals(JvmNativeExecutionEnvironment.SimulatedJni, frame.environment)
        assertFailsWith<IllegalArgumentException> { nativeFrame(ownerClassName = "") }
        assertFailsWith<IllegalArgumentException> { nativeFrame(methodName = "") }
        assertFailsWith<IllegalArgumentException> { nativeFrame(methodDescriptor = "") }
        assertFailsWith<IllegalArgumentException> { nativeFrame(libraryName = "") }
        assertFailsWith<IllegalArgumentException> { nativeFrame(entryPoint = "") }
    }


    @Test
    fun `native method frames can be derived from resolved native bindings`() {
        val signature = JvmNativeMethodSignature(
            ownerClassName = "pkg/NativeApi",
            methodName = "call",
            methodDescriptor = "(I)J",
            isStatic = true,
        )
        val binding = JvmNativeMethodBinding(
            signature = signature,
            environment = JvmNativeExecutionEnvironment.VmIntrinsic,
            bindingName = "pkg.NativeApi.call",
        )

        val frame = JvmNativeMethodFrame.fromBinding(binding)

        assertEquals(signature, frame.signature)
        assertEquals("pkg/NativeApi", frame.ownerClassName)
        assertEquals("call", frame.methodName)
        assertEquals("(I)J", frame.methodDescriptor)
        assertEquals(true, frame.isStatic)
        assertEquals(JvmNativeExecutionEnvironment.VmIntrinsic, frame.environment)
        assertEquals("pkg.NativeApi.call", frame.entryPoint)
    }

    private fun nativeFrame(
        ownerClassName: String = "pkg/NativeApi",
        methodName: String = "call",
        methodDescriptor: String = "()V",
        isStatic: Boolean = true,
        libraryName: String? = null,
        entryPoint: String? = null,
        environment: JvmNativeExecutionEnvironment = JvmNativeExecutionEnvironment.SimulatedJni,
    ): JvmNativeMethodFrame = JvmNativeMethodFrame(
        ownerClassName = ownerClassName,
        methodName = methodName,
        methodDescriptor = methodDescriptor,
        isStatic = isStatic,
        libraryName = libraryName,
        entryPoint = entryPoint,
        environment = environment,
    )
}
