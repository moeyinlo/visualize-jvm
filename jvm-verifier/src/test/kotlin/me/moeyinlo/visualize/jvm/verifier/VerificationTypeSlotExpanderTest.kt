package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals

class VerificationTypeSlotExpanderTest {
    @Test
    fun `expands two-word verification types with trailing top slots`() {
        assertEquals(
            listOf(
                VerificationType.Integer,
                VerificationType.Long,
                VerificationType.Top,
                VerificationType.Double,
                VerificationType.Top,
                VerificationType.ClassType("java/lang/Object"),
            ),
            VerificationTypeSlotExpander.expand(
                listOf(
                    VerificationType.Integer,
                    VerificationType.Long,
                    VerificationType.Double,
                    VerificationType.ClassType("java/lang/Object"),
                ),
            ),
        )
    }
}
