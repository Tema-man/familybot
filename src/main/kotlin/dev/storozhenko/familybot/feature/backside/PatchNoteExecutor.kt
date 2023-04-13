package dev.storozhenko.familybot.feature.backside

import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.getLogger
import dev.storozhenko.familybot.telegram.send
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PatchNoteExecutor(
    private val commonRepository: CommonRepository,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {

    private val patchNotePrefix = "patch_note"
    private val log = getLogger()

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Action? {
        if (context.message.isReply.not()) {
            return { sender -> sender.send(context, "No reply message found, master"); null }
        }
        return { sender ->
            val chats = commonRepository.getChats()
            log.info("Sending in {} chats", chats.size)
            chats.forEach { tryToSendMessage(sender, it, context) }
            null
        }
    }

    override fun getMessagePrefix() = patchNotePrefix

    private fun markChatAsInactive(chat: Chat) {
        commonRepository.changeChatActiveStatus(chat, false)
    }

    private suspend fun tryToSendMessage(
        sender: AbsSender,
        chat: Chat,
        context: ExecutorContext
    ) {
        coroutineScope {
            launch {
                delay(500)
                runCatching {
                    sender.execute(
                        ForwardMessage(
                            chat.idString,
                            context.user.id.toString(),
                            context.message.replyToMessage.messageId
                        )
                    )
                    log.info("Sent patchnote to chatId={}", chat.idString)
                }.onFailure { throwable ->
                    log.warn("Can not send message by patchnote executor", throwable)
                    markChatAsInactive(chat)
                }
            }
        }
    }
}