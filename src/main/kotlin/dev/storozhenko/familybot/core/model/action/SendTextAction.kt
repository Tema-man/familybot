package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

data class SendTextAction(
    val text: String,
    val asReplyToIntentId: String? = null,
    val showTypeDelay: Boolean = true,
    val formatAsHtml: Boolean = false,
    override val chat: Chat
) : Action(chat)
