package me.moeyinlo.visualize.jvm.classfile

class CodeAttribute(
    override val nameIndex: ConstantPoolIndex,
    val maxStack: Int,
    val maxLocals: Int,
    code: ByteArray,
    val exceptionTable: List<CodeExceptionHandler> = emptyList(),
    val attributes: List<AttributeInfo> = emptyList(),
) : AttributeInfo {
    private val codeBytes = code.copyOf()

    val code: ByteArray
        get() = codeBytes.copyOf()
}

data class CodeExceptionHandler(
    val startPc: Int,
    val endPc: Int,
    val handlerPc: Int,
    val catchType: ConstantPoolIndex?,
)

object CodeAttributeParser : AttributeBodyParser {
    override fun parse(context: AttributeParseContext): AttributeInfo {
        val maxStack = context.reader.readU2()
        val maxLocals = context.reader.readU2()
        val codeLength = context.reader.readU4()
        if (codeLength == 0L) {
            throw ClassFileFormatException(
                "Invalid Code code_length=0 at ${context.ownerPath}: must be greater than zero",
            )
        }
        if (codeLength >= 65_536L) {
            throw ClassFileFormatException(
                "Invalid Code code_length=$codeLength at ${context.ownerPath}: must be less than 65536",
            )
        }

        val code = context.reader.readSlice(codeLength.toInt())
        val instructionLayout = CodeInstructionValidator.validate(
            code = code,
            ownerPath = context.ownerPath,
            constantPool = context.constantPool,
            majorVersion = context.majorVersion,
            maxLocals = maxLocals,
        )
        val exceptionTable = parseExceptionTable(context, code.size, instructionLayout)
        val attributes = AttributeInfoParser.parseAttributes(
            reader = context.reader,
            constantPool = context.constantPool,
            registry = context.registry,
            ownerPath = context.ownerPath,
            majorVersion = context.majorVersion,
        )
        validateCodeAttributes(context, attributes, exceptionTable.size, instructionLayout, code, maxLocals)

        return CodeAttribute(
            nameIndex = context.nameIndex,
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = code,
            exceptionTable = exceptionTable,
            attributes = attributes,
        )
    }

    private fun validateCodeAttributes(
        context: AttributeParseContext,
        attributes: List<AttributeInfo>,
        exceptionTableLength: Int,
        instructionLayout: CodeInstructionLayout,
        code: ByteArray,
        maxLocals: Int,
    ) {
        val stackMapTablePaths = mutableListOf<String>()
        val runtimeVisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleTypeAnnotationsPaths = mutableListOf<String>()
        val runtimeVisibleParameterAnnotationsPaths = mutableListOf<String>()
        val runtimeInvisibleParameterAnnotationsPaths = mutableListOf<String>()
        val codePaths = mutableListOf<String>()
        val constantValuePaths = mutableListOf<String>()
        val sourceFilePaths = mutableListOf<String>()
        val sourceDebugExtensionPaths = mutableListOf<String>()
        val innerClassesPaths = mutableListOf<String>()
        val enclosingMethodPaths = mutableListOf<String>()
        val bootstrapMethodsPaths = mutableListOf<String>()
        val methodParametersPaths = mutableListOf<String>()
        val modulePaths = mutableListOf<String>()
        val modulePackagesPaths = mutableListOf<String>()
        val moduleMainClassPaths = mutableListOf<String>()
        val nestHostPaths = mutableListOf<String>()
        val nestMembersPaths = mutableListOf<String>()
        val recordPaths = mutableListOf<String>()
        val permittedSubclassesPaths = mutableListOf<String>()
        attributes.forEachIndexed { index, attribute ->
            val attributePath = "${context.ownerPath}.attributes[$index]"
            val name = attributeName(context, attribute, "$attributePath.attribute_name_index")
            when (name) {
                "StackMapTable" -> {
                    stackMapTablePaths += "${context.ownerPath}.attributes[$index]"
                    validateStackMapTableFrameOffsets(attributePath, attribute, instructionLayout)
                    validateStackMapTableUninitializedVariables(attributePath, attribute, instructionLayout, code)
                }
                "LineNumberTable" -> validateLineNumberTable(attributePath, attribute, instructionLayout)
                "LocalVariableTable" -> validateLocalVariableTable(
                    attributePath,
                    attribute,
                    instructionLayout,
                    code.size,
                    maxLocals,
                    context.constantPool,
                )
                "LocalVariableTypeTable" -> validateLocalVariableTypeTable(
                    attributePath,
                    attribute,
                    instructionLayout,
                    code.size,
                    maxLocals,
                )
                "RuntimeVisibleTypeAnnotations" -> {
                    runtimeVisibleTypeAnnotationsPaths += attributePath
                    validateCodeTypeAnnotationTargets(
                        attributePath,
                        attribute,
                        exceptionTableLength,
                        instructionLayout,
                        code.size,
                        maxLocals,
                    )
                }
                "RuntimeInvisibleTypeAnnotations" -> {
                    runtimeInvisibleTypeAnnotationsPaths += attributePath
                    validateCodeTypeAnnotationTargets(
                        attributePath,
                        attribute,
                        exceptionTableLength,
                        instructionLayout,
                        code.size,
                        maxLocals,
                    )
                }
                "Code" -> codePaths += attributePath
                "ConstantValue" -> constantValuePaths += attributePath
                "RuntimeVisibleParameterAnnotations" -> runtimeVisibleParameterAnnotationsPaths += attributePath
                "RuntimeInvisibleParameterAnnotations" -> runtimeInvisibleParameterAnnotationsPaths += attributePath
                "SourceFile" -> sourceFilePaths += attributePath
                "SourceDebugExtension" -> sourceDebugExtensionPaths += attributePath
                "InnerClasses" -> innerClassesPaths += attributePath
                "EnclosingMethod" -> enclosingMethodPaths += attributePath
                "BootstrapMethods" -> bootstrapMethodsPaths += attributePath
                "MethodParameters" -> methodParametersPaths += attributePath
                "Module" -> modulePaths += attributePath
                "ModulePackages" -> modulePackagesPaths += attributePath
                "ModuleMainClass" -> moduleMainClassPaths += attributePath
                "NestHost" -> nestHostPaths += attributePath
                "NestMembers" -> nestMembersPaths += attributePath
                "Record" -> recordPaths += attributePath
                "PermittedSubclasses" -> permittedSubclassesPaths += attributePath
            }
        }
        requireAbsentAttribute(codePaths, "Code", "method_info", context.ownerPath)
        requireAbsentAttribute(constantValuePaths, "ConstantValue", "field_info", context.ownerPath)
        requireAbsentAttribute(sourceFilePaths, "SourceFile", "ClassFile", context.ownerPath)
        requireAbsentAttribute(sourceDebugExtensionPaths, "SourceDebugExtension", "ClassFile", context.ownerPath)
        requireAbsentAttribute(runtimeVisibleParameterAnnotationsPaths, "RuntimeVisibleParameterAnnotations", "method_info", context.ownerPath)
        requireAbsentAttribute(runtimeInvisibleParameterAnnotationsPaths, "RuntimeInvisibleParameterAnnotations", "method_info", context.ownerPath)
        requireAbsentAttribute(innerClassesPaths, "InnerClasses", "ClassFile", context.ownerPath)
        requireAbsentAttribute(enclosingMethodPaths, "EnclosingMethod", "ClassFile", context.ownerPath)
        requireAbsentAttribute(bootstrapMethodsPaths, "BootstrapMethods", "ClassFile", context.ownerPath)
        requireAbsentAttribute(methodParametersPaths, "MethodParameters", "method_info", context.ownerPath)
        requireAbsentAttribute(modulePaths, "Module", "ClassFile", context.ownerPath)
        requireAbsentAttribute(modulePackagesPaths, "ModulePackages", "ClassFile", context.ownerPath)
        requireAbsentAttribute(moduleMainClassPaths, "ModuleMainClass", "ClassFile", context.ownerPath)
        requireAbsentAttribute(nestHostPaths, "NestHost", "ClassFile", context.ownerPath)
        requireAbsentAttribute(nestMembersPaths, "NestMembers", "ClassFile", context.ownerPath)
        requireAbsentAttribute(recordPaths, "Record", "ClassFile", context.ownerPath)
        requireAbsentAttribute(permittedSubclassesPaths, "PermittedSubclasses", "ClassFile", context.ownerPath)
        requireAtMostOneAttribute(stackMapTablePaths, "StackMapTable", context.ownerPath)
        requireAtMostOneAttribute(runtimeVisibleTypeAnnotationsPaths, "RuntimeVisibleTypeAnnotations", context.ownerPath)
        requireAtMostOneAttribute(runtimeInvisibleTypeAnnotationsPaths, "RuntimeInvisibleTypeAnnotations", context.ownerPath)
    }

