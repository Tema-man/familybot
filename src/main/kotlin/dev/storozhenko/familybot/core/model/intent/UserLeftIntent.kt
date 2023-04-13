package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

class UserLeftIntent(
    user: User,
    chat: Chat,
    date: Instant
) : Intent(user, chat, date)
