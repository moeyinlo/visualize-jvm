package me.moeyinlo.visualize.jvm.classfile

enum class AttributeParserCoverageStatus {
    Parsed,
    RawPreserved,
    NotYetImplemented,
}

data class AttributeParserCoverageEntry(
    val attributeName: String,
    val status: AttributeParserCoverageStatus,
    val ownerScopes: List<String>,
    val parserObjectName: String?,
    val coveringTestClass: String?,
)

object AttributeParserCoverage {
    val entries: List<AttributeParserCoverageEntry> = listOf(
        parsed("ConstantValue", "field_info", "ConstantValueAttributeParser", "ConstantValueAttributeParserTest"),
        parsed("Code", "method_info", "CodeAttributeParser", "CodeAttributeHeaderParserTest"),
        parsed("StackMapTable", "Code", "StackMapTableAttributeParser", "StackMapTableAttributeParserTest"),
        parsed("Exceptions", "method_info", "ExceptionsAttributeParser", "ExceptionsAttributeParserTest"),
        parsed("InnerClasses", "ClassFile", "InnerClassesAttributeParser", "NestedClassAttributesParserTest"),
        parsed("EnclosingMethod", "ClassFile", "EnclosingMethodAttributeParser", "NestedClassAttributesParserTest"),
        parsed("Synthetic", "ClassFile, field_info, method_info", "SyntheticAttributeParser", "SimpleAttributesParserTest"),
        parsed("Signature", "ClassFile, field_info, method_info, record_component_info", "SignatureAttributeParser", "SignatureAttributeParserTest"),
        parsed("SourceFile", "ClassFile", "SourceFileAttributeParser", "SimpleAttributesParserTest"),
        parsed("SourceDebugExtension", "ClassFile", "SourceDebugExtensionAttributeParser", "SourceDebugExtensionAttributeParserTest"),
        parsed("LineNumberTable", "Code", "LineNumberTableAttributeParser", "LineNumberTableAttributeParserTest"),
        parsed("LocalVariableTable", "Code", "LocalVariableTableAttributeParser", "LocalVariableTableAttributeParserTest"),
        parsed("LocalVariableTypeTable", "Code", "LocalVariableTypeTableAttributeParser", "LocalVariableTypeTableAttributeParserTest"),
        parsed("Deprecated", "ClassFile, field_info, method_info", "DeprecatedAttributeParser", "SimpleAttributesParserTest"),
        parsed("RuntimeVisibleAnnotations", "ClassFile, field_info, method_info, record_component_info", "RuntimeVisibleAnnotationsAttributeParser", "RuntimeVisibleAnnotationsAttributeParserTest"),
        parsed("RuntimeInvisibleAnnotations", "ClassFile, field_info, method_info, record_component_info", "RuntimeInvisibleAnnotationsAttributeParser", "RuntimeInvisibleAnnotationsAttributeParserTest"),
        parsed("RuntimeVisibleParameterAnnotations", "method_info", "RuntimeVisibleParameterAnnotationsAttributeParser", "ParameterAnnotationsAttributeParserTest"),
        parsed("RuntimeInvisibleParameterAnnotations", "method_info", "RuntimeInvisibleParameterAnnotationsAttributeParser", "ParameterAnnotationsAttributeParserTest"),
        parsed("RuntimeVisibleTypeAnnotations", "ClassFile, field_info, method_info, Code, record_component_info", "RuntimeVisibleTypeAnnotationsAttributeParser", "TypeAnnotationsAttributeParserTest"),
        parsed("RuntimeInvisibleTypeAnnotations", "ClassFile, field_info, method_info, Code, record_component_info", "RuntimeInvisibleTypeAnnotationsAttributeParser", "TypeAnnotationsAttributeParserTest"),
        parsed("AnnotationDefault", "method_info", "AnnotationDefaultAttributeParser", "AnnotationDefaultAttributeParserTest"),
        parsed("BootstrapMethods", "ClassFile", "BootstrapMethodsAttributeParser", "BootstrapMethodsAttributeParserTest"),
        parsed("MethodParameters", "method_info", "MethodParametersAttributeParser", "MethodParametersAttributeParserTest"),
        parsed("Module", "ClassFile", "ModuleAttributeParser", "ModuleAttributeParserTest"),
        parsed("ModulePackages", "ClassFile", "ModulePackagesAttributeParser", "ModuleMetadataAttributesParserTest"),
        parsed("ModuleMainClass", "ClassFile", "ModuleMainClassAttributeParser", "ModuleMetadataAttributesParserTest"),
        parsed("NestHost", "ClassFile", "NestHostAttributeParser", "NestAttributesParserTest"),
        parsed("NestMembers", "ClassFile", "NestMembersAttributeParser", "NestAttributesParserTest"),
        parsed("Record", "ClassFile", "RecordAttributeParser", "RecordAttributeParserTest"),
        parsed("PermittedSubclasses", "ClassFile", "PermittedSubclassesAttributeParser", "PermittedSubclassesAttributeParserTest"),
    )

    private fun parsed(
        attributeName: String,
        ownerScopes: String,
        parserObjectName: String,
        coveringTestClass: String,
    ): AttributeParserCoverageEntry =
        AttributeParserCoverageEntry(
            attributeName = attributeName,
            status = AttributeParserCoverageStatus.Parsed,
            ownerScopes = ownerScopes.split(",").map(String::trim),
            parserObjectName = parserObjectName,
            coveringTestClass = coveringTestClass,
        )
}
