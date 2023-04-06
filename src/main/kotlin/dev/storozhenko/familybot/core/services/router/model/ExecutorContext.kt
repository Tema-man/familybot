package dev.storozhenko.familybot.core.services.router.model

import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

class ExecutorContext(
    val update: Update,
    val message: Message,
    val command: Command?,
    val isFromDeveloper: Boolean,
    val chat: Chat,
    val user: User,
    val userAndChatKey: UserAndChatEasyKey,
    val userKey: UserEasyKey,
    val chatKey: ChatEasyKey,
    val testEnvironment: Boolean,
    private val dictionary: Dictionary
) {
    fun phrase(phrase: Phrase) = dictionary.get(phrase, chatKey)
    fun allPhrases(phrase: Phrase) = dictionary.getAll(phrase)
}
