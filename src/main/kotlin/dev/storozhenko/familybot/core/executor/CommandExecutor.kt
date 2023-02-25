package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.telegram.model.Command

abstract class CommandExecutor : Executor {
  override fun canExecute(context: ExecutorContext): Boolean {
    return command() == context.command
  }

  override fun priority(context: ExecutorContext): Priority {
    return Priority.MEDIUM
  }

  open fun isLoggable(): Boolean {
    return true
  }

  abstract fun command(): Command
}
