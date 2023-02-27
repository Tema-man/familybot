package dev.storozhenko.familybot.feature.help

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.model.Command
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class HelpCommandExecutor : CommandExecutor() {

    override fun command() = Command.HELP

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
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
        }
    }
}
