package dev.storozhenko.familybot.telegram.mappers

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TextMessageHandler : MessageHandler() {

    override suspend fun handle(message: Action, sender: AbsSender): Boolean {
        if (message !is SendTextAction) return false

        sendInternal(
            chatId = message.context.chat.idString,
            testEnvironment = false,
            messageId = null,
            update = null,
            text = { message.text },
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
