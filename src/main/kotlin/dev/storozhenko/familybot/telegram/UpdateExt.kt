package dev.storozhenko.familybot.telegram

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import org.telegram.telegrambots.meta.api.objects.EntityType
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Instant
import org.telegram.telegrambots.meta.api.objects.Chat as TelegramChat
import org.telegram.telegrambots.meta.api.objects.User as TelegramUser

fun TelegramChat.toChat(): Chat = Chat(id, title)

fun TelegramUser.toUser(chat: Chat? = null, telegramChat: TelegramChat? = null): User {
    val internalChat = telegramChat?.toChat()
        ?: chat
        ?: throw TelegramBot.InternalException("Should be some chat to map user to internal model")
    val formattedName = if (lastName != null) {
        "$firstName $lastName"
    } else {
        firstName
    }
    return User(id, internalChat, formattedName, userName)
}

fun Update.toChat(): Chat {
    val (id, title) = when {
        hasMessage() -> message.chat.id to message.chat.title
        hasEditedMessage() -> editedMessage.chat.id to editedMessage.chat.title
        else -> callbackQuery.message.chat.id to callbackQuery.message.chat.title
    }

    return Chat(
        id = id,
        name = title,
        isGroup = message?.chat?.let { it.isGroupChat || it.isSuperGroupChat } ?: false
    )
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

internal fun Update.toUser(botConfig: BotConfig?, user: TelegramUser): User {
    val formattedName = (user.firstName.let { "$it " }) + (user.lastName ?: "")
    val isFromDeveloper = botConfig?.let { it.developer == user.userName } ?: false
    val role = when {
        isFromDeveloper -> User.Role.DEVELOPER
        user.isBot -> User.Role.BOT
        else -> User.Role.USER
    }
    return User(
        id = user.id,
        chat = toChat(),
        role = role,
        name = formattedName,
        nickname = user.userName
    )
}

internal fun Update.toUser(config: BotConfig): User = toUser(config, from())

fun Update.toUser(): User = toUser(null, from())

fun Update.from(): TelegramUser {
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

internal fun Update.command(botConfig: BotConfig): Command? = message?.getCommand(botConfig.botName)

fun Update.getMessageTokens(delimiter: String = " "): List<String> {
    return if (message.hasText()) {
        message.text.split(delimiter)
    } else {
        emptyList()
    }
}

internal fun Update.date(): Instant =
    message?.date?.toLong()
        ?.let { Instant.ofEpochSecond(it) }
        ?: Instant.now()
