package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.core.executor.ContinuousConversationExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class QuoteContinuousExecutor(
    private val quoteRepository: QuoteRepository,
    botConfig: BotConfig
) : ContinuousConversationExecutor(botConfig) {

    override fun command() = Command.QUOTE_BY_TAG

    override fun getDialogMessages(context: ExecutorContext): Set<String> = setOf(QUOTE_MESSAGE)

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        return {
            val callbackQuery = context.update.callbackQuery
            it.execute(AnswerCallbackQuery(callbackQuery.id))
            it.execute(
                SendMessage(
                    callbackQuery.message.chatId.toString(),
                    quoteRepository.getByTag(callbackQuery.data) ?: "Такого тега нет, идите нахуй"
                )
            )
            null
        }
    }
}
