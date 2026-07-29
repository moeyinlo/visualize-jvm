package me.moeyinlo.visualize.jvm.runtime

data class JvmRuntimePackageKey(
    val packageName: String,
    val definingLoader: JvmClassLoaderIdentity,
)

fun JvmClassDefinition.runtimePackageName(): String? = internalName.runtimePackageNameOrNull()

fun JvmLoadedClassKey.runtimePackageKey(): JvmRuntimePackageKey? =
    internalName.runtimePackageNameOrNull()?.let { packageName ->
        JvmRuntimePackageKey(
            packageName = packageName,
            definingLoader = definingLoader,
        )
    }

private fun String.runtimePackageNameOrNull(): String? {
    require(isNotBlank()) { "class internal name must not be blank" }
    if (startsWith("[")) {
        return null
    }
    val separatorIndex = lastIndexOf('/')
    return if (separatorIndex < 0) "" else substring(0, separatorIndex)
}
