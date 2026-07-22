package me.moeyinlo.visualize.jvm.runtime

object JvmExceptionHandlerTable {
    fun findHandler(
        handlers: List<JvmExceptionHandler>,
        thrownAtPc: Int,
        throwableClassName: String,
        classHierarchy: JvmClassHierarchy,
    ): JvmExceptionHandler? {
        require(thrownAtPc >= 0) { "throw bytecode offset must be non-negative: $thrownAtPc" }
        require(throwableClassName.isNotBlank()) { "throwable class name must not be blank" }

        return handlers.firstOrNull { handler ->
            thrownAtPc in handler.startPc until handler.endPc &&
                (handler.catchClassName == null || classHierarchy.isAssignable(throwableClassName, handler.catchClassName))
        }
    }
}
