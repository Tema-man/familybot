package dev.storozhenko.familybot.feature.scenario

import dev.storozhenko.familybot.common.extensions.isFromAdmin
import dev.storozhenko.familybot.core.executor.ContiniousConversationExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.core.telegram.FamilyBot
import dev.storozhenko.familybot.core.telegram.model.Command
import dev.storozhenko.familybot.feature.scenario.services.ScenarioService
import dev.storozhenko.familybot.feature.scenario.services.ScenarioSessionManagementService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ScenarioContiniousExecutor(
    private val scenarioSessionManagementService: ScenarioSessionManagementService,
    private val scenarioService: ScenarioService,
    botConfig: BotConfig
) :
    ContiniousConversationExecutor(botConfig) {
    override fun getDialogMessages(context: ExecutorContext) =
        context.allPhrases(Phrase.SCENARIO_CHOOSE)

    override fun command() = Command.SCENARIO

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
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
                    ?: throw FamilyBot.InternalException("Can't find a scenario ${callbackQuery.data}")
                scenarioSessionManagementService.startGame(context, scenarioToStart).invoke(it)
            }
        }
    }
}
