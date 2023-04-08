package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.feature.pidor.services.PidorStrikeStorage
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PidorStatsStrikesExecutor(
    private val pidorStrikeStorage: PidorStrikeStorage,
    private val commonRepository: CommonRepository
) : CommandExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override fun command() = Command.STATS_STRIKES

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val chat = context.chat
        val strikes =
            pidorStrikeStorage.get(chat.key()).stats.filter { (_, stats) -> stats.maxStrike > 1 }
        val users = commonRepository.getUsers(chat).associateBy(User::id)
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
                    one = { context.phrase(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_ONE) },
                    few = { context.phrase(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_FEW) },
                    many = { context.phrase(Phrase.PIDOR_STRIKE_STAT_PLURALIZED_MANY) }
                )
            )
        val title = "${context.phrase(Phrase.PIDOR_STRIKE_STAT_TITLE)}:\n".bold()
        if (stats.isNotEmpty()) {
            return { it.send(context, title + stats.joinToString("\n"), enableHtml = true); null }
        } else {
            return {
                it.send(
                    context,
                    title + context.phrase(Phrase.PIDOR_STRIKE_STAT_NONE),
                    enableHtml = true
                )
                null
            }
        }
    }
}
