package dev.storozhenko.familybot.core.model.message

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

class SimpleTextMessage(
    val text: String,
    override val context: ExecutorContext
) : Message(context)
