package me.moeyinlo.visualize.jvm.runtime

class JvmLoadingConstraintSet {
    private val loaderGroupsByClassName = linkedMapOf<String, MutableList<MutableSet<JvmClassLoaderIdentity>>>()
    private val resolutions = linkedMapOf<JvmLoadingConstraintResolutionKey, JvmLoadedClassKey>()

    fun addConstraint(
        internalName: String,
        firstLoader: JvmClassLoaderIdentity,
        secondLoader: JvmClassLoaderIdentity,
    ) {
        require(internalName.isNotBlank()) { "loading constraint class name must not be blank" }
        val groups = loaderGroupsByClassName.getOrPut(internalName) { mutableListOf() }
        val matchingGroups = groups.filter { group -> firstLoader in group || secondLoader in group }

        val merged = when (matchingGroups.size) {
            0 -> mutableSetOf(firstLoader, secondLoader)
            1 -> matchingGroups.single().also { group ->
                group += firstLoader
                group += secondLoader
            }
            else -> {
                val first = matchingGroups.first()
                matchingGroups.drop(1).forEach { group ->
                    first += group
                    groups -= group
                }
                first += firstLoader
                first += secondLoader
                first
            }
        }
        if (matchingGroups.isEmpty()) {
            groups += merged
        }
        checkResolvedClasses(internalName, merged)
    }

    fun constrainedLoaders(
        internalName: String,
        initiatingLoader: JvmClassLoaderIdentity,
    ): Set<JvmClassLoaderIdentity> {
        require(internalName.isNotBlank()) { "loading constraint class name must not be blank" }
        return loaderGroupsByClassName[internalName]
            ?.firstOrNull { group -> initiatingLoader in group }
            ?.toSet()
            ?: setOf(initiatingLoader)
    }

    fun recordResolution(
        internalName: String,
        initiatingLoader: JvmClassLoaderIdentity,
        resolvedClass: JvmLoadedClassKey,
    ) {
        require(internalName.isNotBlank()) { "loading constraint class name must not be blank" }
        require(resolvedClass.internalName == internalName) {
            "resolved class ${resolvedClass.internalName} does not match loading constraint name $internalName"
        }
        val constrained = constrainedLoaders(internalName, initiatingLoader)
        checkCompatibleResolution(internalName, constrained, resolvedClass)
        constrained.forEach { loader ->
            resolutions[JvmLoadingConstraintResolutionKey(internalName, loader)] = resolvedClass
        }
    }

    fun resolvedClass(
        internalName: String,
        initiatingLoader: JvmClassLoaderIdentity,
    ): JvmLoadedClassKey? {
        require(internalName.isNotBlank()) { "loading constraint class name must not be blank" }
        return resolutions[JvmLoadingConstraintResolutionKey(internalName, initiatingLoader)]
    }

    private fun checkResolvedClasses(
        internalName: String,
        loaders: Set<JvmClassLoaderIdentity>,
    ) {
        val resolved = loaders.mapNotNull { loader ->
            resolutions[JvmLoadingConstraintResolutionKey(internalName, loader)]
        }
        val expected = resolved.firstOrNull() ?: return
        resolved.drop(1).firstOrNull { actual -> actual != expected }?.let { actual ->
            throw JvmLoadingConstraintViolationException(
                internalName = internalName,
                expectedClass = expected,
                actualClass = actual,
            )
        }
        loaders.forEach { loader ->
            resolutions[JvmLoadingConstraintResolutionKey(internalName, loader)] = expected
        }
    }

    private fun checkCompatibleResolution(
        internalName: String,
        constrainedLoaders: Set<JvmClassLoaderIdentity>,
        resolvedClass: JvmLoadedClassKey,
    ) {
        constrainedLoaders.forEach { loader ->
            val previous = resolutions[JvmLoadingConstraintResolutionKey(internalName, loader)] ?: return@forEach
            if (previous != resolvedClass) {
                throw JvmLoadingConstraintViolationException(
                    internalName = internalName,
                    expectedClass = previous,
                    actualClass = resolvedClass,
                )
            }
        }
    }
}

private data class JvmLoadingConstraintResolutionKey(
    val internalName: String,
    val initiatingLoader: JvmClassLoaderIdentity,
)

class JvmLoadingConstraintViolationException(
    val internalName: String,
    val expectedClass: JvmLoadedClassKey,
    val actualClass: JvmLoadedClassKey,
) : IllegalStateException(
    "Loading constraint violation for $internalName: expected ${expectedClass.diagnosticName}, " +
        "actual ${actualClass.diagnosticName}",
) {
    val guestThrowableClassName: String = "java/lang/LinkageError"
}
