package me.moeyinlo.visualize.jvm.runtime

import me.moeyinlo.visualize.jvm.classfile.BootstrapMethodIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantClassEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDoubleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFieldRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantFloatEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantIntegerEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantLongEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodHandleEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodRefEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantMethodTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantInvokeDynamicEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantNameAndTypeEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantPool
import me.moeyinlo.visualize.jvm.classfile.ConstantPoolIndex
import me.moeyinlo.visualize.jvm.classfile.ConstantStringEntry
import me.moeyinlo.visualize.jvm.classfile.ConstantUtf8Entry
import me.moeyinlo.visualize.jvm.classfile.MethodHandleReferenceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class JvmInvokeDynamicCallSiteRegistryTest {
    private val BOOTSTRAP_DESCRIPTOR =
        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)" +
            "Ljava/lang/invoke/CallSite;"

    @Test
    fun `call site resolver reads bootstrap index name and descriptor from invoke dynamic constants`() {
        val spec = JvmInvokeDynamicCallSiteResolver.resolveSpec(
            constantPool = invokedynamicConstantPool(),
            index = ConstantPoolIndex(1),
        )

        assertEquals(
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 7,
                name = "run",
                descriptor = "(I)Ljava/lang/String;",
            ),
            spec,
        )
    }

    @Test
    fun `call site resolver links invoke dynamic specs to bootstrap methods by zero based index`() {
        val linkageSpec = JvmInvokeDynamicCallSiteResolver.resolveLinkageSpec(
            constantPool = invokedynamicConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(9),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(10),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(11),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(12),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(13),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(14),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(15),
                        bootstrapArguments = emptyList(),
                    ),
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(16),
                        bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(4)),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmInvokeDynamicLinkageSpec(
                callSite = JvmInvokeDynamicCallSiteSpec(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                    bootstrapMethodIndex = 7,
                    name = "run",
                    descriptor = "(I)Ljava/lang/String;",
                ),
                bootstrapMethod = JvmBootstrapMethod(
                    bootstrapMethodRef = JvmRuntimeConstantPoolIndex(16),
                    bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(4)),
                ),
            ),
            linkageSpec,
        )
    }

    @Test
    fun `call site resolver rejects missing bootstrap method table entries`() {
        val exception = assertFailsWith<JvmBootstrapMethodAccessException> {
            JvmInvokeDynamicCallSiteResolver.resolveLinkageSpec(
                constantPool = invokedynamicConstantPool(),
                index = ConstantPoolIndex(1),
                bootstrapMethods = JvmBootstrapMethodTable(),
            )
        }

        assertEquals("Bootstrap method index #7 is outside 0..-1", exception.message)
    }

    @Test
    fun `call site resolver materializes bootstrap method handle and static arguments`() {
        val invocation = JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
            constantPool = bootstrapInvocationConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(5),
                        bootstrapArguments = listOf(
                            JvmRuntimeConstantPoolIndex(12),
                            JvmRuntimeConstantPoolIndex(13),
                            JvmRuntimeConstantPoolIndex(14),
                            JvmRuntimeConstantPoolIndex(16),
                            JvmRuntimeConstantPoolIndex(18),
                            JvmRuntimeConstantPoolIndex(20),
                            JvmRuntimeConstantPoolIndex(22),
                            JvmRuntimeConstantPoolIndex(24),
                            JvmRuntimeConstantPoolIndex(29),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 6,
            ),
            invocation.bootstrapMethodHandle,
        )
        assertEquals(
            listOf(
                JvmBootstrapArgument.IntegerConstant(JvmRuntimeConstantPoolIndex(12), JvmIntValue(42)),
                JvmBootstrapArgument.FloatConstant(JvmRuntimeConstantPoolIndex(13), JvmFloatValue(1.5f)),
                JvmBootstrapArgument.LongConstant(JvmRuntimeConstantPoolIndex(14), JvmLongValue(9L)),
                JvmBootstrapArgument.DoubleConstant(JvmRuntimeConstantPoolIndex(16), JvmDoubleValue(2.25)),
                JvmBootstrapArgument.StringConstant(JvmRuntimeConstantPoolIndex(18), "static"),
                JvmBootstrapArgument.ClassConstant(JvmRuntimeConstantPoolIndex(20), "pkg/Arg"),
                JvmBootstrapArgument.MethodTypeConstant(JvmRuntimeConstantPoolIndex(22), "(I)V"),
                JvmBootstrapArgument.MethodHandleConstant(
                    JvmRuntimeConstantPoolIndex(24),
                    JvmMethodHandlePayload(
                        referenceKind = JvmMethodHandleReferenceKind.GetStatic,
                        referenceIndex = 25,
                    ),
                ),
                JvmBootstrapArgument.DynamicConstant(
                    constantPoolIndex = JvmRuntimeConstantPoolIndex(29),
                    bootstrapMethodIndex = 0,
                    name = "dyn",
                    descriptor = "I",
                ),
            ),
            invocation.staticArguments,
        )
    }

    @Test
    fun `call site resolver rejects non method handle bootstrap method refs`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
                constantPool = bootstrapInvocationConstantPool(),
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
            "invokedynamic bootstrap_method_ref index #12 expected CONSTANT_MethodHandle_info but found " +
                "ConstantIntegerEntry",
            exception.message,
        )
    }


    @Test
    fun `bootstrap invocation materializes lookup name type and resolved static arguments as guest values`() {
        val heap = JvmHeap()
        val invocation = JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
            constantPool = bootstrapInvocationConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(5),
                        bootstrapArguments = listOf(
                            JvmRuntimeConstantPoolIndex(12),
                            JvmRuntimeConstantPoolIndex(13),
                            JvmRuntimeConstantPoolIndex(14),
                            JvmRuntimeConstantPoolIndex(16),
                            JvmRuntimeConstantPoolIndex(18),
                            JvmRuntimeConstantPoolIndex(20),
                            JvmRuntimeConstantPoolIndex(22),
                            JvmRuntimeConstantPoolIndex(24),
                        ),
                    ),
                ),
            ),
        )

        val arguments = invocation.materializeBootstrapMethodArguments(
            heap = heap,
            lookupClassName = "pkg/Caller",
        )

        assertEquals(11, arguments.size)
        val lookup = arguments[0] as JvmObjectReferenceValue
        assertEquals("java/lang/invoke/MethodHandles\$Lookup", heap.get(lookup).className)
        assertEquals(JvmMethodHandlesLookupPayload("pkg/Caller"), heap.get(lookup).payload)
        assertEquals(JvmStringPayload("run"), heap.get(arguments[1] as JvmObjectReferenceValue).payload)
        assertEquals(
            JvmMethodTypePayload("(I)Ljava/lang/String;"),
            heap.get(arguments[2] as JvmObjectReferenceValue).payload,
        )
        assertEquals(JvmIntValue(42), arguments[3])
        assertEquals(JvmFloatValue(1.5f), arguments[4])
        assertEquals(JvmLongValue(9L), arguments[5])
        assertEquals(JvmDoubleValue(2.25), arguments[6])
        assertEquals(JvmStringPayload("static"), heap.get(arguments[7] as JvmObjectReferenceValue).payload)
        assertEquals(JvmClassPayload("pkg/Arg"), heap.get(arguments[8] as JvmObjectReferenceValue).payload)
        assertEquals(JvmMethodTypePayload("(I)V"), heap.get(arguments[9] as JvmObjectReferenceValue).payload)
        assertEquals(
            JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.GetStatic,
                referenceIndex = 25,
            ),
            heap.get(arguments[10] as JvmObjectReferenceValue).payload,
        )
    }

    @Test
    fun `bootstrap invocation leaves dynamic static arguments to dynamic constant resolution`() {
        val invocation = JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
            constantPool = bootstrapInvocationConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(5),
                        bootstrapArguments = listOf(JvmRuntimeConstantPoolIndex(29)),
                    ),
                ),
            ),
        )

        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            invocation.materializeBootstrapMethodArguments(
                heap = JvmHeap(),
                lookupClassName = "pkg/Caller",
            )
        }

        assertEquals(
            "CONSTANT_Dynamic bootstrap argument #29 requires dynamic-constant resolution",
            exception.message,
        )
    }


    @Test
    fun `bootstrap invocation extracts call site target method handle from bootstrap result`() {
        val heap = JvmHeap()
        val invocation = simpleBootstrapInvocation()
        val target = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 6,
        )
        val returnedCallSite = heap.allocateCallSite(target)

        val result = invocation.extractBootstrapResult(heap, returnedCallSite)

        assertEquals(returnedCallSite, result.callSiteReference)
        assertEquals(target, result.targetMethodHandle)
        assertEquals(
            JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 6,
            ),
            result.targetMethodHandlePayload,
        )
    }

    @Test
    fun `bootstrap invocation rejects non call site bootstrap results`() {
        val heap = JvmHeap()
        val invocation = simpleBootstrapInvocation()
        val stringReference = heap.internString("not-call-site")

        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            invocation.extractBootstrapResult(heap, stringReference)
        }

        assertEquals(
            "invokedynamic bootstrap method returned java/lang/String, expected java/lang/invoke/CallSite",
            exception.message,
        )
    }

    @Test
    fun `bootstrap invocation rejects call sites without target payloads`() {
        val heap = JvmHeap()
        val invocation = simpleBootstrapInvocation()
        val emptyCallSite = heap.allocateObject("java/lang/invoke/CallSite")

        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            invocation.extractBootstrapResult(heap, emptyCallSite)
        }

        assertEquals(
            "invokedynamic bootstrap method returned CallSite without a target MethodHandle payload",
            exception.message,
        )
    }


    @Test
    fun `method handle resolver resolves invoke static targets through the class hierarchy`() {
        val resolvedMethod = JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
            constantPool = bootstrapInvocationConstantPool(),
            classHierarchy = bootstrapTargetHierarchy(),
            methodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 6,
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "pkg/Bootstrap",
                name = "bootstrap",
                descriptor = BOOTSTRAP_DESCRIPTOR,
                isStatic = true,
            ),
            resolvedMethod,
        )
    }

    @Test
    fun `method handle resolver resolves invoke virtual targets through the class hierarchy`() {
        val resolvedMethod = JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
            constantPool = bootstrapInvocationConstantPool(),
            classHierarchy = JvmClassHierarchy(
                listOf(
                    JvmClassDefinition(
                        internalName = "pkg/Bootstrap",
                        methods = listOf(
                            JvmMethodDefinition(
                                name = "bootstrap",
                                descriptor = BOOTSTRAP_DESCRIPTOR,
                                isStatic = false,
                            ),
                        ),
                    ),
                ),
            ),
            methodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeVirtual,
                referenceIndex = 6,
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "pkg/Bootstrap",
                name = "bootstrap",
                descriptor = BOOTSTRAP_DESCRIPTOR,
                isStatic = false,
            ),
            resolvedMethod,
        )
    }

    @Test
    fun `method handle resolver rejects invoke virtual targets that resolve to static methods`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
                constantPool = bootstrapInvocationConstantPool(),
                classHierarchy = bootstrapTargetHierarchy(),
                methodHandle = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeVirtual,
                    referenceIndex = 6,
                ),
            )
        }

        assertEquals(
            "MethodHandle InvokeVirtual target pkg/Bootstrap.bootstrap:$BOOTSTRAP_DESCRIPTOR resolved to a static method",
            exception.message,
        )
    }

    @Test
    fun `method handle resolver keeps unsupported reference kinds explicit`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveMethodHandleTargetMethod(
                constantPool = bootstrapInvocationConstantPool(),
                classHierarchy = JvmClassHierarchy(),
                methodHandle = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeSpecial,
                    referenceIndex = 6,
                ),
            )
        }

        assertEquals(
            "MethodHandle reference kind InvokeSpecial target resolution is not implemented yet",
            exception.message,
        )
    }


    @Test
    fun `bootstrap result binding caches invokedynamic target methods by call site key`() {
        val heap = JvmHeap()
        val invocation = simpleBootstrapInvocation()
        val targetHandle = heap.internMethodHandle(
            referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
            referenceIndex = 6,
        )
        val bootstrapResult = invocation.extractBootstrapResult(
            heap = heap,
            returnValue = heap.allocateCallSite(targetHandle),
        )
        val registry = JvmInvokeDynamicCallSiteRegistry()
        val key = JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = 4)

        val linked = JvmInvokeDynamicCallSiteResolver.bindBootstrapResult(
            key = key,
            constantPool = bootstrapInvocationConstantPool(),
            classHierarchy = bootstrapTargetHierarchy(),
            invocation = invocation,
            bootstrapResult = bootstrapResult,
            registry = registry,
        )

        assertSame(linked, registry.linked(key))
        assertEquals(invocation.callSite, linked.spec)
        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "pkg/Bootstrap",
                name = "bootstrap",
                descriptor = BOOTSTRAP_DESCRIPTOR,
                isStatic = true,
            ),
            linked.targetMethod,
        )
    }
    @Test
    fun `call site resolver rejects non invoke dynamic constant pool entries`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(listOf(ConstantIntegerEntry(1))),
                index = ConstantPoolIndex(1),
            )
        }

        assertEquals(
            "invokedynamic constant pool index #1 expected CONSTANT_InvokeDynamic_info but found ConstantIntegerEntry",
            exception.message,
        )
    }

    @Test
    fun `call site resolver reports malformed invoke dynamic name and type references`() {
        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            JvmInvokeDynamicCallSiteResolver.resolveSpec(
                constantPool = ConstantPool.fromEntries(
                    listOf(
                        ConstantInvokeDynamicEntry(
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
            "invokedynamic name_and_type index #2 expected CONSTANT_NameAndType_info but found ConstantClassEntry",
            exception.message,
        )
    }

    @Test
    fun `call site registry caches linked invokedynamic targets by owner and bytecode offset`() {
        val registry = JvmInvokeDynamicCallSiteRegistry()
        val key = JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = 12)
        val callSite = JvmLinkedInvokeDynamicCallSite(
            spec = JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(3),
                bootstrapMethodIndex = 0,
                name = "run",
                descriptor = "()I",
            ),
            targetMethodHandle = JvmMethodHandlePayload(
                referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                referenceIndex = 1,
            ),
            targetMethod = JvmResolvedMethod(
                ownerClassName = "pkg/BootstrapTarget",
                name = "run",
                descriptor = "()I",
                isStatic = true,
            ),
        )

        assertNull(registry.linked(key))

        assertSame(callSite, registry.bind(key, callSite))
        assertSame(callSite, registry.linked(key))
        assertSame(callSite, registry.bind(key, callSite))
    }

    @Test
    fun `call site registry rejects rebinding an already linked bytecode offset`() {
        val registry = JvmInvokeDynamicCallSiteRegistry()
        val key = JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = 12)
        val spec = JvmInvokeDynamicCallSiteSpec(
            constantPoolIndex = JvmRuntimeConstantPoolIndex(3),
            bootstrapMethodIndex = 0,
            name = "run",
            descriptor = "()I",
        )
        registry.bind(
            key,
            JvmLinkedInvokeDynamicCallSite(
                spec = spec,
                targetMethodHandle = JvmMethodHandlePayload(
                    referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = 1,
                ),
                targetMethod = JvmResolvedMethod(
                    ownerClassName = "pkg/FirstTarget",
                    name = "run",
                    descriptor = "()I",
                    isStatic = true,
                ),
            ),
        )

        val exception = assertFailsWith<JvmInvokeDynamicLinkageException> {
            registry.bind(
                key,
                JvmLinkedInvokeDynamicCallSite(
                    spec = spec,
                    targetMethodHandle = JvmMethodHandlePayload(
                        referenceKind = JvmMethodHandleReferenceKind.InvokeStatic,
                        referenceIndex = 2,
                    ),
                    targetMethod = JvmResolvedMethod(
                        ownerClassName = "pkg/SecondTarget",
                        name = "run",
                        descriptor = "()I",
                        isStatic = true,
                    ),
                ),
            )
        }

        assertEquals("invokedynamic call site pkg/Caller@12 is already linked", exception.message)
    }

    @Test
    fun `call site model validates owner offset bootstrap index and descriptor identity`() {
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteKey(ownerClassName = "", bytecodeOffset = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteKey(ownerClassName = "pkg/Caller", bytecodeOffset = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = -1,
                name = "run",
                descriptor = "()V",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "",
                descriptor = "()V",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmInvokeDynamicCallSiteSpec(
                constantPoolIndex = JvmRuntimeConstantPoolIndex(1),
                bootstrapMethodIndex = 0,
                name = "run",
                descriptor = "",
            )
        }
    }

    private fun invokedynamicConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantInvokeDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(7),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("run", "run".encodeToByteArray()),
                ConstantUtf8Entry("(I)Ljava/lang/String;", "(I)Ljava/lang/String;".encodeToByteArray()),
            ),
        )

    private fun bootstrapTargetHierarchy(): JvmClassHierarchy =
        JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "pkg/Bootstrap",
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "bootstrap",
                            descriptor = BOOTSTRAP_DESCRIPTOR,
                            isStatic = true,
                        ),
                    ),
                ),
            ),
        )

    private fun simpleBootstrapInvocation(): JvmInvokeDynamicBootstrapInvocation =
        JvmInvokeDynamicCallSiteResolver.resolveBootstrapInvocation(
            constantPool = bootstrapInvocationConstantPool(),
            index = ConstantPoolIndex(1),
            bootstrapMethods = JvmBootstrapMethodTable(
                listOf(
                    JvmBootstrapMethod(
                        bootstrapMethodRef = JvmRuntimeConstantPoolIndex(5),
                        bootstrapArguments = emptyList(),
                    ),
                ),
            ),
        )

    private fun bootstrapInvocationConstantPool(): ConstantPool =
        ConstantPool.fromEntries(
            listOf(
                ConstantInvokeDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(2),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(3),
                    descriptorIndex = ConstantPoolIndex(4),
                ),
                ConstantUtf8Entry("run", "run".encodeToByteArray()),
                ConstantUtf8Entry("(I)Ljava/lang/String;", "(I)Ljava/lang/String;".encodeToByteArray()),
                ConstantMethodHandleEntry(
                    referenceKind = MethodHandleReferenceKind.InvokeStatic,
                    referenceIndex = ConstantPoolIndex(6),
                ),
                ConstantMethodRefEntry(
                    classIndex = ConstantPoolIndex(7),
                    nameAndTypeIndex = ConstantPoolIndex(9),
                ),
                ConstantClassEntry(ConstantPoolIndex(8)),
                ConstantUtf8Entry("pkg/Bootstrap", "pkg/Bootstrap".encodeToByteArray()),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(10),
                    descriptorIndex = ConstantPoolIndex(11),
                ),
                ConstantUtf8Entry("bootstrap", "bootstrap".encodeToByteArray()),
                ConstantUtf8Entry(
                    BOOTSTRAP_DESCRIPTOR,
                    BOOTSTRAP_DESCRIPTOR.encodeToByteArray(),
                ),
                ConstantIntegerEntry(42),
                ConstantFloatEntry(1.5f),
                ConstantLongEntry(9L),
                ConstantDoubleEntry(2.25),
                ConstantStringEntry(ConstantPoolIndex(19)),
                ConstantUtf8Entry("static", "static".encodeToByteArray()),
                ConstantClassEntry(ConstantPoolIndex(21)),
                ConstantUtf8Entry("pkg/Arg", "pkg/Arg".encodeToByteArray()),
                ConstantMethodTypeEntry(ConstantPoolIndex(23)),
                ConstantUtf8Entry("(I)V", "(I)V".encodeToByteArray()),
                ConstantMethodHandleEntry(
                    referenceKind = MethodHandleReferenceKind.GetStatic,
                    referenceIndex = ConstantPoolIndex(25),
                ),
                ConstantFieldRefEntry(
                    classIndex = ConstantPoolIndex(20),
                    nameAndTypeIndex = ConstantPoolIndex(26),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(27),
                    descriptorIndex = ConstantPoolIndex(28),
                ),
                ConstantUtf8Entry("field", "field".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
                ConstantDynamicEntry(
                    bootstrapMethodIndex = BootstrapMethodIndex(0),
                    nameAndTypeIndex = ConstantPoolIndex(30),
                ),
                ConstantNameAndTypeEntry(
                    nameIndex = ConstantPoolIndex(31),
                    descriptorIndex = ConstantPoolIndex(32),
                ),
                ConstantUtf8Entry("dyn", "dyn".encodeToByteArray()),
                ConstantUtf8Entry("I", "I".encodeToByteArray()),
            ),
        )
}
