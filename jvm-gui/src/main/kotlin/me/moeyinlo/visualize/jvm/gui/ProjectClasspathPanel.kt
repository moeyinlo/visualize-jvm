package me.moeyinlo.visualize.jvm.gui

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import java.nio.file.Path

data class JvmGuiProjectModel(
    val classpathEntries: List<Path> = emptyList(),
)

object ProjectClasspathPanelModel {
    const val Title: String = "Project / Classpath"
}

class ProjectClasspathPanel(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : VBox() {
    val classpathListView: ListView<String> = ListView<String>()

    init {
        spacing = 8.0
        padding = Insets(12.0)
        children += Label(ProjectClasspathPanelModel.Title)
        children += classpathListView
        VBox.setVgrow(classpathListView, Priority.ALWAYS)
        setProject(project)
    }

    fun setProject(project: JvmGuiProjectModel) {
        classpathListView.items.setAll(project.classpathEntries.map(Path::toString))
    }
}
