package me.moeyinlo.visualize.jvm.runtime

class JvmExecutionFrame private constructor(
    val method: JvmResolvedMethod,
    val localVariables: JvmLocalVariables,
    val operandStack: JvmOperandStack,
    val runtimeConstantPool: JvmRuntimeConstantPool,
    initialProgramCounter: JvmProgramCounter,
) : JvmFrame {
    var programCounter: JvmProgramCounter = initialProgramCounter
        private set

    fun moveToBytecodeOffset(offset: Int) {
        if (method.isNative) {
            throw JvmFrameStateException(
                "Native method ${method.ownerClassName}.${method.name}${method.descriptor} has an undefined pc register",
            )
        }
        programCounter = JvmProgramCounter.BytecodeOffset(offset)
    }

    fun completeNormally(returnValue: JvmValue? = null): JvmMethodCompletion.Normal =
        method.normalCompletion(returnValue)

    companion object {
        fun create(
            method: JvmResolvedMethod,
            runtimeConstantPool: JvmRuntimeConstantPool,
        ): JvmExecutionFrame = JvmExecutionFrame(
            method = method,
            localVariables = JvmLocalVariables(method.maxLocals),
            operandStack = JvmOperandStack(method.maxStack),
            runtimeConstantPool = runtimeConstantPool,
            initialProgramCounter = if (method.isNative) {
                JvmProgramCounter.UndefinedForNativeMethod
            } else {
                JvmProgramCounter.BytecodeOffset(0)
            },
        )
    }
}

class JvmFrameStateException(message: String) : IllegalStateException(message)
