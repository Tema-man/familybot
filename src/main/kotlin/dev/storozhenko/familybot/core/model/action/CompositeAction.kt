package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.model.Chat

class CompositeAction(
    val actions: List<Action>,
    override val chat: Chat,
    override val silent: Boolean = false
) : Action(chat, silent)
