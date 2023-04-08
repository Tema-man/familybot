package dev.storozhenko.familybot.core.executor

import dev.storozhenko.familybot.telegram.TrackingAbsSender
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.getLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import kotlin.time.Duration.Companion.minutes

abstract class OnlyBotOwnerExecutor(private val botConfig: BotConfig) : PrivateMessageExecutor {
    companion object {
        private val deleteMessageScope = CoroutineScope(Dispatchers.Default)
    }

    override fun canExecute(context: ExecutorContext): Boolean {
        val message = context.message
        return botConfig.developer == message.from.userName &&
            message.text.orEmpty().startsWith(getMessagePrefix(), ignoreCase = true)
    }

    override fun priority(context: ExecutorContext) = Priority.HIGH

    abstract fun getMessagePrefix(): String

    abstract fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Action?

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return { sender ->
            val trackingAbsSender = TrackingAbsSender(sender)
            val message = executeInternal(context).invoke(trackingAbsSender)
            val idsToDelete = trackingAbsSender.tracking
                .map { DeleteMessage(it.chat.id.toString(), it.messageId) }
                .plus(DeleteMessage(context.chat.idString, context.message.messageId))

            deleteMessageScope.launch {
                runCatching {
                    if (context.testEnvironment.not()) {
                        delay(3.minutes)
                        idsToDelete.forEach(sender::execute)
                    }
                }.onFailure { e -> getLogger().error("Failed to delete message", e) }
            }
            message
        }
    }
}
