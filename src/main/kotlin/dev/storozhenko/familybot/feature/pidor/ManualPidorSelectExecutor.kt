package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.feature.pidor.services.PidorAutoSelectService
import dev.storozhenko.familybot.core.telegram.BotConfig
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ManualPidorSelectExecutor(
    private val pidorAutoSelectService: PidorAutoSelectService,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {
    override fun getMessagePrefix() = "pidor_manual"

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        return {
            val response = runCatching {
                pidorAutoSelectService.autoSelect(it)
                "it's done"
            }
                .onFailure { exception -> exception.message }
            it.send(context, response.getOrDefault("error"))
        }
    }
}
