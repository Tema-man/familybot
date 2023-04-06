package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.services.talking.model.Pluralization
import dev.storozhenko.familybot.telegram.TelegramBot
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class TopPidorsByMonthsExecutor(
    private val commonRepository: CommonRepository
) : CommandExecutor(), Configurable {

    private val delimiter = "\n========================\n"

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override fun command() = Command.LEADERBOARD

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val result = commonRepository
            .getPidorsByChat(context.chat)
            .filter { it.date.isBefore(startOfCurrentMonth()) }
            .groupBy { map(it.date) }
            .mapValues { monthPidors -> calculateStats(monthPidors.value) }
            .toSortedMap()
            .asIterable()
            .reversed()
            .map(formatLeaderBoard(context))
        if (result.isEmpty()) {
            return {
                it.send(context, context.phrase(Phrase.LEADERBOARD_NONE))
                null
            }
        }
        val message = "${context.phrase(Phrase.LEADERBOARD_TITLE)}:\n".bold()
        return {
            it.send(context, message + "\n" + result.joinToString(delimiter), enableHtml = true)
            null
        }
    }

    private fun formatLeaderBoard(context: ExecutorContext): (Map.Entry<LocalDate, PidorStat>) -> String =
        {
            val month = it.key.month.toRussian().capitalized()
            val year = it.key.year
            val userName = it.value.user.name.dropLastDelimiter()
            val position = it.value.position
            val leaderboardPhrase = getLeaderboardPhrase(
                Pluralization.getPlur(it.value.position),
                context
            )
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
        context: ExecutorContext
    ): String {
        return when (pluralization) {
            Pluralization.ONE -> context.phrase(Phrase.PLURALIZED_LEADERBOARD_ONE)
            Pluralization.FEW -> context.phrase(Phrase.PLURALIZED_LEADERBOARD_FEW)
            Pluralization.MANY -> context.phrase(Phrase.PLURALIZED_LEADERBOARD_MANY)
        }
    }

    private class PidorStat(val user: User, val position: Int)
}