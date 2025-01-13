package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class PackAsCrxAction : PackAsZipAction(
    ".crx for Chrome",
    "Pack project files into a .crx archive"
) {
    override fun actionPerformed(e: AnActionEvent) {
        super.actionPerformed(e) // Perform `.zip` packing first
        val project = e.project ?: return
        val projectPath = project.basePath ?: return

        val zipPath = Paths.get(projectPath, "${project.name}.zip")
        val crxPath = Paths.get(projectPath, "${project.name}.crx")

        try {
            java.nio.file.Files.move(zipPath, crxPath, StandardCopyOption.REPLACE_EXISTING)
            println("Project packed as .crx: $crxPath")
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
