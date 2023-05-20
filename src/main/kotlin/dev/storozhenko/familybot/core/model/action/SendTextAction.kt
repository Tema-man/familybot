package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.model.Chat

data class SendTextAction(
    val text: String,
    val asReplyToIntentId: String? = null,
    val showTypeDelay: Boolean = true,
    val enableRichFormatting: Boolean = false,
    override val chat: Chat,
    override val silent: Boolean = false
) : Action(chat, silent)
