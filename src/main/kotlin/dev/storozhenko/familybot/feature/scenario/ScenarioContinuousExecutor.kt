package dev.storozhenko.familybot.feature.scenario

import dev.storozhenko.familybot.telegram.isFromAdmin
import dev.storozhenko.familybot.core.executor.ContinuousConversationExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.telegram.TelegramBot
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.feature.scenario.services.ScenarioService
import dev.storozhenko.familybot.feature.scenario.services.ScenarioSessionManagementService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ScenarioContinuousExecutor(
    private val scenarioSessionManagementService: ScenarioSessionManagementService,
    private val scenarioService: ScenarioService,
    botConfig: BotConfig
) :
    ContinuousConversationExecutor(botConfig) {
    override fun getDialogMessages(context: ExecutorContext) =
        context.allPhrases(Phrase.SCENARIO_CHOOSE)

    override fun command() = Command.SCENARIO

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return {
            val callbackQuery = context.update.callbackQuery

            if (!it.isFromAdmin(context)) {
                it.execute(
                    AnswerCallbackQuery(callbackQuery.id)
                        .apply {
                            showAlert = true
                            text = context.phrase(Phrase.ACCESS_DENIED)
                        }
                )
            } else {
                val scenarioToStart = scenarioService.getScenarios()
                    .find { (id) -> id.toString() == callbackQuery.data }
                    ?: throw TelegramBot.InternalException("Can't find a scenario ${callbackQuery.data}")
                scenarioSessionManagementService.startGame(context, scenarioToStart).invoke(it)
            }
            null
        }
    }
}
