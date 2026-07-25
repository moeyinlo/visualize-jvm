package me.moeyinlo.visualize.jvm.runtime

enum class JvmClassExecutionMode {
    Interpreted,
    HostDelegated,
}

class JvmClassExecutionPolicy(
    val hostDelegatedClassNames: Set<String> = emptySet(),
    val hostDelegatedPackagePrefixes: Set<String> = emptySet(),
    val interpretedClassNames: Set<String> = emptySet(),
) {
    init {
        hostDelegatedClassNames.forEach { className ->
            require(className.isNotBlank()) { "host delegated class name must not be blank" }
        }
        hostDelegatedPackagePrefixes.forEach { packagePrefix ->
            require(packagePrefix.isNotBlank()) { "host delegated package prefix must not be blank" }
        }
        interpretedClassNames.forEach { className ->
            require(className.isNotBlank()) { "interpreted class name must not be blank" }
        }
    }

    fun modeFor(internalClassName: String): JvmClassExecutionMode {
        require(internalClassName.isNotBlank()) { "class internal name must not be blank" }
        if (internalClassName in interpretedClassNames) {
            return JvmClassExecutionMode.Interpreted
        }
        if (internalClassName in hostDelegatedClassNames) {
            return JvmClassExecutionMode.HostDelegated
        }
        if (hostDelegatedPackagePrefixes.any { packagePrefix -> internalClassName.startsWith(packagePrefix) }) {
            return JvmClassExecutionMode.HostDelegated
        }
        return JvmClassExecutionMode.Interpreted
    }

    companion object {
        val StandardPlatformPackagePrefixes: Set<String> = setOf(
            "java/",
            "javax/",
            "jdk/",
            "sun/",
            "com/sun/",
        )

        val Default: JvmClassExecutionPolicy = JvmClassExecutionPolicy(
            hostDelegatedPackagePrefixes = StandardPlatformPackagePrefixes,
        )
    }
}
