package me.moeyinlo.visualize.jvm.runtime

data class JvmMethodAreaEntry(
    val definition: JvmClassDefinition,
    val staticFields: JvmStaticFields = JvmStaticFields(),
    val loadedClassKey: JvmLoadedClassKey? = null,
    val initiatingLoaders: Set<JvmClassLoaderIdentity> = emptySet(),
    val runtimeModuleName: String? = null,
) {
    init {
        require(runtimeModuleName == null || runtimeModuleName.isNotBlank()) {
            "runtime module name must not be blank"
        }
    }

    val runtimePackageKey: JvmRuntimePackageKey?
        get() = (loadedClassKey ?: JvmLoadedClassKey(definition.internalName, JvmClassLoaderIdentity.Bootstrap))
            .runtimePackageKey()
}

data class JvmRuntimeNestmateCheck(
    val areNestmates: Boolean,
    val failure: JvmRuntimeNestmateFailure? = null,
)

sealed interface JvmRuntimeNestmateFailure {
    data class MissingClass(val classKey: JvmLoadedClassKey) : JvmRuntimeNestmateFailure

    data class MissingHost(
        val memberKey: JvmLoadedClassKey,
        val hostKey: JvmLoadedClassKey,
    ) : JvmRuntimeNestmateFailure

    data class HostNotSelfHosted(
        val memberKey: JvmLoadedClassKey,
        val hostKey: JvmLoadedClassKey,
        val nominatedHostName: String,
    ) : JvmRuntimeNestmateFailure

    data class HostMissingMember(
        val memberKey: JvmLoadedClassKey,
        val hostKey: JvmLoadedClassKey,
    ) : JvmRuntimeNestmateFailure

    data class DifferentRuntimePackage(
        val memberKey: JvmLoadedClassKey,
        val hostKey: JvmLoadedClassKey,
        val memberPackageKey: JvmRuntimePackageKey?,
        val hostPackageKey: JvmRuntimePackageKey?,
    ) : JvmRuntimeNestmateFailure

    data class DifferentNestHosts(
        val firstHostKey: JvmLoadedClassKey,
        val secondHostKey: JvmLoadedClassKey,
    ) : JvmRuntimeNestmateFailure
}

class JvmMethodArea {
    private val entriesByLoadedClassKey = linkedMapOf<JvmLoadedClassKey, JvmMethodAreaEntry>()
    private val loadedClassKeysByInitiatingLoader = linkedMapOf<JvmInitiatingClassKey, JvmLoadedClassKey>()

    val classCount: Int
        get() = entriesByLoadedClassKey.size

    fun defineClass(entry: JvmMethodAreaEntry) {
        val className = entry.definition.internalName
        require(className.isNotBlank()) { "class internal name must not be blank" }

        val loadedClassKey = entry.loadedClassKey ?: JvmLoadedClassKey(
            internalName = className,
            definingLoader = JvmClassLoaderIdentity.Bootstrap,
        )
        require(loadedClassKey.internalName == className) {
            "loaded class key ${loadedClassKey.diagnosticName} must match class internal name $className"
        }
        validateRuntimePackageModule(entry, loadedClassKey)
        val previous = entriesByLoadedClassKey.putIfAbsent(loadedClassKey, entry)
        if (previous != null) {
            throw JvmMethodAreaDefinitionException("Class $className is already defined in the method area")
        }
        indexInitiatingLoaders(className, loadedClassKey, entry.initiatingLoaders + loadedClassKey.definingLoader)
    }

    fun getClass(internalName: String): JvmMethodAreaEntry {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
        return getClass(JvmLoadedClassKey(internalName, JvmClassLoaderIdentity.Bootstrap))
            ?: throw JvmMethodAreaAccessException("Class $internalName is not defined in the method area")
    }

    fun getClass(loadedClassKey: JvmLoadedClassKey): JvmMethodAreaEntry? =
        entriesByLoadedClassKey[loadedClassKey]

    fun recordInitiatingLoader(
        loadedClassKey: JvmLoadedClassKey,
        initiatingLoader: JvmClassLoaderIdentity,
    ): JvmMethodAreaEntry {
        val entry = entriesByLoadedClassKey[loadedClassKey]
            ?: throw JvmMethodAreaAccessException("Class ${loadedClassKey.diagnosticName} is not defined in the method area")
        val initiatingLoaders = entry.initiatingLoaders + loadedClassKey.definingLoader + initiatingLoader
        val updated = entry.copy(initiatingLoaders = initiatingLoaders)
        entriesByLoadedClassKey[loadedClassKey] = updated
        indexInitiatingLoaders(loadedClassKey.internalName, loadedClassKey, initiatingLoaders)
        return updated
    }

