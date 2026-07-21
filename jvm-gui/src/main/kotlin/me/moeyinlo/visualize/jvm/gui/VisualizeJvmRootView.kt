package me.moeyinlo.visualize.jvm.gui

import javafx.scene.layout.BorderPane

class VisualizeJvmRootView(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : BorderPane() {
    val projectClasspathPanel: ProjectClasspathPanel = ProjectClasspathPanel(project)
    val classTreeView: ClassTreeView = ClassTreeView(ClassTreeModel.fromClasspathEntries(project.classpathEntries))

    init {
        left = projectClasspathPanel
        center = classTreeView
    }
}
