package dev.storozhenko.familybot.feature.keyword

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import org.telegram.telegrambots.meta.bots.AbsSender

interface KeyWordProcessor {

    fun isRandom(context: ExecutorContext): Boolean = false

    fun canProcess(context: ExecutorContext): Boolean

    fun process(context: ExecutorContext): suspend (AbsSender) -> Action?
}
