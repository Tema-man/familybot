package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class SendTextActionHandler : ActionHandler() {

    override suspend fun handle(action: Action, sender: AbsSender): Boolean {
        if (action !is SendTextAction) return false

        sendInternal(
            chatId = action.chat.idString,
            testEnvironment = false,
            text = { action.text },
            replyMessageId = action.asReplyToIntentId?.toIntOrNull(),
            enableHtml = action.formatAsHtml,
            customization = {},
            shouldTypeBeforeSend = action.showTypeDelay,
            sender = sender
        )

        return true
    }
}
