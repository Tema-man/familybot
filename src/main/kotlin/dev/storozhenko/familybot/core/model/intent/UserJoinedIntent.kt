package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

class UserJoinedIntent(
    user: User,
    chat: Chat,
    date: Instant,
) : Intent(
    from = user,
    chat = chat,
    date = date
)
