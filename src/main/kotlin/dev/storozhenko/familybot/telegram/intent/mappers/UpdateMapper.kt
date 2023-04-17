package dev.storozhenko.familybot.telegram.intent.mappers

import dev.storozhenko.familybot.TelegramConfig
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.intent.*
import dev.storozhenko.familybot.telegram.command
import dev.storozhenko.familybot.telegram.date
import dev.storozhenko.familybot.telegram.toChat
import dev.storozhenko.familybot.telegram.toUser
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update


@Component
class UpdateMapper(
    private val botConfig: BotConfig
) {

    fun map(update: Update): List<Intent> = buildList {
        mapUserLeftIntent(update)?.let { add(it) }
        addAll(mapUserJoinedIntent(update))

        (mapCommandIntent(update) ?: mapTextMessageIntent(update))?.let { add(it) }
    }

    private fun mapCommandIntent(update: Update): Intent? {
        val command = update.command(botConfig) ?: return null
        return CommandIntent(
            from = update.toUser(botConfig),
            chat = update.toChat(),
            date = update.date(),
            command = command
        )
    }

    private fun mapTextMessageIntent(update: Update): Intent? {
        val message = update.message ?: update.editedMessage ?: update.callbackQuery.message ?: return null
        return TextMessageIntent(
            from = update.toUser(botConfig),
            chat = update.toChat(),
            date = update.date(),
            text = message.text.orEmpty(),
        )
    }

    private fun mapUserLeftIntent(update: Update): Intent? {
        val userLeft = update.message?.leftChatMember ?: return null
        return UserLeftIntent(
            from = update.toUser(botConfig, userLeft),
            chat = update.toChat(),
            date = update.date(),
        )
    }

    private fun mapUserJoinedIntent(update: Update): List<Intent> =
        update.message?.newChatMembers.orEmpty().map { tgUser ->
            UserJoinedIntent(
                from = update.toUser(botConfig, tgUser),
                chat = update.toChat(),
                date = update.date(),
            )
        }
}

