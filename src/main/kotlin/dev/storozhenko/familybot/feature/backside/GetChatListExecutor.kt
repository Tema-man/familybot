package dev.storozhenko.familybot.feature.backside

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.message.Message
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class GetChatListExecutor(
    private val commonRepository: CommonRepository,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {

    override fun getMessagePrefix() = "chats"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val chats = commonRepository.getChats()
        return { sender ->
            sender.send(context, "Active chats count=${chats.size}")
            val totalUsersCount =
                chats.sumOf { chat -> calculate(sender, chat) }
            sender.send(context, "Total users count=$totalUsersCount")
            null
        }
    }

    private fun calculate(sender: AbsSender, chat: Chat): Int = runCatching {
        sender.execute(GetChatMemberCount(chat.idString))
    }.getOrElse { 0 }
}
