package dev.storozhenko.familybot.feature.marriage.model

import dev.storozhenko.familybot.core.model.User
import java.time.Instant

data class Marriage(
    val chatId: Long,
    val firstUser: User,
    val secondUser: User,
    val startDate: Instant = Instant.now()
)
