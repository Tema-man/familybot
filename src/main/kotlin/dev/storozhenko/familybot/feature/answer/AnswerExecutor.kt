package dev.storozhenko.familybot.feature.answer

import dev.storozhenko.familybot.common.extensions.capitalized
import dev.storozhenko.familybot.common.extensions.dropLastDelimiter
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.util.regex.Pattern

@Component
class AnswerExecutor : CommandExecutor() {

    private val log = getLogger()
    private val orPattern = Pattern.compile(" (или|або) ")

    override fun command() = Command.ANSWER

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val text = context.message.text

        val message = text
            .removeRange(0, getIndexOfQuestionStart(text) + 1)
            .split(orPattern)
            .filter(String::isNotEmpty)
            .takeIf(this::isOptionsCountEnough)
            ?.random()
            ?.capitalized()
            ?.dropLastDelimiter()
            ?: return {
                log.info("Bad argument was passed, text of message is [{}]", text)
                it.send(context, context.phrase(Phrase.BAD_COMMAND_USAGE), replyToUpdate = true)
                null
            }
        return { it.send(context, message, replyToUpdate = true, shouldTypeBeforeSend = true); null }
    }

    private fun isOptionsCountEnough(options: List<String>) = options.size >= 2

    private fun getIndexOfQuestionStart(text: String) =
        text.indexOfFirst { it == ' ' }.takeIf { it >= 0 } ?: 0
}
