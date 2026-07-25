package me.moeyinlo.visualize.jvm.host

import me.moeyinlo.visualize.jvm.runtime.JvmClassExecutionMode

data class JvmHostDelegatedClassMirror(
    val guestInternalName: String,
    val hostClass: Class<*>,
) {
    init {
        require(guestInternalName.isNotBlank()) { "guest internal name must not be blank" }
        val expectedInternalName = hostClass.guestInternalName()
        if (guestInternalName != expectedInternalName) {
            throw JvmHostMirrorMismatchException(
                "Host class ${hostClass.name} does not match guest internal name $guestInternalName",
            )
        }
    }

    val hostBinaryName: String = hostClass.name

    val executionMode: JvmClassExecutionMode = JvmClassExecutionMode.HostDelegated

    companion object {
        fun fromHostClass(hostClass: Class<*>): JvmHostDelegatedClassMirror =
            JvmHostDelegatedClassMirror(
                guestInternalName = hostClass.guestInternalName(),
                hostClass = hostClass,
            )
    }
}

class JvmHostMirrorMismatchException(message: String) : IllegalArgumentException(message)

private fun Class<*>.guestInternalName(): String =
    name.replace('.', '/')
