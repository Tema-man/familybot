package dev.storozhenko.familybot.core.model.intent

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User

class CommandIntent(
    override val from: User,
    override val chat: Chat,
    val command: Command
) : Intent(from, chat)
