package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

data class BotAddedToChatIntent(
    override val id: String,
    override val chat: Chat,
    override val date: Instant
) : Intent(id, User.SYSTEM, chat, date)
