package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class SetDefaultToZipAction : AnAction("Use .zip as default") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        project.service<UserChoiceService>().setUserChoice("zip")
    }
}
