package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

open class PackAsXpiAction : PackAsZipAction(
    ".xpi for Firefox", // New text
    "Pack project files into a .xpi archive" // New description
) {
    override fun actionPerformed(e: AnActionEvent) {
        super.actionPerformed(e) // Perform `.zip` packing first
        val project = e.project ?: return
        project.service<UserChoiceService>().setUserChoice("xpi")
        val projectPath = project.basePath ?: return

        val zipPath = Paths.get(projectPath, "${project.name}.zip")
        val xpiPath = Paths.get(projectPath, "${project.name}.xpi")

        try {
            // Ensure old .xpi file is deleted if it exists
            if (Files.exists(xpiPath)) {
                Files.delete(xpiPath)
            }

            // Move .zip to .xpi
            Files.move(zipPath, xpiPath, StandardCopyOption.REPLACE_EXISTING)

            // Refresh file system changes
            val localFileSystem = LocalFileSystem.getInstance()

            // Refresh the .xpi file (marked as dirty)
            val xpiVFile = localFileSystem.refreshAndFindFileByNioFile(xpiPath)
            xpiVFile?.let {
                VfsUtil.markDirtyAndRefresh(
                    /* async = */ false,     // Synchronous
                    /* recursive = */ false, // Individual file
                    /* reloadChildren = */ false,
                    it
                )
            }

            // Refresh the project directory to ensure .zip is removed from project tree
            val projectDirVFile = localFileSystem.refreshAndFindFileByNioFile(Paths.get(projectPath))
            projectDirVFile?.let {
                VfsUtil.markDirtyAndRefresh(
                    /* async = */ false,     // Synchronous
                    /* recursive = */ true,  // Ensure all contents are refreshed recursively
                    /* reloadChildren = */ true,
                    it
                )
            }

            // Force synchronize the Virtual File System
            VirtualFileManager.getInstance().syncRefresh()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}