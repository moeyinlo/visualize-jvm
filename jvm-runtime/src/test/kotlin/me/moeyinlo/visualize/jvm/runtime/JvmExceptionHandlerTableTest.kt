package me.moeyinlo.visualize.jvm.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JvmExceptionHandlerTableTest {
    @Test
    fun `handler lookup matches protected range start inclusively and end exclusively`() {
        val handler = handler(startPc = 2, endPc = 5, handlerPc = 9, catchClassName = "java/lang/RuntimeException")
        val hierarchy = hierarchy()

        assertNull(
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(handler),
                thrownAtPc = 1,
                throwableClassName = "java/lang/RuntimeException",
                classHierarchy = hierarchy,
            ),
        )
        assertEquals(
            handler,
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(handler),
                thrownAtPc = 2,
                throwableClassName = "java/lang/RuntimeException",
                classHierarchy = hierarchy,
            ),
        )
        assertNull(
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(handler),
                thrownAtPc = 5,
                throwableClassName = "java/lang/RuntimeException",
                classHierarchy = hierarchy,
            ),
        )
    }

    @Test
    fun `handler lookup selects the first matching handler in table order`() {
        val first = handler(startPc = 0, endPc = 3, handlerPc = 7, catchClassName = "java/lang/RuntimeException")
        val second = handler(startPc = 0, endPc = 3, handlerPc = 9, catchClassName = "java/lang/Throwable")

        assertEquals(
            first,
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(first, second),
                thrownAtPc = 1,
                throwableClassName = "pkg/CustomException",
                classHierarchy = hierarchy(),
            ),
        )
    }

    @Test
    fun `handler lookup treats catch all handlers as matching any throwable class`() {
        val catchAll = handler(startPc = 0, endPc = 3, handlerPc = 7, catchClassName = null)

        assertEquals(
            catchAll,
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(catchAll),
                thrownAtPc = 1,
                throwableClassName = "pkg/UnrelatedThrowable",
                classHierarchy = JvmClassHierarchy.Empty,
            ),
        )
    }

    @Test
    fun `handler lookup rejects non assignable typed catches`() {
        val handler = handler(startPc = 0, endPc = 3, handlerPc = 7, catchClassName = "java/lang/RuntimeException")

        assertNull(
            JvmExceptionHandlerTable.findHandler(
                handlers = listOf(handler),
                thrownAtPc = 1,
                throwableClassName = "java/lang/Error",
                classHierarchy = hierarchy(),
            ),
        )
    }

    @Test
    fun `handler model and lookup validate non negative offsets and class names`() {
        assertFailsWith<IllegalArgumentException> { handler(startPc = -1, endPc = 1, handlerPc = 1) }
        assertFailsWith<IllegalArgumentException> { handler(startPc = 2, endPc = 1, handlerPc = 1) }
        assertFailsWith<IllegalArgumentException> { handler(startPc = 0, endPc = 1, handlerPc = -1) }
        assertFailsWith<IllegalArgumentException> {
            JvmExceptionHandlerTable.findHandler(
                handlers = emptyList(),
                thrownAtPc = -1,
                throwableClassName = "java/lang/Throwable",
                classHierarchy = JvmClassHierarchy.Empty,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            JvmExceptionHandlerTable.findHandler(
                handlers = emptyList(),
                thrownAtPc = 0,
                throwableClassName = "",
                classHierarchy = JvmClassHierarchy.Empty,
            )
        }
    }

    private fun handler(
        startPc: Int,
        endPc: Int,
        handlerPc: Int,
        catchClassName: String? = "java/lang/Throwable",
    ): JvmExceptionHandler = JvmExceptionHandler(
        startPc = startPc,
        endPc = endPc,
        handlerPc = handlerPc,
        catchClassName = catchClassName,
    )

    private fun hierarchy(): JvmClassHierarchy = JvmClassHierarchy(
        listOf(
            JvmClassDefinition(internalName = "java/lang/Throwable", superclassName = "java/lang/Object"),
            JvmClassDefinition(internalName = "java/lang/Exception", superclassName = "java/lang/Throwable"),
            JvmClassDefinition(internalName = "java/lang/RuntimeException", superclassName = "java/lang/Exception"),
            JvmClassDefinition(internalName = "pkg/CustomException", superclassName = "java/lang/RuntimeException"),
            JvmClassDefinition(internalName = "java/lang/Error", superclassName = "java/lang/Throwable"),
        ),
    )
}
