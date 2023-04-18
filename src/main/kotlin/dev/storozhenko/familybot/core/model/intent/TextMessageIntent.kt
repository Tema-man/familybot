package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

data class TextMessageIntent(
    override val id: String,
    override val from: User,
    override val chat: Chat,
    override val date: Instant,
    val text: String
) : Intent(id, from, chat, date)
