package me.moeyinlo.visualize.jvm.runtime

object JvmRuntimeDynamicLinker {
    fun resolve(
        constantPool: JvmRuntimeConstantPool,
        index: JvmRuntimeConstantPoolIndex,
        classHierarchy: JvmClassHierarchy,
    ): JvmRuntimeResolvedConstant {
        constantPool.resolved(index)?.let { resolved -> return resolved }

        val resolved = when (val entry = constantPool[index]) {
            is JvmRuntimeClassSymbolicReference -> JvmRuntimeResolvedConstant.Class(entry.internalName)
            is JvmRuntimeFieldSymbolicReference -> JvmRuntimeResolvedConstant.Field(
                classHierarchy.resolveField(
                    ownerClassName = entry.field.ownerClassName,
                    name = entry.field.name,
                    descriptor = entry.field.descriptor,
                ),
            )
            is JvmRuntimeLiteralConstant -> JvmRuntimeResolvedConstant.Value(entry.value)
            is JvmRuntimeMethodSymbolicReference -> JvmRuntimeResolvedConstant.Method(
                classHierarchy.resolveMethod(
                    ownerClassName = entry.ownerClassName,
                    name = entry.name,
                    descriptor = entry.descriptor,
                ),
            )
            is JvmRuntimeStringConstant -> JvmRuntimeResolvedConstant.String(entry.value)
        }
        return constantPool.cacheResolved(index, resolved)
    }
}
