package dev.storozhenko.familybot.telegram.mappers

import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.model.message.SimpleTextMessage
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TextMessageHandler : MessageHandler() {

    override suspend fun handle(message: Message, sender: AbsSender): Boolean {
        if (message !is SimpleTextMessage) return false

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
