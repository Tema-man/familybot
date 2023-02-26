package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.model.Command
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class QuoteExecutor(private val quoteRepository: QuoteRepository) : CommandExecutor() {

    override fun command() = Command.QUOTE

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit = {
        it.send(context, quoteRepository.getRandom())
    }
}
