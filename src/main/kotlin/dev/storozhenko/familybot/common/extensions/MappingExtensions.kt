package dev.storozhenko.familybot.common.extensions

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.telegram.TelegramBot
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import org.telegram.telegrambots.meta.api.objects.EntityType
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import org.telegram.telegrambots.meta.api.objects.Chat as TelegramChat
import org.telegram.telegrambots.meta.api.objects.User as TelegramUser

fun Message.key(): UserAndChatEasyKey {
    return UserAndChatEasyKey(from.id, chatId)
}

fun User.key(): UserEasyKey {
    return UserEasyKey(userId = id)
}

fun Chat.key(): ChatEasyKey {
    return ChatEasyKey(chatId = id)
}

fun Month.toRussian(): String {
    return this.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"))
}

fun Boolean.toEmoji(): String {
    return if (this) "✅" else "❌"
}

fun Int.rubles() = this * 100

private val objectMapper = jsonMapper {
    addModule(kotlinModule())
    addModule(JavaTimeModule())
}

fun mapper() = objectMapper

fun Any.toJson(): String = objectMapper.writeValueAsString(this)
inline fun <reified T> String.parseJson(): T = mapper().readValue(this, T::class.java)
