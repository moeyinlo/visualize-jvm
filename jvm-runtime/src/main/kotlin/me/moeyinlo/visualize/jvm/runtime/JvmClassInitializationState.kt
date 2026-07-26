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

    fun get(className: String): JvmClassInitializationState {
        require(className.isNotBlank()) { "class name must not be blank" }
        return statesByClassName[className] ?: JvmClassInitializationState.Prepared
    }

    fun startInitialization(className: String, threadId: String) {
        require(className.isNotBlank()) { "class name must not be blank" }
        require(threadId.isNotBlank()) { "thread id must not be blank" }
        statesByClassName[className] = JvmClassInitializationState.Initializing(threadId)
    }

    fun completeInitialization(className: String, threadId: String) {
        requireInitializationOwner(className, threadId)
        statesByClassName[className] = JvmClassInitializationState.Initialized
    }

    fun failInitialization(className: String, threadId: String, errorClassName: String) {
        require(errorClassName.isNotBlank()) { "error class name must not be blank" }
        requireInitializationOwner(className, threadId)
        statesByClassName[className] = JvmClassInitializationState.Erroneous(errorClassName)
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
