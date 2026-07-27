package me.moeyinlo.visualize.jvm.runtime

fun interface JvmHostActiveUseHandler {
    fun handleActiveUse(
        className: String,
        classInitializationStates: JvmClassInitializationStates,
    ): Boolean

    companion object {
        val None: JvmHostActiveUseHandler = JvmHostActiveUseHandler { className, _ ->
            require(className.isNotBlank()) { "class name must not be blank" }
            false
        }
    }
}
