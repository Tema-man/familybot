package dev.storozhenko.familybot.core.services.router

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.common.meteredCanExecute
import dev.storozhenko.familybot.common.meteredExecute
import dev.storozhenko.familybot.common.meteredPriority
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.executor.PrivateMessageExecutor
import dev.storozhenko.familybot.core.repository.ChatLogRepository
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.repository.FunctionsConfigureRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.*
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.core.telegram.model.CommandByUser
import dev.storozhenko.familybot.feature.security.AntiDdosExecutor
import dev.storozhenko.familybot.getLogger
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class Router(
    private val repository: CommonRepository,
    private val commandHistoryRepository: CommandHistoryRepository,
    private val executors: List<Executor>,
    private val chatLogRepository: ChatLogRepository,
    private val configureRepository: FunctionsConfigureRepository,
    private val rawUpdateLogger: dev.storozhenko.familybot.core.services.misc.RawUpdateLogger,
    private val botConfig: BotConfig,
    private val dictionary: Dictionary,
    private val meterRegistry: MeterRegistry,
    private val easyKeyValueService: EasyKeyValueService
) {

    private val logger = getLogger()
    private val chatLogRegex = Regex("[а-яА-Яё\\s,.!?]+")
    private val loggingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.error("Exception in logging job", exception)
    }

    suspend fun processUpdate(update: Update): suspend (AbsSender) -> Unit {
        val message = update.message
            ?: update.editedMessage
            ?: update.callbackQuery.message

        val chat = message.chat

        val isGroup = chat.isSuperGroupChat || chat.isGroupChat
        if (!isGroup) {
            logger.warn("Someone is sending private messages: $update")
        } else {
            registerUpdate(message, update)
            if (update.hasEditedMessage()) {
                return {}
            }
        }
        val context = update.context(botConfig, dictionary)
        val executor = selectExecutor(context, forSingleUser = !isGroup)
        logger.info("Executor to apply: ${executor.javaClass.simpleName}")

        return if (isExecutorDisabled(executor, context)) {
            when (executor) {
                is CommandExecutor -> disabledCommand(context)
                is AntiDdosExecutor -> antiDdosSkip(context)
                else -> { _ -> }
            }
        } else {
            executor.meteredExecute(context, meterRegistry)
        }.also {
            loggingScope.launch(loggingExceptionHandler) {
                logChatCommand(executor, context)
            }
        }
    }

    private fun registerUpdate(
        message: Message,
        update: Update
    ) {
        register(message)
        loggingScope.launch(loggingExceptionHandler) {
            rawUpdateLogger.log(update)
        }
    }

    private fun selectRandom(context: ExecutorContext): Executor {
        logger.info("No executor found, trying to find random priority executors")

        saveMessageInLog(context)
        val executor = executors
            .filter { it.meteredPriority(context, meterRegistry) == Priority.LOWEST }
            .random()

        logger.info("Random priority executor ${executor.javaClass.simpleName} was selected")
        return executor
    }

    private fun saveMessageInLog(context: ExecutorContext) = loggingScope.launch(loggingExceptionHandler) {
        logChatMessage(context)
    }

    private fun antiDdosSkip(context: ExecutorContext): suspend (AbsSender) -> Unit =
        marker@{ it ->
            val executor = executors
                .filterIsInstance<CommandExecutor>()
                .find { it.meteredCanExecute(context, meterRegistry) } ?: return@marker
            val function = if (isExecutorDisabled(executor, context)) {
                disabledCommand(context)
            } else {
                executor.meteredExecute(context, meterRegistry)
            }

            function.invoke(it)
        }

    private fun disabledCommand(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val phrase = context.phrase(Phrase.COMMAND_IS_OFF)
        return { it -> it.send(context, phrase) }
    }

    private fun isExecutorDisabled(executor: Executor, context: ExecutorContext): Boolean {
        if (executor !is Configurable) return false

        val functionId = executor.getFunctionId(context)
        val isExecutorDisabled = !configureRepository.isEnabled(functionId, context.chat)

        if (isExecutorDisabled) {
            logger.info("Executor ${executor::class.simpleName} is disabled")
        }
        return isExecutorDisabled
    }

    private fun logChatCommand(executor: Executor, context: ExecutorContext) {
        if (executor is CommandExecutor && executor.isLoggable()) {
            val key = context.userAndChatKey
            val currentValue = easyKeyValueService.get(CommandLimit, key)
            if (currentValue == null) {
                easyKeyValueService.put(CommandLimit, key, 1, Duration.of(5, ChronoUnit.MINUTES))
            } else {
                easyKeyValueService.increment(CommandLimit, key)
            }

            commandHistoryRepository.add(
                CommandByUser(
                    context.user,
                    executor.command(),
                    Instant.now()
                )
            )
        }
    }

    private fun logChatMessage(context: ExecutorContext) {
        val key = context.userAndChatKey
        if (easyKeyValueService.get(MessageCounter, key) != null) {
            easyKeyValueService.increment(MessageCounter, key)
        }

        val text = context.message.text
            ?.takeIf {
                botConfig.botNameAliases.none { alias ->
                    it.contains(
                        alias,
                        ignoreCase = true
                    )
                }
            }
            ?.takeIf { it.split(" ").size in (3..7) }
            ?.takeIf { it.length < 600 }
            ?.takeIf { chatLogRegex.matches(it) } ?: return

        chatLogRepository.add(context.user, text)
    }

    private fun selectExecutor(
        context: ExecutorContext,
        forSingleUser: Boolean = false
    ): Executor {
        val executorsToProcess = when {
            context.isFromDeveloper -> executors
            forSingleUser -> executors.filterIsInstance<PrivateMessageExecutor>()
            else -> executors.filterNot { it is PrivateMessageExecutor }
        }

        return executorsToProcess
            .asSequence()
            .map { executor -> executor to executor.meteredPriority(context, meterRegistry) }
            .filter { (_, priority) -> priority higherThan Priority.LOWEST }
            .sortedByDescending { (_, priority) -> priority.score }
            .find { (executor, _) -> executor.meteredCanExecute(context, meterRegistry) }
            ?.also { (_, priority) -> if (priority < Priority.LOW) saveMessageInLog(context) }
            ?.first
            ?: selectRandom(context)
    }

    private fun register(message: Message) {
        val chat = message.chat.toChat()

        repository.addChat(chat)
        val key = chat.key()
        val firstBotInteractionDate = easyKeyValueService.get(FirstBotInteraction, key)
        if (firstBotInteractionDate == null) {
            easyKeyValueService.put(FirstBotInteraction, key, Instant.now().prettyFormat())
            easyKeyValueService.put(FirstTimeInChat, key, true, Duration.of(1, ChronoUnit.DAYS))
        }
        val leftChatMember = message.leftChatMember
        val newChatMembers = message.newChatMembers

        when {
            leftChatMember != null -> {
                if (leftChatMember.isBot && leftChatMember.userName == botConfig.botName) {
                    logger.info("Bot was removed from $chat")
                    repository.changeChatActiveStatus(chat, false)
                    repository.disableUsersInChat(chat)
                } else {
                    logger.info("User $leftChatMember has left")
                    repository.changeUserActiveStatusNew(leftChatMember.toUser(chat = chat), false)
                }
            }

            newChatMembers?.isNotEmpty() == true -> {
                if (newChatMembers.any { it.isBot && it.userName == botConfig.botName }) {
                    logger.info("Bot was added to $chat")
                    repository.changeChatActiveStatus(chat, true)
                } else {
                    logger.info("New users was added: $newChatMembers")
                    newChatMembers
                        .filterNot(User::getIsBot)
                        .forEach { repository.addUser(it.toUser(chat = chat)) }
                }
            }

            message.from.isBot.not() || message.from.userName == "GroupAnonymousBot" -> {
                repository.addUser(message.from.toUser(chat = chat))
            }
        }
    }
}
