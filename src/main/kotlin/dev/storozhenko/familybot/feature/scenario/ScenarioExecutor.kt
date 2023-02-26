package dev.storozhenko.familybot.feature.scenario

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.model.Command
import dev.storozhenko.familybot.feature.scenario.services.ScenarioGameplayService
import dev.storozhenko.familybot.feature.scenario.services.ScenarioService
import dev.storozhenko.familybot.feature.scenario.services.ScenarioSessionManagementService
import kotlinx.coroutines.delay
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ScenarioExecutor(
    private val scenarioSessionManagementService: ScenarioSessionManagementService,
    private val scenarioService: ScenarioService,
    private val scenarioGameplayService: ScenarioGameplayService
) : CommandExecutor() {

    override fun command() = Command.SCENARIO

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit = when {
        context.message.text.contains(STORY_PREFIX) -> tellTheStory(context)
        context.isFromDeveloper && context.message.text.contains(MOVE_PREFIX) -> moveState(context)
        else -> processGame(context)
    }

    private fun processGame(
        context: ExecutorContext
    ): suspend (AbsSender) -> Unit {
        val chat = context.chat
        val currentGame = scenarioService.getCurrentGame(chat)
        return when {
            currentGame == null -> scenarioSessionManagementService.listGames(context)
            currentGame.isEnd -> handleEndOfStory(context)
            else -> scenarioSessionManagementService.processCurrentGame(context)
        }
    }

    private fun handleEndOfStory(
        context: ExecutorContext
    ): suspend (AbsSender) -> Unit = {
        scenarioSessionManagementService.processCurrentGame(context).invoke(it)
        delay(2000L)
        scenarioSessionManagementService.listGames(context).invoke(it)
    }

    private fun tellTheStory(
        context: ExecutorContext
    ): suspend (AbsSender) -> Unit {
        val story = scenarioService.getAllStoryOfCurrentGame(context.chat)
        return { it.send(context, story, enableHtml = true) }
    }

    private fun moveState(
        context: ExecutorContext
    ): suspend (AbsSender) -> Unit {
        val nextMove = scenarioGameplayService.nextState(context.chat)
        return {
            if (nextMove == null) {
                it.send(context, "State hasn't been moved")
            }
        }
    }

    companion object {
        const val MOVE_PREFIX = "move"
        const val STORY_PREFIX = "story"
    }
}
