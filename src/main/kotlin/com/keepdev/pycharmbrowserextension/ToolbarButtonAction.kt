package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class ToolbarButtonAction : ActionGroup() {
    private val packAsXpiAction = PackAsXpiAction()
    private val packAsZipAction = PackAsZipAction()

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(packAsZipAction, packAsXpiAction)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val stateService = project.service<UserChoiceService>()

        // Perform the last chosen default action
        when (stateService.getUserChoice()) {
            "xpi" -> packAsXpiAction.actionPerformed(e)
            else -> packAsZipAction.actionPerformed(e)
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val stateService = project.service<UserChoiceService>()

        // Update the button's text based on the last user choice
        val actionName = when (stateService.getUserChoice()) {
            "xpi" -> "Pack as .xpi"
            else -> "Pack as .zip"
        }
        e.presentation.text = actionName
        e.presentation.isPopupGroup = true  // Enable dropdown functionality
    }
}