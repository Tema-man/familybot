package dev.storozhenko.familybot.feature.ban

import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.feature.ban.service.BanService
import dev.storozhenko.familybot.telegram.getMessageTokens
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class BanSomeoneExecutor(
    private val banService: BanService,
    private val commonRepository: CommonRepository,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {

    private val banPrefix = "ban|"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val command = context.update.getMessageTokens(delimiter = "|")
        val identification = command[1]
        val isUnban = command.getOrNull(3) == "unban"
        val isForever = command.getOrNull(3) == "forever"
        val chats = commonRepository.getChats()

        val chat =
            chats.find { it.name == identification || it.id == identification.toLongOrNull() }

        val description = command[2]
        if (chat != null) {
            return {
                if (isUnban) {
                    banService.removeBan(context.chatKey)
                    it.send(context, "Unbanned chat: $chat")
                } else {
                    banService.banChat(chat, description, isForever)
                    it.send(context, "Banned chat: $chat")
                }
                null
            }
        }

        val user = chats
            .asSequence()
            .flatMap { commonRepository.getUsers(it, activeOnly = true).asSequence() }
            .firstOrNull {
                identification.replace("@", "") in listOf(
                    it.name,
                    it.nickname,
                    it.id.toString()
                )
            }

        if (user != null) {
            return {
                if (isUnban) {
                    banService.removeBan(context.userKey)
                    it.send(context, "Unbanned user: $user")
                } else {
                    banService.banUser(user, description, isForever)
                    it.send(context, "Banned user: $user")
                }

                null
            }
        }

        return {
            it.send(context, "No one found")
            null
        }
    }

    override fun getMessagePrefix() = banPrefix
}