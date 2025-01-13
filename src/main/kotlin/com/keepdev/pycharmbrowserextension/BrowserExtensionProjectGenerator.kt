package com.keepdev.pycharmbrowserextension

import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.openapi.util.IconLoader
import java.io.IOException
import javax.swing.Icon

class BrowserExtensionProjectGenerator : DirectoryProjectGenerator<Any> {

    override fun getName(): String = "Browser Extension"

    override fun getLogo(): Icon? {
        return IconLoader.getIcon("/META-INF/pluginIcon.svg", this::class.java)
    }
    override fun validate(baseDirPath: String): ValidationResult = ValidationResult.OK

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: Any,
        module: Module
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            // 1) Create "icons" directory
            val iconsDir = baseDir.createChildDirectory(this, "icons")

            // 2) Copy puzzle icon files (converted to PNG) from plugin resources to "icons" directory
            copyIconResource(iconsDir, "icon16.png")
            copyIconResource(iconsDir, "icon32.png")
            copyIconResource(iconsDir, "icon48.png")
            copyIconResource(iconsDir, "icon128.png")

            // 3) Create manifest.json
            val manifestFile = baseDir.createChildData(this, "manifest.json")
            val manifestContent = """
                {
                  "manifest_version": 3,
                  "name": "Generic Browser Extension",
                  "version": "1.0",
                  "description": "A generic boilerplate for building browser extensions",
                  "permissions": ["activeTab"],
                  "action": {
                    "default_icon": {
                      "16": "icons/icon16.png",
                      "32": "icons/icon32.png",
                      "48": "icons/icon48.png",
                      "128": "icons/icon128.png"
                    }
                  },
                  "background": {
                    "scripts": ["background.js"]
                  },
                  "content_scripts": [{
                    "matches": ["<all_urls>"],
                    "js": ["content.js"]
                  }]
                }
            """.trimIndent()
            VfsUtil.saveText(manifestFile, manifestContent)

            // 4) Create background.js with explanatory comments
            val backgroundJsFile = baseDir.createChildData(this, "background.js")
            val backgroundJs = """
                // background.js
                // ---------------------------------------------------------
                // This script runs in the background, separate from any webpage content.
                // It can listen for extension events, perform network requests,
                // handle messaging between different parts of the extension, etc.
                
                console.log("Hello from background.js! (Generic Browser Extension)");
                
                // Example: Listening for an install event
                // self.addEventListener('install', event => {
                //   console.log("Extension installed!");
                // });
            """.trimIndent()
            VfsUtil.saveText(backgroundJsFile, backgroundJs)

            // 5) Create content.js with explanatory comments
            val contentJsFile = baseDir.createChildData(this, "content.js")
            val contentJs = """
                // content.js
                // ---------------------------------------------------------
                // This script runs in the context of web pages you specify
                // (in manifest.json under "content_scripts"). It can access
                // and modify the DOM of the page, listen for user actions,
                // and communicate with background.js via messaging.
                
                console.log("Hello from content.js! (Generic Browser Extension)");
                
                // Example: Logging selection changes
                document.addEventListener('mouseup', () => {
                    const selection = window.getSelection()?.toString().trim();
                    if (selection) {
                        console.log("User selected text:", selection);
                    }
                });
            """.trimIndent()
            VfsUtil.saveText(contentJsFile, contentJs)

            // 6) Create README.md
            val readmeFile = baseDir.createChildData(this, "README.md")
            val readmeContent = """
                This is a sample extension. Please note that it may not work in future versions of Chrome and Firefox.
                If you see some error messages or other issues, please let us know by email [ij-browser-extension-support@keepdev.com](mailto:ij-browser-extension-support@keepdev.com).
                
                Please always refer to official instructions on how to build extemsions. 
                Chrome: https://developer.chrome.com/docs/extensions/get-started
                Firefox: https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/Your_first_WebExtension
                
                ## Developing browser extensions for Firefox
                
                Change 
                
                                "manifest_version": 3,
                
                to 
                
                                "manifest_version": 2,
                                
                and the section "action" must be called "browser_action" in the manifest.
                
                Also, add the following section when you need to pack the extension for Firefox (for example, right after the "permissions" section):
                
                                "browser_specific_settings": {
                                    "gecko": {
                                        "strict_min_version": "56.0a1",
                                        "id": "YOUR-SUPPORT-EMAIL-OR-ANOTHER-ID@your-company.com"
                                    }
                                },
                
                REMEMBER TO CHANGE THE "id" VALUE TO A UNIQUE ID FOR EACH EXTENSION YOU CREATE
            """.trimIndent()
            VfsUtil.saveText(readmeFile, readmeContent)
        }
    }

    /**
     * Copies a PNG icon from src/main/resources/icons/<file> into the project's icons/ directory.
     */
    private fun copyIconResource(iconsDir: VirtualFile, iconFileName: String) {
        val resourcePath = "/icons/$iconFileName"
        val inputStream = this::class.java.getResourceAsStream(resourcePath)
            ?: throw IOException("Resource not found: $resourcePath")

        val iconVFile = iconsDir.createChildData(this, iconFileName)
        inputStream.use { input ->
            val bytes = input.readBytes()
            VfsUtil.saveText(iconVFile, "")  // Initialize as empty text
            iconVFile.getOutputStream(this).use { output ->
                output.write(bytes)
            }
        }
    }
}
