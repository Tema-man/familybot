package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.telegram.BotConfig

abstract class OnlyBotOwnerExecutor(private val botConfig: BotConfig) : PrivateMessageExecutor {

    override fun canExecute(context: ExecutorContext): Boolean = with(context.message) {
        from.userName == botConfig.developer && text.orEmpty().startsWith(getMessagePrefix(), ignoreCase = true)
    }

    override fun priority(context: ExecutorContext) = Priority.HIGHEST

    abstract fun getMessagePrefix(): String
}
