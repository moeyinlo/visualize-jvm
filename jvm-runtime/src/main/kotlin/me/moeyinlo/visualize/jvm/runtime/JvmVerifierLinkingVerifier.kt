package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.StackMapTableAttribute
import me.moeyinlo.visualize.jvm.verifier.JvmClassVerificationRequest
import me.moeyinlo.visualize.jvm.verifier.JvmClassVerifier
import me.moeyinlo.visualize.jvm.verifier.JvmMethodVerificationRequest

class JvmVerifierLinkingVerifier(
    private val classVerifier: JvmClassVerifier = JvmClassVerifier(),
) : JvmLinkingVerifier {
    override fun verify(definition: JvmClassDefinition) {
        classVerifier.verify(
            JvmClassVerificationRequest(
                className = definition.internalName,
                majorVersion = definition.majorVersion,
                methods = definition.methods.map { method -> method.toVerifierRequest() },
            ),
        )
    }

    private fun JvmMethodDefinition.toVerifierRequest(): JvmMethodVerificationRequest =
        JvmMethodVerificationRequest(
            name = name,
            descriptor = descriptor,
            isStatic = isStatic,
            isAbstract = isAbstract,
            isNative = isNative,
            code = toCodeAttribute(),
            constantPool = constantPool,
            hasStackMapTable = hasStackMapTable,
        )


    private fun JvmMethodDefinition.stackMapTableAttribute(): List<StackMapTableAttribute> =
        if (hasStackMapTable) {
            listOf(StackMapTableAttribute(nameIndex = ConstantPoolIndex(1), entries = stackMapTableEntries))
        } else {
            emptyList()
        }

    private fun JvmMethodDefinition.toCodeAttribute(): CodeAttribute? {
        val methodCode = code ?: return null
        return CodeAttribute(
            nameIndex = ConstantPoolIndex(1),
            maxStack = maxStack,
            maxLocals = maxLocals,
            code = methodCode,
            attributes = stackMapTableAttribute(),
        )
    }
}
