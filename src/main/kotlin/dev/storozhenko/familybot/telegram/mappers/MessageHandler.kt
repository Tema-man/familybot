package dev.storozhenko.familybot.telegram.mappers

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.telegram.SenderLogger
import kotlinx.coroutines.delay
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

abstract class MessageHandler {
    abstract suspend fun handle(message: Message, sender: AbsSender): Boolean

    protected suspend fun sendInternal(
        chatId: String,
        testEnvironment: Boolean,
        messageId: Int? = null,
        update: Update? = null,
        text: suspend () -> String,
        replyMessageId: Int? = null,
        enableHtml: Boolean = false,
        replyToUpdate: Boolean = false,
        customization: SendMessage.() -> Unit = { },
        shouldTypeBeforeSend: Boolean = false,
        typeDelay: Pair<Int, Int> = 1000 to 2000,
        sender: AbsSender
    ): org.telegram.telegrambots.meta.api.objects.Message {
        SenderLogger.log.info(
            "Sending message, update=${update?.toJson() ?: "[N/A]"}, " +
                "replyMessageId=$replyMessageId," +
                "enableHtml=$enableHtml," +
                "replyToUpdate=$replyToUpdate," +
                "shouldTypeBeforeSend=$shouldTypeBeforeSend," +
                "typeDelay=$typeDelay"
        )
        if (shouldTypeBeforeSend) {
            sender.execute(SendChatAction(chatId, "typing", null))
            if (testEnvironment.not()) {
                delay(randomInt(typeDelay.first, typeDelay.second).toLong())
            }
        }
        val textToSend = text()
        SenderLogger.log.info("Sending message, text=$textToSend")
        return textToSend
            .chunked(3900)
            .map {
                SendMessage(chatId, textToSend)
                    .apply {
                        enableHtml(enableHtml)
                        if (replyMessageId != null) {
                            replyToMessageId = replyMessageId
                        }
                        if (replyToUpdate) {
                            replyToMessageId = messageId
                        }
                        customization()
                    }
            }.map { message ->
                SenderLogger.log.info("Sending message: ${message.toJson()}")
                sender.execute(message)
            }.first()
    }
}
