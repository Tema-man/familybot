package dev.storozhenko.familybot.feature.marriage

import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.pluralize
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.feature.marriage.model.Marriage
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Duration
import java.time.Instant

@Component
class MarryListExecutor(
    private val marriagesRepository: MarriagesRepository
) : CommandExecutor() {

    private val loveEmojis = listOf(
        "🥰",
        "😍",
        "😘",
        "😻",
        "💌",
        "💘",
        "💝",
        "💖",
        "💗",
        "💓",
        "💞",
        "💕",
        "💟",
        "❣️",
        "💔",
        "❤️‍🔥",
        "❤️‍🩹",
        "❤️",
        "🧡",
        "💛",
        "💚",
        "💙",
        "💜",
        "🤎",
        "🖤",
        "🤍",
        "🫀",
        "💏",
        "👩‍❤️‍💋‍👨",
        "👨‍❤️‍💋‍👨",
        "👩‍❤️‍💋‍👩",
        "💑",
        "👩‍❤️‍👨",
        "👨‍❤️‍👨",
        "👩‍❤️‍👩",
        "🏩",
        "💒",
        "♥️"
    )

    override fun command() = Command.MARRY_LIST

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val marriages = marriagesRepository.getAllMarriages(context.chat.id)
        if (marriages.isEmpty()) {
            return { sender -> sender.send(context, context.phrase(Phrase.MARRY_EMPTY_LIST)); null }
        } else {
            val marriageList = format(marriages, context)
            return { sender -> sender.send(context, marriageList, enableHtml = true); null }
        }
    }

    private fun format(marriages: List<Marriage>, context: ExecutorContext): String {
        val title = context.phrase(Phrase.MARRY_LIST_TITLE) + "\n"
        return title + marriages
            .sortedBy(Marriage::startDate)
            .mapIndexed { i, marriage ->
                val index = "${i + 1}.".bold()
                val firstUser = marriage.firstUser.getGeneralName(mention = false).bold()
                val secondUser = marriage.secondUser.getGeneralName(mention = false).bold()
                val daysTogether = Duration.between(marriage.startDate, Instant.now()).toDays()
                val ending = getEnding(daysTogether, context, marriage)
                "$index $firstUser + $secondUser = $daysTogether $ending"
            }
            .joinToString(separator = "\n")
    }

    private fun getEnding(
        amountOfDays: Long,
        context: ExecutorContext,
        marriage: Marriage
    ): String {
        val pluralization = PluralizedWordsProvider(
            one = { context.phrase(Phrase.PLURALIZED_DAY_ONE) },
            few = { context.phrase(Phrase.PLURALIZED_DAY_FEW) },
            many = { context.phrase(Phrase.PLURALIZED_DAY_MANY) }
        )
        val emojiId = (marriage.firstUser.id + marriage.secondUser.id) % loveEmojis.size
        return pluralize(
            amountOfDays.toInt(),
            pluralization
        ) + " " + (loveEmojis.getOrNull(emojiId.toInt()) ?: "")
    }
}
