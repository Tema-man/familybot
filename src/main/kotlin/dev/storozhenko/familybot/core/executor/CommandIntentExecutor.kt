package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.router.model.Priority

abstract class CommandIntentExecutor : IntentExecutor {

    abstract val command: Command

    override val priority: Priority = Priority.HIGHEST
    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command
}
