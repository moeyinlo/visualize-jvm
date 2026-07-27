package me.moeyinlo.visualize.jvm.runtime

class JvmVmTerminationState {
    var result: JvmVmTerminationResult? = null
        private set

    val isTerminated: Boolean
        get() = result != null

    fun terminateNormally(exitCode: Int): JvmVmTerminationResult =
        terminate(JvmVmTerminationResult.Normal(exitCode))

    fun terminateAbruptly(throwable: JvmObjectReferenceValue): JvmVmTerminationResult =
        terminate(JvmVmTerminationResult.UncaughtGuestException(throwable))

    private fun terminate(nextResult: JvmVmTerminationResult): JvmVmTerminationResult {
        val previousResult = result
        if (previousResult != null) {
            throw JvmVmTerminationException("VM already terminated with $previousResult")
        }
        result = nextResult
        return nextResult
    }
}

sealed interface JvmVmTerminationResult {
    data class Normal(val exitCode: Int) : JvmVmTerminationResult

    data class UncaughtGuestException(val throwable: JvmObjectReferenceValue) : JvmVmTerminationResult
}

class JvmVmTerminationException(message: String) : IllegalStateException(message)
