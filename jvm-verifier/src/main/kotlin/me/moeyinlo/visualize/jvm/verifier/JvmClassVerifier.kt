package me.moeyinlo.visualize.jvm.verifier

import me.moeyinlo.visualize.jvm.classfile.CodeAttribute
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.StackMapTableAttribute

class JvmClassVerifier {
    fun verify(request: JvmClassVerificationRequest): JvmClassVerificationResult {
        val strategy = JvmVerificationStrategySelector.select(
            JvmVerificationInput(
                majorVersion = request.majorVersion,
                hasStackMapTable = request.methods.any { method -> method.hasStackMapTable },
            ),
        )
        val verifiedMethods = mutableListOf<String>()

        request.methods.forEach { method ->
            val code = method.code
            if (code == null) {
                if (!method.isAbstract && !method.isNative) {
                    throw JvmClassVerificationException(
                        className = request.className,
                        methodSignature = method.signature,
                        cause = MethodVerificationException("Non-abstract, non-native method has no Code attribute"),
                    )
                }
                return@forEach
            }
            try {
                verifyMethod(request, method, code)
            } catch (exception: RuntimeException) {
                throw JvmClassVerificationException(
                    className = request.className,
                    methodSignature = method.signature,
                    cause = exception,
                )
            }
            verifiedMethods += method.signature
        }

        return JvmClassVerificationResult(
            className = request.className,
            strategy = strategy,
            verifiedMethodSignatures = verifiedMethods,
        )
    }

    private fun verifyMethod(
        request: JvmClassVerificationRequest,
        method: JvmMethodVerificationRequest,
        code: CodeAttribute,
    ) {
        val initialFrame = initialFrame(request.className, method, code.maxLocals)
        val stackMapFrames = code.attributes
            .filterIsInstance<StackMapTableAttribute>()
            .flatMap { attribute -> StackMapFrameExpander.expand(initialFrame.locals, attribute.entries) }
        if (method.constantPool == null) {
            MethodTypeCheckingVerifier.verify(
                code = code,
                initialFrame = initialFrame,
                throwableType = VerificationType.ObjectType(ConstantPoolIndex(1)),
                frameStates = stackMapFrames,
            )
        } else {
            MethodTypeCheckingVerifier.verify(
                code = code,
                constantPool = method.constantPool,
                initialFrame = initialFrame,
                throwableType = VerificationType.ObjectType(ConstantPoolIndex(1)),
                frameStates = stackMapFrames,
            )
        }
    }

    private fun initialFrame(
        className: String,
        method: JvmMethodVerificationRequest,
        maxLocals: Int,
    ): MethodInitialFrame =
        when {
            method.isStatic -> MethodInitialFrameBuilder.buildStatic(method.descriptor, maxLocals)
            method.name == "<init>" && className == "java/lang/Object" -> {
                MethodInitialFrameBuilder.buildObjectConstructor(method.descriptor, maxLocals)
            }
            method.name == "<init>" -> MethodInitialFrameBuilder.buildSubclassConstructor(method.descriptor, maxLocals)
            else -> MethodInitialFrameBuilder.buildInstance(className, descriptor = method.descriptor, maxLocals = maxLocals)
        }
}

data class JvmClassVerificationRequest(
    val className: String,
    val majorVersion: Int,
    val methods: List<JvmMethodVerificationRequest> = emptyList(),
) {
    init {
        require(className.isNotBlank()) { "class name must not be blank" }
        require(majorVersion > 0) { "classfile major_version must be positive: $majorVersion" }
    }
}

data class JvmMethodVerificationRequest(
    val name: String,
    val descriptor: String,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val isNative: Boolean = false,
    val code: CodeAttribute? = null,
    val constantPool: ConstantPool? = null,
    val hasStackMapTable: Boolean = code?.attributes?.any { attribute -> attribute is StackMapTableAttribute } ?: false,
) {
    init {
        require(name.isNotBlank()) { "method name must not be blank" }
        require(descriptor.isNotBlank()) { "method descriptor must not be blank" }
        require(!(code != null && (isAbstract || isNative))) {
            "abstract or native method $name$descriptor must not declare a Code attribute"
        }
    }

    val signature: String = "$name$descriptor"
}

data class JvmClassVerificationResult(
    val className: String,
    val strategy: JvmVerificationStrategy,
    val verifiedMethodSignatures: List<String>,
)

class JvmClassVerificationException(
    val className: String,
    val methodSignature: String,
    override val cause: RuntimeException,
) : RuntimeException("Verification failed for $className.$methodSignature: ${cause.message}", cause)
