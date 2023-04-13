package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

class TextMessageIntent(
    from: User,
    chat: Chat,
    date: Instant,
    val text: String
) : Intent(from, chat, date)
