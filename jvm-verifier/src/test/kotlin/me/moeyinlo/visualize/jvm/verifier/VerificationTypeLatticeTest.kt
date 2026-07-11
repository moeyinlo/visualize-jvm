package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationTypeLatticeTest {
    @Test
    fun `class verification type is a one word reference type`() {
        val stringType = VerificationType.ClassType("java/lang/String")

        assertTrue(stringType.isAssignableTo(VerificationType.Reference))
        assertTrue(stringType.isAssignableTo(VerificationType.OneWord))
        assertTrue(stringType.isAssignableTo(VerificationType.Top))
        assertFalse(stringType.isAssignableTo(VerificationType.TwoWord))
    }

    @Test
    fun `null is assignable to class verification type`() {
        assertTrue(VerificationType.Null.isAssignableTo(VerificationType.ClassType("java/lang/Object")))
    }
}
