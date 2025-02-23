package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class ToolbarButtonAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val stateService = project.service<UserChoiceService>()

        when (stateService.getUserChoice()) {
            "xpi" -> PackAsXpiAction().actionPerformed(e)
            else -> PackAsZipAction().actionPerformed(e)
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val stateService = project.service<UserChoiceService>()
        val actionName = when (stateService.getUserChoice()) {
            "xpi" -> "Pack as .xpi"
            else -> "Pack as .zip"
        }
        e.presentation.text = actionName
    }
}
