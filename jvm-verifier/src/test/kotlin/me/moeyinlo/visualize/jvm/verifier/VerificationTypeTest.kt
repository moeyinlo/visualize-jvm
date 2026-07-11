package me.moeyinlo.visualize.jvm.verifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.VerificationTypeInfo as ClassfileVerificationTypeInfo

class VerificationTypeTest {
    @Test
    fun `maps classfile verification type atoms into verifier types`() {
        assertSame(VerificationType.Top, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Top))
        assertSame(VerificationType.Integer, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Integer))
        assertSame(VerificationType.Float, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Float))
        assertSame(VerificationType.Double, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Double))
        assertSame(VerificationType.Long, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Long))
        assertSame(VerificationType.Null, VerificationType.fromClassfile(ClassfileVerificationTypeInfo.Null))
        assertSame(
            VerificationType.UninitializedThis,
            VerificationType.fromClassfile(ClassfileVerificationTypeInfo.UninitializedThis),
        )
    }

    @Test
    fun `maps classfile object and uninitialized verification types`() {
        assertEquals(
            VerificationType.ObjectType(ConstantPoolIndex(12)),
            VerificationType.fromClassfile(ClassfileVerificationTypeInfo.ObjectVariable(ConstantPoolIndex(12))),
        )
        assertEquals(
            VerificationType.Uninitialized(offset = 34),
            VerificationType.fromClassfile(ClassfileVerificationTypeInfo.UninitializedVariable(offset = 34)),
        )
    }

    @Test
    fun `reports verification type location counts`() {
        val oneLocationTypes = listOf(
            VerificationType.Top,
            VerificationType.Integer,
            VerificationType.Float,
            VerificationType.Null,
            VerificationType.UninitializedThis,
            VerificationType.ObjectType(ConstantPoolIndex(1)),
            VerificationType.Uninitialized(offset = 0),
        )

        oneLocationTypes.forEach { type ->
            assertEquals(1, type.locationCount, "unexpected location count for $type")
        }
        assertEquals(2, VerificationType.Long.locationCount)
        assertEquals(2, VerificationType.Double.locationCount)
    }
}
