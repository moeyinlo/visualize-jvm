package me.moeyinlo.visualize.jvm.verifier

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

enum class VerifierRuleCoverageStatus {
    RuleTested,
    InternalSupport,
    NotYetImplemented,
}

data class VerifierRuleCoverageEntry(
    val sourceFile: String,
    val specArea: String,
    val status: VerifierRuleCoverageStatus,
    val coveringTestClass: String?,
)

object VerifierRuleCoverage {
    private val sourceDirectory = verifierPath("src/main/kotlin/me/moeyinlo/visualize/jvm/verifier")
    private val testDirectory = verifierPath("src/test/kotlin/me/moeyinlo/visualize/jvm/verifier")
    private val alternateCoveringTests: Map<String, String> = mapOf(
        "LdcInstructionVerifier" to "MethodTypeCheckingVerifierTest",
    )

    private val internalSupportFiles: Set<String> = setOf(
        "VerificationType",
        "VerificationTypeSlotExpander",
        "VerifierLocalVariables",
        "VerifierOperandStack",
    )

    val sourceFiles: List<String> = Files.list(sourceDirectory).use { stream ->
        stream
            .filter { path -> path.name.endsWith(".kt") }
            .map { path -> path.name }
            .sorted()
            .toList()
    }

    private val testClasses: Set<String> = Files.list(testDirectory).use { stream ->
        stream
            .filter { path -> path.name.endsWith("Test.kt") }
            .map { path -> path.nameWithoutExtension }
            .toList()
            .toSet()
    }

    val entries: List<VerifierRuleCoverageEntry> = sourceFiles.map { sourceFile ->
        val sourceClassName = sourceFile.removeSuffix(".kt")
        val conventionalTestClass = "${sourceClassName}Test"
        val coveringTestClass = when {
            conventionalTestClass in testClasses -> conventionalTestClass
            sourceClassName in alternateCoveringTests -> alternateCoveringTests.getValue(sourceClassName)
            else -> null
        }
        VerifierRuleCoverageEntry(
            sourceFile = sourceFile,
            specArea = specAreaFor(sourceClassName),
            status = when {
                sourceClassName in internalSupportFiles -> VerifierRuleCoverageStatus.InternalSupport
                coveringTestClass != null -> VerifierRuleCoverageStatus.RuleTested
                else -> VerifierRuleCoverageStatus.NotYetImplemented
            },
            coveringTestClass = coveringTestClass,
        )
    }

    private fun specAreaFor(sourceClassName: String): String =
        when {
            sourceClassName.endsWith("InstructionVerifier") -> "JVMS 4.10.1 type checking and 6.5 instruction rule"
            sourceClassName == "MethodControlFlowGraph" -> "JVMS 4.9 control-flow structural constraints"
            sourceClassName == "MethodTypeCheckingVerifier" -> "JVMS 4.10.1 method type checking"
            sourceClassName == "MethodInitialFrameBuilder" -> "JVMS 4.10.1 initial frame construction"
            sourceClassName == "MethodResourceLimitsVerifier" -> "JVMS 4.7.3 Code max_stack and max_locals limits"
            sourceClassName == "MethodDescriptorVerificationTypeParser" -> "JVMS 4.3.3 method descriptor verification types"
            sourceClassName == "VerificationType" -> "JVMS 4.10.1.2 verification type lattice"
            sourceClassName == "VerificationTypeSlotExpander" -> "JVMS 4.10.1 local variable slot expansion"
            sourceClassName == "VerifierLocalVariables" -> "JVMS 4.10.1 local variable state"
            sourceClassName == "VerifierOperandStack" -> "JVMS 4.10.1 operand stack state"
            sourceClassName == "StackMapFrameExpander" -> "JVMS 4.7.4 stack map frame expansion"
            sourceClassName == "ObjectInitializationRules" -> "JVMS 4.10.1.4 object initialization"
            sourceClassName == "UninitializedThisRules" -> "JVMS 4.10.1.4 uninitializedThis rules"
            sourceClassName == "ProtectedMemberAccessVerifier" -> "JVMS 4.10.1.8 protected member access"
            sourceClassName == "ExceptionHandlerEdgesVerifier" -> "JVMS 4.10.1 exception handler edges"
            else -> "JVMS verifier support"
        }

    private fun verifierPath(relativePath: String): Path {
        val userDir = Path.of(System.getProperty("user.dir"))
        return listOf(
            userDir.resolve("jvm-verifier").resolve(relativePath),
            userDir.resolve(relativePath),
        ).first { candidate -> Files.isDirectory(candidate) }
    }
}
