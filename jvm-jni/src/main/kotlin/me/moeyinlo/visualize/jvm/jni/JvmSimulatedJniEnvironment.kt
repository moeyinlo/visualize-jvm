package me.moeyinlo.visualize.jvm.jni

import me.moeyinlo.visualize.jvm.runtime.JvmClassHierarchy
import me.moeyinlo.visualize.jvm.runtime.JvmBooleanValue
import me.moeyinlo.visualize.jvm.runtime.JvmByteValue
import me.moeyinlo.visualize.jvm.runtime.JvmCharValue
import me.moeyinlo.visualize.jvm.runtime.JvmDoubleValue
import me.moeyinlo.visualize.jvm.runtime.JvmFieldReference
import me.moeyinlo.visualize.jvm.runtime.JvmFloatValue
import me.moeyinlo.visualize.jvm.runtime.JvmHeap
import me.moeyinlo.visualize.jvm.runtime.JvmIntValue
import me.moeyinlo.visualize.jvm.runtime.JvmLongValue
import me.moeyinlo.visualize.jvm.runtime.JvmNoClassDefFoundError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchFieldError
import me.moeyinlo.visualize.jvm.runtime.JvmNoSuchMethodError
import me.moeyinlo.visualize.jvm.runtime.JvmNullValue
import me.moeyinlo.visualize.jvm.runtime.JvmObjectReferenceValue
import me.moeyinlo.visualize.jvm.runtime.JvmShortValue
import me.moeyinlo.visualize.jvm.runtime.JvmStaticFields
import me.moeyinlo.visualize.jvm.runtime.JvmStringPayload

