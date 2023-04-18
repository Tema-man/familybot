package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import java.time.Instant

abstract class Intent(
    open val id: String,
    open val from: User,
    open val chat: Chat,
    open val date: Instant,
) {
    val userAndChatKey get() = UserAndChatEasyKey(from.id, chat.id)
}
