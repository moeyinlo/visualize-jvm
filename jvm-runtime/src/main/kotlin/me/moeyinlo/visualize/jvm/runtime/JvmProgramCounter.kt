package me.moeyinlo.visualize.jvm.runtime

sealed interface JvmProgramCounter {
    data class BytecodeOffset(val offset: Int) : JvmProgramCounter {
        init {
            require(offset >= 0) { "program counter offset must be non-negative: $offset" }
        }
    }

    data object UndefinedForNativeMethod : JvmProgramCounter
}
