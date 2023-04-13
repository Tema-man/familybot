package dev.storozhenko.familybot.core.services.router

import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.repository.FunctionsConfigureRepository
import dev.storozhenko.familybot.core.services.chatlog.ChatLogger
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.*
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.security.AntiDdosExecutor
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component

@Component
class IntentsRouter(
    private val chatLogger: ChatLogger,
    private val configureRepository: FunctionsConfigureRepository,
    private val dictionary: Dictionary,
    private val executors: List<IntentExecutor>
) {

    private val logger = getLogger()

    suspend fun processIntent(intent: Intent): Action? {
        val isGroup = intent.chat.isGroup
        if (!isGroup) logger.warn("Received private message: $intent")

        val executor = selectExecutor(intent, forSingleUser = !isGroup)
        logger.info("Executor to apply: ${executor.javaClass.simpleName}")
        chatLogger.registerIntent(intent)

        return if (isExecutorDisabled(executor, intent)) {
            when (executor) {
                is CommandExecutor -> sendDisabledCommandMessage(intent)
                is AntiDdosExecutor -> antiDdosSkip(intent)
                else -> null
            }
        } else {
            executor.execute(intent)
        }
    }

    private fun selectExecutor(intent: Intent, forSingleUser: Boolean = false): IntentExecutor {
        val executorsToProcess = when {
            intent.from.role == User.Role.DEVELOPER -> executors
                /* TODO: temporary disabled, will enable after all the executors will be refactored to IntentExecutor
            forSingleUser -> intentExecutors.filterIsInstance<PrivateMessageExecutor>()
            else -> intentExecutors.filterNot { it is PrivateMessageExecutor }

                 */
            else -> executors
        }

        return executorsToProcess.asSequence()
            .map { executor -> executor to executor.priority }
            .filter { (_, priority) -> priority higherThan Priority.LOWEST }
            .sortedByDescending { (_, priority) -> priority.score }
            .find { (executor, _) -> executor.canExecute(intent) }
            ?.first
            ?: selectRandomExecutor()
    }

    private fun selectRandomExecutor(): IntentExecutor {
        logger.info("No executor found, trying to find random priority executors")
        val executor = executors.filter { it.priority == Priority.LOWEST }.random()
        logger.info("Random priority executor ${executor.javaClass.simpleName} was selected")
        return executor
    }

    private fun antiDdosSkip(intent: Intent): Action? {
        val executor = executors //executors.filterIsInstance<CommandExecutor>()
            .find { it.canExecute(intent) } ?: return null

        if (isExecutorDisabled(executor, intent)) {
            sendDisabledCommandMessage(intent)
        } else {
            executor.execute(intent)
        }

        return null
    }

    private fun sendDisabledCommandMessage(intent: Intent): Action {
        val phrase = dictionary.get(Phrase.COMMAND_IS_OFF, ChatEasyKey(intent.chat.id))
        return SendTextAction(
            text = phrase,
            chat = intent.chat
        )
    }

    private fun isExecutorDisabled(executor: IntentExecutor, intent: Intent): Boolean {
        if (executor !is Configurable) return false

        val functionId = executor.getFunctionId()
        val isExecutorDisabled = !configureRepository.isEnabled(functionId, intent.chat)

        if (isExecutorDisabled) {
            logger.info("Executor ${executor::class.simpleName} is disabled")
        }
        return isExecutorDisabled
    }

    private fun IntentExecutor.getFunctionId(): FunctionId {
        //TODO: just a placeholder for now. replace with proper function from executor
        return FunctionId.CHATTING;
    }
}
