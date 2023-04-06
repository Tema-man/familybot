package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.model.Command

abstract class CommandExecutor : Executor {
    override fun canExecute(context: ExecutorContext): Boolean = command() == context.command

    override fun priority(context: ExecutorContext): Priority = Priority.MEDIUM

    open fun isLoggable(): Boolean = true

    abstract fun command(): Command
}
