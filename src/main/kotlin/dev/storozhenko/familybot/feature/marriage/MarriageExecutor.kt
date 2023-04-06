package dev.storozhenko.familybot.feature.marriage

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.ProposalTo
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.marriage.model.Marriage
import dev.storozhenko.familybot.telegram.*
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import java.io.InputStream
import kotlin.time.Duration.Companion.minutes

@Component
class MarriageExecutor(
    private val marriagesRepository: MarriagesRepository,
    private val keyValueService: EasyKeyValueService
) : CommandExecutor() {
    private val selfSuckStream: InputStream = this::class.java.classLoader
        .getResourceAsStream("static/selfsuck.webp")
        ?: throw TelegramBot.InternalException("selfsuck.webp is missing")

    override fun command() = Command.MARRY

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        if (!context.message.isReply) {
            return { sender -> sender.send(context, context.phrase(Phrase.MARRY_RULES)); null }
        }
        val chat = context.chat
        val proposalTarget = context.message.replyToMessage
        val proposalSource = context.message

        if (proposalTarget.from.id == proposalSource.from.id) {
            return { sender ->
                sender.execute(
                    SendSticker(
                        chat.idString,
                        InputFile(selfSuckStream, "selfsuck")
                    ).apply { replyToMessageId = context.message.messageId }
                )
                null
            }
        }
        if (proposalTarget.from.isBot) {
            return { sender ->
                sender.send(
                    context,
                    context.phrase(Phrase.MARRY_PROPOSED_TO_BOT),
                    replyToUpdate = true
                )
                null
            }
        }
        if (isMarriedAlready(chat, proposalSource)) {
            return { sender ->
                sender.send(
                    context,
                    context.phrase(Phrase.MARRY_SOURCE_IS_MARRIED),
                    replyToUpdate = true
                )
                null
            }
        }

        if (isMarriedAlready(chat, proposalTarget)) {
            return { sender ->
                sender.send(
                    context,
                    context.phrase(Phrase.MARRY_TARGET_IS_MARRIED),
                    replyToUpdate = true
                )
                null
            }
        }
        if (isProposedAlready(proposalSource, proposalTarget)) {
            return { sender ->
                sender.send(
                    context,
                    context.phrase(Phrase.MARRY_PROPOSED_AGAIN),
                    replyToUpdate = true
                )
                null
            }
        }

        val proposal = keyValueService.get(ProposalTo, proposalSource.key())
        return if (proposal != null && proposal == proposalTarget.from.id) {
            marry(context)
        } else {
            propose(proposalSource, proposalTarget, context)
        }
    }

    private fun isProposedAlready(
        proposalSource: TelegramMessage,
        proposalTarget: TelegramMessage
    ): Boolean {
        return keyValueService.get(ProposalTo, proposalTarget.key()) == proposalSource.from.id
    }

    private fun isMarriedAlready(
        chat: Chat,
        proposalSource: TelegramMessage
    ) = marriagesRepository.getMarriage(chat.id, proposalSource.from.id) != null

    private fun propose(
        proposalSource: TelegramMessage,
        proposalTarget: TelegramMessage,
        context: ExecutorContext
    ): suspend (AbsSender) -> Message? {
        keyValueService.put(
            ProposalTo,
            key = proposalTarget.key(),
            value = proposalSource.from.id,
            duration = 10.minutes
        )
        return { sender ->
            sender.send(
                context,
                context.phrase(Phrase.MARRY_PROPOSED),
                replyMessageId = proposalTarget.messageId
            )
            null
        }
    }

    private fun marry(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val update = context.update
        val proposalTarget = context.message.replyToMessage.from.toUser(chat = context.chat)
        val proposalSource = context.user
        val marriage = Marriage(update.chatId(), proposalTarget, proposalSource)
        marriagesRepository.addMarriage(marriage)
        keyValueService.remove(
            ProposalTo,
            UserAndChatEasyKey(proposalTarget.id, update.toChat().id)
        )
        return { sender -> sender.send(context, context.phrase(Phrase.MARRY_CONGRATS)); null }
    }
}
