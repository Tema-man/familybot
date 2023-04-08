package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

class SendTextAction(
    val text: String,
    override val context: ExecutorContext
) : Action(context)
