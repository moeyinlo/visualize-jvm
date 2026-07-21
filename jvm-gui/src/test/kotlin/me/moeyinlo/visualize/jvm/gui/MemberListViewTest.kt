package me.moeyinlo.visualize.jvm.gui

import me.moeyinlo.visualize.jvm.classfile.ClassFileParser
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberListViewTest {
    @Test
    fun `member list model extracts fields and methods from class file`(@TempDir tempDir: Path) {
        val classFile = compileJavaFixture(
            tempDir,
            "Sample.java",
            """
            public class Sample {
                public int count;
                private static java.lang.String name;
                public Sample() {}
                public long value(int input) { return input + count; }
            }
            """.trimIndent(),
        )
        val parsed = ClassFileParser.parse(Files.readAllBytes(classFile), source = classFile.toString())

        val model = MemberListModel.fromClassFile(parsed)

        assertEquals(
            listOf(
                MemberListItem(MemberKind.Field, "count", "I"),
                MemberListItem(MemberKind.Field, "name", "Ljava/lang/String;"),
                MemberListItem(MemberKind.Method, "<init>", "()V"),
                MemberListItem(MemberKind.Method, "value", "(I)J"),
            ),
            model.items,
        )
    }

    @Test
    fun `member list view is exposed as a JavaFX list view type`() {
        assertEquals("Members", MemberListViewModel.Title)
        assertTrue(javafx.scene.control.ListView::class.java.isAssignableFrom(MemberListView::class.java))
    }

    private fun compileJavaFixture(tempDir: Path, sourceName: String, source: String): Path {
        val sourcePath = tempDir.resolve(sourceName)
        Files.writeString(sourcePath, source)
        val compiler = ToolProvider.getSystemJavaCompiler()
            ?: error("JDK compiler is required for GUI member list fixture")
        val success = compiler.run(null, null, null, "-d", tempDir.toString(), sourcePath.toString()) == 0
        check(success) { "fixture compilation failed" }
        return tempDir.resolve(sourceName.removeSuffix(".java") + ".class")
    }
}