class JvmSimulatedJniEnvironment(
    private val classHierarchy: JvmClassHierarchy,
    private val heap: JvmHeap = JvmHeap(),
    private val staticFields: JvmStaticFields = JvmStaticFields(),
    val handles: JvmJniHandleTable = JvmJniHandleTable(),
) {
    fun findClass(className: String): JvmJniHandleId {
        if (!classHierarchy.hasClass(className)) {
            throw JvmNoClassDefFoundError(
                guestClassName = "java/lang/NoClassDefFoundError",
                message = className,
            )
        }
        return handles.newClassHandle(className)
    }

    fun getStaticMethodId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val method = classHierarchy.resolveMethod(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (!method.isStatic) {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newMethodIdHandle(method)
    }

    fun getMethodId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val method = classHierarchy.resolveMethod(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (method.isStatic) {
            throw JvmNoSuchMethodError(
                guestClassName = "java/lang/NoSuchMethodError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newMethodIdHandle(method)
    }

    fun getObjectClass(objectHandle: JvmJniHandleId): JvmJniHandleId {
        val reference = handles.resolveObject(objectHandle)
        val className = heap.get(reference).className
        return handles.newClassHandle(className)
    }

    fun isInstanceOf(objectHandle: JvmJniHandleId?, classHandle: JvmJniHandleId): Boolean {
        if (objectHandle == null) {
            return true
        }
        val reference = handles.resolveObject(objectHandle)
        val sourceClassName = heap.get(reference).className
        val targetClassName = handles.resolveClass(classHandle)
        return classHierarchy.isAssignable(sourceClassName = sourceClassName, targetClassName = targetClassName)
    }

    fun getFieldId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val field = classHierarchy.resolveField(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (field.isStatic) {
            throw JvmNoSuchFieldError(
                guestClassName = "java/lang/NoSuchFieldError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newFieldIdHandle(field)
    }

    fun getStaticFieldId(
        classHandle: JvmJniHandleId,
        name: String,
        descriptor: String,
    ): JvmJniHandleId {
        val className = handles.resolveClass(classHandle)
        val field = classHierarchy.resolveField(
            ownerClassName = className,
            name = name,
            descriptor = descriptor,
        )
        if (!field.isStatic) {
            throw JvmNoSuchFieldError(
                guestClassName = "java/lang/NoSuchFieldError",
                message = "$className.$name:$descriptor",
            )
        }
        return handles.newFieldIdHandle(field)
    }

    fun newString(chars: CharArray, length: Int): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateString(chars.concatToString(startIndex = 0, endIndex = length)))

    fun newStringUtf(value: String): JvmJniHandleId =
        handles.newObjectHandle(heap.allocateString(value))

    fun getStringLength(stringHandle: JvmJniHandleId): Int =
        resolveStringValue(stringHandle).length

    fun getStringUtfLength(stringHandle: JvmJniHandleId): Int =
        resolveStringValue(stringHandle).sumOf { codeUnit ->
            when (codeUnit.code) {
                0 -> 2
                in 1..0x7f -> 1
                in 0x80..0x7ff -> 2
                else -> 3
            }
        }

    fun getIntField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetIntField requires an instance int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmIntValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetIntField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setIntField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetIntField requires an instance int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmIntValue(value),
        )
    }

    fun getLongField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Long {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetLongField requires an instance long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmLongValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetLongField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setLongField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Long) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetLongField requires an instance long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmLongValue(value),
        )
    }

    fun getFloatField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Float {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetFloatField requires an instance float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmFloatValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetFloatField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setFloatField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Float) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetFloatField requires an instance float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmFloatValue(value),
        )
    }

    fun getDoubleField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Double {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetDoubleField requires an instance double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmDoubleValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetDoubleField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setDoubleField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Double) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetDoubleField requires an instance double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmDoubleValue(value),
        )
    }

    fun getBooleanField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Boolean {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetBooleanField requires an instance boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmBooleanValue -> value.value
            is JvmIntValue -> value.value != 0
            else -> throw JvmJniFieldAccessException(
                "GetBooleanField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setBooleanField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Boolean) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetBooleanField requires an instance boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmBooleanValue(value),
        )
    }

    fun getByteField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetByteField requires an instance byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmByteValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetByteField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setByteField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetByteField requires an instance byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmByteValue(value),
        )
    }

    fun getCharField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetCharField requires an instance char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmCharValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetCharField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setCharField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetCharField requires an instance char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmCharValue(value),
        )
    }

    fun getShortField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetShortField requires an instance short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmShortValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetShortField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun getStaticIntField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticIntField requires a static int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmIntValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticIntField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticIntField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "I" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticIntField requires a static int field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmIntValue(value),
        )
    }

    fun getStaticLongField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Long {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticLongField requires a static long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmLongValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticLongField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticLongField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Long) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "J" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticLongField requires a static long field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmLongValue(value),
        )
    }

    fun getStaticFloatField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Float {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticFloatField requires a static float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmFloatValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticFloatField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticFloatField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Float) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "F" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticFloatField requires a static float field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmFloatValue(value),
        )
    }

    fun getStaticDoubleField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Double {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticDoubleField requires a static double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return (value as? JvmDoubleValue)?.value
            ?: throw JvmJniFieldAccessException(
                "GetStaticDoubleField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
    }

    fun setStaticDoubleField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Double) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "D" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticDoubleField requires a static double field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmDoubleValue(value),
        )
    }

    fun getStaticBooleanField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Boolean {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticBooleanField requires a static boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmBooleanValue -> value.value
            is JvmIntValue -> value.value != 0
            else -> throw JvmJniFieldAccessException(
                "GetStaticBooleanField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticBooleanField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Boolean) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "Z" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticBooleanField requires a static boolean field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmBooleanValue(value),
        )
    }

    fun getStaticObjectField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): JvmJniHandleId? {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticObjectField requires a static reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(value)
            else -> throw JvmJniFieldAccessException(
                "GetStaticObjectField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticObjectField(
        classHandle: JvmJniHandleId,
        fieldIdHandle: JvmJniHandleId,
        valueHandle: JvmJniHandleId?,
    ) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticObjectField requires a static reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = if (valueHandle == null) {
            JvmNullValue
        } else {
            handles.resolveObject(valueHandle)
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            value,
        )
    }

    fun getStaticByteField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticByteField requires a static byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmByteValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticByteField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticByteField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "B" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticByteField requires a static byte field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmByteValue(value),
        )
    }

    fun getStaticCharField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticCharField requires a static char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmCharValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticCharField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticCharField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "C" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticCharField requires a static char field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmCharValue(value),
        )
    }

    fun getStaticShortField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): Int {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetStaticShortField requires a static short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = staticFields.get(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            is JvmShortValue -> value.value
            is JvmIntValue -> value.value
            else -> throw JvmJniFieldAccessException(
                "GetStaticShortField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setStaticShortField(classHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        handles.resolveClass(classHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || !field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetStaticShortField requires a static short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        staticFields.put(
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmShortValue(value),
        )
    }

    fun setShortField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId, value: Int) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (field.descriptor != "S" || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetShortField requires an instance short field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            JvmShortValue(value),
        )
    }

    fun getObjectField(objectHandle: JvmJniHandleId, fieldIdHandle: JvmJniHandleId): JvmJniHandleId? {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || field.isStatic) {
            throw JvmJniFieldAccessException(
                "GetObjectField requires an instance reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = heap.getInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
        )
        return when (value) {
            JvmNullValue -> null
            is JvmObjectReferenceValue -> handles.newObjectHandle(value)
            else -> throw JvmJniFieldAccessException(
                "GetObjectField read ${value::class.simpleName} from ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
    }

    fun setObjectField(
        objectHandle: JvmJniHandleId,
        fieldIdHandle: JvmJniHandleId,
        valueHandle: JvmJniHandleId?,
    ) {
        val reference = handles.resolveObject(objectHandle)
        val field = handles.resolveFieldId(fieldIdHandle)
        if (!field.descriptor.isReferenceFieldDescriptor() || field.isStatic) {
            throw JvmJniFieldAccessException(
                "SetObjectField requires an instance reference field, got ${field.ownerClassName}.${field.name}:${field.descriptor}",
            )
        }
        val value = if (valueHandle == null) {
            JvmNullValue
        } else {
            handles.resolveObject(valueHandle)
        }
        heap.putInstanceField(
            reference,
            JvmFieldReference(
                ownerClassName = field.ownerClassName,
                name = field.name,
                descriptor = field.descriptor,
            ),
            value,
        )
    }

    private fun resolveStringValue(stringHandle: JvmJniHandleId): String {
        val reference = handles.resolveObject(stringHandle)
        val heapObject = heap.get(reference)
        val payload = heapObject.payload as? JvmStringPayload
            ?: throw JvmJniStringAccessException(
                "JNI string helper requires java/lang/String payload, got ${heapObject.className}",
            )
        return payload.value
    }
}

class JvmJniFieldAccessException(message: String) : IllegalStateException(message)

class JvmJniStringAccessException(message: String) : IllegalStateException(message)

private fun String.isReferenceFieldDescriptor(): Boolean =
    startsWith("L") || startsWith("[")
