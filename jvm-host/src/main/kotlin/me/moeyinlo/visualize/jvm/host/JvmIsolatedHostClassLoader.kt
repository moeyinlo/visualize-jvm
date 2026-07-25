package me.moeyinlo.visualize.jvm.host

import java.net.URL
import java.net.URLClassLoader

class JvmIsolatedHostClassLoader private constructor(
    private val classLoader: ClassLoader,
) : AutoCloseable {
    constructor(
        classpathUrls: List<URL> = emptyList(),
        parent: ClassLoader? = ClassLoader.getPlatformClassLoader(),
    ) : this(URLClassLoader(classpathUrls.toTypedArray(), parent))

    fun loadClassMirror(guestInternalName: String): JvmHostDelegatedClassMirror {
        require(guestInternalName.isNotBlank()) { "guest internal name must not be blank" }
        val hostBinaryName = guestInternalName.replace('/', '.')
        val hostClass = try {
            Class.forName(hostBinaryName, false, classLoader)
        } catch (exception: ClassNotFoundException) {
            throw JvmHostClassLoadingException(
                "Host class $hostBinaryName for $guestInternalName was not found",
                exception,
            )
        }
        return JvmHostDelegatedClassMirror(
            guestInternalName = guestInternalName,
            hostClass = hostClass,
        )
    }

    override fun close() {
        (classLoader as? URLClassLoader)?.close()
    }
}

class JvmHostClassLoadingException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
