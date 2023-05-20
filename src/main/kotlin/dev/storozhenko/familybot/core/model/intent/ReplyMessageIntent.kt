package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.User
import java.time.Instant

data class ReplyMessageIntent(
    override val id: String,
    override val from: User,
    override val chat: Chat,
    override val date: Instant,
    override val text: String,
    val reply: Reply
) : MessageIntent(id, from, chat, date, text) {

    data class Reply(
        val text: String,
        val from: User
    )
}
