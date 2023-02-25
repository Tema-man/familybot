package dev.storozhenko.familybot.core.telegram.model

import java.time.Instant

data class CommandByUser(val user: User, val command: Command, val date: Instant)
