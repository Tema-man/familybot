package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.feature.pidor.services.PidorAutoSelectService
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.model.message.SimpleTextMessage
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ManualPidorSelectExecutor(
    private val pidorAutoSelectService: PidorAutoSelectService,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {
    override fun getMessagePrefix() = "pidor_manual"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Message? {
        return {
            val response = runCatching {
                pidorAutoSelectService.autoSelect(it)
                "it's done"
            }
                .onFailure { exception -> exception.message }

//            it.send(context, response.getOrDefault("error"))

            SimpleTextMessage(
                text = response.getOrDefault("error"),
                context = context
            )
        }
    }
}
