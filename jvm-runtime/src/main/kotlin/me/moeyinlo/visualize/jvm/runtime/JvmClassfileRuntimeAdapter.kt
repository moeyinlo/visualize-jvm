package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.ClassAccessFlag
import me.moeyinlo.visualize.jvm.classfile.ClassFile
import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.CodeExceptionHandler
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolFormatException
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.ConstantValueAttribute
import me.moeyinlo.visualize.jvm.classfile.FieldInfo
import me.moeyinlo.visualize.jvm.classfile.LineNumberTableAttribute
import me.moeyinlo.visualize.jvm.classfile.MethodInfo
import me.moeyinlo.visualize.jvm.classfile.SourceFileAttribute

fun ClassFile.toJvmClassDefinition(): JvmClassDefinition {
    val sourceFile = attributes.filterIsInstance<SourceFileAttribute>().singleOrNull()
        ?.let { attribute -> constantPool.utf8(attribute.sourceFileIndex) }
    return JvmClassDefinition(
        internalName = constantPool.className(identity.thisClassIndex),
        superclassName = identity.superClassIndex?.let(constantPool::className),
        interfaceNames = identity.interfaceIndexes.map(constantPool::className),
        fields = fields.map { field -> field.toJvmFieldDefinition(constantPool) },
        methods = methods.map { method -> method.toJvmMethodDefinition(constantPool, sourceFile) },
        isInterface = accessFlags.has(ClassAccessFlag.Interface),
    )
}

fun ClassFile.toJvmMethodAreaEntry(): JvmMethodAreaEntry =
    JvmMethodAreaEntry(definition = toJvmClassDefinition())

private fun FieldInfo.toJvmFieldDefinition(constantPool: ConstantPool): JvmFieldDefinition =
    JvmFieldDefinition(
        name = constantPool.utf8(nameIndex),
        descriptor = constantPool.utf8(descriptorIndex),
        isStatic = has(FieldAccessFlag.Static),
        isPrivate = has(FieldAccessFlag.Private),
        isPackagePrivate = !has(FieldAccessFlag.Public) && !has(FieldAccessFlag.Private) && !has(FieldAccessFlag.Protected),
        isProtected = has(FieldAccessFlag.Protected),
        constantValue = constantValue(constantPool),
    )

private fun FieldInfo.constantValue(constantPool: ConstantPool): JvmFieldConstantValue? {
    val attribute = attributes.filterIsInstance<ConstantValueAttribute>().singleOrNull()
        ?: return null
    return when (val entry = constantPool[attribute.constantValueIndex]) {
        is ConstantIntegerEntry -> JvmFieldConstantValue.Numeric(JvmIntValue(entry.value))
        is ConstantFloatEntry -> JvmFieldConstantValue.Numeric(JvmFloatValue(entry.value))
        is ConstantLongEntry -> JvmFieldConstantValue.Numeric(JvmLongValue(entry.value))
        is ConstantDoubleEntry -> JvmFieldConstantValue.Numeric(JvmDoubleValue(entry.value))
        is ConstantStringEntry -> JvmFieldConstantValue.StringLiteral(constantPool.utf8(entry.stringIndex))
        else -> throw JvmClassfileRuntimeAdapterException(
            "Invalid ConstantValue entry ${attribute.constantValueIndex}: found ${entry.javaClass.simpleName}",
        )
    }
}

private fun MethodInfo.toJvmMethodDefinition(
    constantPool: ConstantPool,
    sourceFile: String?,
): JvmMethodDefinition {
    val codeAttribute = attributes.filterIsInstance<CodeAttribute>().singleOrNull()
    return JvmMethodDefinition(
        name = constantPool.utf8(nameIndex),
        descriptor = constantPool.utf8(descriptorIndex),
        isStatic = has(MethodAccessFlag.Static),
        isPrivate = has(MethodAccessFlag.Private),
        isPackagePrivate = !has(MethodAccessFlag.Public) && !has(MethodAccessFlag.Private) && !has(MethodAccessFlag.Protected),
        isProtected = has(MethodAccessFlag.Protected),
        isAbstract = has(MethodAccessFlag.Abstract),
        isNative = has(MethodAccessFlag.Native),
        isVarargs = has(MethodAccessFlag.Varargs),
        code = codeAttribute?.code,
        maxStack = codeAttribute?.maxStack ?: 0,
        maxLocals = codeAttribute?.maxLocals ?: 0,
        exceptionHandlers = codeAttribute?.exceptionTable?.map { handler ->
            handler.toJvmExceptionHandler(constantPool)
        } ?: emptyList(),
        sourceFile = sourceFile,
        lineNumberTable = codeAttribute?.attributes
            ?.filterIsInstance<LineNumberTableAttribute>()
            ?.singleOrNull()
            ?.entries
            ?.map { entry ->
                JvmLineNumberTableEntry(
                    startPc = entry.startPc,
                    lineNumber = entry.lineNumber,
                )
            } ?: emptyList(),
    )
}

private fun CodeExceptionHandler.toJvmExceptionHandler(constantPool: ConstantPool): JvmExceptionHandler =
    JvmExceptionHandler(
        startPc = startPc,
        endPc = endPc,
        handlerPc = handlerPc,
        catchClassName = catchType?.let(constantPool::className),
    )

private fun ConstantPool.className(index: ConstantPoolIndex): String {
    val classEntry = getEntry<ConstantClassEntry>(index, "class reference")
    return utf8(classEntry.nameIndex)
}

private fun ConstantPool.utf8(index: ConstantPoolIndex): String =
    getEntry<ConstantUtf8Entry>(index, "UTF-8 reference").value

private inline fun <reified T> ConstantPool.getEntry(index: ConstantPoolIndex, role: String): T {
    val entry = try {
        this[index]
    } catch (exception: ConstantPoolFormatException) {
        throw JvmClassfileRuntimeAdapterException("Invalid $role $index: ${exception.message}")
    }
    return entry as? T
        ?: throw JvmClassfileRuntimeAdapterException(
            "Invalid $role $index: expected ${T::class.simpleName} but found ${entry.javaClass.simpleName}",
        )
}

private fun FieldInfo.has(flag: FieldAccessFlag): Boolean = accessFlags and flag.mask != 0

private fun MethodInfo.has(flag: MethodAccessFlag): Boolean = accessFlags and flag.mask != 0

private enum class FieldAccessFlag(val mask: Int) {
    Public(0x0001),
    Private(0x0002),
    Protected(0x0004),
    Static(0x0008),
}

private enum class MethodAccessFlag(val mask: Int) {
    Public(0x0001),
    Private(0x0002),
    Protected(0x0004),
    Static(0x0008),
    Varargs(0x0080),
    Native(0x0100),
    Abstract(0x0400),
}

class JvmClassfileRuntimeAdapterException(message: String) : IllegalStateException(message)
