package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

class UserJoinedIntent(
    chat: Chat,
    date: Instant,
) : Intent(
    from = User.SERVICE,
    chat = chat,
    date = date
)
