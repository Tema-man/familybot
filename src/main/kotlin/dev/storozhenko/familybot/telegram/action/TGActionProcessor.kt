package dev.storozhenko.familybot.telegram.action

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.getLogger
import dev.storozhenko.familybot.telegram.action.handlers.TGActionHandler
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TGActionProcessor(
    handlersList: List<TGActionHandler>
) {
    private val handlers: Map<Class<*>, TGActionHandler>

    init {
        handlers = handlersList.associateBy { it.javaClass }
    }

    suspend fun handle(action: Action, sender: AbsSender) {
        handlers[action.javaClass]?.handle(action, sender)
            ?: getLogger().warn("No handler found for ${action.javaClass}")
    }
}
