package dev.storozhenko.familybot.core.model.message

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

abstract class Message(
    open val context: ExecutorContext
)