    fun getClass(
        internalName: String,
        initiatingLoader: JvmClassLoaderIdentity,
    ): JvmMethodAreaEntry? {
        require(internalName.isNotBlank()) { "class internal name must not be blank" }
        val loadedClassKey = loadedClassKeysByInitiatingLoader[JvmInitiatingClassKey(internalName, initiatingLoader)]
            ?: return null
        return entriesByLoadedClassKey[loadedClassKey]
    }

    fun hasClass(internalName: String): Boolean =
        hasClass(JvmLoadedClassKey(internalName, JvmClassLoaderIdentity.Bootstrap))

    fun hasClass(loadedClassKey: JvmLoadedClassKey): Boolean =
        loadedClassKey in entriesByLoadedClassKey

    fun classHierarchy(strictClassResolution: Boolean = false): JvmClassHierarchy =
        JvmClassHierarchy(
            classes = entriesByLoadedClassKey.values.map { entry -> entry.definition },
            strictClassResolution = strictClassResolution,
        )

    fun areRuntimeNestmates(firstKey: JvmLoadedClassKey, secondKey: JvmLoadedClassKey): Boolean {
        return checkRuntimeNestmates(firstKey, secondKey).areNestmates
    }

    fun checkRuntimeNestmates(firstKey: JvmLoadedClassKey, secondKey: JvmLoadedClassKey): JvmRuntimeNestmateCheck {
        val firstEntry = entriesByLoadedClassKey[firstKey]
            ?: return JvmRuntimeNestmateCheck(
                areNestmates = false,
                failure = JvmRuntimeNestmateFailure.MissingClass(firstKey),
            )
        val secondEntry = entriesByLoadedClassKey[secondKey]
            ?: return JvmRuntimeNestmateCheck(
                areNestmates = false,
                failure = JvmRuntimeNestmateFailure.MissingClass(secondKey),
            )
        val firstHostKey = when (val firstHost = runtimeNestHostKey(firstKey, firstEntry)) {
            is RuntimeNestHostLookup.Found -> firstHost.hostKey
            is RuntimeNestHostLookup.Failed -> return JvmRuntimeNestmateCheck(
                areNestmates = false,
                failure = firstHost.failure,
            )
        }
        val secondHostKey = when (val secondHost = runtimeNestHostKey(secondKey, secondEntry)) {
            is RuntimeNestHostLookup.Found -> secondHost.hostKey
            is RuntimeNestHostLookup.Failed -> return JvmRuntimeNestmateCheck(
                areNestmates = false,
                failure = secondHost.failure,
            )
        }
        return if (firstHostKey == secondHostKey) {
            JvmRuntimeNestmateCheck(areNestmates = true)
        } else {
            JvmRuntimeNestmateCheck(
                areNestmates = false,
                failure = JvmRuntimeNestmateFailure.DifferentNestHosts(
                    firstHostKey = firstHostKey,
                    secondHostKey = secondHostKey,
                ),
            )
        }
    }

    fun superclassDefinitionsFor(loadedClassKey: JvmLoadedClassKey): List<JvmClassDefinition> {
        var currentKey = loadedClassKey
        var currentEntry = entriesByLoadedClassKey[currentKey]
            ?: throw JvmMethodAreaAccessException("Class ${loadedClassKey.diagnosticName} is not defined in the method area")
        val definitions = mutableListOf<JvmClassDefinition>()
        val visited = linkedSetOf(currentKey)

        while (true) {
            val superclassName = currentEntry.definition.superclassName ?: break
            val superclassKey = JvmLoadedClassKey(
                internalName = superclassName,
                definingLoader = loadedClassKey.definingLoader,
            )
            if (!visited.add(superclassKey)) {
                throw JvmMethodAreaAccessException(
                    "Superclass cycle detected while resolving ${loadedClassKey.diagnosticName}: " +
                        superclassKey.diagnosticName,
                )
            }
            val superclassEntry = entriesByLoadedClassKey[superclassKey]
                ?: throw JvmMethodAreaAccessException(
                    "Superclass ${superclassKey.diagnosticName} of ${currentKey.diagnosticName} " +
                        "is not defined in the method area",
                )
            definitions += superclassEntry.definition
            currentKey = superclassKey
            currentEntry = superclassEntry
        }

        return definitions.asReversed()
    }

