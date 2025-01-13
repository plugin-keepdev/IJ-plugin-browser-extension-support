package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class PackAsXpiAction : PackAsZipAction(
    ".xpi for Firefox", // New text
    "Pack project files into a .xpi archive" // New description
) {
    override fun actionPerformed(e: AnActionEvent) {
        super.actionPerformed(e) // Perform `.zip` packing first
        val project = e.project ?: return
        val projectPath = project.basePath ?: return

        val zipPath = Paths.get(projectPath, "${project.name}.zip")
        val xpiPath = Paths.get(projectPath, "${project.name}.xpi")

        try {
            java.nio.file.Files.move(zipPath, xpiPath, StandardCopyOption.REPLACE_EXISTING)
            println("Project packed as .xpi: $xpiPath")
            val localFileSystem = LocalFileSystem.getInstance()
            // Find VirtualFile for the project folder:
            val projectDirVFile = localFileSystem.refreshAndFindFileByNioFile(Paths.get(projectPath))
            // Force a synchronous refresh on the project directory, recursively:
            projectDirVFile?.refresh(/* asynchronous = */ false, /* recursive = */ true)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
