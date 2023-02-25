package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.telegram.BotConfig

abstract class OnlyBotOwnerExecutor(private val botConfig: BotConfig) : PrivateMessageExecutor {

  override fun canExecute(context: ExecutorContext): Boolean {
    val message = context.message
    return botConfig.developer == message.from.userName &&
        message.text.startsWith(getMessagePrefix(), ignoreCase = true)
  }

  override fun priority(context: ExecutorContext) = Priority.HIGH

  abstract fun getMessagePrefix(): String
}
