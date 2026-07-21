package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

data class ClassTreeClassNode(
    val internalName: String,
    val source: Path,
)

data class ClassTreeModel(
    val classes: List<ClassTreeClassNode> = emptyList(),
) {
    companion object {
        fun fromClasspathEntries(entries: List<Path>): ClassTreeModel {
            val classes = entries.flatMap { entry -> entry.discoverClassNodes() }
                .distinctBy { node -> node.internalName }
                .sortedBy { node -> node.internalName }
            return ClassTreeModel(classes)
        }

        private fun Path.discoverClassNodes(): List<ClassTreeClassNode> {
            if (!Files.isRegularFile(this)) {
                return emptyList()
            }
            val fileName = fileName?.toString().orEmpty()
            return when {
                fileName.endsWith(".class", ignoreCase = true) -> listOf(
                    ClassTreeClassNode(
                        internalName = fileName.removeSuffix(".class"),
                        source = this,
                    ),
                )
                fileName.endsWith(".jar", ignoreCase = true) -> JarFile(toFile()).use { jar ->
                    jar.entries().asSequence()
                        .filter { entry -> !entry.isDirectory && entry.name.endsWith(".class") }
                        .map { entry ->
                            ClassTreeClassNode(
                                internalName = entry.name.removeSuffix(".class"),
                                source = this,
                            )
                        }
                        .toList()
                }
                else -> emptyList()
            }
        }
    }
}

object ClassTreeViewModel {
    const val Title: String = "Classes"
}

class ClassTreeView(
    model: ClassTreeModel = ClassTreeModel(),
) : TreeView<String>() {
    init {
        setModel(model)
    }

    fun setModel(model: ClassTreeModel) {
        root = TreeItem(ClassTreeViewModel.Title).also { rootItem ->
            rootItem.isExpanded = true
            model.classes.forEach { classNode ->
                rootItem.children += TreeItem(classNode.internalName)
            }
        }
        isShowRoot = true
    }
}
