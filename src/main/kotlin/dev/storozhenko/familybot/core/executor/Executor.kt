package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import org.telegram.telegrambots.meta.bots.AbsSender

interface Executor {

  fun execute(context: ExecutorContext): suspend (AbsSender) -> Message?

  fun canExecute(context: ExecutorContext): Boolean

  fun priority(context: ExecutorContext): Priority
}
