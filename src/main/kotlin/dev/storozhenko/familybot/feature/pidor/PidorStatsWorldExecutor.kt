package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.model.Command
import dev.storozhenko.familybot.core.telegram.model.Pidor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PidorStatsWorldExecutor(
    private val repository: CommonRepository
) : CommandExecutor(), Configurable {

    override fun getFunctionId(context: ExecutorContext): FunctionId {
        return FunctionId.PIDOR
    }

    override fun command(): Command {
        return Command.STATS_WORLD
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val pidorsByChat = repository.getAllPidors(
            startDate = DateConstants.theBirthDayOfFamilyBot
        )
            .map(Pidor::user)
            .formatTopList(
                PluralizedWordsProvider(
                    one = { context.phrase(Phrase.PLURALIZED_COUNT_ONE) },
                    few = { context.phrase(Phrase.PLURALIZED_COUNT_FEW) },
                    many = { context.phrase(Phrase.PLURALIZED_COUNT_MANY) }
                )
            )
            .take(100)

        val title = "${context.phrase(Phrase.PIDOR_STAT_WORLD)}:\n".bold()
        return { it.send(context, title + pidorsByChat.joinToString("\n"), enableHtml = true) }
    }
}
