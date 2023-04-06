package dev.storozhenko.familybot.feature.ask_world

import dev.storozhenko.familybot.common.extensions.boldNullable
import dev.storozhenko.familybot.common.extensions.italic
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.MessageContentType
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.ask_world.model.AskWorldReply
import dev.storozhenko.familybot.telegram.TelegramBot
import dev.storozhenko.familybot.telegram.send
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage

@Component
class AskWorldReceiveReplyExecutor(
    private val askWorldRepository: AskWorldRepository,
    private val botConfig: BotConfig,
    private val dictionary: Dictionary
) : Executor, Configurable {
    private val log = LoggerFactory.getLogger(AskWorldReceiveReplyExecutor::class.java)

    override fun getFunctionId(context: ExecutorContext) = FunctionId.ASK_WORLD

    override fun canExecute(context: ExecutorContext): Boolean {
        val message = context.message
        if (message.isReply.not()) {
            return false
        }

        val chatId = message.chatId
        val messageId = message.replyToMessage.messageId
        if (message.replyToMessage.hasPoll()) {
            return askWorldRepository.findQuestionByMessageId(messageId + chatId, chatId) != null
        }

        val text = message.replyToMessage
            .takeIf { it.from.isBot && it.from.userName == botConfig.botName }
            ?.text
            ?: return false

        val allPrefixes = dictionary.getAll(Phrase.ASK_WORLD_QUESTION_FROM_CHAT)

        return allPrefixes.map { "$it " }.any { text.startsWith(it) }
    }

    override fun priority(context: ExecutorContext): Priority {
        return Priority.LOW
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val message: TelegramMessage = context.message
        val reply = message.text ?: "MEDIA: $message"
        val chat = context.chat
        val user = context.user
        val chatId = chat.id
        val messageId = message.replyToMessage.messageId
        val question =
            askWorldRepository.findQuestionByMessageId(messageId + chatId, chatId) ?: return { null }
        if (askWorldRepository.isReplied(question, chat, user)) {
            return {
                it.execute(
                    SendMessage(
                        chat.idString,
                        context.phrase(Phrase.ASK_WORLD_ANSWER_COULD_BE_ONLY_ONE)
                    ).apply {
                        replyToMessageId = message.messageId
                    }
                )
                null
            }
        }
        val contentType = detectContentType(message)
        val askWorldReply = AskWorldReply(
            null,
            question.id
                ?: throw TelegramBot.InternalException("Question id is missing, seems like internal logic error"),
            reply,
            user,
            chat,
            Instant.now()
        )

        return { sender ->
            runCatching {
                coroutineScope { launch { askWorldRepository.addReply(askWorldReply) } }
                val questionTitle = question.message.takeIf { it.length < 100 }
                    ?: (question.message.take(100) + "...")
                val chatIdToReply = question.chat.idString

                val answerTitle =
                    dictionary.get(Phrase.ASK_WORLD_REPLY_FROM_CHAT, ChatEasyKey(question.chat.id))
                if (contentType == MessageContentType.TEXT) {
                    sendAnswerWithQuestion(
                        sender,
                        chatIdToReply,
                        answerTitle,
                        context,
                        questionTitle,
                        reply
                    )
                } else {
                    sendOnlyQuestion(
                        sender,
                        chatIdToReply,
                        answerTitle,
                        context,
                        questionTitle
                    )
                    dispatchMedia(sender, contentType, chatIdToReply, message)
                }
                sender.send(context, "Принято и отправлено")
            }.onFailure { e ->
                sender.send(context, "Принято")
                log.info("Could not send reply instantly", e)
            }
            null
        }
    }

    private fun dispatchMedia(
        sender: AbsSender,
        contentType: MessageContentType,
        chatIdToReply: String,
        message: TelegramMessage
    ) {
        when (contentType) {
            MessageContentType.PHOTO ->
                sender.execute(sendPhoto(chatIdToReply, message))

            MessageContentType.AUDIO ->
                sender.execute(sendAudio(chatIdToReply, message))

            MessageContentType.ANIMATION -> sender.execute(sendAnimation(chatIdToReply, message))

            MessageContentType.DOCUMENT -> sender.execute(sendDocument(chatIdToReply, message))

            MessageContentType.VOICE ->
                sender.execute(sendVoice(chatIdToReply, message))

            MessageContentType.VIDEO_NOTE ->
                sender.execute(sendVideoNote(chatIdToReply, message))

            MessageContentType.LOCATION -> sender.execute(sendLocation(chatIdToReply, message))

            MessageContentType.STICKER -> sender.execute(sendSticker(chatIdToReply, message))

            MessageContentType.CONTACT -> sender.execute(sendContact(chatIdToReply, message))

            MessageContentType.VIDEO -> sender.execute(sendVideo(chatIdToReply, message))
            else -> log.warn("Something went wrong with content type detection logic")
        }
    }

    private fun sendVideo(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendVideo {
        return SendVideo(
            chatIdToReply,
            InputFile(message.video.fileId)
        ).apply {
            if (message.hasText()) {
                caption = message.text
            }
        }
    }

    private fun sendContact(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendContact {
        return SendContact(
            chatIdToReply,
            message.contact.phoneNumber,
            message.contact.firstName
        ).apply {
            lastName = message.contact.lastName
        }
    }

    private fun sendSticker(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendSticker {
        return SendSticker(chatIdToReply, InputFile(message.sticker.fileId))
    }

    private fun sendLocation(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendLocation {
        return SendLocation(
            chatIdToReply,
            message.location.latitude,
            message.location.longitude
        )
    }

    private fun sendVideoNote(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendVideoNote {
        return SendVideoNote(chatIdToReply, InputFile(message.videoNote.fileId))
    }

    private fun sendVoice(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendVoice {
        return SendVoice(chatIdToReply, InputFile(message.voice.fileId))
    }

    private fun sendDocument(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendDocument {
        return SendDocument(chatIdToReply, InputFile(message.document.fileId)).apply {
            if (message.hasText()) {
                caption = message.text
            }
        }
    }

    private fun sendAnimation(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendAnimation {
        return SendAnimation(chatIdToReply, InputFile(message.animation.fileId))
    }

    private fun sendAudio(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendAudio {
        return SendAudio(
            chatIdToReply,
            InputFile(message.audio.fileId)
        ).apply {
            if (message.hasText()) {
                caption = message.text
            }
        }
    }

    private fun sendPhoto(
        chatIdToReply: String,
        message: TelegramMessage
    ): SendPhoto {
        return SendPhoto(chatIdToReply, InputFile(message.photo.first().fileId))
            .apply {
                if (message.hasText()) {
                    caption = message.text
                }
            }
    }

    private fun sendOnlyQuestion(
        it: AbsSender,
        chatIdToReply: String,
        answerTitle: String,
        context: ExecutorContext,
        questionTitle: String
    ) {
        it.execute(
            SendMessage(
                chatIdToReply,
                "$answerTitle ${context.chat.name.boldNullable()} " +
                    "от ${context.user.getGeneralName()} на вопрос \"$questionTitle\":"
            ).apply {
                enableHtml(true)
            }
        )
    }

    private fun sendAnswerWithQuestion(
        it: AbsSender,
        chatIdToReply: String,
        answerTitle: String,
        context: ExecutorContext,
        questionTitle: String,
        reply: String
    ) {
        it.execute(
            SendMessage(
                chatIdToReply,
                "$answerTitle ${context.chat.name.boldNullable()} " +
                    "от ${context.user.getGeneralName()} на вопрос \"$questionTitle\": ${reply.italic()}"
            ).apply {
                enableHtml(true)
            }
        )
    }

    private fun detectContentType(message: TelegramMessage): MessageContentType {
        return when {
            message.hasLocation() -> MessageContentType.LOCATION
            message.hasAnimation() -> MessageContentType.ANIMATION
            message.hasAudio() -> MessageContentType.AUDIO
            message.hasContact() -> MessageContentType.CONTACT
            message.hasDocument() -> MessageContentType.DOCUMENT
            message.hasPhoto() -> MessageContentType.PHOTO
            message.hasSticker() -> MessageContentType.STICKER
            message.hasVideoNote() -> MessageContentType.VIDEO_NOTE
            message.hasVideo() -> MessageContentType.VIDEO
            message.hasVoice() -> MessageContentType.VOICE
            else -> MessageContentType.TEXT
        }
    }
}