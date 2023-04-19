package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.core.model.action.Action
import org.telegram.telegrambots.meta.bots.AbsSender

interface ActionHandler {
    suspend fun handle(action: Action, sender: AbsSender): Boolean
}
