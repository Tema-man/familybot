package dev.storozhenko.familybot.feature.settings

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.repository.FunctionsConfigureRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.model.Chat
import dev.storozhenko.familybot.core.telegram.model.Command
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class SettingsExecutor(
    private val configureRepository: FunctionsConfigureRepository
) : CommandExecutor() {
    override fun command(): Command {
        return Command.SETTINGS
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        return {
            it.send(
                context,
                context.phrase(Phrase.WHICH_SETTING_SHOULD_CHANGE),
                replyToUpdate = true,
                customization = customization(context.chat)
            )
        }
    }

    private fun customization(chat: Chat): SendMessage.() -> Unit {
        return {
            replyMarkup = FunctionId.toKeyBoard { configureRepository.isEnabled(it, chat) }
        }
    }
}
