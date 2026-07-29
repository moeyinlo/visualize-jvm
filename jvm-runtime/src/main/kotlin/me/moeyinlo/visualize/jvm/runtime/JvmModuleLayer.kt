package me.moeyinlo.visualize.jvm.runtime

data class JvmModuleDescriptor(
    val name: String,
    val packages: Set<String> = emptySet(),
    val requires: Set<String> = emptySet(),
) {
    init {
        require(name.isNotBlank()) { "module name must not be blank" }
        require(packages.all(String::isNotBlank)) { "module packages must not contain blank names" }
        require(requires.all(String::isNotBlank)) { "module requires must not contain blank names" }
    }
}

class JvmModuleLayer(
    private val parent: JvmModuleLayer? = null,
) {
    private val modulesByName = linkedMapOf<String, JvmModuleDescriptor>()
    private val packageOwners = linkedMapOf<String, String>()

    fun define(module: JvmModuleDescriptor): JvmModuleLayer {
        if (module.name in modulesByName) {
            throw JvmModuleLayerException("Module ${module.name} is already defined in this layer")
        }
        module.packages.forEach { packageName ->
            val previous = packageOwners[packageName]
            if (previous != null) {
                throw JvmModuleLayerException("Package $packageName is already defined by module $previous in this layer")
            }
        }

        modulesByName[module.name] = module
        module.packages.forEach { packageName ->
            packageOwners[packageName] = module.name
        }
        return this
    }

    fun findModule(name: String): JvmModuleDescriptor? {
        require(name.isNotBlank()) { "module name must not be blank" }
        return modulesByName[name] ?: parent?.findModule(name)
    }

    fun findPackageOwner(packageName: String): JvmModuleDescriptor? {
        require(packageName.isNotBlank()) { "package name must not be blank" }
        val moduleName = packageOwners[packageName]
            ?: return parent?.findPackageOwner(packageName)
        return modulesByName[moduleName]
    }

    fun modules(): List<JvmModuleDescriptor> = modulesByName.values.toList()

    fun canRead(
        sourceModuleName: String,
        targetModuleName: String,
    ): Boolean {
        val source = requireModule(sourceModuleName)
        requireModule(targetModuleName)
        return sourceModuleName == targetModuleName || targetModuleName in source.requires
    }

    private fun requireModule(name: String): JvmModuleDescriptor =
        findModule(name) ?: throw JvmModuleLayerException("Module $name is not defined in this layer graph")
}

class JvmModuleLayerException(message: String) : IllegalStateException(message)
