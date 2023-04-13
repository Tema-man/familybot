package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.router.model.Priority

interface IntentExecutor {
    val priority: Priority
    fun canExecute(intent: Intent): Boolean
    fun execute(intent: Intent): Action?
}
