package dev.storozhenko.familybot.feature.backside

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.message.Message
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class OwnerPrivateMessageHelpExecutor(
    onlyBotOwnerExecutors: List<OnlyBotOwnerExecutor>,
    botConfig: BotConfig
) : OnlyBotOwnerExecutor(botConfig) {

    private val helpMessage = onlyBotOwnerExecutors
        .map { executor -> executor.getMessagePrefix() to executor::class.java.simpleName }
        .sortedBy { (prefix, _) -> prefix }
        .joinToString("\n") { (prefix, executorName) -> "$prefix — $executorName" }

    override fun getMessagePrefix() = "help"

    override fun executeInternal(context: ExecutorContext): suspend (AbsSender) -> Message? {
        return { sender -> sender.send(context, helpMessage); null}
    }
}
