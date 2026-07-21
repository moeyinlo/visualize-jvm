package me.moeyinlo.visualize.jvm.gui

import javafx.scene.control.Label
import javafx.scene.layout.BorderPane

class VisualizeJvmRootView(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : BorderPane() {
    val projectClasspathPanel: ProjectClasspathPanel = ProjectClasspathPanel(project)

    init {
        left = projectClasspathPanel
        center = Label("Select a classpath entry to inspect JVM structures")
    }
}
