package dev.storozhenko.familybot.feature.answer

import dev.storozhenko.familybot.common.extensions.capitalized
import dev.storozhenko.familybot.common.extensions.dropLastDelimiter
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class AnswerExecutor(
    private val dictionary: Dictionary
) : CommandIntentExecutor() {

    private val log = getLogger()
    private val orPattern = Pattern.compile(" (или|або) ")

    override val command = Command.ANSWER
    override fun execute(intent: Intent): Action? {
        val text = (intent as? CommandIntent)?.text ?: return null

        val message = text
            .removeRange(0, getIndexOfQuestionStart(text) + 1)
            .split(orPattern)
            .filter(String::isNotEmpty)
            .takeIf(this::isOptionsCountEnough)
            ?.random()
            ?.capitalized()
            ?.dropLastDelimiter()
            ?: run {
                log.info("Bad argument was passed, text of message is [{}]", text)
                dictionary.get(Phrase.BAD_COMMAND_USAGE, intent.chat.key);
            }

        return SendTextAction(
            text = message,
            chat = intent.chat,
            asReplyToIntentId = intent.id
        )
    }

    private fun isOptionsCountEnough(options: List<String>) = options.size >= 2

    private fun getIndexOfQuestionStart(text: String) =
        text.indexOfFirst { it == ' ' }.takeIf { it >= 0 } ?: 0
}
