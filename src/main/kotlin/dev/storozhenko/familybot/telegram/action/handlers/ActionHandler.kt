package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.telegram.SenderLogger
import kotlinx.coroutines.delay
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.bots.AbsSender

abstract class ActionHandler {
    abstract suspend fun handle(action: Action, sender: AbsSender): Boolean

    protected suspend fun sendInternal(
        chatId: String,
        testEnvironment: Boolean,
        text: suspend () -> String,
        replyMessageId: Int? = null,
        enableHtml: Boolean = false,
        customization: SendMessage.() -> Unit = { },
        shouldTypeBeforeSend: Boolean = false,
        sender: AbsSender
    ): Message {
        val textToSend = text()
        val typeDelay = if (shouldTypeBeforeSend) generateTypeDelay(textToSend.length) else 0

        SenderLogger.log.info(
            buildString {
                append("--> Sending message to Telegram:")
                append(" | enableHtml = $enableHtml")
                append(" | replyMessageId = $replyMessageId")
                append(" | replyToUpdate = ${replyMessageId != null}")
                append(" | shouldTypeBeforeSend = $shouldTypeBeforeSend")
                append(" | typeDelay = $typeDelay")
            }
        )

        if (shouldTypeBeforeSend) {
            sender.execute(SendChatAction(chatId, "typing", null))
            if (testEnvironment.not()) delay(typeDelay)
        }

        return textToSend
            .chunked(3900)
            .map {
                SendMessage(chatId, textToSend).apply {
                    enableHtml(enableHtml)
                    if (replyMessageId != null) replyToMessageId = replyMessageId
                    customization()
                }
            }
            .mapIndexed { index, message ->
                SenderLogger.log.info("--> text_chunk_$index = ${message.toJson()}")
                sender.execute(message)
            }
            .first()
            .also {
                SenderLogger.log.info("--> (end)\n")
            }
    }

    private fun generateTypeDelay(messageLength: Int): Long {
        val baseline = if (messageLength >= 500) 10 else messageLength
        val min = baseline * ONE_SYMBOL_TYPING_SPEED_MILLS * 0.9f
        val max = baseline * ONE_SYMBOL_TYPING_SPEED_MILLS * 1.2f
        return randomInt(min.toInt(), max.toInt()).toLong()
    }

    private companion object {
        const val ONE_SYMBOL_TYPING_SPEED_MILLS = 20
    }
}
