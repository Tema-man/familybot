package dev.storozhenko.familybot.feature.backside

import dev.storozhenko.familybot.common.extensions.getMessageTokens
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.core.telegram.model.Chat
import dev.storozhenko.familybot.core.telegram.model.User
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class FindUserExecutor(
    private val commonRepository: CommonRepository,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {
    private val delimiter = "\n===================\n"
    override fun getMessagePrefix() = "user|"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val tokens = context.update.getMessageTokens("|")
        val usersToChats = commonRepository
            .findUsersByName(tokens[1])
            .distinctBy(User::id)
            .associateWith { user -> commonRepository.getChatsByUser(user) }
        return { sender ->
            if (usersToChats.isEmpty()) {
                sender.send(context, "No one found, master")
            } else {
                usersToChats.toList().chunked(5).forEach { chunk ->
                    sender.send(context, format(chunk))
                }
            }
        }
    }

    private fun format(userToChats: List<Pair<User, List<Chat>>>): String {
        return "Search user result:\n" +
            userToChats
                .joinToString(separator = delimiter) { (user, chats) ->
                    "User: ${formatUser(user)} in chats [${formatChats(chats)}]"
                }
    }

    private fun formatUser(user: User): String {
        val parts = listOfNotNull(
            "id=${user.id}",
            user.nickname?.let { nickname -> "username=$nickname" },
            user.name?.let { name -> "name=$name" }
        )

        return "[${parts.joinToString(separator = ", ")}]"
    }

    private fun formatChats(chats: List<Chat>): String =
        chats.joinToString(separator = ",\n") { (id, name): Chat -> "id=$id, chatname=$name" }
}
