package com.keepdev.pycharmbrowserextension

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "UserChoiceService", storages = [Storage("PyCharmBrowserExtension.xml")])
@Service(Service.Level.PROJECT)
class UserChoiceService : PersistentStateComponent<UserChoiceService.State> {

    class State {
        var userChoice: String = "zip"
    }

    private var state = State()

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }

    fun getUserChoice(): String {
        return state.userChoice
    }

    fun setUserChoice(choice: String) {
        state.userChoice = choice
    }
}
