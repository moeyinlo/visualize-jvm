package me.moeyinlo.visualize.jvm.classfile

import java.io.ByteArrayOutputStream

class ClassFileByteWriter {
    private val output = ByteArrayOutputStream()

    val position: Int
        get() = output.size()

    fun writeU1(value: Int): ClassFileByteWriter {
        require(value in 0..0xFF) { "u1 value out of range: $value" }
        output.write(value)
        return this
    }

    fun writeU2(value: Int): ClassFileByteWriter {
        require(value in 0..0xFFFF) { "u2 value out of range: $value" }
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
        return this
    }

    fun writeU4(value: Long): ClassFileByteWriter {
        require(value in 0..0xFFFF_FFFFL) { "u4 value out of range: $value" }
        output.write(((value ushr 24) and 0xFF).toInt())
        output.write(((value ushr 16) and 0xFF).toInt())
        output.write(((value ushr 8) and 0xFF).toInt())
        output.write((value and 0xFF).toInt())
        return this
    }

    fun writeBytes(bytes: ByteArray): ClassFileByteWriter {
        output.write(bytes.copyOf())
        return this
    }

    fun toByteArray(): ByteArray = output.toByteArray()
}

object ClassFileWriter {
    private const val ConstantUtf8Tag = 1
    private const val ConstantIntegerTag = 3
    private const val ConstantFloatTag = 4
    private const val ConstantLongTag = 5
    private const val ConstantDoubleTag = 6
    private const val ConstantClassTag = 7
    private const val ConstantStringTag = 8
    private const val ConstantFieldRefTag = 9
    private const val ConstantMethodRefTag = 10
    private const val ConstantInterfaceMethodRefTag = 11
    private const val ConstantNameAndTypeTag = 12
    private const val ConstantMethodHandleTag = 15
    private const val ConstantMethodTypeTag = 16
    private const val ConstantDynamicTag = 17
    private const val ConstantInvokeDynamicTag = 18
    private const val ConstantModuleTag = 19
    private const val ConstantPackageTag = 20

    fun writeHeader(version: ClassFileVersion): ByteArray {
        val writer = ClassFileByteWriter()
        writeHeader(version, writer)
        return writer.toByteArray()
    }

    fun writeConstantPool(constantPool: ConstantPool): ByteArray {
        val writer = ClassFileByteWriter()
        writeConstantPool(constantPool, writer)
        return writer.toByteArray()
    }

    fun writeFields(fields: List<FieldInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeFields(fields, writer)
        return writer.toByteArray()
    }

    fun writeMethods(methods: List<MethodInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeMethods(methods, writer)
        return writer.toByteArray()
    }

    fun writeAttributes(attributes: List<AttributeInfo>): ByteArray {
        val writer = ClassFileByteWriter()
        writeAttributes(attributes, writer)
        return writer.toByteArray()
    }

    fun writeClassFile(classFile: ClassFile): ByteArray {
        val writer = ClassFileByteWriter()
        writeHeader(classFile.version, writer)
        writeConstantPool(classFile.constantPool, writer)
        writer.writeU2(classFile.accessFlags.raw)
            .writeConstantPoolIndex(classFile.identity.thisClassIndex)
            .writeOptionalConstantPoolIndex(classFile.identity.superClassIndex)
            .writeU2(classFile.identity.interfaceIndexes.size)
        classFile.identity.interfaceIndexes.forEach { interfaceIndex ->
            writer.writeConstantPoolIndex(interfaceIndex)
        }
        writeFields(classFile.fields, writer)
        writeMethods(classFile.methods, writer)
        writeAttributes(classFile.attributes, writer, "ClassFile.attributes")
        return writer.toByteArray()
    }

    internal fun writeHeader(
        version: ClassFileVersion,
        writer: ClassFileByteWriter,
    ) {
        val supportedVersion = ClassFileHeaderParser.validateJava26Version(version)
        writer.writeU4(ClassFileHeaderParser.ExpectedMagic)
            .writeU2(supportedVersion.minor)
            .writeU2(supportedVersion.major)
    }

