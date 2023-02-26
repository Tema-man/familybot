package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.telegram.BotConfig

abstract class ContinuousConversationExecutor(private val config: BotConfig) : CommandExecutor() {

    override fun priority(context: ExecutorContext): Priority = Priority.MEDIUM

    override fun canExecute(context: ExecutorContext): Boolean = with(context.message) {
        from.userName == config.botName && (text ?: "") in getDialogMessages(context)
    }

    abstract fun getDialogMessages(context: ExecutorContext): Set<String>
}
