package dev.storozhenko.familybot.feature.bet

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.BetTolerance
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class BetExecutor(
    private val easyKeyValueService: EasyKeyValueService
) : CommandExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override fun command() = Command.BET

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        if (isBetAlreadyDone(context.userAndChatKey)) {
            return {
                it.send(
                    context,
                    context.phrase(Phrase.BET_ALREADY_WAS),
                    shouldTypeBeforeSend = true
                )
                null
            }
        }
        return {
            it.send(
                context,
                context.phrase(Phrase.BET_INITIAL_MESSAGE),
                replyToUpdate = true,
                shouldTypeBeforeSend = true,
                customization = {
                    replyMarkup = ForceReplyKeyboard
                        .builder()
                        .forceReply(true)
                        .selective(true)
                        .build()
                }
            )
            null
        }
    }

    override fun isLoggable() = false

    private fun isBetAlreadyDone(key: UserAndChatEasyKey) =
        easyKeyValueService.get(BetTolerance, key, false)
}