    internal fun writeConstantPool(
        constantPool: ConstantPool,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(constantPool.constantPoolCount)

        var index = 1
        while (index < constantPool.constantPoolCount) {
            when (val slot = constantPool.slotAt(ConstantPoolIndex(index))) {
                is ConstantPoolSlot.Entry -> {
                    writeConstantPoolEntry(slot.value, writer)
                    index += if (slot.value.occupiesTwoSlots) 2 else 1
                }

                ConstantPoolSlot.Unusable -> throw ClassFileFormatException(
                    "Invalid constant pool slot #$index: unusable slot without preceding two-slot entry",
                )
            }
        }
    }

    internal fun writeFields(
        fields: List<FieldInfo>,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(fields.size)
        fields.forEachIndexed { index, field ->
            writer.writeU2(field.accessFlags)
                .writeConstantPoolIndex(field.nameIndex)
                .writeConstantPoolIndex(field.descriptorIndex)
            writeAttributes(field.attributes, writer, "fields[$index].attributes")
        }
    }

    internal fun writeMethods(
        methods: List<MethodInfo>,
        writer: ClassFileByteWriter,
    ) {
        writer.writeU2(methods.size)
        methods.forEachIndexed { index, method ->
            writer.writeU2(method.accessFlags)
                .writeConstantPoolIndex(method.nameIndex)
                .writeConstantPoolIndex(method.descriptorIndex)
            writeAttributes(method.attributes, writer, "methods[$index].attributes")
        }
    }

    internal fun writeAttributes(
        attributes: List<AttributeInfo>,
        writer: ClassFileByteWriter,
        ownerPath: String = "attributes",
    ) {
        writer.writeU2(attributes.size)
        attributes.forEachIndexed { index, attribute ->
            writeAttribute(attribute, writer, "$ownerPath[$index]")
        }
    }

    private fun writeConstantPoolEntry(
        entry: ConstantPoolEntry,
        writer: ClassFileByteWriter,
    ) {
        when (entry) {
            is ConstantUtf8Entry -> {
                val encodedBytes = entry.encodedBytes
                writer.writeU1(ConstantUtf8Tag)
                    .writeU2(encodedBytes.size)
                    .writeBytes(encodedBytes)
            }

            is ConstantIntegerEntry -> writer.writeU1(ConstantIntegerTag)
                .writeU4(entry.value.toLong() and 0xFFFF_FFFFL)

            is ConstantFloatEntry -> writer.writeU1(ConstantFloatTag)
                .writeU4(java.lang.Float.floatToRawIntBits(entry.value).toLong() and 0xFFFF_FFFFL)

            is ConstantLongEntry -> {
                val bits = entry.value
                writer.writeU1(ConstantLongTag)
                    .writeU4((bits ushr 32) and 0xFFFF_FFFFL)
                    .writeU4(bits and 0xFFFF_FFFFL)
            }

            is ConstantDoubleEntry -> {
                val bits = java.lang.Double.doubleToRawLongBits(entry.value)
                writer.writeU1(ConstantDoubleTag)
                    .writeU4((bits ushr 32) and 0xFFFF_FFFFL)
                    .writeU4(bits and 0xFFFF_FFFFL)
            }

            is ConstantClassEntry -> writer.writeU1(ConstantClassTag)
                .writeConstantPoolIndex(entry.nameIndex)

            is ConstantStringEntry -> writer.writeU1(ConstantStringTag)
                .writeConstantPoolIndex(entry.stringIndex)

            is ConstantNameAndTypeEntry -> writer.writeU1(ConstantNameAndTypeTag)
                .writeConstantPoolIndex(entry.nameIndex)
                .writeConstantPoolIndex(entry.descriptorIndex)

            is ConstantFieldRefEntry -> writer.writeU1(ConstantFieldRefTag)
                .writeMemberRef(entry)

            is ConstantMethodRefEntry -> writer.writeU1(ConstantMethodRefTag)
                .writeMemberRef(entry)

            is ConstantInterfaceMethodRefEntry -> writer.writeU1(ConstantInterfaceMethodRefTag)
                .writeMemberRef(entry)

            is ConstantMethodHandleEntry -> writer.writeU1(ConstantMethodHandleTag)
                .writeU1(entry.referenceKind.value)
                .writeConstantPoolIndex(entry.referenceIndex)

            is ConstantMethodTypeEntry -> writer.writeU1(ConstantMethodTypeTag)
                .writeConstantPoolIndex(entry.descriptorIndex)

            is ConstantDynamicEntry -> writer.writeU1(ConstantDynamicTag)
                .writeU2(entry.bootstrapMethodIndex.value)
                .writeConstantPoolIndex(entry.nameAndTypeIndex)

            is ConstantInvokeDynamicEntry -> writer.writeU1(ConstantInvokeDynamicTag)
                .writeU2(entry.bootstrapMethodIndex.value)
                .writeConstantPoolIndex(entry.nameAndTypeIndex)

            is ConstantModuleEntry -> writer.writeU1(ConstantModuleTag)
                .writeConstantPoolIndex(entry.nameIndex)

            is ConstantPackageEntry -> writer.writeU1(ConstantPackageTag)
                .writeConstantPoolIndex(entry.nameIndex)
        }
    }

