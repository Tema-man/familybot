package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

class CommandIntent(
    from: User,
    chat: Chat,
    date: Instant,
    val command: Command
) : Intent(from, chat, date)
