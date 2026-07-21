package me.moeyinlo.visualize.jvm.gui

import javafx.geometry.Insets
import javafx.scene.control.Button
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
    const val ImportButtonText: String = "Import JAR/Class"
}

class ProjectClasspathPanel(
    project: JvmGuiProjectModel = JvmGuiProjectModel(),
) : VBox() {
    val importButton: Button = Button(ProjectClasspathPanelModel.ImportButtonText)
    val classpathListView: ListView<String> = ListView<String>()
    var project: JvmGuiProjectModel = project
        private set

    init {
        spacing = 8.0
        padding = Insets(12.0)
        children += Label(ProjectClasspathPanelModel.Title)
        children += importButton
        children += classpathListView
        VBox.setVgrow(classpathListView, Priority.ALWAYS)
        setProject(project)
    }

    fun setProject(project: JvmGuiProjectModel) {
        this.project = project
        classpathListView.items.setAll(project.classpathEntries.map(Path::toString))
    }

    fun importClasspathEntries(entries: List<Path>): ClasspathImportResult {
        val result = ClasspathImportAction.importFiles(project, entries)
        setProject(result.project)
        return result
    }
}
