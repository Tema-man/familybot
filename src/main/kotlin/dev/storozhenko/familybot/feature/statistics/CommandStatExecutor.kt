package dev.storozhenko.familybot.feature.statistics

import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.CommandByUser
import dev.storozhenko.familybot.core.model.action.Action
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class CommandStatExecutor(
    private val repositoryCommand: CommandHistoryRepository
) : CommandExecutor() {

    override fun command() = Command.COMMAND_STATS

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val all = repositoryCommand.getAll(context.chat).groupBy(CommandByUser::command)

        val topList = all
            .filterNot { it.key == command() }
            .map { format(it, context) }
            .joinToString("\n")

        return {
            it.send(
                context,
                "${context.phrase(Phrase.STATS_BY_COMMAND)}:\n".bold() + topList,
                enableHtml = true
            )
            null
        }
    }

    private fun format(it: Map.Entry<Command, List<CommandByUser>>, context: ExecutorContext) =
        "${context.phrase(Phrase.COMMAND)} " + "${it.key.command}:".bold() + "\n" + it.value.map { it.user }
            .formatTopList()
            .joinToString("\n")
}
