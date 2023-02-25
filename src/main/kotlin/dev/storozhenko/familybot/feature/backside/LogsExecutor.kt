package dev.storozhenko.familybot.feature.backside

import dev.storozhenko.familybot.common.ErrorLogsDeferredAppender
import dev.storozhenko.familybot.common.extensions.getMessageTokens
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.OnlyBotOwnerExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.BotConfig
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class LogsExecutor(botConfig: BotConfig) : OnlyBotOwnerExecutor(botConfig) {
    override fun getMessagePrefix() = "logs"

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val tokens = context.update.getMessageTokens()
        if (tokens.getOrNull(1) == "clear") {
            ErrorLogsDeferredAppender.errors.clear()
            return { sender -> sender.send(context, "Cleared") }
        }

        return { sender ->
            if (ErrorLogsDeferredAppender.errors.isEmpty()) {
                sender.send(context, "No errors yet")
            } else {
                val errors = ErrorLogsDeferredAppender
                    .errors
                    .joinToString(separator = "\n")
                    .byteInputStream()
                sender.execute(
                    SendDocument(
                        context.chat.idString,
                        InputFile(errors, "error_logs.txt")
                    )
                )
            }
        }
    }
}
