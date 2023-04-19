package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.Command
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

@Component
class PidorStatsExecutor(
    private val repository: CommonRepository,
    private val dictionary: Dictionary
) : IntentExecutor /*CommandExecutor()*/, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    val command = Command.STATS_TOTAL

    override val priority: Priority = Priority.MEDIUM

    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command

    override fun execute(intent: Intent): Action? {
        val chat = intent.chat

        val pidorsByChat = repository.getPidorsByChat(chat)
            .map { it.user }
            .formatTopList(
                PluralizedWordsProvider(
                    one = { dictionary.get(Phrase.PLURALIZED_COUNT_ONE, intent.chat.key) },
                    few = { dictionary.get(Phrase.PLURALIZED_COUNT_FEW, intent.chat.key) },
                    many = { dictionary.get(Phrase.PLURALIZED_COUNT_MANY, intent.chat.key) }
                )
            )
            .take(100)

        val title = "${dictionary.get(Phrase.PIDOR_STAT_ALL_TIME, intent.chat.key)}:\n".bold()
        return SendTextAction(
            text = title + pidorsByChat.joinToString("\n"),
            showTypeDelay = false,
            enableRichFormatting = true,
            chat = intent.chat
        )
    }
}
