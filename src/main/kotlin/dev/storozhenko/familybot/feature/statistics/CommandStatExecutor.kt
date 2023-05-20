package dev.storozhenko.familybot.feature.statistics

import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.formatTopList
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.CommandByUser
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import org.springframework.stereotype.Component

@Component
class CommandStatExecutor(
    private val repositoryCommand: CommandHistoryRepository,
    private val dictionary: Dictionary
) : CommandIntentExecutor() {

    override val command = Command.COMMAND_STATS

    override fun execute(intent: Intent): Action? {
        val all = repositoryCommand.getAll(intent.chat).groupBy(CommandByUser::command)

        val topList = all
            .filterNot { it.key == command }
            .map { format(it, intent.chat) }
            .joinToString("\n")

        return SendTextAction(
            chat = intent.chat,
            text = "${dictionary.get(Phrase.STATS_BY_COMMAND, intent.chat.key)}:\n".bold() + topList,
            enableRichFormatting = true
        )
    }

    private fun format(it: Map.Entry<Command, List<CommandByUser>>, chat: Chat) =
        "${dictionary.get(Phrase.COMMAND, chat.key)} " + "${it.key.command}:".bold() + "\n" + it.value.map { it.user }
            .formatTopList()
            .joinToString("\n")
}