    private fun validateLocalVariableTable(
        attributePath: String,
        attribute: AttributeInfo,
        instructionLayout: CodeInstructionLayout,
        codeLength: Int,
        maxLocals: Int,
        constantPool: ConstantPool,
    ) {
        if (attribute !is LocalVariableTableAttribute) {
            return
        }

        attribute.entries.forEachIndexed { entryIndex, entry ->
            val entryPath = "$attributePath.local_variable_table[$entryIndex]"
            validateLocalVariableRange(
                attributeName = "LocalVariableTable",
                entryPath = entryPath,
                startPc = entry.startPc,
                length = entry.length,
                instructionLayout = instructionLayout,
                codeLength = codeLength,
            )
            val slots = localVariableTableSlots(entryPath, entry, constantPool)
            if (entry.index + slots > maxLocals) {
                throw ClassFileFormatException(
                    "Invalid $entryPath LocalVariableTable.index=${entry.index}: " +
                        "requires slots=$slots within max_locals=$maxLocals",
                )
            }
        }
    }

    private fun localVariableTableSlots(
        entryPath: String,
        entry: LocalVariableTableEntry,
        constantPool: ConstantPool,
    ): Int {
        val descriptor = try {
            constantPool[entry.descriptorIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $entryPath.descriptor_index=${entry.descriptorIndex}: ${exception.message}",
            )
        }
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $entryPath.descriptor_index=${entry.descriptorIndex}: " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        return if (descriptor.value == "J" || descriptor.value == "D") 2 else 1
    }

    private fun validateLocalVariableTypeTable(
        attributePath: String,
        attribute: AttributeInfo,
        instructionLayout: CodeInstructionLayout,
        codeLength: Int,
        maxLocals: Int,
    ) {
        if (attribute !is LocalVariableTypeTableAttribute) {
            return
        }

        attribute.entries.forEachIndexed { entryIndex, entry ->
            val entryPath = "$attributePath.local_variable_type_table[$entryIndex]"
            validateLocalVariableRange(
                attributeName = "LocalVariableTypeTable",
                entryPath = entryPath,
                startPc = entry.startPc,
                length = entry.length,
                instructionLayout = instructionLayout,
                codeLength = codeLength,
            )
            if (entry.index >= maxLocals) {
                throw ClassFileFormatException(
                    "Invalid $entryPath LocalVariableTypeTable.index=${entry.index}: " +
                        "must be less than max_locals=$maxLocals",
                )
            }
        }
    }

