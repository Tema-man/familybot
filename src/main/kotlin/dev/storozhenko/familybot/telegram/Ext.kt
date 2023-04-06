package dev.storozhenko.familybot.telegram

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import org.telegram.telegrambots.meta.api.objects.*

fun Chat.toChat(): dev.storozhenko.familybot.core.model.Chat = dev.storozhenko.familybot.core.model.Chat(id, title)

fun User.toUser(chat: dev.storozhenko.familybot.core.model.Chat? = null, telegramChat: Chat? = null): dev.storozhenko.familybot.core.model.User {
    val internalChat = telegramChat?.toChat()
        ?: chat
        ?: throw TelegramBot.InternalException("Should be some chat to map user to internal model")
    val formattedName = if (lastName != null) {
        "$firstName $lastName"
    } else {
        firstName
    }
    return dev.storozhenko.familybot.core.model.User(id, internalChat, formattedName, userName)
}

fun Update.toChat(): dev.storozhenko.familybot.core.model.Chat {
    return when {
        hasMessage() -> dev.storozhenko.familybot.core.model.Chat(message.chat.id, message.chat.title)
        hasEditedMessage() -> dev.storozhenko.familybot.core.model.Chat(editedMessage.chat.id, editedMessage.chat.title)
        else -> dev.storozhenko.familybot.core.model.Chat(
            callbackQuery.message.chat.id,
            callbackQuery.message.chat.title
        )
    }
}

fun Update.chatId(): Long {
    return when {
        hasMessage() -> message.chat.id
        hasEditedMessage() -> editedMessage.chat.id
        else -> callbackQuery.message.chat.id
    }
}

fun Update.chatIdString(): String {
    return chatId().toString()
}

fun Update.toUser(): dev.storozhenko.familybot.core.model.User {
    val user = from()
    val formattedName = (user.firstName.let { "$it " }) + (user.lastName ?: "")
    return dev.storozhenko.familybot.core.model.User(user.id, toChat(), formattedName, user.userName)
}

fun Update.from(): User {
    return when {
        hasMessage() -> message.from
        hasEditedMessage() -> editedMessage.from
        hasCallbackQuery() -> callbackQuery.from
        hasPollAnswer() -> pollAnswer.user
        hasPreCheckoutQuery() -> preCheckoutQuery.from
        else -> throw TelegramBot.InternalException("Cant process $this")
    }
}

fun Update.context(botConfig: BotConfig, dictionary: Dictionary): ExecutorContext {
    val message = message ?: editedMessage ?: callbackQuery.message
    val isFromDeveloper = botConfig.developer == from().userName
    val chat = toChat()
    val user = toUser()
    return ExecutorContext(
        this,
        message,
        message.getCommand(botConfig.botName),
        isFromDeveloper,
        chat,
        user,
        UserAndChatEasyKey(user.id, chat.id),
        user.key(),
        chat.key(),
        botConfig.testEnvironment,
        dictionary
    )
}

fun Message.getCommand(botName: String): Command? {
    val entities = entities ?: return null
    for (entity in entities) {
        if (entity.offset == 0 && entity.type == EntityType.BOTCOMMAND) {
            val parts = entity.text.split("@")
            if (parts.size == 1) {
                return Command.LOOKUP[parts[0]]
            }
            if (parts[1] == botName) {
                return Command.LOOKUP[parts[0]]
            }
        }
    }
    return null
}

fun Update.getMessageTokens(delimiter: String = " "): List<String> {
    return if (message.hasText()) {
        message.text.split(delimiter)
    } else {
        emptyList()
    }
}
