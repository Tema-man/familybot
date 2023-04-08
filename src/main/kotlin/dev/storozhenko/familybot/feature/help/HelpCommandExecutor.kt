package dev.storozhenko.familybot.feature.help

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class HelpCommandExecutor : CommandExecutor() {

    override fun command() = Command.HELP

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return {
            it.send(
                context,
                context.phrase(Phrase.HELP_MESSAGE),
                enableHtml = true,
                customization = {
                    disableWebPagePreview = true
                    disableNotification = true
                }
            )
            null
        }
    }
}
