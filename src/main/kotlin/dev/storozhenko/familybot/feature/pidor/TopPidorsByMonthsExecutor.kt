package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.services.talking.model.Pluralization
import dev.storozhenko.familybot.telegram.TelegramBot
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class TopPidorsByMonthsExecutor(
    private val commonRepository: CommonRepository,
    private val dictionary: Dictionary
) : IntentExecutor /*CommandExecutor()*/, Configurable {

    private val delimiter = "\n========================\n"

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    val command = Command.LEADERBOARD

    override val priority: Priority = Priority.MEDIUM

    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command

    override fun execute(intent: Intent): Action? {
        val result = commonRepository
            .getPidorsByChat(intent.chat)
            .filter { it.date.isBefore(startOfCurrentMonth()) }
            .groupBy { map(it.date) }
            .mapValues { monthPidors -> calculateStats(monthPidors.value) }
            .toSortedMap()
            .asIterable()
            .reversed()
            .map(formatLeaderBoard(intent))

        if (result.isEmpty()) {
            return SendTextAction(text = dictionary.get(Phrase.LEADERBOARD_NONE, intent.chat.key), chat = intent.chat)
        }

        val message = "${dictionary.get(Phrase.LEADERBOARD_TITLE, intent.chat.key)}:\n".bold()
        return SendTextAction(
            text = message + "\n" + result.joinToString(delimiter),
            chat = intent.chat,
            enableRichFormatting = true
        )
    }

    private fun formatLeaderBoard(intent: Intent): (Map.Entry<LocalDate, PidorStat>) -> String = {
        val month = it.key.month.toRussian().capitalized()
        val year = it.key.year
        val userName = it.value.user.name.dropLastDelimiter()
        val position = it.value.position
        val leaderboardPhrase = getLeaderboardPhrase(Pluralization.getPlur(it.value.position), intent.chat.key)

        "$month, $year:\n".italic() + "$userName, $position $leaderboardPhrase"
    }

    private fun map(instant: Instant): LocalDate {
        val time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return LocalDate.of(time.year, time.month, 1)
    }

    private fun calculateStats(pidors: List<Pidor>): PidorStat {
        val pidor = pidors
            .groupBy { it.user }
            .maxByOrNull { it.value.size }
            ?: throw TelegramBot.InternalException("List of pidors should be not empty to calculate stats")
        return PidorStat(pidor.key, pidors.count { it.user == pidor.key })
    }

    private fun getLeaderboardPhrase(
        pluralization: Pluralization,
        chatKey: ChatEasyKey
    ): String = when (pluralization) {
        Pluralization.ONE -> dictionary.get(Phrase.PLURALIZED_LEADERBOARD_ONE, chatKey)
        Pluralization.FEW -> dictionary.get(Phrase.PLURALIZED_LEADERBOARD_FEW, chatKey)
        Pluralization.MANY -> dictionary.get(Phrase.PLURALIZED_LEADERBOARD_MANY, chatKey)
    }

    private class PidorStat(val user: User, val position: Int)
}
