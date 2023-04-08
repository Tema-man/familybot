package dev.storozhenko.familybot.feature.help

import dev.storozhenko.familybot.core.executor.PrivateMessageExecutor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PrivateMessageHelpExecutor(
    private val helpExecutor: HelpCommandExecutor
) : PrivateMessageExecutor {
    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        if (helpExecutor.canExecute(context)) {
            return helpExecutor.execute(context)
        } else {
            return {
//                it.send(
//                    context,
//                    context.phrase(Phrase.PRIVATE_MESSAGE_HELP),
//                    shouldTypeBeforeSend = true
//                )

                SendTextAction(
                    text = context.phrase(Phrase.PRIVATE_MESSAGE_HELP),
                    context = context
                )
            }
        }
    }

    override fun canExecute(context: ExecutorContext) = context.isFromDeveloper.not()

    override fun priority(context: ExecutorContext) = Priority.MEDIUM
}
