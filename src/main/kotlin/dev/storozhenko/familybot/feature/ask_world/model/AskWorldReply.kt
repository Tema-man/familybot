package dev.storozhenko.familybot.feature.ask_world.model

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

data class AskWorldReply(
    val id: Long?,
    val questionId: Long,
    val message: String,
    val user: User,
    val chat: Chat,
    val date: Instant
)
