package me.moeyinlo.visualize.jvm.runtime

sealed interface JvmClassLoaderIdentity {
    val displayName: String
    val diagnosticName: String

    data object Bootstrap : JvmClassLoaderIdentity {
        override val displayName: String = "<bootstrap>"
        override val diagnosticName: String = "bootstrap"
    }

    data class UserDefined(
        val id: Long,
        override val displayName: String,
    ) : JvmClassLoaderIdentity {
        init {
            require(id > 0) { "user-defined class loader id must be positive: $id" }
            require(displayName.isNotBlank()) { "user-defined class loader display name must not be blank" }
        }

        override val diagnosticName: String = "${displayName.trim()}#$id"
    }
}

data class JvmLoadedClassKey(
    val internalName: String,
    val definingLoader: JvmClassLoaderIdentity,
) {
    init {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
    }

    val diagnosticName: String = "$internalName @ ${definingLoader.diagnosticName}"
}
