package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey

abstract class Intent(
    open val from: User,
    open val chat: Chat
) {

    @Suppress("LeakingThis")
    val userAndChatKey get() = UserAndChatEasyKey(from.id, chat.id)

}
