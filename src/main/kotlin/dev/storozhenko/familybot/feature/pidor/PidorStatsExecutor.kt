package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.model.Command
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PidorStatsExecutor(
    private val repository: CommonRepository
) : CommandExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    override fun command() = Command.STATS_TOTAL

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val chat = context.chat

        val pidorsByChat = repository.getPidorsByChat(chat)
            .map { it.user }
            .formatTopList(
                PluralizedWordsProvider(
                    one = { context.phrase(Phrase.PLURALIZED_COUNT_ONE) },
                    few = { context.phrase(Phrase.PLURALIZED_COUNT_FEW) },
                    many = { context.phrase(Phrase.PLURALIZED_COUNT_MANY) }
                )
            )
            .take(100)
        val title = "${context.phrase(Phrase.PIDOR_STAT_ALL_TIME)}:\n".bold()
        return {
            it.send(context, title + pidorsByChat.joinToString("\n"), enableHtml = true)
        }
    }
}
