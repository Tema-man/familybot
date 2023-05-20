package dev.storozhenko.familybot.feature.help

import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class HelpCommandExecutor(
    private val dictionary: Dictionary
) : CommandIntentExecutor() {

    override val command = Command.HELP

    override fun execute(intent: Intent): Action? = SendTextAction(
        text = dictionary.get(Phrase.HELP_MESSAGE, intent.chat.key),
        chat = intent.chat,
        enableRichFormatting = true,
        silent = true
    )
}
