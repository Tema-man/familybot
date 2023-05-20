package dev.storozhenko.familybot.telegram.intent.mappers

import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.intent.*
import dev.storozhenko.familybot.telegram.*
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User as TelegramUser


@Component
class UpdateMapper(
    private val botConfig: BotConfig
) {

    fun map(update: Update): List<Intent> = buildList {
        mapUserLeftIntent(update)?.let { add(it) }
        addAll(mapUserJoinedIntent(update))
        mapBotAddedToChatIntent(update)?.let { add(it) }
        (mapCommandIntent(update)
            ?: mapReplyMessageIntent(update)
            ?: mapTextMessageIntent(update))?.let { add(it) }
    }

    private fun mapCommandIntent(update: Update): Intent? {
        val command = update.command(botConfig) ?: return null
        val message = if (update.message?.isReply == true) {
            update.message?.replyToMessage?.text
        } else {
            update.message?.text
                ?: update.editedMessage?.text
                ?: update.callbackQuery?.message?.text
        } ?: return null

        return CommandIntent(
            id = update.getMessageId(),
            from = update.toUser(botConfig),
            chat = update.toChat(),
            date = update.date(),
            command = command,
            text = message.removePrefix(command.toString())
                .removePrefix("@${botConfig.botName}")
                .trim(),
        )
    }

    private fun mapTextMessageIntent(update: Update): Intent? {
        val message = update.message ?: update.editedMessage ?: update.callbackQuery.message ?: return null
        val chat = update.toChat()
        return TextMessageIntent(
            id = update.getMessageId(),
            from = update.toUser(botConfig),
            chat = chat,
            date = update.date(),
            text = message.text.orEmpty()
        )
    }

    private fun mapReplyMessageIntent(update: Update): Intent? {
        val message = update.message ?: update.editedMessage ?: update.callbackQuery.message ?: return null
        val chat = update.toChat()
        if (!message.isReply) return null

        val replyToMessage = message.replyToMessage ?: return null
        val replyText = replyToMessage.text ?: return null
        val replyUser = replyToMessage.from?.toUser(chat) ?: return null

        return ReplyMessageIntent(
            id = update.getMessageId(),
            from = update.toUser(botConfig),
            chat = chat,
            date = update.date(),
            text = message.text.orEmpty(),
            reply = ReplyMessageIntent.Reply(
                text = replyText,
                from = replyUser
            )
        )
    }

    private fun mapUserLeftIntent(update: Update): Intent? {
        val userLeft = update.message?.leftChatMember ?: return null
        return UserLeftIntent(
            id = update.getMessageId(),
            from = update.toUser(botConfig, userLeft),
            chat = update.toChat(),
            date = update.date(),
        )
    }

    private fun mapUserJoinedIntent(update: Update): List<Intent> =
        update.message?.newChatMembers.orEmpty()
            .filter { !it.isThisBot() }
            .map { tgUser ->
                UserJoinedIntent(
                    id = update.getMessageId(),
                    from = update.toUser(botConfig, tgUser),
                    chat = update.toChat(),
                    date = update.date(),
                )
            }

    private fun mapBotAddedToChatIntent(update: Update): Intent? {
        val message = update.message ?: return null
        if (message.newChatMembers?.none { it.isThisBot() } == true) return null
        return BotAddedToChatIntent(
            id = update.getMessageId(),
            chat = update.toChat(),
            date = update.date()
        )
    }

    private fun TelegramUser.isThisBot() = isBot && userName == botConfig.botName
}

