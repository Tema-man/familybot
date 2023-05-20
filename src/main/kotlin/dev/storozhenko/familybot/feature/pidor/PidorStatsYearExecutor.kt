package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.common.extensions.startOfTheYear
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
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
class PidorStatsYearExecutor(
    private val repository: CommonRepository,
    private val dictionary: Dictionary
) : CommandIntentExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override val command = Command.STATS_YEAR

    override fun execute(intent: Intent): Action? {
        val now = LocalDate.now()
        val pidorsByChat = repository.getPidorsByChat(intent.chat, startDate = startOfTheYear())
            .map(Pidor::user)
            .formatTopList(
                PluralizedWordsProvider(
                    one = { dictionary.get(Phrase.PLURALIZED_COUNT_ONE, intent.chat.key) },
                    few = { dictionary.get(Phrase.PLURALIZED_COUNT_FEW, intent.chat.key) },
                    many = { dictionary.get(Phrase.PLURALIZED_COUNT_MANY, intent.chat.key) }
                )
            )

        val title = "${dictionary.get(Phrase.PIDOR_STAT_YEAR, intent.chat.key)} ${now.year}:\n".bold()

        return SendTextAction(
            text = title + pidorsByChat.joinToString("\n"),
            chat = intent.chat,
            enableRichFormatting = true
        )
    }
}
