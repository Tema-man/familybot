package dev.storozhenko.familybot.feature.keyword.processor

import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.PshenitsinTolerance
import dev.storozhenko.familybot.core.services.talking.TalkingService
import dev.storozhenko.familybot.feature.keyword.KeyWordProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import kotlin.time.Duration.Companion.minutes

@Component
class PshenitsinKeyWordProcessor(
    @Qualifier("Old") private val talkingService: TalkingService,
    private val keyValueService: EasyKeyValueService
) : KeyWordProcessor {

    override fun canProcess(context: ExecutorContext): Boolean {
        val text = context.message.text ?: return false
        return containsSymbolsY(text) && isTolerant(context.message.chatId).not()
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return { sender ->
            val text = talkingService
                .getReplyToUser(context)
                .toCharArray()
                .map { ch ->
                    when {
                        ch.isLetter() && ch.isUpperCase() -> 'Ы'
                        ch.isLetter() && ch.isLowerCase() -> 'ы'
                        else -> ch
                    }
                }
                .toCharArray()
                .let(::String)

            sender.send(
                context,
                text,
                shouldTypeBeforeSend = true,
                replyToUpdate = true
            )

            keyValueService.put(PshenitsinTolerance, context.chatKey, true, 1.minutes)
            null
        }
    }

    private fun isTolerant(chatId: Long): Boolean {
        return keyValueService.get(PshenitsinTolerance, ChatEasyKey(chatId), false)
    }

    private fun containsSymbolsY(text: String): Boolean {
        val splitText = text.split(Regex("\\s+"))
        return if (splitText.first().toCharArray().isEmpty()) {
            false
        } else {
            splitText.any { word -> word.toCharArray().all { c -> c.lowercaseChar() == 'ы' } }
        }
    }
}