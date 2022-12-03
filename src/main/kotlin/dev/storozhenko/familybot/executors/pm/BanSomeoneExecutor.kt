package dev.storozhenko.familybot.executors.pm

import dev.storozhenko.familybot.common.extensions.getMessageTokens
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.models.router.ExecutorContext
import dev.storozhenko.familybot.repos.CommonRepository
import dev.storozhenko.familybot.services.misc.BanService
import dev.storozhenko.familybot.telegram.BotConfig
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class BanSomeoneExecutor(
    private val banService: BanService,
    private val commonRepository: CommonRepository,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {

    private val banPrefix = "ban|"

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val command = context.update.getMessageTokens(delimiter = "|")
        val identification = command[1]
        val isUnban = command.getOrNull(3) == "unban"
        val isForever = command.getOrNull(3) == "forever"
        val chats = commonRepository.getChats()

        val chat = chats.find { it.name == identification || it.id == identification.toLongOrNull() }

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
            }
        }

        val user = chats
            .asSequence()
            .flatMap { commonRepository.getUsers(it, activeOnly = true).asSequence() }
            .firstOrNull { identification.replace("@", "") in listOf(it.name, it.nickname, it.id.toString()) }

        if (user != null) {
            return {
                if (isUnban) {
                    banService.removeBan(context.userKey)
                    it.send(context, "Unbanned user: $user")
                } else {
                    banService.banUser(user, description, isForever)
                    it.send(context, "Banned user: $user")
                }
            }
        }

        return {
            it.send(context, "No one found")
        }
    }

    override fun getMessagePrefix() = banPrefix
}
