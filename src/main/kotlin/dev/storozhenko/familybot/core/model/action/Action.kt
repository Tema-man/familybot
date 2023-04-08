package dev.storozhenko.familybot.core.model.action

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext

abstract class Action(
    open val context: ExecutorContext
)
