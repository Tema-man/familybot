package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.feature.pidor.services.PidorAutoSelectService
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ManualPidorSelectExecutor(
    private val pidorAutoSelectService: PidorAutoSelectService,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {
    override fun getMessagePrefix() = "pidor_manual"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return {
            val response = runCatching {
                pidorAutoSelectService.autoSelect(it)
                "it's done"
            }
                .onFailure { exception -> exception.message }

//            it.send(context, response.getOrDefault("error"))

            SendTextAction(
                text = response.getOrDefault("error"),
                context = context
            )
        }
    }
}
