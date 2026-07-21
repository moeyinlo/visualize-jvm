package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerifierRuleCoverageTest {
    @Test
    fun `verifier rule coverage classifies every verifier source file`() {
        val coverageByFile = VerifierRuleCoverage.entries.associateBy(VerifierRuleCoverageEntry::sourceFile)

        assertEquals(VerifierRuleCoverage.sourceFiles, VerifierRuleCoverage.entries.map(VerifierRuleCoverageEntry::sourceFile))
        assertEquals(VerifierRuleCoverage.sourceFiles.toSet(), coverageByFile.keys)
        assertTrue(VerifierRuleCoverage.entries.all { entry -> entry.specArea.isNotBlank() })
    }

    @Test
    fun `verifier rule coverage binds rule files to tests or explicit support status`() {
        val missingTests = VerifierRuleCoverage.entries.filter { entry ->
            entry.status == VerifierRuleCoverageStatus.RuleTested && entry.coveringTestClass == null
        }

        assertEquals(emptyList(), missingTests)
        assertTrue(
            VerifierRuleCoverage.entries.any { entry -> entry.status == VerifierRuleCoverageStatus.InternalSupport },
        )
    }

    @Test
    fun `verifier rule coverage has no unimplemented rule files`() {
        val unimplemented = VerifierRuleCoverage.entries
            .filter { entry -> entry.status == VerifierRuleCoverageStatus.NotYetImplemented }
            .map(VerifierRuleCoverageEntry::sourceFile)

        assertEquals(emptyList(), unimplemented)
    }
}
