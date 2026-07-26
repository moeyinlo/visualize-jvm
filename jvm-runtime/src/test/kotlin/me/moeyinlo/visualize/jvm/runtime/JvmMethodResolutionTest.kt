package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmMethodResolutionTest {
    @Test
    fun `method resolution finds a static method declared directly by the referenced class`() {
        val method = JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(method),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution rejects interface symbolic references`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "ExampleFace",
                    isInterface = true,
                    methods = listOf(JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = false)),
                ),
            ),
        )

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            hierarchy.resolveMethod(
                ownerClassName = "ExampleFace",
                name = "answer",
                descriptor = "()I",
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("ExampleFace.answer:()I", exception.message)
    }

    @Test
    fun `method resolution searches the superclass chain after the referenced class`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    superclassName = "Grandparent",
                ),
                JvmClassDefinition(
                    internalName = "Grandparent",
                    methods = listOf(JvmMethodDefinition(name = "answer", descriptor = "()I", isStatic = true)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Grandparent",
                name = "answer",
                descriptor = "()I",
                isStatic = true,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution searches superinterface defaults after class and superclass miss`() {
        val defaultCode = byteArrayOf(0x05)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                    interfaceNames = listOf("DefaultFace"),
                ),
                JvmClassDefinition(internalName = "Parent"),
                JvmClassDefinition(
                    internalName = "DefaultFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "answer",
                            descriptor = "()I",
                            isStatic = false,
                            code = defaultCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "DefaultFace",
                name = "answer",
                descriptor = "()I",
                isStatic = false,
                code = defaultCode,
                maxStack = 1,
                maxLocals = 1,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution recursively searches superclass superinterface defaults before child interfaces`() {
        val parentDefaultCode = byteArrayOf(0x05)
        val childDefaultCode = byteArrayOf(0x06)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                    interfaceNames = listOf("ChildFace"),
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    interfaceNames = listOf("ParentFace"),
                ),
                JvmClassDefinition(
                    internalName = "ParentFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "answer",
                            descriptor = "()I",
                            isStatic = false,
                            code = parentDefaultCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
                JvmClassDefinition(
                    internalName = "ChildFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "answer",
                            descriptor = "()I",
                            isStatic = false,
                            code = childDefaultCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "ParentFace",
                name = "answer",
                descriptor = "()I",
                isStatic = false,
                code = parentDefaultCode,
                maxStack = 1,
                maxLocals = 1,
            ),
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "answer",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `method resolution does not inherit instance initialization methods from superclasses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(JvmMethodDefinition(name = "<init>", descriptor = "()V", isStatic = false)),
                ),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "<init>",
                descriptor = "()V",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.<init>:()V", exception.message)
    }

    @Test
    fun `virtual method resolution starts at the receiver class before superclasses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Sub",
                    superclassName = "Base",
                    methods = listOf(JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false)),
                ),
                JvmClassDefinition(
                    internalName = "Base",
                    methods = listOf(JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false)),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Sub",
                name = "value",
                descriptor = "()I",
                isStatic = false,
            ),
            hierarchy.resolveVirtualMethod(
                receiverClassName = "Sub",
                name = "value",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `interface method target resolution uses receiver class implementation before defaults`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("DefaultFace"),
                    methods = listOf(
                        JvmMethodDefinition(name = "value", descriptor = "()I", isStatic = false),
                    ),
                ),
                JvmClassDefinition(
                    internalName = "DefaultFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = byteArrayOf(0x05),
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "value",
                descriptor = "()I",
                isStatic = false,
            ),
            hierarchy.resolveInterfaceMethodTarget(
                receiverClassName = "Example",
                name = "value",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `interface method target resolution finds direct interface default method`() {
        val defaultCode = byteArrayOf(0x05)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("DefaultFace"),
                ),
                JvmClassDefinition(
                    internalName = "DefaultFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = defaultCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "DefaultFace",
                name = "value",
                descriptor = "()I",
                isStatic = false,
                code = defaultCode,
                maxStack = 1,
                maxLocals = 1,
            ),
            hierarchy.resolveInterfaceMethodTarget(
                receiverClassName = "Example",
                name = "value",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `interface method target resolution prefers child interface default over parent default`() {
        val parentCode = byteArrayOf(0x05)
        val childCode = byteArrayOf(0x06)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("ParentFace", "ChildFace"),
                ),
                JvmClassDefinition(
                    internalName = "ParentFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = parentCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
                JvmClassDefinition(
                    internalName = "ChildFace",
                    interfaceNames = listOf("ParentFace"),
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = childCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "ChildFace",
                name = "value",
                descriptor = "()I",
                isStatic = false,
                code = childCode,
                maxStack = 1,
                maxLocals = 1,
            ),
            hierarchy.resolveInterfaceMethodTarget(
                receiverClassName = "Example",
                name = "value",
                descriptor = "()I",
            ),
        )
    }

    @Test
    fun `interface method target resolution lets abstract child suppress parent default`() {
        val parentCode = byteArrayOf(0x05)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("ParentFace", "ChildFace"),
                ),
                JvmClassDefinition(
                    internalName = "ParentFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = parentCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
                JvmClassDefinition(
                    internalName = "ChildFace",
                    interfaceNames = listOf("ParentFace"),
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            isAbstract = true,
                        ),
                    ),
                ),
            ),
        )

        val exception = assertFailsWith<JvmAbstractMethodError> {
            hierarchy.resolveInterfaceMethodTarget(
                receiverClassName = "Example",
                name = "value",
                descriptor = "()I",
            )
        }

        assertEquals("java/lang/AbstractMethodError", exception.guestClassName)
        assertEquals("Example.value:()I", exception.message)
    }
    @Test
    fun `interface method target resolution rejects unrelated default method conflict`() {
        val leftCode = byteArrayOf(0x05)
        val rightCode = byteArrayOf(0x06)
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    interfaceNames = listOf("LeftFace", "RightFace"),
                ),
                JvmClassDefinition(
                    internalName = "LeftFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = leftCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
                JvmClassDefinition(
                    internalName = "RightFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            code = rightCode,
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        val exception = assertFailsWith<JvmIncompatibleClassChangeError> {
            hierarchy.resolveInterfaceMethodTarget(
                receiverClassName = "Example",
                name = "value",
                descriptor = "()I",
            )
        }

        assertEquals("java/lang/IncompatibleClassChangeError", exception.guestClassName)
        assertEquals("Example.value:()I", exception.message)
    }

    @Test
    fun `interface method resolution ignores private superinterface fallback methods`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "ExampleFace",
                    isInterface = true,
                    interfaceNames = listOf("PrivateFace"),
                ),
                JvmClassDefinition(
                    internalName = "PrivateFace",
                    isInterface = true,
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "value",
                            descriptor = "()I",
                            isStatic = false,
                            isPrivate = true,
                            code = byteArrayOf(0x05),
                            maxStack = 1,
                            maxLocals = 1,
                        ),
                    ),
                ),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveInterfaceMethod(
                ownerClassName = "ExampleFace",
                name = "value",
                descriptor = "()I",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("ExampleFace.value:()I", exception.message)
    }

    @Test
    fun `class initialization method lookup returns declared static void clinit only`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(
                        JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = true, maxStack = 1),
                    ),
                ),
            ),
        )

        assertEquals(
            JvmResolvedMethod(
                ownerClassName = "Example",
                name = "<clinit>",
                descriptor = "()V",
                isStatic = true,
                maxStack = 1,
            ),
            hierarchy.classInitializationMethod("Example"),
        )
    }

    @Test
    fun `class initialization method lookup does not inherit superclass clinit`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example", superclassName = "Parent"),
                JvmClassDefinition(
                    internalName = "Parent",
                    methods = listOf(JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = true)),
                ),
            ),
        )

        assertNull(hierarchy.classInitializationMethod("Example"))
    }

    @Test
    fun `class initialization method lookup ignores invalid clinit shapes already rejected by classfile validation`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    methods = listOf(
                        JvmMethodDefinition(name = "<clinit>", descriptor = "(I)V", isStatic = true),
                        JvmMethodDefinition(name = "<clinit>", descriptor = "()V", isStatic = false),
                    ),
                ),
            ),
        )

        assertNull(hierarchy.classInitializationMethod("Example"))
    }

    @Test
    fun `method handle invoke declarations are recognized as signature polymorphic`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "java/lang/invoke/MethodHandle",
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "invokeExact",
                            descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                            isStatic = false,
                            isNative = true,
                            isVarargs = true,
                        ),
                    ),
                ),
            ),
        )

        val method = hierarchy.resolveMethod(
            ownerClassName = "java/lang/invoke/MethodHandle",
            name = "invokeExact",
            descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
        )

        assertEquals(true, method.isSignaturePolymorphic)
    }

    @Test
    fun `signature polymorphic recognition requires method handle owner native varargs and erased descriptor`() {
        val declarations = listOf(
            JvmMethodDefinition(
                name = "invoke",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                isStatic = false,
                isNative = true,
                isVarargs = true,
            ),
            JvmMethodDefinition(
                name = "invoke",
                descriptor = "(I)I",
                isStatic = false,
                isNative = true,
                isVarargs = true,
            ),
        )
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "java/lang/invoke/MethodHandle",
                    methods = declarations,
                ),
                JvmClassDefinition(
                    internalName = "other/MethodHandle",
                    methods = listOf(declarations.first()),
                ),
            ),
        )

        assertEquals(
            true,
            hierarchy.resolveMethod(
                ownerClassName = "java/lang/invoke/MethodHandle",
                name = "invoke",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
            ).isSignaturePolymorphic,
        )
        assertEquals(
            false,
            JvmResolvedMethod(
                ownerClassName = "java/lang/invoke/MethodHandle",
                name = "invoke",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                isStatic = false,
                isNative = false,
                isVarargs = true,
            ).isSignaturePolymorphic,
        )
        assertEquals(
            false,
            JvmResolvedMethod(
                ownerClassName = "java/lang/invoke/MethodHandle",
                name = "invoke",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                isStatic = false,
                isNative = true,
                isVarargs = false,
            ).isSignaturePolymorphic,
        )
        assertEquals(
            false,
            hierarchy.resolveMethod(
                ownerClassName = "java/lang/invoke/MethodHandle",
                name = "invoke",
                descriptor = "(I)I",
            ).isSignaturePolymorphic,
        )
        assertEquals(
            false,
            hierarchy.resolveMethod(
                ownerClassName = "other/MethodHandle",
                name = "invoke",
                descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
            ).isSignaturePolymorphic,
        )
    }

    @Test
    fun `method resolution maps signature polymorphic call site descriptors to the erased declaration`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "java/lang/invoke/MethodHandle",
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "invokeExact",
                            descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                            isStatic = false,
                            isNative = true,
                            isVarargs = true,
                        ),
                    ),
                ),
            ),
        )

        val resolved = hierarchy.resolveMethod(
            ownerClassName = "java/lang/invoke/MethodHandle",
            name = "invokeExact",
            descriptor = "(Ljava/lang/String;I)J",
        )

        assertEquals("java/lang/invoke/MethodHandle", resolved.ownerClassName)
        assertEquals("invokeExact", resolved.name)
        assertEquals("(Ljava/lang/String;I)J", resolved.descriptor)
        assertEquals("([Ljava/lang/Object;)Ljava/lang/Object;", resolved.signaturePolymorphicDeclarationDescriptor)
        assertEquals(true, resolved.isSignaturePolymorphic)
    }

    @Test
    fun `method resolution does not apply signature polymorphic fallback to ordinary method handle methods`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "java/lang/invoke/MethodHandle",
                    methods = listOf(
                        JvmMethodDefinition(
                            name = "invokeExact",
                            descriptor = "([Ljava/lang/Object;)Ljava/lang/Object;",
                            isStatic = false,
                            isNative = true,
                            isVarargs = true,
                        ),
                    ),
                ),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "java/lang/invoke/MethodHandle",
                name = "bindTo",
                descriptor = "(Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals(
            "java/lang/invoke/MethodHandle.bindTo:(Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
            exception.message,
        )
    }

    @Test
    fun `class hierarchy exposes only the direct superclass name`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(
                    internalName = "Example",
                    superclassName = "Parent",
                ),
                JvmClassDefinition(
                    internalName = "Parent",
                    superclassName = "Grandparent",
                ),
                JvmClassDefinition(internalName = "Grandparent"),
            ),
        )

        assertEquals("Parent", hierarchy.directSuperclassName("Example"))
        assertEquals("Grandparent", hierarchy.directSuperclassName("Parent"))
        assertNull(hierarchy.directSuperclassName("Grandparent"))
        assertNull(hierarchy.directSuperclassName("Missing"))
    }

    @Test
    fun `method resolution throws guest NoSuchMethodError when lookup misses`() {
        val hierarchy = JvmClassHierarchy(
            listOf(
                JvmClassDefinition(internalName = "Example"),
            ),
        )

        val exception = assertFailsWith<JvmNoSuchMethodError> {
            hierarchy.resolveMethod(
                ownerClassName = "Example",
                name = "missing",
                descriptor = "()V",
            )
        }

        assertEquals("java/lang/NoSuchMethodError", exception.guestClassName)
        assertEquals("Example.missing:()V", exception.message)
    }
}