    private fun validateLocalVariableRange(
        attributeName: String,
        entryPath: String,
        startPc: Int,
        length: Int,
        instructionLayout: CodeInstructionLayout,
        codeLength: Int,
    ) {
        if (startPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $entryPath $attributeName.start_pc=$startPc: must point to the opcode of an instruction",
            )
        }
        val endPc = startPc + length
        if (endPc > codeLength) {
            throw ClassFileFormatException(
                "Invalid $entryPath $attributeName range: start_pc=$startPc length=$length exceeds code_length=$codeLength",
            )
        }
        if (endPc != codeLength && endPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $entryPath $attributeName.end_pc=$endPc: " +
                    "must be code_length=$codeLength or point to the opcode of an instruction",
            )
        }
    }

    private fun validateLineNumberTable(
        attributePath: String,
        attribute: AttributeInfo,
        instructionLayout: CodeInstructionLayout,
    ) {
        if (attribute !is LineNumberTableAttribute) {
            return
        }

        attribute.entries.forEachIndexed { entryIndex, entry ->
            if (entry.startPc !in instructionLayout.instructionOffsets) {
                throw ClassFileFormatException(
                    "Invalid $attributePath LineNumberTable.line_number_table[$entryIndex].start_pc=${entry.startPc}: " +
                        "must point to the opcode of an instruction",
                )
            }
        }
    }

    private fun validateStackMapTableFrameOffsets(
        attributePath: String,
        attribute: AttributeInfo,
        instructionLayout: CodeInstructionLayout,
    ) {
        if (attribute !is StackMapTableAttribute) {
            return
        }

        var previousOffset = -1L
        attribute.entries.forEachIndexed { frameIndex, frame ->
            val frameOffset = if (previousOffset < 0) {
                frame.offsetDelta.toLong()
            } else {
                previousOffset + frame.offsetDelta.toLong() + 1L
            }
            val isInstructionOffset = frameOffset <= Int.MAX_VALUE &&
                frameOffset.toInt() in instructionLayout.instructionOffsets
            if (!isInstructionOffset) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.entries[$frameIndex] StackMapTable frame offset=$frameOffset: " +
                        "must point to the opcode of an instruction",
                )
            }
            previousOffset = frameOffset
        }
    }

    private fun validateStackMapTableUninitializedVariables(
        attributePath: String,
        attribute: AttributeInfo,
        instructionLayout: CodeInstructionLayout,
        code: ByteArray,
    ) {
        if (attribute !is StackMapTableAttribute) {
            return
        }

        attribute.entries.forEachIndexed { frameIndex, frame ->
            val framePath = "$attributePath.entries[$frameIndex]"
            when (frame) {
                is SameLocalsOneStackItemFrame -> validateUninitializedVariable(
                    path = "$framePath.stack[0]",
                    verificationType = frame.stack,
                    instructionLayout = instructionLayout,
                    code = code,
                )
                is SameLocalsOneStackItemFrameExtended -> validateUninitializedVariable(
                    path = "$framePath.stack[0]",
                    verificationType = frame.stack,
                    instructionLayout = instructionLayout,
                    code = code,
                )
                is AppendStackMapFrame -> frame.locals.forEachIndexed { localIndex, local ->
                    validateUninitializedVariable(
                        path = "$framePath.locals[$localIndex]",
                        verificationType = local,
                        instructionLayout = instructionLayout,
                        code = code,
                    )
                }
                is FullStackMapFrame -> {
                    frame.locals.forEachIndexed { localIndex, local ->
                        validateUninitializedVariable(
                            path = "$framePath.locals[$localIndex]",
                            verificationType = local,
                            instructionLayout = instructionLayout,
                            code = code,
                        )
                    }
                    frame.stack.forEachIndexed { stackIndex, stack ->
                        validateUninitializedVariable(
                            path = "$framePath.stack[$stackIndex]",
                            verificationType = stack,
                            instructionLayout = instructionLayout,
                            code = code,
                        )
                    }
                }
                is ChopStackMapFrame,
                is SameStackMapFrame,
                is SameStackMapFrameExtended,
                -> Unit
            }
        }
    }

    private fun validateUninitializedVariable(
        path: String,
        verificationType: VerificationTypeInfo,
        instructionLayout: CodeInstructionLayout,
        code: ByteArray,
    ) {
        if (verificationType !is VerificationTypeInfo.UninitializedVariable) {
            return
        }

        val offset = verificationType.offset
        val isNewInstruction = offset in instructionLayout.instructionOffsets &&
            offset < code.size &&
            (code[offset].toInt() and 0xFF) == 0xBB
        if (!isNewInstruction) {
            throw ClassFileFormatException(
                "Invalid $path Uninitialized_variable_info offset=$offset: must point to a new instruction opcode",
            )
        }
    }

    private fun validateCodeTypeAnnotationTargets(
        attributePath: String,
        attribute: AttributeInfo,
        exceptionTableLength: Int,
        instructionLayout: CodeInstructionLayout,
        codeLength: Int,
        maxLocals: Int,
    ) {
        val annotations = when (attribute) {
            is RuntimeVisibleTypeAnnotationsAttribute -> attribute.annotations
            is RuntimeInvisibleTypeAnnotationsAttribute -> attribute.annotations
            else -> return
        }
        annotations.forEachIndexed { index, annotation ->
            val targetInfo = annotation.targetInfo
            if (targetInfo is TypeAnnotationTargetInfo.CatchTarget &&
                targetInfo.exceptionTableIndex !in 0 until exceptionTableLength
            ) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$index].target_info.exception_table_index=" +
                        "${targetInfo.exceptionTableIndex}: must be less than exception_table_length=$exceptionTableLength",
                )
            }
            if (targetInfo is TypeAnnotationTargetInfo.OffsetTarget &&
                targetInfo.offset !in instructionLayout.instructionOffsets
            ) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$index].target_info.offset=${targetInfo.offset}: " +
                        "must point to the opcode of an instruction",
                )
            }
            if (targetInfo is TypeAnnotationTargetInfo.TypeArgumentTarget &&
                targetInfo.offset !in instructionLayout.instructionOffsets
            ) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$index].target_info.offset=${targetInfo.offset}: " +
                        "must point to the opcode of an instruction",
                )
            }
            if (targetInfo is TypeAnnotationTargetInfo.LocalVarTarget) {
                validateCodeTypeAnnotationLocalVariableTarget(
                    attributePath,
                    index,
                    targetInfo,
                    codeLength,
                    instructionLayout,
                    maxLocals,
                )
            }
        }
    }

    private fun validateCodeTypeAnnotationLocalVariableTarget(
        attributePath: String,
        annotationIndex: Int,
        targetInfo: TypeAnnotationTargetInfo.LocalVarTarget,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
        maxLocals: Int,
    ) {
        targetInfo.table.forEachIndexed { tableIndex, entry ->
            if (entry.index >= maxLocals) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$annotationIndex].target_info.localvar_target.table[$tableIndex]" +
                        ".index=${entry.index}: must be less than max_locals=$maxLocals",
                )
            }
            if (entry.startPc !in instructionLayout.instructionOffsets) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$annotationIndex].target_info.localvar_target.table[$tableIndex]" +
                        ".start_pc=${entry.startPc}: must point to the opcode of an instruction",
                )
            }
            val endPc = entry.startPc + entry.length
            if (endPc > codeLength) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$annotationIndex].target_info.localvar_target.table[$tableIndex] " +
                        "range: start_pc=${entry.startPc} length=${entry.length} exceeds code_length=$codeLength",
                )
            }
            if (endPc != codeLength && endPc !in instructionLayout.instructionOffsets) {
                throw ClassFileFormatException(
                    "Invalid $attributePath.annotations[$annotationIndex].target_info.localvar_target.table[$tableIndex]" +
                        ".end_pc=$endPc: must be code_length=$codeLength or point to the opcode of an instruction",
                )
            }
        }
    }


    private fun requireAbsentAttribute(
        paths: List<String>,
        attributeName: String,
        allowedLocation: String,
        ownerPath: String,
    ) {
        if (paths.isNotEmpty()) {
            throw ClassFileFormatException(
                "Invalid $ownerPath attributes: $attributeName is permitted only in " +
                    "$allowedLocation attributes but found at ${paths.joinToString()}",
            )
        }
    }

    private fun requireAtMostOneAttribute(
        paths: List<String>,
        attributeName: String,
        ownerPath: String,
    ) {
        if (paths.size <= 1) {
            return
        }
        throw ClassFileFormatException(
            "Invalid $ownerPath attributes: at most one $attributeName attribute is permitted " +
                "but found ${paths.size} at ${paths.joinToString()}",
        )
    }
    private fun attributeName(
        context: AttributeParseContext,
        attribute: AttributeInfo,
        role: String,
    ): String {
        val entry = try {
            context.constantPool[attribute.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException("Invalid $role=${attribute.nameIndex}: ${exception.message}")
        }
        if (entry !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $role=${attribute.nameIndex}: expected CONSTANT_Utf8_info but found ${entry.javaClass.simpleName}",
            )
        }
        return entry.value
    }

    private fun parseExceptionTable(
        context: AttributeParseContext,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
    ): List<CodeExceptionHandler> {
        val exceptionTableLength = context.reader.readU2()
        return List(exceptionTableLength) { index ->
            parseExceptionHandler(context, codeLength, instructionLayout, index)
        }
    }

    private fun parseExceptionHandler(
        context: AttributeParseContext,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
        index: Int,
    ): CodeExceptionHandler {
        val ownerPath = "${context.ownerPath}.exception_table[$index]"
        val startPc = context.reader.readU2()
        val endPc = context.reader.readU2()
        val handlerPc = context.reader.readU2()
        val catchTypeIndex = context.reader.readU2()
        validateHandlerRange(ownerPath, codeLength, instructionLayout, startPc, endPc, handlerPc)
        val catchType = validateCatchType(context, ownerPath, catchTypeIndex)
        return CodeExceptionHandler(
            startPc = startPc,
            endPc = endPc,
            handlerPc = handlerPc,
            catchType = catchType,
        )
    }

    private fun validateHandlerRange(
        ownerPath: String,
        codeLength: Int,
        instructionLayout: CodeInstructionLayout,
        startPc: Int,
        endPc: Int,
        handlerPc: Int,
    ) {
        if (startPc >= codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.start_pc=$startPc: must be less than code_length=$codeLength",
            )
        }
        if (endPc > codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.end_pc=$endPc: must be at most code_length=$codeLength",
            )
        }
        if (startPc >= endPc) {
            throw ClassFileFormatException(
                "Invalid $ownerPath range: start_pc=$startPc must be less than end_pc=$endPc",
            )
        }
        if (startPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.start_pc=$startPc: must point to the opcode of an instruction",
            )
        }
        if (endPc != codeLength && endPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.end_pc=$endPc: must be code_length=$codeLength " +
                    "or point to the opcode of an instruction",
            )
        }
        if (handlerPc >= codeLength) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.handler_pc=$handlerPc: must be less than code_length=$codeLength",
            )
        }
        if (handlerPc !in instructionLayout.instructionOffsets) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.handler_pc=$handlerPc: must point to the opcode of an instruction",
            )
        }
    }

    private fun validateCatchType(
        context: AttributeParseContext,
        ownerPath: String,
        catchTypeIndex: Int,
    ): ConstantPoolIndex? {
        if (catchTypeIndex == 0) {
            return null
        }
        val index = ConstantPoolIndex(catchTypeIndex)
        val entry = try {
            context.constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index: expected CONSTANT_Class_info " +
                    "but found ${entry.javaClass.simpleName}",
            )
        }
        val name = try {
            context.constantPool[entry.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index name_index=${entry.nameIndex}: ${exception.message}",
            )
        }
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index name_index=${entry.nameIndex}: " +
                    "expected CONSTANT_Utf8_info but found ${name.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateInternalBinaryName(index, "name_index", name.value)
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.catch_type=$index name_index=${entry.nameIndex}: ${exception.message}",
            )
        }
        return index
    }
}

private object CodeInstructionValidator {
    private val fixedInstructionLengths: IntArray = IntArray(256) { -1 }.also { lengths ->
        fun length(opcode: Int, length: Int) {
            lengths[opcode] = length
        }

        for (opcode in 0x00..0x0F) length(opcode, 1)
        length(0x10, 2)
        length(0x11, 3)
        length(0x12, 2)
        length(0x13, 3)
        length(0x14, 3)
        for (opcode in 0x15..0x19) length(opcode, 2)
        for (opcode in 0x1A..0x35) length(opcode, 1)
        for (opcode in 0x36..0x3A) length(opcode, 2)
        for (opcode in 0x3B..0x83) length(opcode, 1)
        length(0x84, 3)
        for (opcode in 0x85..0x98) length(opcode, 1)
        for (opcode in 0x99..0xA8) length(opcode, 3)
        length(0xA9, 2)
        for (opcode in 0xAC..0xB1) length(opcode, 1)
        for (opcode in 0xB2..0xB8) length(opcode, 3)
        length(0xB9, 5)
        length(0xBA, 5)
        length(0xBB, 3)
        length(0xBC, 2)
        length(0xBD, 3)
        length(0xBE, 1)
        length(0xBF, 1)
        length(0xC0, 3)
        length(0xC1, 3)
        length(0xC2, 1)
        length(0xC3, 1)
        length(0xC5, 4)
        length(0xC6, 3)
        length(0xC7, 3)
        length(0xC8, 5)
        length(0xC9, 5)
    }