    private fun ClassFileByteWriter.writeMemberRef(entry: ConstantMemberRefEntry): ClassFileByteWriter =
        writeConstantPoolIndex(entry.classIndex)
            .writeConstantPoolIndex(entry.nameAndTypeIndex)

    private fun ClassFileByteWriter.writeConstantPoolIndex(index: ConstantPoolIndex): ClassFileByteWriter =
        writeU2(index.value)

    private fun ClassFileByteWriter.writeOptionalConstantPoolIndex(index: ConstantPoolIndex?): ClassFileByteWriter =
        writeU2(index?.value ?: 0)

    private fun writeAttribute(
        attribute: AttributeInfo,
        writer: ClassFileByteWriter,
        ownerPath: String,
    ) {
        when (attribute) {
            is RawAttributeInfo -> writer.writeAttributeInfo(attribute.nameIndex, attribute.info)
            is UnknownAttributeInfo -> writer.writeAttributeInfo(attribute.nameIndex, attribute.info)
            is SyntheticAttribute -> writer.writeAttributeInfo(attribute.nameIndex, byteArrayOf())
            is DeprecatedAttribute -> writer.writeAttributeInfo(attribute.nameIndex, byteArrayOf())
            is SourceFileAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.sourceFileIndex)
            }
            is ConstantValueAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.constantValueIndex)
            }
            is CodeAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.maxStack)
                writeU2(attribute.maxLocals)
                val code = attribute.code
                writeU4(code.size.toLong())
                writeBytes(code)
                writeU2(attribute.exceptionTable.size)
                attribute.exceptionTable.forEach { handler ->
                    writeU2(handler.startPc)
                    writeU2(handler.endPc)
                    writeU2(handler.handlerPc)
                    writeOptionalConstantPoolIndex(handler.catchType)
                }
                writeAttributes(attribute.attributes, this, "$ownerPath.attributes")
            }
            is StackMapTableAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.entries.size)
                attribute.entries.forEach { frame ->
                    writeStackMapFrame(frame)
                }
            }
            is ExceptionsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.exceptionIndexTable.size)
                attribute.exceptionIndexTable.forEach { exceptionIndex ->
                    writeConstantPoolIndex(exceptionIndex)
                }
            }
            is InnerClassesAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.classes.size)
                attribute.classes.forEach { entry ->
                    writeConstantPoolIndex(entry.innerClassInfoIndex)
                    writeOptionalConstantPoolIndex(entry.outerClassInfoIndex)
                    writeOptionalConstantPoolIndex(entry.innerNameIndex)
                    writeU2(entry.innerClassAccessFlags)
                }
            }
            is EnclosingMethodAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.classIndex)
                writeOptionalConstantPoolIndex(attribute.methodIndex)
            }
            is SignatureAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.signatureIndex)
            }
            is RuntimeVisibleAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeAnnotations(attribute.annotations)
            }
            is RuntimeInvisibleAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeAnnotations(attribute.annotations)
            }
            is RuntimeVisibleParameterAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeParameterAnnotations(attribute.parameterAnnotations)
            }
            is RuntimeInvisibleParameterAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeParameterAnnotations(attribute.parameterAnnotations)
            }
            is AnnotationDefaultAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeElementValue(attribute.defaultValue)
            }
            is RuntimeVisibleTypeAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeTypeAnnotations(attribute.annotations)
            }
            is RuntimeInvisibleTypeAnnotationsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeTypeAnnotations(attribute.annotations)
            }
            is BootstrapMethodsAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.bootstrapMethods.size)
                attribute.bootstrapMethods.forEach { bootstrapMethod ->
                    writeConstantPoolIndex(bootstrapMethod.bootstrapMethodRef)
                    writeU2(bootstrapMethod.bootstrapArguments.size)
                    bootstrapMethod.bootstrapArguments.forEach { argumentIndex ->
                        writeConstantPoolIndex(argumentIndex)
                    }
                }
            }
            is SourceDebugExtensionAttribute -> writer.writeAttributeInfo(
                attribute.nameIndex,
                attribute.debugExtension,
            )
            is LineNumberTableAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.entries.size)
                attribute.entries.forEach { entry ->
                    writeU2(entry.startPc)
                    writeU2(entry.lineNumber)
                }
            }
            is LocalVariableTableAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.entries.size)
                attribute.entries.forEach { entry ->
                    writeU2(entry.startPc)
                    writeU2(entry.length)
                    writeConstantPoolIndex(entry.nameIndex)
                    writeConstantPoolIndex(entry.descriptorIndex)
                    writeU2(entry.index)
                }
            }
            is LocalVariableTypeTableAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.entries.size)
                attribute.entries.forEach { entry ->
                    writeU2(entry.startPc)
                    writeU2(entry.length)
                    writeConstantPoolIndex(entry.nameIndex)
                    writeConstantPoolIndex(entry.signatureIndex)
                    writeU2(entry.index)
                }
            }
            is MethodParametersAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU1(attribute.parameters.size)
                attribute.parameters.forEach { parameter ->
                    writeOptionalConstantPoolIndex(parameter.nameIndex)
                    writeU2(parameter.accessFlags)
                }
            }
            is ModuleAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.moduleNameIndex)
                writeU2(attribute.moduleFlags)
                writeOptionalConstantPoolIndex(attribute.moduleVersionIndex)
                writeU2(attribute.requires.size)
                attribute.requires.forEach { requires ->
                    writeConstantPoolIndex(requires.requiresIndex)
                    writeU2(requires.requiresFlags)
                    writeOptionalConstantPoolIndex(requires.requiresVersionIndex)
                }
                writeU2(attribute.exports.size)
                attribute.exports.forEach { exports ->
                    writeConstantPoolIndex(exports.exportsIndex)
                    writeU2(exports.exportsFlags)
                    writeU2(exports.exportsToIndexes.size)
                    exports.exportsToIndexes.forEach { moduleIndex ->
                        writeConstantPoolIndex(moduleIndex)
                    }
                }
                writeU2(attribute.opens.size)
                attribute.opens.forEach { opens ->
                    writeConstantPoolIndex(opens.opensIndex)
                    writeU2(opens.opensFlags)
                    writeU2(opens.opensToIndexes.size)
                    opens.opensToIndexes.forEach { moduleIndex ->
                        writeConstantPoolIndex(moduleIndex)
                    }
                }
                writeU2(attribute.uses.size)
                attribute.uses.forEach { usesIndex ->
                    writeConstantPoolIndex(usesIndex)
                }
                writeU2(attribute.provides.size)
                attribute.provides.forEach { provides ->
                    writeConstantPoolIndex(provides.providesIndex)
                    writeU2(provides.providesWithIndexes.size)
                    provides.providesWithIndexes.forEach { providesWithIndex ->
                        writeConstantPoolIndex(providesWithIndex)
                    }
                }
            }
            is ModulePackagesAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.packageIndexes.size)
                attribute.packageIndexes.forEach { packageIndex ->
                    writeConstantPoolIndex(packageIndex)
                }
            }
            is ModuleMainClassAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.mainClassIndex)
            }
            is NestHostAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeConstantPoolIndex(attribute.hostClassIndex)
            }
            is NestMembersAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.classes.size)
                attribute.classes.forEach { classIndex ->
                    writeConstantPoolIndex(classIndex)
                }
            }
            is PermittedSubclassesAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.classes.size)
                attribute.classes.forEach { classIndex ->
                    writeConstantPoolIndex(classIndex)
                }
            }
            is RecordAttribute -> writer.writeAttributeInfo(attribute.nameIndex) {
                writeU2(attribute.components.size)
                attribute.components.forEachIndexed { componentIndex, component ->
                    writeConstantPoolIndex(component.nameIndex)
                    writeConstantPoolIndex(component.descriptorIndex)
                    writeAttributes(component.attributes, this, "$ownerPath.components[$componentIndex].attributes")
                }
            }
            else -> throw UnsupportedOperationException(
                "Writing ${attribute::class.simpleName} at $ownerPath requires a specific attribute writer",
            )
        }
    }

    private fun ClassFileByteWriter.writeStackMapFrame(frame: StackMapFrame) {
        writeU1(frame.frameType)
        when (frame) {
            is SameStackMapFrame -> Unit
            is SameLocalsOneStackItemFrame -> writeVerificationType(frame.stack)
            is SameLocalsOneStackItemFrameExtended -> {
                writeU2(frame.offsetDelta)
                writeVerificationType(frame.stack)
            }
            is ChopStackMapFrame -> writeU2(frame.offsetDelta)
            is SameStackMapFrameExtended -> writeU2(frame.offsetDelta)
            is AppendStackMapFrame -> {
                writeU2(frame.offsetDelta)
                frame.locals.forEach { local ->
                    writeVerificationType(local)
                }
            }
            is FullStackMapFrame -> {
                writeU2(frame.offsetDelta)
                writeU2(frame.locals.size)
                frame.locals.forEach { local ->
                    writeVerificationType(local)
                }
                writeU2(frame.stack.size)
                frame.stack.forEach { stack ->
                    writeVerificationType(stack)
                }
            }
        }
    }

    private fun ClassFileByteWriter.writeVerificationType(type: VerificationTypeInfo) {
        writeU1(type.tag)
        when (type) {
            VerificationTypeInfo.Top,
            VerificationTypeInfo.Integer,
            VerificationTypeInfo.Float,
            VerificationTypeInfo.Double,
            VerificationTypeInfo.Long,
            VerificationTypeInfo.Null,
            VerificationTypeInfo.UninitializedThis,
            -> Unit
            is VerificationTypeInfo.ObjectVariable -> writeConstantPoolIndex(type.cpoolIndex)
            is VerificationTypeInfo.UninitializedVariable -> writeU2(type.offset)
        }
    }

    private fun ClassFileByteWriter.writeTypeAnnotations(annotations: List<TypeAnnotationInfo>) {
        writeU2(annotations.size)
        annotations.forEach { annotation ->
            writeU1(annotation.targetType)
            writeTypeAnnotationTargetInfo(annotation.targetInfo)
            writeTypePath(annotation.targetPath)
            writeAnnotation(annotation.annotation)
        }
    }

    private fun ClassFileByteWriter.writeTypeAnnotationTargetInfo(targetInfo: TypeAnnotationTargetInfo) {
        when (targetInfo) {
            is TypeAnnotationTargetInfo.TypeParameterTarget -> writeU1(targetInfo.typeParameterIndex)
            is TypeAnnotationTargetInfo.SupertypeTarget -> writeU2(targetInfo.supertypeIndex)
            is TypeAnnotationTargetInfo.TypeParameterBoundTarget -> {
                writeU1(targetInfo.typeParameterIndex)
                writeU1(targetInfo.boundIndex)
            }
            TypeAnnotationTargetInfo.EmptyTarget -> Unit
            is TypeAnnotationTargetInfo.FormalParameterTarget -> writeU1(targetInfo.formalParameterIndex)
            is TypeAnnotationTargetInfo.ThrowsTarget -> writeU2(targetInfo.throwsTypeIndex)
            is TypeAnnotationTargetInfo.LocalVarTarget -> {
                writeU2(targetInfo.table.size)
                targetInfo.table.forEach { entry ->
                    writeU2(entry.startPc)
                    writeU2(entry.length)
                    writeU2(entry.index)
                }
            }
            is TypeAnnotationTargetInfo.CatchTarget -> writeU2(targetInfo.exceptionTableIndex)
            is TypeAnnotationTargetInfo.OffsetTarget -> writeU2(targetInfo.offset)
            is TypeAnnotationTargetInfo.TypeArgumentTarget -> {
                writeU2(targetInfo.offset)
                writeU1(targetInfo.typeArgumentIndex)
            }
        }
    }

    private fun ClassFileByteWriter.writeTypePath(typePath: TypePath) {
        writeU1(typePath.entries.size)
        typePath.entries.forEach { entry ->
            writeU1(entry.typePathKind)
            writeU1(entry.typeArgumentIndex)
        }
    }

    private fun ClassFileByteWriter.writeParameterAnnotations(parameterAnnotations: List<ParameterAnnotations>) {
        writeU1(parameterAnnotations.size)
        parameterAnnotations.forEach { parameter ->
            writeU2(parameter.annotations.size)
            parameter.annotations.forEach { annotation ->
                writeAnnotation(annotation)
            }
        }
    }

    private fun ClassFileByteWriter.writeAnnotations(annotations: List<AnnotationInfo>) {
        writeU2(annotations.size)
        annotations.forEach { annotation ->
            writeAnnotation(annotation)
        }
    }

    private fun ClassFileByteWriter.writeAnnotation(annotation: AnnotationInfo) {
        writeConstantPoolIndex(annotation.typeIndex)
        writeU2(annotation.elementValuePairs.size)
        annotation.elementValuePairs.forEach { pair ->
            writeConstantPoolIndex(pair.elementNameIndex)
            writeElementValue(pair.value)
        }
    }

    private fun ClassFileByteWriter.writeElementValue(value: ElementValue) {
        writeU1(value.tag.code)
        when (value) {
            is ElementValue.Const -> writeConstantPoolIndex(value.constValueIndex)
            is ElementValue.EnumConst -> {
                writeConstantPoolIndex(value.typeNameIndex)
                writeConstantPoolIndex(value.constNameIndex)
            }
            is ElementValue.ClassInfo -> writeConstantPoolIndex(value.classInfoIndex)
            is ElementValue.NestedAnnotation -> writeAnnotation(value.annotation)
            is ElementValue.ArrayValue -> {
                writeU2(value.values.size)
                value.values.forEach { nestedValue ->
                    writeElementValue(nestedValue)
                }
            }
        }
    }

    private fun ClassFileByteWriter.writeAttributeInfo(
        nameIndex: ConstantPoolIndex,
        info: ByteArray,
    ): ClassFileByteWriter =
        writeConstantPoolIndex(nameIndex)
            .writeU4(info.size.toLong())
            .writeBytes(info)

    private fun ClassFileByteWriter.writeAttributeInfo(
        nameIndex: ConstantPoolIndex,
        writeInfo: ClassFileByteWriter.() -> Unit,
    ): ClassFileByteWriter {
        val infoWriter = ClassFileByteWriter()
        infoWriter.writeInfo()
        return writeAttributeInfo(nameIndex, infoWriter.toByteArray())
    }
}
