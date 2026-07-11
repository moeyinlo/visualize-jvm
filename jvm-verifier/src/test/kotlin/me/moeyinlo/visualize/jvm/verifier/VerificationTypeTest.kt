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
            VerificationType.OneWord,
            VerificationType.Reference,
            VerificationType.UninitializedReference,
            VerificationType.Byte,
            VerificationType.Boolean,
            VerificationType.Integer,
            VerificationType.Float,
            VerificationType.Null,
            VerificationType.UninitializedThis,
            VerificationType.ObjectType(ConstantPoolIndex(1)),
            VerificationType.ArrayOf(VerificationType.Integer),
            VerificationType.Uninitialized(offset = 0),
        )

        oneLocationTypes.forEach { type ->
            assertEquals(1, type.locationCount, "unexpected location count for $type")
        }
        assertEquals(2, VerificationType.Long.locationCount)
        assertEquals(2, VerificationType.Double.locationCount)
        assertEquals(2, VerificationType.TwoWord.locationCount)
    }

    @Test
    fun `assignability is reflexive`() {
        val types = listOf(
            VerificationType.Top,
            VerificationType.OneWord,
            VerificationType.TwoWord,
            VerificationType.Reference,
            VerificationType.UninitializedReference,
            VerificationType.Byte,
            VerificationType.Boolean,
            VerificationType.Integer,
            VerificationType.Float,
            VerificationType.Long,
            VerificationType.Double,
            VerificationType.Null,
            VerificationType.UninitializedThis,
            VerificationType.ObjectType(ConstantPoolIndex(1)),
            VerificationType.ArrayOf(VerificationType.Integer),
            VerificationType.Uninitialized(offset = 0),
        )

        types.forEach { type ->
            assertEquals(true, type.isAssignableTo(type), "expected $type to be assignable to itself")
        }
    }

    @Test
    fun `assigns primitive verification types through abstract categories`() {
        assertEquals(true, VerificationType.Integer.isAssignableTo(VerificationType.OneWord))
        assertEquals(true, VerificationType.Float.isAssignableTo(VerificationType.OneWord))
        assertEquals(true, VerificationType.Long.isAssignableTo(VerificationType.TwoWord))
        assertEquals(true, VerificationType.Double.isAssignableTo(VerificationType.TwoWord))

        assertEquals(true, VerificationType.Integer.isAssignableTo(VerificationType.Top))
        assertEquals(true, VerificationType.Long.isAssignableTo(VerificationType.Top))

        assertEquals(false, VerificationType.Integer.isAssignableTo(VerificationType.TwoWord))
        assertEquals(false, VerificationType.Long.isAssignableTo(VerificationType.OneWord))
    }

    @Test
    fun `byte and boolean are exact primitive array component markers`() {
        val byteArrayType = VerificationType.ArrayOf(VerificationType.Byte)
        val booleanArrayType = VerificationType.ArrayOf(VerificationType.Boolean)

        assertEquals(true, byteArrayType.isAssignableTo(VerificationType.Reference))
        assertEquals(true, booleanArrayType.isAssignableTo(VerificationType.Reference))
        assertEquals(true, VerificationType.Null.isAssignableTo(byteArrayType))
        assertEquals(true, VerificationType.Null.isAssignableTo(booleanArrayType))

        assertEquals(false, VerificationType.Byte.isAssignableTo(VerificationType.Integer))
        assertEquals(false, VerificationType.Boolean.isAssignableTo(VerificationType.Integer))
        assertEquals(false, byteArrayType.isAssignableTo(booleanArrayType))
    }

    @Test
    fun `assigns reference null and uninitialized verification types through abstract categories`() {
        val objectType = VerificationType.ObjectType(ConstantPoolIndex(1))
        val intArrayType = VerificationType.ArrayOf(VerificationType.Integer)
        val floatArrayType = VerificationType.ArrayOf(VerificationType.Float)

        assertEquals(true, objectType.isAssignableTo(VerificationType.Reference))
        assertEquals(true, objectType.isAssignableTo(VerificationType.OneWord))
        assertEquals(true, objectType.isAssignableTo(VerificationType.Top))

        assertEquals(true, VerificationType.Null.isAssignableTo(objectType))
        assertEquals(true, VerificationType.Null.isAssignableTo(intArrayType))
        assertEquals(true, VerificationType.Null.isAssignableTo(VerificationType.Reference))
        assertEquals(true, intArrayType.isAssignableTo(VerificationType.Reference))
        assertEquals(true, intArrayType.isAssignableTo(VerificationType.OneWord))
        assertEquals(true, intArrayType.isAssignableTo(VerificationType.Top))

        assertEquals(true, VerificationType.UninitializedThis.isAssignableTo(VerificationType.UninitializedReference))
        assertEquals(true, VerificationType.Uninitialized(offset = 10).isAssignableTo(VerificationType.UninitializedReference))
        assertEquals(true, VerificationType.Uninitialized(offset = 10).isAssignableTo(VerificationType.Reference))
        assertEquals(true, VerificationType.Uninitialized(offset = 10).isAssignableTo(VerificationType.Top))

        assertEquals(false, objectType.isAssignableTo(VerificationType.Null))
        assertEquals(false, intArrayType.isAssignableTo(floatArrayType))
        assertEquals(false, VerificationType.Integer.isAssignableTo(VerificationType.Reference))
    }
}
