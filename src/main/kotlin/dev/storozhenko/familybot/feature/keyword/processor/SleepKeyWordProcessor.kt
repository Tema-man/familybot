package dev.storozhenko.familybot.feature.keyword.processor

import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.telegram.sendSticker
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.telegram.stickers.Sticker
import dev.storozhenko.familybot.feature.keyword.KeyWordProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class SleepKeyWordProcessor : KeyWordProcessor {
    override fun canProcess(context: ExecutorContext): Boolean {
        val text = context.message.text ?: return false
        return text.contains("спать", ignoreCase = true) ||
                text.contains("сплю", ignoreCase = true)
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Message? {
        return { it.sendSticker(context, Sticker.SWEET_DREAMS, replyToUpdate = true); null }
    }

    override fun isRandom(context: ExecutorContext) = false
}
