package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmDynamicConstantTest {
    @Test
    fun `dynamic constant resolver reads bootstrap index name and descriptor`() {
        val spec = JvmDynamicConstantResolver.resolveSpec(
            constantPool = dynamicConstantPool(),
            index = ConstantPoolIndex(1),
        )

        assertEquals(
            JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 2,
                name = "answer",
                descriptor = "I",
            ),
            spec,
        )
    }

    @Test
    fun `dynamic constant resolver links bootstrap methods by zero based index`() {
        val linkageSpec = JvmDynamicConstantResolver.resolveLinkageSpec(
            constantPool = dynamicConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(JvmRuntimeConstantPoolIndex(7), emptyList()),
                    JvmBootstrapMethod(JvmRuntimeConstantPoolIndex(8), emptyList()),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(9),
                        bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(10)),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmDynamicConstantLinkageSpec(
                constant = JvmDynamicConstantSpec(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                    bootstrapMethodIndex = 2,
                    name = "answer",
                    descriptor = "I",
                ),
                bootstrapMethod = JvmBootstrapMethod(
                    bootstrapMethodRef = JvmRuntimeConstantPoolIndex(9),
                    bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(10)),
                ),
            ),
            linkageSpec,
        )
    }

    @Test
    fun `dynamic constant bootstrap invocation materializes lookup name class and static arguments`() {
        val heap = JvmHeap()
        val invocation = JvmDynamicConstantBootstrapInvocation(
            constant = JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "answer",
                descriptor = "Ljava/lang/Integer;",
            ),
            bootstrapMethodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 5,
            ),
            staticArguments = listOf(
                JvmBootstrapArgument.IntegerConstant(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(6),
                    value = JvmIntValue(42),
                ),
                JvmBootstrapArgument.StringConstant(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(7),
                    value = "seed",
                ),
            ),
        )

        val arguments = invocation.materializeBootstrapMethodArguments(
            heap = heap,
            lookupClassName = "Example",
        )

        val lookupReference = JvmObjectReferenceValue(JvmReferenceId(1))
        val nameReference = JvmObjectReferenceValue(JvmReferenceId(2))
        val classReference = JvmObjectReferenceValue(JvmReferenceId(3))
        val staticStringReference = JvmObjectReferenceValue(JvmReferenceId(4))
        assertEquals(
            listOf(lookupReference, nameReference, classReference, JvmIntValue(42), staticStringReference),
            arguments,
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/invoke/MethodHandles\$Lookup",
                payload = JvmMethodHandlesLookupPayload("Example"),
            ),
            heap.get(lookupReference),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("answer"),
            ),
            heap.get(nameReference),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("java/lang/Integer"),
            ),
            heap.get(classReference),
        )
        assertEquals(
            JvmHeapObject(
                className = "java/lang/String",
                payload = JvmStringPayload("seed"),
            ),
            heap.get(staticStringReference),
        )
    }

    @Test
    fun `dynamic constant bootstrap invocation materializes primitive class mirrors`() {
        val heap = JvmHeap()
        val invocation = JvmDynamicConstantBootstrapInvocation(
            constant = JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "answer",
                descriptor = "I",
            ),
            bootstrapMethodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 5,
            ),
            staticArguments = emptyList(),
        )

        val arguments = invocation.materializeBootstrapMethodArguments(
            heap = heap,
            lookupClassName = "Example",
        )

        val classReference = JvmObjectReferenceValue(JvmReferenceId(3))
        assertEquals(classReference, arguments[2])
        assertEquals(
            JvmHeapObject(
                className = "java/lang/Class",
                payload = JvmClassPayload("int"),
            ),
            heap.get(classReference),
        )
    }

    @Test
    fun `dynamic constant bootstrap invocation rejects method descriptors for constant type mirrors`() {
        val invocation = JvmDynamicConstantBootstrapInvocation(
            constant = JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "badDescriptor",
                descriptor = "(I)V",
            ),
            bootstrapMethodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 5,
            ),
            staticArguments = emptyList(),
        )

        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            invocation.materializeBootstrapMethodArguments(
                heap = JvmHeap(),
                lookupClassName = "Example",
            )
        }

        assertEquals("dynamic constant descriptor (I)V is not a field descriptor", exception.message)
    }

    @Test
    fun `dynamic constant bootstrap invocation leaves nested dynamic static arguments unresolved`() {
        val invocation = JvmDynamicConstantBootstrapInvocation(
            constant = JvmDynamicConstantSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "answer",
                descriptor = "I",
            ),
            bootstrapMethodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 5,
            ),
            staticArguments = listOf(
                JvmBootstrapArgument.DynamicConstant(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(9),
                    bootstrapMethodIndex = 1,
                    name = "nested",
                    descriptor = "I",
                ),
            ),
        )

        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            invocation.materializeBootstrapMethodArguments(
                heap = JvmHeap(),
                lookupClassName = "Example",
            )
        }

        assertEquals(
            "CONSTANT_Dynamic bootstrap argument #9 requires dynamic-constant resolution",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant resolver creates bootstrap invocation from bootstrap method table`() {
        val invocation = JvmDynamicConstantResolver.resolveBootstrapInvocation(
            constantPool = dynamicConstantBootstrapPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(11),
                        bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(12)),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmDynamicConstantBootstrapInvocation(
                constant = JvmDynamicConstantSpec(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                    bootstrapMethodIndex = 0,
                    name = "answer",
                    descriptor = "I",
                ),
                bootstrapMethodHandle = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = 10,
                ),
                staticArguments = listOf(
                    JvmBootstrapArgument.IntegerConstant(
                        constantPoolIndex = JvmRuntimeConstantPoolIndex(12),
                        value = JvmIntValue(42),
                    ),
                ),
            ),
            invocation,
        )
    }

    @Test
    fun `dynamic constant resolver reports invalid bootstrap method handles as dynamic linkage errors`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveBootstrapInvocation(
                constantPool = dynamicConstantBootstrapPool(),
                index = ConstantPoolIndex(1),
                bootstrapMethods = JvmBootstrapMethodTable(
                    listOf(
                        JvmBootstrapMethod(
                            bootstrapMethodRef = JvmRuntimeConstantPoolIndex(12),
                            bootstrapArguments = emptyList(),
                        ),
                    ),
                ),
            )
        }

        assertEquals(
            "dynamic constant bootstrap_method_ref index #12 expected CONSTANT_MethodHandle_info but found " +
                "ConstantIntegerEntry",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant resolver rejects non dynamic constant entries`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(listOf(ConstantIntegerEntry(1))),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "dynamic constant pool index #1 expected CONSTANT_Dynamic_info but found ConstantIntegerEntry",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant resolver rejects method descriptors`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantDynamicEntry(
                            bootstrapMethodIndex = BootstrapMethodIndex(0),
                            nameAndTypeIndex = ConstantPoolIndex(2),
                        ),
                        ConstantNameAndTypeEntry(
                            nameIndex = ConstantPoolIndex(3),
                            descriptorIndex = ConstantPoolIndex(4),
                        ),
                        ConstantUtf8Entry("badDescriptor", "badDescriptor".encodeToByteArray()),
                        ConstantUtf8Entry("(I)V", "(I)V".encodeToByteArray()),
                    ),
                ),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals("dynamic constant descriptor (I)V is not a field descriptor", exception.message)
    }

    @Test
    fun `dynamic constant resolver reports malformed name and type references`() {
        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            JvmDynamicConstantResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantDynamicEntry(
                            bootstrapMethodIndex = BootstrapMethodIndex(0),
                            nameAndTypeIndex = ConstantPoolIndex(2),
                        ),
                        ConstantClassEntry(ConstantPoolIndex(3)),
                        ConstantUtf8Entry("pkg/NotNameAndType", "pkg/NotNameAndType".encodeToByteArray()),
                    ),
                ),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "dynamic constant name_and_type_index #2 expected CONSTANT_NameAndType_info but found ConstantClassEntry",
            exception.message,
        )
    }

    @Test
    fun `dynamic constant registry caches resolved values by constant pool index`() {
        val registry = JvmDynamicConstantRegistry()
        val index = JvmRuntimeConstantPoolIndex(4)
        val value = JvmIntValue(42)

        assertNull(registry.resolved(index))
        assertEquals(value, registry.bind(index, value))
        assertEquals(value, registry.resolved(index))
        assertEquals(value, registry.bind(index, value))
    }

    @Test
    fun `dynamic constant registry rejects rebinding a resolved constant`() {
        val registry = JvmDynamicConstantRegistry()
        val index = JvmRuntimeConstantPoolIndex(4)
        registry.bind(index, JvmIntValue(42))

        val exception = assertFailsWith<JvmDynamicConstantLinkageException> {
            registry.bind(index, JvmIntValue(43))
        }

        assertEquals("dynamic constant #4 is already resolved", exception.message)
    }

    private fun dynamicConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(2),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
            ),
        )

    private fun dynamicConstantBootstrapPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("answer", "answer".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ConstantUtf8Entry("Bootstrap", "Bootstrap".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(5)),
                ConstantUtf8Entry("bootstrap", "bootstrap".encodeToByteArray()),
                ConstantUtf8Entry(
                    "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/Class;I)" +
                        "Ljava/lang/Integer;",
                    (
                        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/Class;I)" +
                            "Ljava/lang/Integer;"
                        ).encodeToByteArray(),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(7),
                    descriptorIndex = ConstantPoolIndex(8),
                ),
                ConstantMethodRefEntry(
                    classIndex = ConstantPoolIndex(6),
                    nameAndTypeIndex = ConstantPoolIndex(9),
                ),
                ConstantMethodHandleEntry(
                    referenceKind = MethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = ConstantPoolIndex(10),
                ),
                ConstantIntegerEntry(42),
            ),
        )
}
