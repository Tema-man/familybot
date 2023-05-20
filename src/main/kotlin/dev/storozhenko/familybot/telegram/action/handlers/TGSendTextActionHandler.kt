package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.telegram.SenderLogger
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.bots.AbsSender
import kotlin.math.ln

@Component
class TGSendTextActionHandler : TGActionHandler {

    override val actionClass = SendTextAction::class.java

    override suspend fun handle(action: Action, sender: AbsSender): Boolean {
        if (action !is SendTextAction) return false

        sendInternal(
            chatId = action.chat.idString,
            testEnvironment = false,
            text = { action.text },
            replyMessageId = action.asReplyToIntentId?.toIntOrNull(),
            enableHtml = action.enableRichFormatting,
            customization = {
                disableWebPagePreview = action.silent
                disableNotification = action.silent
            },
            shouldTypeBeforeSend = action.showTypeDelay,
            sender = sender
        )

        return true
    }

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
        val baseline = ln(if (messageLength >= 500) 500.0 else messageLength.toDouble())
        val min = baseline * ONE_SYMBOL_TYPING_SPEED_MILLS * 0.9f
        val max = baseline * ONE_SYMBOL_TYPING_SPEED_MILLS * 1.2f
        return randomInt(min.toInt(), max.toInt()).toLong()
    }

    private companion object {
        const val ONE_SYMBOL_TYPING_SPEED_MILLS = 20
    }
}
