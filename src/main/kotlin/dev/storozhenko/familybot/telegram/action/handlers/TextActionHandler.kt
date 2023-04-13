package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TextActionHandler : ActionHandler() {

    override suspend fun handle(action: Action, sender: AbsSender): Boolean {
        if (action !is SendTextAction) return false

        sendInternal(
            chatId = action.chat.idString,
            testEnvironment = false,
            messageId = null,
            update = null,
            text = { action.text },
            replyMessageId = null,
            enableHtml = false,
            replyToUpdate = false,
            customization = {},
            shouldTypeBeforeSend = false,
            typeDelay = 1000 to 2000,
            sender = sender
        )

        return true
    }
}
