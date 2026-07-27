package me.moeyinlo.visualize.jvm.runtime

sealed interface JvmClassInitializationState {
    data object Prepared : JvmClassInitializationState

    data class Initializing(val threadId: String) : JvmClassInitializationState {
        init {
            require(threadId.isNotBlank()) { "initializing thread id must not be blank" }
        }
    }

    data object Initialized : JvmClassInitializationState

    data class Erroneous(val errorClassName: String) : JvmClassInitializationState {
        init {
            require(errorClassName.isNotBlank()) { "initialization error class name must not be blank" }
        }
    }
}

class JvmClassInitializationStates {
    private val statesByClassName = linkedMapOf<String, JvmClassInitializationState>()
    private val waitingThreadIdsByClassName = linkedMapOf<String, LinkedHashSet<String>>()

    fun get(className: String): JvmClassInitializationState {
        require(className.isNotBlank()) { "class name must not be blank" }
        return statesByClassName[className] ?: JvmClassInitializationState.Prepared
    }

    fun startInitialization(className: String, threadId: String) {
        require(className.isNotBlank()) { "class name must not be blank" }
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        statesByClassName[className] = JvmClassInitializationState.Initializing(threadId)
    }

    fun completeInitialization(className: String, threadId: String): List<String> {
        requireInitializationOwner(className, threadId)
        statesByClassName[className] = JvmClassInitializationState.Initialized
        return releaseWaitingThreads(className)
    }

    fun failInitialization(className: String, threadId: String, errorClassName: String): List<String> {
        require(errorClassName.isNotBlank()) { "error class name must not be blank" }
        requireInitializationOwner(className, threadId)
        statesByClassName[className] = JvmClassInitializationState.Erroneous(errorClassName)
        return releaseWaitingThreads(className)
    }

    fun recordInitializationWaiter(className: String, threadId: String) {
        require(className.isNotBlank()) { "class name must not be blank" }
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        val waitingThreadIds = waitingThreadIdsByClassName.getOrPut(className) { linkedSetOf() }
        waitingThreadIds.add(threadId)
    }

    fun waitingThreads(className: String): List<String> {
        require(className.isNotBlank()) { "class name must not be blank" }
        return waitingThreadIdsByClassName[className]?.toList().orEmpty()
    }

    private fun releaseWaitingThreads(className: String): List<String> {
        val waitingThreadIds = waitingThreadIdsByClassName.remove(className)
        return waitingThreadIds?.toList().orEmpty()
    }

    private fun requireInitializationOwner(className: String, threadId: String) {
        require(className.isNotBlank()) { "class name must not be blank" }
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        when (val state = get(className)) {
            is JvmClassInitializationState.Initializing -> {
                if (state.threadId != threadId) {
                    throw JvmClassInitializationStateException(
                        "Class $className initialization is owned by thread ${state.threadId}, not $threadId",
                    )
                }
            }
            else -> throw JvmClassInitializationStateException(
                "Class $className is not being initialized by thread $threadId",
            )
        }
    }
}

class JvmClassInitializationStateException(message: String) : IllegalStateException(message)