    fun classesInRuntimePackage(runtimePackageKey: JvmRuntimePackageKey): List<JvmMethodAreaEntry> =
        entriesByLoadedClassKey.values.filter { entry -> entry.runtimePackageKey == runtimePackageKey }

    fun toList(): List<JvmMethodAreaEntry> = entriesByLoadedClassKey.values.toList()

    private fun validateRuntimePackageModule(
        entry: JvmMethodAreaEntry,
        loadedClassKey: JvmLoadedClassKey,
    ) {
        val runtimePackageKey = loadedClassKey.runtimePackageKey() ?: return
        val conflictingEntry = classesInRuntimePackage(runtimePackageKey)
            .firstOrNull { existingEntry -> existingEntry.runtimeModuleName != entry.runtimeModuleName }
            ?: return
        throw JvmMethodAreaDefinitionException(
            "Runtime package ${runtimePackageKey.packageName} @ ${runtimePackageKey.definingLoader.diagnosticName} " +
                "is already associated with module ${conflictingEntry.runtimeModuleName.diagnosticModuleName()}, " +
                "cannot define ${entry.definition.internalName} in module ${entry.runtimeModuleName.diagnosticModuleName()}",
        )
    }

    private fun runtimeNestHostKey(
        memberKey: JvmLoadedClassKey,
        memberEntry: JvmMethodAreaEntry,
    ): RuntimeNestHostLookup {
        val hostKey = JvmLoadedClassKey(
            internalName = memberEntry.definition.nestHostInternalName,
            definingLoader = memberKey.definingLoader,
        )
        val hostEntry = entriesByLoadedClassKey[hostKey]
            ?: return RuntimeNestHostLookup.Failed(
                JvmRuntimeNestmateFailure.MissingHost(
                    memberKey = memberKey,
                    hostKey = hostKey,
                ),
            )
        if (memberKey == hostKey) {
            return RuntimeNestHostLookup.Found(hostKey)
        }
        if (hostEntry.definition.nestHostInternalName != hostEntry.definition.internalName) {
            return RuntimeNestHostLookup.Failed(
                JvmRuntimeNestmateFailure.HostNotSelfHosted(
                    memberKey = memberKey,
                    hostKey = hostKey,
                    nominatedHostName = hostEntry.definition.nestHostInternalName,
                ),
            )
        }
        if (memberEntry.definition.internalName !in hostEntry.definition.nestMemberInternalNames) {
            return RuntimeNestHostLookup.Failed(
                JvmRuntimeNestmateFailure.HostMissingMember(
                    memberKey = memberKey,
                    hostKey = hostKey,
                ),
            )
        }
        if (memberEntry.runtimePackageKey != hostEntry.runtimePackageKey) {
            return RuntimeNestHostLookup.Failed(
                JvmRuntimeNestmateFailure.DifferentRuntimePackage(
                    memberKey = memberKey,
                    hostKey = hostKey,
                    memberPackageKey = memberEntry.runtimePackageKey,
                    hostPackageKey = hostEntry.runtimePackageKey,
                ),
            )
        }
        return RuntimeNestHostLookup.Found(hostKey)
    }

    private fun indexInitiatingLoaders(
        internalName: String,
        loadedClassKey: JvmLoadedClassKey,
        initiatingLoaders: Set<JvmClassLoaderIdentity>,
    ) {
        initiatingLoaders.forEach { initiatingLoader ->
            loadedClassKeysByInitiatingLoader[JvmInitiatingClassKey(internalName, initiatingLoader)] = loadedClassKey
        }
    }
}

class JvmMethodAreaDefinitionException(message: String) : IllegalStateException(message)

class JvmMethodAreaAccessException(message: String) : IllegalStateException(message)

private fun String?.diagnosticModuleName(): String = this ?: "<unnamed>"

private sealed interface RuntimeNestHostLookup {
    data class Found(val hostKey: JvmLoadedClassKey) : RuntimeNestHostLookup

    data class Failed(val failure: JvmRuntimeNestmateFailure) : RuntimeNestHostLookup
}

private data class JvmInitiatingClassKey(
    val internalName: String,
    val initiatingLoader: JvmClassLoaderIdentity,
)
