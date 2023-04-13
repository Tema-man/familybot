package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

data class SendTextAction(
    val text: String,
    override val chat: Chat
) : Action(chat)
