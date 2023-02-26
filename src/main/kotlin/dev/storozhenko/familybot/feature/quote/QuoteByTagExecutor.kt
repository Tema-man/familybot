package dev.storozhenko.familybot.feature.quote

import dev.storozhenko.familybot.common.extensions.capitalized
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.model.Command
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.bots.AbsSender

const val QUOTE_MESSAGE = "Тег?"

@Component
class QuoteByTagExecutor(private val quoteRepository: QuoteRepository) : CommandExecutor() {

    override fun command() = Command.QUOTE_BY_TAG

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        return {
            val rows = quoteRepository
                .getTags()
                .map { tag ->
                    InlineKeyboardButton(tag.capitalized())
                        .apply { callbackData = tag }
                }
                .chunked(3)
            it.send(
                context,
                QUOTE_MESSAGE,
                replyToUpdate = true,
                customization = { replyMarkup = InlineKeyboardMarkup(rows) }
            )
        }
    }
}
