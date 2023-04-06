package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.model.message.SimpleTextMessage
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class QuoteExecutor(private val quoteRepository: QuoteRepository) : CommandExecutor() {

    override fun command() = Command.QUOTE

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? = {
        SimpleTextMessage(
            text = quoteRepository.getRandom(),
            context = context
        )
    }
}
