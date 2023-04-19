package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
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
import dev.storozhenko.familybot.feature.pidor.services.PidorStrikeStorage
import org.springframework.stereotype.Component

@Component
class PidorStatsStrikesExecutor(
    private val pidorStrikeStorage: PidorStrikeStorage,
    private val commonRepository: CommonRepository,
    private val dictionary: Dictionary
) : IntentExecutor /*CommandExecutor()*/, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    val command = Command.STATS_STRIKES

    override val priority: Priority = Priority.MEDIUM

    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command

    override fun execute(intent: Intent): Action? {
        val chatKey = intent.chat.key
        val strikes = pidorStrikeStorage.get(chatKey).stats.filter { (_, stats) -> stats.maxStrike > 1 }
        val users = commonRepository.getUsers(intent.chat).associateBy(User::id)

        val stats = strikes
            .map {
                val user = users[it.key]
                if (user != null) {
                    (1..it.value.maxStrike).map { user }
                } else {
                    emptyList()
                }
            }
            .flatten()
            .formatTopList(
                PluralizedWordsProvider(
                    one = { dictionary.get(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_ONE, chatKey) },
                    few = { dictionary.get(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_FEW, chatKey) },
                    many = { dictionary.get(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_MANY, chatKey) }
                )
            )
        val title = "${dictionary.get(Phrase.PIDOR_STRIKE_STAT_TITLE, chatKey)}:\n".bold()
        val message = if (stats.isNotEmpty()) {
            title + stats.joinToString("\n")
        } else {
            title + dictionary.get(Phrase.PIDOR_STRIKE_STAT_NONE, chatKey)
        }

        return SendTextAction(
            text = message,
            enableRichFormatting = true,
            chat = intent.chat
        )
    }
}
