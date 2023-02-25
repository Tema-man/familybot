package dev.storozhenko.familybot.feature.security

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
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
    override fun getFunctionId(context: ExecutorContext): FunctionId {
        return FunctionId.ANTIDDOS
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val update = context.update
        val message = context.phrase(Phrase.STOP_DDOS)
        return when {
            update.hasCallbackQuery() -> callbackQueryCase(context, message)
            update.hasMessage() -> messageCase(context, message)
            else -> { _ -> }
        }
    }

    override fun canExecute(context: ExecutorContext): Boolean {
        return if (context.command != null) {
            easyKeyValueService.get(CommandLimit, context.userAndChatKey, 0) >= 5
        } else {
            false
        }
    }

    override fun priority(context: ExecutorContext): Priority {
        return Priority.HIGH
    }

    private fun messageCase(
        context: ExecutorContext,
        message: String
    ): suspend (AbsSender) -> Unit = { it.send(context, message) }

    private fun callbackQueryCase(
        context: ExecutorContext,
        message: String
    ): suspend (AbsSender) -> Unit = { it ->
        it.execute(
            AnswerCallbackQuery(context.update.callbackQuery.id)
                .apply {
                    showAlert = true
                    text = message
                }

        )
    }
}
