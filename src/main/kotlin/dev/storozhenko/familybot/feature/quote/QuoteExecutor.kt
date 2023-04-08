package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class QuoteExecutor(private val quoteRepository: QuoteRepository) : CommandExecutor() {

    override fun command() = Command.QUOTE

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? = {
        SendTextAction(
            text = quoteRepository.getRandom(),
            context = context
        )
    }
}
