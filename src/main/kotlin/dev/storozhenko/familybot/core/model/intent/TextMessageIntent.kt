package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User

class TextMessageIntent(
    override val from: User,
    override val chat: Chat,
    val text: String
) : Intent(from, chat)
