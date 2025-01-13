package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

open class PackActionGroup : DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            PackAsZipAction(),
            PackAsCrxAction(),
            PackAsXpiAction()
        )
    }
}
