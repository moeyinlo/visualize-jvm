package me.moeyinlo.visualize.jvm.classfile

data class BootstrapMethodsAttribute(
    override val nameIndex: ConstantPoolIndex,
    val bootstrapMethods: List<BootstrapMethodSpecifier>,
) : AttributeInfo

data class BootstrapMethodSpecifier(
    val bootstrapMethodRef: ConstantPoolIndex,
    val bootstrapArguments: List<ConstantPoolIndex>,
)

object BootstrapMethodsAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val numBootstrapMethods = context.reader.readU2()
        return BootstrapMethodsAttribute(
            nameIndex = context.nameIndex,
            bootstrapMethods = List(numBootstrapMethods) { index ->
                parseBootstrapMethod(context, "${context.ownerPath}.bootstrap_methods[$index]")
            },
        )
    }

    private fun parseBootstrapMethod(
        context: AttributeParseContext,
        ownerPath: String,
    ): BootstrapMethodSpecifier {
        val bootstrapMethodRef = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
            reader = context.reader,
            role = "$ownerPath.bootstrap_method_ref",
        )
        expectMethodHandle(context, "$ownerPath.bootstrap_method_ref", bootstrapMethodRef)
        val numBootstrapArguments = context.reader.readU2()
        return BootstrapMethodSpecifier(
            bootstrapMethodRef = bootstrapMethodRef,
            bootstrapArguments = List(numBootstrapArguments) { index ->
                val argumentIndex = RawAttributeInfoParser.readNonZeroConstantPoolIndex(
                    reader = context.reader,
                    role = "$ownerPath.bootstrap_arguments[$index]",
                )
                expectLoadable(context, "$ownerPath.bootstrap_arguments[$index]", argumentIndex)
                argumentIndex
            },
        )
    }

    private fun expectMethodHandle(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ) {
        val entry = constantPoolEntry(context, role, index)
        if (entry !is ConstantMethodHandleEntry) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected CONSTANT_MethodHandle_info but found ${entry.javaClass.simpleName}",
            )
        }
    }

    private fun expectLoadable(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ) {
        val entry = constantPoolEntry(context, role, index)
        if (!entry.isLoadableConstant()) {
            throw ClassFileFormatException(
                "Invalid $role=$index: expected a loadable constant_pool entry but found ${entry.javaClass.simpleName}",
            )
        }
    }

    private fun ConstantPoolEntry.isLoadableConstant(): Boolean =
        this is ConstantIntegerEntry ||
            this is ConstantFloatEntry ||
            this is ConstantLongEntry ||
            this is ConstantDoubleEntry ||
            this is ConstantClassEntry ||
            this is ConstantStringEntry ||
            this is ConstantMethodHandleEntry ||
            this is ConstantMethodTypeEntry ||
            this is ConstantDynamicEntry

    private fun constantPoolEntry(
        context: AttributeParseContext,
        role: String,
        index: ConstantPoolIndex,
    ): ConstantPoolEntry =
        try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=$index: ${exception.message}")
        }
}
