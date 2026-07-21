package me.moeyinlo.visualize.jvm.classfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MalformedClassfileCorpusTest {
    @Test
    fun `malformed classfile corpus enumerates structural rejection points`() {
        val casesByName = MalformedClassfileCorpus.cases.associateBy(MalformedClassfileCase::name)

        assertEquals(MalformedClassfileCorpus.cases.size, casesByName.size)
        assertTrue(MalformedClassfileCorpus.cases.all { case -> case.bytes.isNotEmpty() })
        assertTrue(MalformedClassfileCorpus.cases.all { case -> case.expectedMessageFragment.isNotBlank() })

        assertTrue(casesByName.containsKey("truncated header"))
        assertTrue(casesByName.containsKey("bad magic"))
        assertTrue(casesByName.containsKey("unsupported future major version"))
        assertTrue(casesByName.containsKey("zero constant pool count"))
        assertTrue(casesByName.containsKey("truncated UTF8 constant"))
    }

    @Test
    fun `malformed classfile corpus is rejected by the parser with expected failures`() {
        MalformedClassfileCorpus.cases.forEach { case ->
            val failure = assertFails(case.name) {
                ClassFileParser.parse(case.bytes, source = case.name)
            }

            assertIs<RuntimeException>(failure, case.name)
            assertEquals(case.expectedExceptionSimpleName, failure::class.simpleName, case.name)
            assertTrue(failure.message.orEmpty().contains(case.expectedMessageFragment), failure.message)
        }
    }
}
