package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.telegram.BotConfig

abstract class ContiniousConversationExecutor(private val config: BotConfig) : CommandExecutor() {

  override fun priority(context: ExecutorContext): Priority {
    return Priority.MEDIUM
  }

  override fun canExecute(context: ExecutorContext): Boolean {
    val message = context.message
    return message.from.userName == config.botName &&
        (message.text ?: "") in getDialogMessages(context)
  }

  abstract fun getDialogMessages(context: ExecutorContext): Set<String>
}
