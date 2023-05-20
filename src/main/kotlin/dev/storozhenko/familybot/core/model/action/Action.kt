package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.model.Chat

abstract class Action(
    open val chat: Chat,
    open val silent: Boolean
)
