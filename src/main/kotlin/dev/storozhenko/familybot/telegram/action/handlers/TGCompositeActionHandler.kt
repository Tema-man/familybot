package dev.storozhenko.familybot.telegram.action.handlers

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.CompositeAction
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TGCompositeActionHandler(
    actionHandlers: List<TGActionHandler>
) : TGActionHandler {

    override val actionClass = CompositeAction::class.java

    private val handlers: Map<Class<*>, TGActionHandler> by lazy { actionHandlers.associateBy { it.actionClass } }

    override suspend fun handle(action: Action, sender: AbsSender): Boolean {
        if (action !is CompositeAction) return false
        if (action.actions.isEmpty()) return false

        var handled = false
        action.actions.forEach { childAction ->
            val handler = handlers[childAction.javaClass]
            handled = if (handler == null) {
                getLogger().warn("No handler found for ${action.javaClass}")
                false
            } else {
                handler.handle(childAction, sender)
            }
        }

        return handled
    }
}
