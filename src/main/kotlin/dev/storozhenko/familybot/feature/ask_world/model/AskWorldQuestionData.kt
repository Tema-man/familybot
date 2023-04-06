package dev.storozhenko.familybot.feature.ask_world.model

import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.message.Message
import org.telegram.telegrambots.meta.api.objects.Message as TelegramMessage
import org.telegram.telegrambots.meta.bots.AbsSender

sealed interface AskWorldQuestionData

class Success(
    val questionTitle: String,
    val isScam: Boolean,
    val action: suspend (AbsSender, Chat, Chat) -> TelegramMessage
) : AskWorldQuestionData

class ValidationError(
    val invalidQuestionAction: suspend (AbsSender) -> Message?
) : AskWorldQuestionData
