package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.router.model.Priority
import org.springframework.stereotype.Component

@Component
class QuoteExecutor(private val quoteRepository: QuoteRepository) : IntentExecutor {

    val command = Command.QUOTE

    override val priority: Priority = Priority.MEDIUM

    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command

    override fun execute(intent: Intent): Action = SendTextAction(text = quoteRepository.getRandom(), chat = intent.chat)
}
