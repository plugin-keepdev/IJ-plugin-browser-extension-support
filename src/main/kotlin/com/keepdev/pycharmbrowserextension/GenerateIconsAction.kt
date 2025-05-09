package com.keepdev.pycharmbrowserextension

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
// Removed unused import: java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.min // Import min function

class GenerateIconsAction : AnAction() {

    private val TARGET_SIZES = listOf(16, 32, 48, 64, 96, 128)
    private val ICONS_DIR_NAME = "icons"
    private val NOTIFICATION_GROUP_ID = "Browser Extension Generator Notifications" // Match ID in plugin.xml

    // --- update() method remains unchanged ---
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.isEnabledAndVisible = false

        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || virtualFile == null || !virtualFile.isInLocalFileSystem || virtualFile.isDirectory) {
            return
        }

        val parent = virtualFile.parent
        if (parent == null || parent.name != ICONS_DIR_NAME) {
            return
        }

        val extension = virtualFile.extension?.lowercase()
        if (extension !in setOf("png", "jpg", "jpeg")) {
            return
        }

        presentation.text = "Generate Standard Icons from '${virtualFile.name}'"
        presentation.isEnabledAndVisible = true
    }

    // --- actionPerformed() method is mostly unchanged, except the call to the processing function ---
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sourceVirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val iconsDir = sourceVirtualFile.parent ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating Icons", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.text = "Reading source image..."
                indicator.fraction = 0.0

                try {
                    // --- Read Source Image ---
                    val sourceImage: BufferedImage = sourceVirtualFile.inputStream.use { inputStream ->
                        indicator.checkCanceled() // Check for cancellation even before reading if desired
                        ImageIO.read(inputStream)
                            ?: throw IOException("Could not read image file: ${sourceVirtualFile.name}. Is it a valid image?")
                    }

                    indicator.text = "Processing and saving icons..."
                    var count = 0.0
                    val total = TARGET_SIZES.size.toDouble()

                    // --- Perform File Modifications within Write Action ---
                    WriteCommandAction.runWriteCommandAction(project, "Generate Browser Extension Icons", null, Runnable {
                        TARGET_SIZES.forEach { size ->
                            // *** CRUCIAL: Check for cancellation *inside* the loop ***
                            indicator.checkCanceled() // Throws ProcessCanceledException if cancelled

                            indicator.fraction = count / total
                            indicator.text2 = "Generating icon${size}.png"

                            val targetFileName = "icon$size.png"
                            try {
                                // Process and save one icon
                                val finalIcon = createSquareIcon(sourceImage, size)
                                val targetFile = iconsDir.findOrCreateChildData(this, targetFileName)

                                targetFile.getOutputStream(this).use { outputStream ->
                                    if (!ImageIO.write(finalIcon, "png", outputStream)) {
                                        throw IOException("Failed to find writer for PNG format.")
                                    }
                                }
                                count++

                            } catch (ioe: IOException) {
                                // Abort the WriteCommandAction on specific file error
                                throw RuntimeException("Failed to create or write $targetFileName: ${ioe.message}", ioe)
                            } catch (imgEx: Exception) { // Catch potential errors during cropping/resizing too
                                throw RuntimeException("Error processing image for $targetFileName: ${imgEx.message}", imgEx)
                            }
                        } // end forEach size
                    }) // end WriteCommandAction

                    // --- Success ---
                    indicator.fraction = 1.0
                    indicator.text = "Icon generation complete"
                    showNotification(
                        project,
                        "Successfully generated ${TARGET_SIZES.size} icons in '${iconsDir.name}' directory.",
                        NotificationType.INFORMATION
                    )

                    // --- Exception Handling ---

                } catch (pce: ProcessCanceledException) {
                    // *** CATCH ADDED: Handle Cancellation Explicitly ***
                    // This exception is thrown by indicator.checkCanceled()
                    indicator.text = "Icon generation cancelled"
                    // You could show a specific "Cancelled" notification if desired,
                    // but often just letting it stop quietly is fine.
                    // showNotification(project, "Operation cancelled by user.", NotificationType.WARNING)
                    throw pce // *** CRUCIAL: Rethrow PCE to allow task termination ***

                } catch (ex: Exception) {
                    // *** CATCH MODIFIED: Handles *other* exceptions ***
                    // This catches IOExceptions from initial read, RuntimeExceptions
                    // thrown from the WriteAction, or any other unexpected errors.
                    indicator.text = "Icon generation failed"
                    showNotification(
                        project,
                        "Error generating icons: ${ex.message}", // Show the specific error
                        NotificationType.ERROR
                    )
                    // No need to rethrow here usually, just report the error.
                }
            } // end run
        }) // end ProgressManager.run
    }

    // --- getActionUpdateThread() method remains unchanged ---
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * Creates a square icon of the target size from a source image.
     * If the source is not square, it crops the center square portion before resizing.
     */
    private fun createSquareIcon(sourceImage: BufferedImage, targetSize: Int): BufferedImage {
        val sourceWidth = sourceImage.width
        val sourceHeight = sourceImage.height

        // 1. Determine the square region to crop from the source
        val cropSize = min(sourceWidth, sourceHeight) // The dimension of the square to crop
        // Calculate top-left corner (x, y) of the crop area in the source image
        val cropX = if (sourceWidth > cropSize) (sourceWidth - cropSize) / 2 else 0
        val cropY = if (sourceHeight > cropSize) (sourceHeight - cropSize) / 2 else 0

        // 2. Create the intermediate cropped square image
        // getSubimage creates a view into the original image data
        val croppedSource: BufferedImage = sourceImage.getSubimage(cropX, cropY, cropSize, cropSize)

        // 3. Create the final target canvas (always square, use ARGB for transparency)
        val targetImage = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
        val g = targetImage.createGraphics()

        try {
            // 4. Set rendering hints for better quality scaling
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // 5. Draw the cropped source image onto the target canvas, scaling it down/up to the target size
            // Source is the cropped square (cropX, cropY, cropSize, cropSize)
            // Destination is the entire target canvas (0, 0, targetSize, targetSize)
            g.drawImage(croppedSource, 0, 0, targetSize, targetSize, null)

        } finally {
            g.dispose() // IMPORTANT: Release system resources used by the graphics context
        }

        return targetImage
    }


    // --- showNotification() method remains unchanged ---
    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(content, type)
            .notify(project)
    }
}