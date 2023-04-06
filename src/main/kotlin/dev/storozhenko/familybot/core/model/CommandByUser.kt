package dev.storozhenko.familybot.core.model

import java.time.Instant

data class CommandByUser(val user: User, val command: Command, val date: Instant)
