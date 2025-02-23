package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.intellij.openapi.fileEditor.FileDocumentManager

open class PackAsZipAction(
    text: String = ".zip for extension store",
    description: String = "Pack project files into a .zip archive"
) : AnAction(text, description, null) {
    override fun actionPerformed(e: AnActionEvent) {

        val project: Project = e.project ?: return
        // Save user choice as "zip"
        project.service<UserChoiceService>().setUserChoice("zip")
        val projectPath: String = project.basePath ?: return

        val zipPath = Paths.get(projectPath, "${project.name}.zip")

        FileDocumentManager.getInstance().saveAllDocuments()

        try {
            // Delete the existing .zip file if it exists
            if (Files.exists(zipPath)) {
                Files.delete(zipPath)
            }

            // Create the .zip file
            ZipOutputStream(Files.newOutputStream(zipPath)).use { zos ->
                Files.walk(Paths.get(projectPath)).forEach { path ->
                    if (shouldInclude(path, projectPath)) {
                        zos.putNextEntry(ZipEntry(Paths.get(projectPath).relativize(path).toString()))
                        Files.copy(path, zos)
                        zos.closeEntry()
                    }
                }
            }

            // Ensure changes are committed to disk and visible in the file system
            val localFileSystem = LocalFileSystem.getInstance()

// Refresh the zip file itself synchronously
            val zipVFile = localFileSystem.refreshAndFindFileByNioFile(zipPath)
            zipVFile?.let {
                // Use markDirtyAndRefresh for better handling
                VfsUtil.markDirtyAndRefresh(
                    /* async = */ false,    // Synchronous refresh
                    /* recursive = */ false, // Single file
                    /* reloadChildren = */ false,
                    it
                )
            }

// Refresh the project directory synchronously
            val projectDirVFile = localFileSystem.refreshAndFindFileByNioFile(Paths.get(projectPath))
            projectDirVFile?.let {
                // Use markDirtyAndRefresh for recursive updates
                VfsUtil.markDirtyAndRefresh(
                    /* async = */ false,     // Synchronous refresh
                    /* recursive = */ true,  // Refresh all directories
                    /* reloadChildren = */ true, // Reload children
                    it
                )
            }

// Ensure file system sync to finalize all updates
            VirtualFileManager.getInstance().syncRefresh()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    // Function to filter excluded files/directories
    private fun shouldInclude(path: Path, projectPath: String): Boolean {
        // Exclude `.DS_Store` globally
        if (path.fileName.toString() == ".DS_Store") return false
        // Exclude the `.idea` directory and its contents
        if (path.startsWith(Paths.get(projectPath, ".idea"))) return false
        // Exclude files with `.xpi` or `.zip` extensions
        if (path.fileName.toString().endsWith(".xpi") || path.fileName.toString().endsWith(".zip")) return false
        // Exclude `icons/.DS_Store`
        if (path == Paths.get(projectPath, "icons", ".DS_Store")) return false
        // Add only regular files (skip directories themselves as entries)
        return Files.isRegularFile(path)
    }
}
