package dev.storozhenko.familybot.feature.ban

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.feature.ban.service.BanService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class BanResponseExecutor(
    private val banService: BanService
) : Executor {

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val banMessage = banService.getChatBan(context)
            ?: banService.getUserBan(context)
            ?: "иди нахуй"
        return {
            if (context.command != null) {
                it.send(context, banMessage, replyToUpdate = true)
            }
            null
        }
    }

    override fun canExecute(context: ExecutorContext): Boolean =
        (banService.getUserBan(context) ?: banService.getChatBan(context)) != null

    override fun priority(context: ExecutorContext) = Priority.HIGHEST
}