    private val wideTwoByteIndexOpcodes = setOf(0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3A, 0xA9)
    private val categoryOneLocalIndexOpcodes = setOf(0x15, 0x17, 0x19, 0x36, 0x38, 0x3A, 0xA9)
    private val categoryTwoLocalIndexOpcodes = setOf(0x16, 0x18, 0x37, 0x39)
    private val discontinuedSubroutineOpcodes = setOf(0xA8, 0xA9, 0xC9)

    fun validate(
        code: ByteArray,
        ownerPath: String,
        constantPool: ConstantPool,
        majorVersion: Int,
        maxLocals: Int,
    ): CodeInstructionLayout {
        val instructionOffsets = mutableSetOf<Int>()
        val modifiedOpcodeOffsets = mutableSetOf<Int>()
        val branchTargets = mutableListOf<BranchTarget>()

        var pc = 0
        while (pc < code.size) {
            instructionOffsets += pc
            val opcode = code.u1(pc)
            val length = instructionLength(
                code = code,
                pc = pc,
                ownerPath = ownerPath,
                constantPool = constantPool,
                majorVersion = majorVersion,
                maxLocals = maxLocals,
                branchTargets = branchTargets,
                modifiedOpcodeOffsets = modifiedOpcodeOffsets,
            )
            if (pc + length > code.size) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] ${mnemonic(opcode)}: " +
                        "truncated instruction length=$length exceeds code_length=${code.size}",
                )
            }
            pc += length
        }

        branchTargets.forEach { target ->
            if (target.offset !in instructionOffsets) {
                val reason = if (target.offset in modifiedOpcodeOffsets) {
                    "points to the opcode operand modified by wide"
                } else {
                    "does not point to an instruction opcode"
                }
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[${target.sourcePc}] ${target.kind} branch target=${target.offset}: $reason",
                )
            }
        }
        return CodeInstructionLayout(instructionOffsets.toSet())
    }

    private fun instructionLength(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        majorVersion: Int,
        maxLocals: Int,
        branchTargets: MutableList<BranchTarget>,
        modifiedOpcodeOffsets: MutableSet<Int>,
    ): Int {
        val opcode = code.u1(pc)
        if (opcode in 0xCA..0xFF) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc]: reserved opcode 0x${opcode.toHex()} must not appear in code arrays",
            )
        }
        if (majorVersion >= 51 && opcode in discontinuedSubroutineOpcodes) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] ${mnemonic(opcode)}: " +
                    "must not appear in class file major version $majorVersion or newer",
            )
        }
        return when (opcode) {
            0xAA -> parseTableSwitch(code, pc, ownerPath, branchTargets)
            0xAB -> parseLookupSwitch(code, pc, ownerPath, branchTargets)
            0xC4 -> parseWide(code, pc, ownerPath, majorVersion, maxLocals, modifiedOpcodeOffsets)
            in categoryOneLocalIndexOpcodes -> {
                ensureAvailable(code, pc, 2, ownerPath, mnemonic(opcode))
                validateLocalVariableIndex(
                    index = code.u1(pc + 1),
                    maxLocals = maxLocals,
                    pc = pc,
                    ownerPath = ownerPath,
                    mnemonic = mnemonic(opcode),
                )
                2
            }
            in categoryTwoLocalIndexOpcodes -> {
                ensureAvailable(code, pc, 2, ownerPath, mnemonic(opcode))
                validateLocalVariableIndex(
                    index = code.u1(pc + 1),
                    maxLocals = maxLocals,
                    requiredSlots = 2,
                    pc = pc,
                    ownerPath = ownerPath,
                    mnemonic = mnemonic(opcode),
                )
                2
            }
            0x84 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateLocalVariableIndex(
                    index = code.u1(pc + 1),
                    maxLocals = maxLocals,
                    pc = pc,
                    ownerPath = ownerPath,
                    mnemonic = mnemonic(opcode),
                )
                3
            }
            in 0x1A..0x1D -> localVariableIndexInstructionLength(opcode, 0x1A, pc, ownerPath, maxLocals)
            in 0x1E..0x21 -> localVariableIndexInstructionLength(opcode, 0x1E, pc, ownerPath, maxLocals, 2)
            in 0x22..0x25 -> localVariableIndexInstructionLength(opcode, 0x22, pc, ownerPath, maxLocals)
            in 0x26..0x29 -> localVariableIndexInstructionLength(opcode, 0x26, pc, ownerPath, maxLocals, 2)
            in 0x2A..0x2D -> localVariableIndexInstructionLength(opcode, 0x2A, pc, ownerPath, maxLocals)
            in 0x3B..0x3E -> localVariableIndexInstructionLength(opcode, 0x3B, pc, ownerPath, maxLocals)
            in 0x3F..0x42 -> localVariableIndexInstructionLength(opcode, 0x3F, pc, ownerPath, maxLocals, 2)
            in 0x43..0x46 -> localVariableIndexInstructionLength(opcode, 0x43, pc, ownerPath, maxLocals)
            in 0x47..0x4A -> localVariableIndexInstructionLength(opcode, 0x47, pc, ownerPath, maxLocals, 2)
            in 0x4B..0x4E -> localVariableIndexInstructionLength(opcode, 0x4B, pc, ownerPath, maxLocals)
            0x12 -> {
                ensureAvailable(code, pc, 2, ownerPath, mnemonic(opcode))
                validateLdcOperand(
                    rawIndex = code.u1(pc + 1),
                    pc = pc,
                    ownerPath = ownerPath,
                    constantPool = constantPool,
                    mnemonic = mnemonic(opcode),
                    requiresCategoryTwo = false,
                )
                2
            }
            0x13 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateLdcOperand(
                    rawIndex = code.u2(pc + 1),
                    pc = pc,
                    ownerPath = ownerPath,
                    constantPool = constantPool,
                    mnemonic = mnemonic(opcode),
                    requiresCategoryTwo = false,
                )
                3
            }
            0x14 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateLdcOperand(
                    rawIndex = code.u2(pc + 1),
                    pc = pc,
                    ownerPath = ownerPath,
                    constantPool = constantPool,
                    mnemonic = mnemonic(opcode),
                    requiresCategoryTwo = true,
                )
                3
            }
            0xBB -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                val className = validateClassReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                if (className.startsWith("[")) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] new: must not reference an array type '$className'",
                    )
                }
                3
            }
            0xBD -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                val className = validateClassReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                if (arrayDimensions(className) >= 255) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] anewarray: must not create an array of more than 255 dimensions",
                    )
                }
                3
            }
            0xBC -> {
                ensureAvailable(code, pc, 2, ownerPath, mnemonic(opcode))
                val atype = code.u1(pc + 1)
                if (atype !in 4..11) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] newarray atype=$atype: expected one of 4..11",
                    )
                }
                2
            }
            in 0xB2..0xB5 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateFieldReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                3
            }
            0xB6 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateMethodReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                3
            }
            0xB7, 0xB8 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateSpecialOrStaticMethodReferenceOperand(
                    code = code,
                    pc = pc,
                    ownerPath = ownerPath,
                    constantPool = constantPool,
                    mnemonic = mnemonic(opcode),
                    majorVersion = majorVersion,
                )
                3
            }
            0xB9 -> {
                ensureAvailable(code, pc, 5, ownerPath, mnemonic(opcode))
                validateInterfaceMethodReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                5
            }
            0xBA -> {
                ensureAvailable(code, pc, 5, ownerPath, mnemonic(opcode))
                validateInvokeDynamicOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                5
            }
            0xC0, 0xC1 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                validateClassReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                3
            }
            0xC5 -> {
                ensureAvailable(code, pc, 4, ownerPath, mnemonic(opcode))
                val className = validateClassReferenceOperand(code, pc, ownerPath, constantPool, mnemonic(opcode))
                val dimensions = code.u1(pc + 3)
                if (dimensions == 0) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] multianewarray: dimensions operand must not be zero",
                    )
                }
                val arrayDimensions = arrayDimensions(className)
                if (dimensions > arrayDimensions) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] multianewarray: dimensions operand $dimensions " +
                            "must not exceed array dimensions $arrayDimensions of '$className'",
                    )
                }
                4
            }
            in 0x99..0xA8, 0xC6, 0xC7 -> {
                ensureAvailable(code, pc, 3, ownerPath, mnemonic(opcode))
                branchTargets += BranchTarget(
                    sourcePc = pc,
                    offset = pc + code.s2(pc + 1),
                    kind = mnemonic(opcode),
                )
                3
            }
            0xC8, 0xC9 -> {
                ensureAvailable(code, pc, 5, ownerPath, mnemonic(opcode))
                branchTargets += BranchTarget(
                    sourcePc = pc,
                    offset = pc + code.s4(pc + 1),
                    kind = mnemonic(opcode),
                )
                5
            }
            else -> {
                val length = fixedInstructionLengths[opcode]
                if (length < 0) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc]: undocumented opcode 0x${opcode.toHex()} must not appear in code arrays",
                    )
                }
                ensureAvailable(code, pc, length, ownerPath, mnemonic(opcode))
                length
            }
        }
    }

    private fun localVariableIndexInstructionLength(
        opcode: Int,
        baseOpcode: Int,
        pc: Int,
        ownerPath: String,
        maxLocals: Int,
        requiredSlots: Int = 1,
    ): Int {
        validateLocalVariableIndex(
            index = opcode - baseOpcode,
            maxLocals = maxLocals,
            requiredSlots = requiredSlots,
            pc = pc,
            ownerPath = ownerPath,
            mnemonic = mnemonic(opcode),
        )
        return 1
    }

    private fun validateLocalVariableIndex(
        index: Int,
        maxLocals: Int,
        requiredSlots: Int = 1,
        pc: Int,
        ownerPath: String,
        mnemonic: String,
    ) {
        if (index > maxLocals - requiredSlots) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic local variable index $index: " +
                    "must be no greater than max_locals=$maxLocals - $requiredSlots",
            )
        }
    }

    private fun validateInvokeDynamicOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
    ) {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        if (entry !is ConstantInvokeDynamicEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected CONSTANT_InvokeDynamic but found ${entry.javaClass.simpleName}",
            )
        }
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "CONSTANT_InvokeDynamic.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InvokeDynamic.name_and_type_index=${entry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val name = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.nameIndex,
            role = "CONSTANT_InvokeDynamic.name_index",
        )
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InvokeDynamic.name_index=${nameAndType.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${name.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateMethodName(
                owner = nameAndType.nameIndex,
                role = "name_index",
                value = name.value,
                allowInit = false,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InvokeDynamic method name_index=${nameAndType.nameIndex}: " +
                    exception.message,
            )
        }
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.descriptorIndex,
            role = "CONSTANT_InvokeDynamic.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InvokeDynamic.descriptor_index=${nameAndType.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = nameAndType.descriptorIndex,
                role = "descriptor_index",
                descriptor = descriptor.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InvokeDynamic.descriptor_index=${nameAndType.descriptorIndex}: " +
                    exception.message,
            )
        }
        val thirdByte = code.u1(pc + 3)
        if (thirdByte != 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic third operand byte must be zero",
            )
        }
        val fourthByte = code.u1(pc + 4)
        if (fourthByte != 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic fourth operand byte must be zero",
            )
        }
    }

    private fun validateInterfaceMethodReferenceOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
    ) {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        if (entry !is ConstantInterfaceMethodRefEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected CONSTANT_InterfaceMethodref but found ${entry.javaClass.simpleName}",
            )
        }
        validateMethodInvocationName(ownerPath, pc, mnemonic, constantPool, index, entry, allowsInstanceInitialization = false)
        val count = code.u1(pc + 3)
        if (count == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic count operand must not be zero",
            )
        }
        val expectedCount = 1 + interfaceMethodParameterUnits(ownerPath, pc, mnemonic, constantPool, index, entry)
        if (count != expectedCount) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic count operand $count: " +
                    "expected descriptor argument slot count $expectedCount",
            )
        }
        val fourthByte = code.u1(pc + 4)
        if (fourthByte != 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic fourth operand byte must be zero",
            )
        }
    }

    private fun interfaceMethodParameterUnits(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantInterfaceMethodRefEntry,
    ): Int {
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "CONSTANT_InterfaceMethodref.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InterfaceMethodref.name_and_type_index=${entry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.descriptorIndex,
            role = "CONSTANT_InterfaceMethodref.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InterfaceMethodref.descriptor_index=${nameAndType.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        return DescriptorValidator.methodParameterUnits(
            owner = nameAndType.descriptorIndex,
            role = "descriptor_index",
            descriptor = descriptor.value,
        )
    }

    private fun validateSpecialOrStaticMethodReferenceOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
        majorVersion: Int,
    ) {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        val valid = if (majorVersion < 52) {
            entry is ConstantMethodRefEntry
        } else {
            entry is ConstantMethodRefEntry || entry is ConstantInterfaceMethodRefEntry
        }
        if (!valid) {
            val expected = if (majorVersion < 52) {
                "CONSTANT_Methodref for class file major version $majorVersion"
            } else {
                "CONSTANT_Methodref or CONSTANT_InterfaceMethodref"
            }
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected $expected but found ${entry.javaClass.simpleName}",
            )
        }
        if (majorVersion < 52 && entry is ConstantInterfaceMethodRefEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_InterfaceMethodref is not permitted before class file major version 52; " +
                    "current major version $majorVersion expects CONSTANT_Methodref",
            )
        }
        if (entry is ConstantMemberRefEntry) {
            validateMethodInvocationName(
                ownerPath = ownerPath,
                pc = pc,
                mnemonic = mnemonic,
                constantPool = constantPool,
                index = index,
                entry = entry,
                allowsInstanceInitialization = mnemonic == "invokespecial",
            )
        }
    }

    private fun validateMethodReferenceOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
    ) {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        if (entry !is ConstantMethodRefEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected CONSTANT_Methodref but found ${entry.javaClass.simpleName}",
            )
        }
        validateMethodInvocationName(ownerPath, pc, mnemonic, constantPool, index, entry, allowsInstanceInitialization = false)
    }

    private fun validateMethodInvocationName(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMemberRefEntry,
        allowsInstanceInitialization: Boolean,
    ) {
        validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, index, entry)
        val methodName = methodReferenceName(ownerPath, pc, mnemonic, constantPool, index, entry)
        validateMethodReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, index, entry)
        if (methodName == "<init>") {
            if (!allowsInstanceInitialization) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] $mnemonic method name <init>: " +
                        "instance initialization methods may only be invoked by invokespecial",
                )
            }
            return
        }
        if (methodName.startsWith("<")) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic method name $methodName: " +
                    "methods whose names begin with '<' must not be called explicitly",
            )
        }
    }

    private fun methodReferenceName(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMemberRefEntry,
    ): String {
        val constantKind = when (entry) {
            is ConstantMethodRefEntry -> "CONSTANT_Methodref"
            is ConstantInterfaceMethodRefEntry -> "CONSTANT_InterfaceMethodref"
            is ConstantFieldRefEntry -> "CONSTANT_Fieldref"
        }
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "$constantKind.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.name_and_type_index=${entry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val name = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.nameIndex,
            role = "$constantKind.name_index",
        )
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.name_index=${nameAndType.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${name.javaClass.simpleName}",
            )
        }
        val methodName = name.value
        validateMethodReferenceNameGrammar(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            index = index,
            nameIndex = nameAndType.nameIndex,
            methodName = methodName,
        )
        return methodName
    }

    private fun validateMethodReferenceNameGrammar(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        index: ConstantPoolIndex,
        nameIndex: ConstantPoolIndex,
        methodName: String,
    ) {
        if (methodName.startsWith("<")) {
            return
        }
        try {
            ClassNameValidator.validateMethodName(
                owner = nameIndex,
                role = "name_index",
                value = methodName,
                allowInit = false,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "method name_index=$nameIndex: ${exception.message}",
            )
        }
    }

    private fun validateMethodReferenceClass(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMemberRefEntry,
    ) {
        val constantKind = when (entry) {
            is ConstantMethodRefEntry -> "CONSTANT_Methodref"
            is ConstantInterfaceMethodRefEntry -> "CONSTANT_InterfaceMethodref"
            is ConstantFieldRefEntry -> "CONSTANT_Fieldref"
        }
        val ownerClass = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.classIndex,
            role = "$constantKind.class_index",
        )
        if (ownerClass !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.class_index=${entry.classIndex} expected CONSTANT_Class_info " +
                    "but found ${ownerClass.javaClass.simpleName}",
            )
        }
        val ownerName = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = ownerClass.nameIndex,
            role = "$constantKind.class_index name_index",
        )
        if (ownerName !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.class_index=${entry.classIndex} name_index=${ownerClass.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${ownerName.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateInternalBinaryName(
                owner = ownerClass.nameIndex,
                role = "name_index",
                value = ownerName.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.class_index=${entry.classIndex} name_index=${ownerClass.nameIndex}: " +
                    exception.message,
            )
        }
    }

    private fun validateFieldReferenceOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
    ) {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        if (entry !is ConstantFieldRefEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected CONSTANT_Fieldref but found ${entry.javaClass.simpleName}",
            )
        }
        validateFieldReferenceClass(ownerPath, pc, mnemonic, constantPool, index, entry)
        validateFieldReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, index, entry)
    }

    private fun validateFieldReferenceClass(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantFieldRefEntry,
    ) {
        val ownerClass = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.classIndex,
            role = "CONSTANT_Fieldref.class_index",
        )
        if (ownerClass !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.class_index=${entry.classIndex} expected CONSTANT_Class_info " +
                    "but found ${ownerClass.javaClass.simpleName}",
            )
        }
        val ownerName = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = ownerClass.nameIndex,
            role = "CONSTANT_Fieldref.class_index name_index",
        )
        if (ownerName !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.class_index=${entry.classIndex} name_index=${ownerClass.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${ownerName.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateInternalBinaryName(
                owner = ownerClass.nameIndex,
                role = "name_index",
                value = ownerName.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.class_index=${entry.classIndex} name_index=${ownerClass.nameIndex}: " +
                    exception.message,
            )
        }
    }

    private fun validateFieldReferenceDescriptor(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantFieldRefEntry,
    ) {
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "CONSTANT_Fieldref.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.name_and_type_index=${entry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val name = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.nameIndex,
            role = "CONSTANT_Fieldref.name_index",
        )
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.name_index=${nameAndType.nameIndex} " +
                    "expected CONSTANT_Utf8_info but found ${name.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateUnqualifiedName(
                owner = nameAndType.nameIndex,
                role = "name_index",
                value = name.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.name_index=${nameAndType.nameIndex}: ${exception.message}",
            )
        }
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.descriptorIndex,
            role = "CONSTANT_Fieldref.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.descriptor_index=${nameAndType.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        try {
            DescriptorValidator.validateFieldDescriptor(
                owner = nameAndType.descriptorIndex,
                role = "descriptor_index",
                descriptor = descriptor.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Fieldref.descriptor_index=${nameAndType.descriptorIndex}: ${exception.message}",
            )
        }
    }

    private fun validateMethodReferenceDescriptor(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMemberRefEntry,
    ): String {
        val constantKind = when (entry) {
            is ConstantMethodRefEntry -> "CONSTANT_Methodref"
            is ConstantInterfaceMethodRefEntry -> "CONSTANT_InterfaceMethodref"
            is ConstantFieldRefEntry -> "CONSTANT_Fieldref"
        }
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "$constantKind.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.name_and_type_index=${entry.nameAndTypeIndex} " +
                    "expected CONSTANT_NameAndType but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.descriptorIndex,
            role = "$constantKind.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.descriptor_index=${nameAndType.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = nameAndType.descriptorIndex,
                role = "descriptor_index",
                descriptor = descriptor.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "$constantKind.descriptor_index=${nameAndType.descriptorIndex}: ${exception.message}",
            )
        }
        return descriptor.value
    }

    private fun validateLdcOperand(
        rawIndex: Int,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
        requiresCategoryTwo: Boolean,
    ) {
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = loadConstantPoolEntry(ownerPath, pc, mnemonic, constantPool, index)
        val category = ldcCategory(entry, ownerPath, pc, mnemonic, constantPool, index)
        if (requiresCategoryTwo && category != LdcCategory.CategoryTwo) {
            if (entry is ConstantDynamicEntry) {
                val descriptor = dynamicConstantDescriptor(ownerPath, pc, mnemonic, constantPool, index, entry)
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                        "CONSTANT_Dynamic descriptor $descriptor must be J or D",
                )
            }
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected long or double loadable constant but found ${entry.javaClass.simpleName}",
            )
        }
        if (!requiresCategoryTwo && category == LdcCategory.CategoryTwo) {
            if (entry is ConstantDynamicEntry) {
                val descriptor = dynamicConstantDescriptor(ownerPath, pc, mnemonic, constantPool, index, entry)
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                        "CONSTANT_Dynamic descriptor $descriptor must not be J or D",
                )
            }
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "must not reference a long or double constant",
            )
        }
    }

    private fun ldcCategory(
        entry: ConstantPoolEntry,
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
    ): LdcCategory =
        when (entry) {
            is ConstantIntegerEntry,
            is ConstantFloatEntry,
            is ConstantStringEntry,
            is ConstantClassEntry,
            -> LdcCategory.CategoryOne

            is ConstantMethodTypeEntry -> {
                validateMethodTypeLoadableConstant(ownerPath, pc, mnemonic, constantPool, index, entry)
                LdcCategory.CategoryOne
            }

            is ConstantMethodHandleEntry -> {
                validateMethodHandleLoadableConstant(ownerPath, pc, mnemonic, constantPool, index, entry)
                LdcCategory.CategoryOne
            }

            is ConstantLongEntry,
            is ConstantDoubleEntry,
            -> LdcCategory.CategoryTwo

            is ConstantDynamicEntry -> {
                val descriptor = dynamicConstantDescriptor(ownerPath, pc, mnemonic, constantPool, index, entry)
                if (descriptor == "J" || descriptor == "D") {
                    LdcCategory.CategoryTwo
                } else {
                    LdcCategory.CategoryOne
                }
            }

            else -> throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected loadable constant but found ${entry.javaClass.simpleName}",
            )
        }

    private fun validateMethodTypeLoadableConstant(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMethodTypeEntry,
    ) {
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.descriptorIndex,
            role = "CONSTANT_MethodType.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_MethodType.descriptor_index=${entry.descriptorIndex} " +
                    "expected CONSTANT_Utf8_info but found ${descriptor.javaClass.simpleName}",
            )
        }
        try {
            DescriptorValidator.validateMethodDescriptor(
                owner = entry.descriptorIndex,
                role = "descriptor_index",
                descriptor = descriptor.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_MethodType.descriptor_index=${entry.descriptorIndex}: ${exception.message}",
            )
        }
    }

    private fun validateMethodHandleLoadableConstant(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantMethodHandleEntry,
    ) {
        when (entry.referenceKind) {
            MethodHandleReferenceKind.GetField,
            MethodHandleReferenceKind.GetStatic,
            MethodHandleReferenceKind.PutField,
            MethodHandleReferenceKind.PutStatic,
            -> {
                val reference = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = entry.referenceIndex,
                    role = "CONSTANT_MethodHandle.reference_index",
                )
                if (reference !is ConstantFieldRefEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "expected CONSTANT_Fieldref but found ${reference.javaClass.simpleName}",
                    )
                }
                validateFieldReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                validateFieldReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
            }

            MethodHandleReferenceKind.InvokeVirtual -> {
                val reference = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = entry.referenceIndex,
                    role = "CONSTANT_MethodHandle.reference_index",
                )
                if (reference !is ConstantMethodRefEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "expected CONSTANT_Methodref but found ${reference.javaClass.simpleName}",
                    )
                }
                validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                validateMethodReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                validateOrdinaryMethodHandleTargetName(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = index,
                    referenceKind = entry.referenceKind,
                    referenceIndex = entry.referenceIndex,
                    nameAndTypeIndex = reference.nameAndTypeIndex,
                )
            }

            MethodHandleReferenceKind.InvokeStatic,
            MethodHandleReferenceKind.InvokeSpecial,
            -> {
                val reference = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = entry.referenceIndex,
                    role = "CONSTANT_MethodHandle.reference_index",
                )
                if (reference !is ConstantMethodRefEntry && reference !is ConstantInterfaceMethodRefEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "expected CONSTANT_Methodref or CONSTANT_InterfaceMethodref " +
                            "but found ${reference.javaClass.simpleName}",
                    )
                }
                val nameAndTypeIndex = when (reference) {
                    is ConstantMethodRefEntry -> {
                        validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                        validateMethodReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                        reference.nameAndTypeIndex
                    }
                    is ConstantInterfaceMethodRefEntry -> {
                        validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                        validateMethodReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                        reference.nameAndTypeIndex
                    }
                }
                validateOrdinaryMethodHandleTargetName(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = index,
                    referenceKind = entry.referenceKind,
                    referenceIndex = entry.referenceIndex,
                    nameAndTypeIndex = nameAndTypeIndex,
                )
            }

            MethodHandleReferenceKind.InvokeInterface -> {
                val reference = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = entry.referenceIndex,
                    role = "CONSTANT_MethodHandle.reference_index",
                )
                if (reference !is ConstantInterfaceMethodRefEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "expected CONSTANT_InterfaceMethodref but found ${reference.javaClass.simpleName}",
                    )
                }
                validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                validateMethodReferenceDescriptor(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                validateOrdinaryMethodHandleTargetName(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = index,
                    referenceKind = entry.referenceKind,
                    referenceIndex = entry.referenceIndex,
                    nameAndTypeIndex = reference.nameAndTypeIndex,
                )
            }

            MethodHandleReferenceKind.NewInvokeSpecial -> {
                val reference = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = entry.referenceIndex,
                    role = "CONSTANT_MethodHandle.reference_index",
                )
                if (reference !is ConstantMethodRefEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "expected CONSTANT_Methodref but found ${reference.javaClass.simpleName}",
                    )
                }
                validateMethodReferenceClass(ownerPath, pc, mnemonic, constantPool, entry.referenceIndex, reference)
                val descriptor = validateMethodReferenceDescriptor(
                    ownerPath,
                    pc,
                    mnemonic,
                    constantPool,
                    entry.referenceIndex,
                    reference,
                )
                if (!descriptor.endsWith("V")) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "reference_kind NewInvokeSpecial descriptor $descriptor must return void",
                    )
                }
                val nameAndType = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = reference.nameAndTypeIndex,
                    role = "CONSTANT_MethodHandle.reference_index name_and_type_index",
                )
                if (nameAndType !is ConstantNameAndTypeEntry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "name_and_type_index=${reference.nameAndTypeIndex} expected CONSTANT_NameAndType " +
                            "but found ${nameAndType.javaClass.simpleName}",
                    )
                }
                val name = loadConstantPoolEntry(
                    ownerPath = ownerPath,
                    pc = pc,
                    mnemonic = mnemonic,
                    constantPool = constantPool,
                    index = nameAndType.nameIndex,
                    role = "CONSTANT_MethodHandle.reference_index name_index",
                )
                if (name !is ConstantUtf8Entry) {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "CONSTANT_MethodHandle.reference_index=${entry.referenceIndex} " +
                            "name_index=${nameAndType.nameIndex} expected CONSTANT_Utf8_info " +
                            "but found ${name.javaClass.simpleName}",
                    )
                }
                if (name.value != "<init>") {
                    throw ClassFileFormatException(
                        "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                            "reference_kind NewInvokeSpecial must target <init> but found ${name.value}",
                    )
                }
            }
        }
    }

    private fun validateOrdinaryMethodHandleTargetName(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        referenceKind: MethodHandleReferenceKind,
        referenceIndex: ConstantPoolIndex,
        nameAndTypeIndex: ConstantPoolIndex,
    ) {
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndTypeIndex,
            role = "CONSTANT_MethodHandle.reference_index name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_MethodHandle.reference_index=$referenceIndex " +
                    "name_and_type_index=$nameAndTypeIndex expected CONSTANT_NameAndType " +
                    "but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val name = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.nameIndex,
            role = "CONSTANT_MethodHandle.reference_index name_index",
        )
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_MethodHandle.reference_index=$referenceIndex " +
                    "name_index=${nameAndType.nameIndex} expected CONSTANT_Utf8_info " +
                    "but found ${name.javaClass.simpleName}",
            )
        }
        if (name.value == "<init>" || name.value == "<clinit>") {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "reference_kind $referenceKind must not target ${name.value}",
            )
        }
        try {
            ClassNameValidator.validateMethodName(
                owner = nameAndType.nameIndex,
                role = "name_index",
                value = name.value,
                allowInit = false,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "reference_kind $referenceKind reference_index=$referenceIndex " +
                    "method name_index=${nameAndType.nameIndex}: ${exception.message}",
            )
        }
    }

    private fun dynamicConstantDescriptor(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        entry: ConstantDynamicEntry,
    ): String {
        val nameAndType = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = entry.nameAndTypeIndex,
            role = "CONSTANT_Dynamic.name_and_type_index",
        )
        if (nameAndType !is ConstantNameAndTypeEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Dynamic.name_and_type_index=${entry.nameAndTypeIndex} expected CONSTANT_NameAndType " +
                    "but found ${nameAndType.javaClass.simpleName}",
            )
        }
        val name = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.nameIndex,
            role = "CONSTANT_Dynamic.name_index",
        )
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Dynamic.name_index=${nameAndType.nameIndex} expected CONSTANT_Utf8_info " +
                    "but found ${name.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateUnqualifiedName(
                owner = nameAndType.nameIndex,
                role = "name_index",
                value = name.value,
            )
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Dynamic.name_index=${nameAndType.nameIndex}: " +
                    exception.message,
            )
        }
        if (name.value == "<init>" || name.value == "<clinit>") {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Dynamic.name_index=${nameAndType.nameIndex}: " +
                    "dynamic constant name ${name.value} is not permitted",
            )
        }
        val descriptor = loadConstantPoolEntry(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = mnemonic,
            constantPool = constantPool,
            index = nameAndType.descriptorIndex,
            role = "CONSTANT_Dynamic.descriptor_index",
        )
        if (descriptor !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "CONSTANT_Dynamic.descriptor_index=${nameAndType.descriptorIndex} expected CONSTANT_Utf8_info " +
                    "but found ${descriptor.javaClass.simpleName}",
            )
        }
        DescriptorValidator.validateFieldDescriptor(nameAndType.descriptorIndex, "descriptor_index", descriptor.value)
        return descriptor.value
    }

    private fun loadConstantPoolEntry(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        constantPool: ConstantPool,
        index: ConstantPoolIndex,
        role: String = "constant_pool index",
    ): ConstantPoolEntry =
        try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic $role $index: ${exception.message}",
            )
        }

    private fun validateClassReferenceOperand(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        constantPool: ConstantPool,
        mnemonic: String,
    ): String {
        val rawIndex = code.u2(pc + 1)
        if (rawIndex == 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index #0: zero is not allowed",
            )
        }
        val index = ConstantPoolIndex(rawIndex)
        val entry = try {
            constantPool[index]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: ${exception.message}",
            )
        }
        if (entry !is ConstantClassEntry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic constant_pool index $index: " +
                    "expected CONSTANT_Class but found ${entry.javaClass.simpleName}",
            )
        }
        val name = try {
            constantPool[entry.nameIndex]
        } catch (exception: ConstantPoolFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic CONSTANT_Class.name_index=${entry.nameIndex}: " +
                    exception.message,
            )
        }
        if (name !is ConstantUtf8Entry) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic CONSTANT_Class.name_index=${entry.nameIndex}: " +
                    "expected CONSTANT_Utf8_info but found ${name.javaClass.simpleName}",
            )
        }
        try {
            ClassNameValidator.validateConstantClassName(index, "name_index", name.value)
        } catch (exception: ClassFileFormatException) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic CONSTANT_Class.name_index=${entry.nameIndex}: " +
                    exception.message,
            )
        }
        return name.value
    }

    private fun arrayDimensions(className: String): Int =
        className.takeWhile { it == '[' }.length

    private fun parseTableSwitch(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        branchTargets: MutableList<BranchTarget>,
    ): Int {
        val padding = switchPadding(pc)
        val operands = pc + 1 + padding
        ensureAvailable(code, pc, 1 + padding + 12, ownerPath, "tableswitch")
        val defaultOffset = code.s4(operands)
        val low = code.s4(operands + 4)
        val high = code.s4(operands + 8)
        if (low > high) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] tableswitch: low=$low must be less than or equal to high=$high",
            )
        }
        val entryCount = high.toLong() - low.toLong() + 1L
        if (entryCount > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] tableswitch: jump table entry count $entryCount is too large",
            )
        }
        val length = switchLength(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = "tableswitch",
            value = 1L + padding + 12L + entryCount * 4L,
        )
        ensureAvailable(code, pc, length, ownerPath, "tableswitch")
        branchTargets += BranchTarget(pc, pc + defaultOffset, "tableswitch default")
        repeat(entryCount.toInt()) { index ->
            branchTargets += BranchTarget(
                sourcePc = pc,
                offset = pc + code.s4(operands + 12 + index * 4),
                kind = "tableswitch",
            )
        }
        return length
    }

    private fun parseLookupSwitch(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        branchTargets: MutableList<BranchTarget>,
    ): Int {
        val padding = switchPadding(pc)
        val operands = pc + 1 + padding
        ensureAvailable(code, pc, 1 + padding + 8, ownerPath, "lookupswitch")
        val defaultOffset = code.s4(operands)
        val pairs = code.s4(operands + 4)
        if (pairs < 0) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] lookupswitch: npairs=$pairs must be non-negative",
            )
        }
        val length = switchLength(
            ownerPath = ownerPath,
            pc = pc,
            mnemonic = "lookupswitch",
            value = 1L + padding + 8L + pairs.toLong() * 8L,
        )
        ensureAvailable(code, pc, length, ownerPath, "lookupswitch")
        var previousMatch: Int? = null
        branchTargets += BranchTarget(pc, pc + defaultOffset, "lookupswitch default")
        repeat(pairs) { index ->
            val pairOffset = operands + 8 + index * 8
            val match = code.s4(pairOffset)
            if (previousMatch?.let { match <= it } == true) {
                throw ClassFileFormatException(
                    "Invalid $ownerPath.code[$pc] lookupswitch: match-offset pairs must be sorted in increasing order",
                )
            }
            previousMatch = match
            branchTargets += BranchTarget(
                sourcePc = pc,
                offset = pc + code.s4(pairOffset + 4),
                kind = "lookupswitch",
            )
        }
        return length
    }

    private fun parseWide(
        code: ByteArray,
        pc: Int,
        ownerPath: String,
        majorVersion: Int,
        maxLocals: Int,
        modifiedOpcodeOffsets: MutableSet<Int>,
    ): Int {
        ensureAvailable(code, pc, 2, ownerPath, "wide")
        val modifiedOpcode = code.u1(pc + 1)
        if (majorVersion >= 51 && modifiedOpcode == 0xA9) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] wide ret: " +
                    "ret must not appear in class file major version $majorVersion or newer",
            )
        }
        modifiedOpcodeOffsets += pc + 1
        return when (modifiedOpcode) {
            in wideTwoByteIndexOpcodes -> {
                ensureAvailable(code, pc, 4, ownerPath, "wide")
                if (modifiedOpcode in categoryOneLocalIndexOpcodes) {
                    validateLocalVariableIndex(
                        index = code.u2(pc + 2),
                        maxLocals = maxLocals,
                        pc = pc,
                        ownerPath = ownerPath,
                        mnemonic = "wide ${mnemonic(modifiedOpcode)}",
                    )
                } else if (modifiedOpcode in categoryTwoLocalIndexOpcodes) {
                    validateLocalVariableIndex(
                        index = code.u2(pc + 2),
                        maxLocals = maxLocals,
                        requiredSlots = 2,
                        pc = pc,
                        ownerPath = ownerPath,
                        mnemonic = "wide ${mnemonic(modifiedOpcode)}",
                    )
                }
                4
            }
            0x84 -> {
                ensureAvailable(code, pc, 6, ownerPath, "wide")
                validateLocalVariableIndex(
                    index = code.u2(pc + 2),
                    maxLocals = maxLocals,
                    pc = pc,
                    ownerPath = ownerPath,
                    mnemonic = "wide ${mnemonic(modifiedOpcode)}",
                )
                6
            }
            else -> throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] wide: unsupported modified opcode 0x${modifiedOpcode.toHex()}",
            )
        }
    }

    private fun switchLength(
        ownerPath: String,
        pc: Int,
        mnemonic: String,
        value: Long,
    ): Int {
        if (value > Int.MAX_VALUE) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic: instruction length $value is too large",
            )
        }
        return value.toInt()
    }

    private fun ensureAvailable(
        code: ByteArray,
        pc: Int,
        length: Int,
        ownerPath: String,
        mnemonic: String,
    ) {
        if (pc + length > code.size) {
            throw ClassFileFormatException(
                "Invalid $ownerPath.code[$pc] $mnemonic: " +
                    "truncated instruction length=$length exceeds code_length=${code.size}",
            )
        }
    }

    private fun switchPadding(pc: Int): Int =
        (4 - ((pc + 1) and 0x03)) and 0x03

    private fun mnemonic(opcode: Int): String =
        when (opcode) {
            0x12 -> "ldc"
            0x13 -> "ldc_w"
            0x14 -> "ldc2_w"
            0x11 -> "sipush"
            0x15 -> "iload"
            0x16 -> "lload"
            0x17 -> "fload"
            0x18 -> "dload"
            0x19 -> "aload"
            in 0x1A..0x1D -> "iload_${opcode - 0x1A}"
            in 0x1E..0x21 -> "lload_${opcode - 0x1E}"
            in 0x22..0x25 -> "fload_${opcode - 0x22}"
            in 0x26..0x29 -> "dload_${opcode - 0x26}"
            in 0x2A..0x2D -> "aload_${opcode - 0x2A}"
            0x36 -> "istore"
            0x37 -> "lstore"
            0x38 -> "fstore"
            0x39 -> "dstore"
            0x3A -> "astore"
            in 0x3B..0x3E -> "istore_${opcode - 0x3B}"
            in 0x3F..0x42 -> "lstore_${opcode - 0x3F}"
            in 0x43..0x46 -> "fstore_${opcode - 0x43}"
            in 0x47..0x4A -> "dstore_${opcode - 0x47}"
            in 0x4B..0x4E -> "astore_${opcode - 0x4B}"
            0x84 -> "iinc"
            0xA8 -> "jsr"
            0xA9 -> "ret"
            0xB2 -> "getstatic"
            0xB3 -> "putstatic"
            0xB4 -> "getfield"
            0xB5 -> "putfield"
            0xB6 -> "invokevirtual"
            0xB7 -> "invokespecial"
            0xB8 -> "invokestatic"
            0xB9 -> "invokeinterface"
            0xBA -> "invokedynamic"
            0xAA -> "tableswitch"
            0xAB -> "lookupswitch"
            0xBB -> "new"
            0xBC -> "newarray"
            0xBD -> "anewarray"
            0xC4 -> "wide"
            0xC5 -> "multianewarray"
            0xC0 -> "checkcast"
            0xC1 -> "instanceof"
            0xC9 -> "jsr_w"
            else -> "opcode 0x${opcode.toHex()}"
        }

    private fun ByteArray.u1(offset: Int): Int =
        this[offset].toInt() and 0xFF

    private fun ByteArray.s2(offset: Int): Int =
        (u1(offset).toShortish() shl 8) or u1(offset + 1)

    private fun ByteArray.u2(offset: Int): Int =
        (u1(offset) shl 8) or u1(offset + 1)

    private fun ByteArray.s4(offset: Int): Int =
        (u1(offset) shl 24) or (u1(offset + 1) shl 16) or (u1(offset + 2) shl 8) or u1(offset + 3)

    private fun Int.toShortish(): Int =
        if (this and 0x80 != 0) this or 0xFFFFFF00.toInt() else this

    private fun Int.toHex(): String =
        toString(16).padStart(2, '0')

    private data class BranchTarget(
        val sourcePc: Int,
        val offset: Int,
        val kind: String,
    )

    private enum class LdcCategory {
        CategoryOne,
        CategoryTwo,
    }
}

private data class CodeInstructionLayout(
    val instructionOffsets: Set<Int>,
)
