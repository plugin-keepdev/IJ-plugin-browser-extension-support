package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.*

class PackActionGroup : ActionGroup() {
    private var lastChosenAction: AnAction = PackAsZipAction() // Default action, set to .zip by default

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            PackAsZipAction().also { if (lastChosenAction is PackAsZipAction) lastChosenAction = it },
            PackAsXpiAction().also { if (lastChosenAction is PackAsXpiAction) lastChosenAction = it }
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        // Execute the last chosen action
        lastChosenAction.actionPerformed(e)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Pack Project (" + (if (lastChosenAction is PackAsZipAction) ".zip" else ".xpi") + ")"
        e.presentation.isPopupGroup = true // Enables dropdown functionality
    }
}