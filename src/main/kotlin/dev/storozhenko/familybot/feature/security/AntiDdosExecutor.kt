package dev.storozhenko.familybot.feature.security

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.CommandLimit
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class AntiDdosExecutor(
    private val easyKeyValueService: EasyKeyValueService
) : Executor, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.ANTIDDOS

    override fun priority(context: ExecutorContext) = Priority.HIGHEST

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val update = context.update
        val message = context.phrase(Phrase.STOP_DDOS)
        return when {
            update.hasCallbackQuery() -> callbackQueryCase(context, message)
            update.hasMessage() -> messageCase(context, message)
            else -> { _ -> null }
        }
    }

    override fun canExecute(context: ExecutorContext): Boolean =
        context.command
            ?.let { easyKeyValueService.get(CommandLimit, context.userAndChatKey, 0) >= 5 }
            ?: false

    private fun messageCase(
        context: ExecutorContext,
        message: String
    ): suspend (AbsSender) -> Message? = { it.send(context, message); null }

    private fun callbackQueryCase(
        context: ExecutorContext,
        message: String
    ): suspend (AbsSender) -> Message? = { it ->
        it.execute(
            AnswerCallbackQuery(context.update.callbackQuery.id)
                .apply {
                    showAlert = true
                    text = message
                }
        )
        null
    }
}
