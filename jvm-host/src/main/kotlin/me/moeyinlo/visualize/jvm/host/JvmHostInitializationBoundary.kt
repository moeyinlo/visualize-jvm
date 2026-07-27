package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionMode
import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionPolicy
import me.moeyinlo.visualize.jvm.runtime.JvmClassInitializationStates
import me.moeyinlo.visualize.jvm.runtime.JvmHostActiveUseHandler

object JvmHostInitializationBoundary {
    fun asActiveUseHandler(
        executionPolicy: JvmClassExecutionPolicy,
        boundaryEvents: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink.None,
    ): JvmHostActiveUseHandler = JvmHostActiveUseHandler { className, classInitializationStates ->
        recordActiveUse(
            className = className,
            executionPolicy = executionPolicy,
            classInitializationStates = classInitializationStates,
            boundaryEvents = boundaryEvents,
        )
    }

    fun recordActiveUse(
        className: String,
        executionPolicy: JvmClassExecutionPolicy,
        classInitializationStates: JvmClassInitializationStates,
        boundaryEvents: JvmHostBoundaryEventSink = JvmHostBoundaryEventSink.None,
    ): Boolean {
        require(className.isNotBlank()) { "class name must not be blank" }
        if (executionPolicy.modeFor(className) != JvmClassExecutionMode.HostDelegated) {
            return false
        }
        val stateBefore = classInitializationStates.get(className)
        boundaryEvents.record(
            action = JvmHostBoundaryAction.Delegated,
            className = className,
            methodName = "<clinit>",
            descriptor = "()V",
            detail = "host-delegated initialization is opaque to guest state",
        )
        check(classInitializationStates.get(className) == stateBefore) {
            "Host-delegated initialization boundary must not mutate guest initialization state for $className"
        }
        return true
    }
}
