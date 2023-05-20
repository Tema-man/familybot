package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PidorStatsMonthExecutor(
    private val repository: CommonRepository,
    private val dictionary: Dictionary
) : CommandIntentExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override val command = Command.STATS_MONTH

    override fun execute(intent: Intent): Action? {
        val now = LocalDate.now()

        val pidorsByChat = repository.getPidorsByChat(
            intent.chat,
            startDate = startOfCurrentMonth()
        )
            .map(Pidor::user)
            .formatTopList(
                PluralizedWordsProvider(
                    one = { dictionary.get(Phrase.PLURALIZED_COUNT_ONE, intent.chat.key) },
                    few = { dictionary.get(Phrase.PLURALIZED_COUNT_FEW, intent.chat.key) },
                    many = { dictionary.get(Phrase.PLURALIZED_COUNT_MANY, intent.chat.key) }
                )
            )
        val title = "${dictionary.get(Phrase.PIDOR_STAT_MONTH, intent.chat.key)} ${now.month.toRussian()}:\n".bold()

        return return SendTextAction(
            text = title + pidorsByChat.joinToString("\n"),
            showTypeDelay = false,
            enableRichFormatting = true,
            chat = intent.chat
        )
    }
}
