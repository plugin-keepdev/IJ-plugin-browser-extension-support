package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class PackAsZipAction(
    text: String = ".zip for extension store",
    description: String = "Pack project files into a .zip archive"
) : AnAction(text, description, null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val projectPath: String = project.basePath ?: return

        val zipPath = Paths.get(projectPath, "${project.name}.zip")

        try {
            ZipOutputStream(Files.newOutputStream(zipPath)).use { zos ->
                Files.walk(Paths.get(projectPath)).forEach { path ->
                    if (Files.isRegularFile(path)) {
                        zos.putNextEntry(ZipEntry(Paths.get(projectPath).relativize(path).toString()))
                        Files.copy(path, zos)
                        zos.closeEntry()
                    }
                }
            }
            println("Project packed as .zip: $zipPath")
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

