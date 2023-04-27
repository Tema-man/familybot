package dev.storozhenko.familybot.telegram

import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.bot.AbstractBot
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.router.IntentsRouter
import dev.storozhenko.familybot.core.services.router.PaymentRouter
import dev.storozhenko.familybot.core.services.router.PollRouter
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.telegram.action.TGActionProcessor
import dev.storozhenko.familybot.telegram.intent.mappers.UpdateMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
class TelegramBot(
    val config: BotConfig,
    val intentsRouter: IntentsRouter,
    val pollRouter: PollRouter,
    val paymentRouter: PaymentRouter,
    val easyKeyValueService: EasyKeyValueService,
    val actionProcessor: TGActionProcessor,
    val updateMapper: UpdateMapper
) : TelegramLongPollingBot(config.botToken), AbstractBot {

    private val log = LoggerFactory.getLogger(TelegramBot::class.java)
    private val routerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channels = HashMap<Long, Channel<Intent>>()

    override fun getBotUsername(): String = config.botName

    override fun onUpdateReceived(tgUpdate: Update?) {
        val update = tgUpdate ?: throw InternalException("Update should not be null")

        routerScope.launch {
            updateMapper.map(update).forEach { intent ->
                processIntent(intent)
            }
        }

//        when {
//            update.hasPollAnswer() -> proceedPollAnswer(update)
//            update.hasPreCheckoutQuery() || update.message?.hasSuccessfulPayment() == true -> proceedPayment(update)
//            update.hasMessage() || update.hasCallbackQuery() || update.hasEditedMessage() -> proceedMessage(update)
//            update.hasPoll() -> {}
//        }
    }

    override suspend fun start(context: ConfigurableApplicationContext) {
        TelegramBotsApi(DefaultBotSession::class.java).also { it.registerBot(this) }

        val commands = context.getBeansOfType(CommandExecutor::class.java).values.asSequence()
            .map { it.command() }.sorted().distinct()
            .map { BotCommand(it.command, it.description) }.toList()

        listOf(
            BotCommandScopeAllGroupChats() to commands,
            BotCommandScopeAllPrivateChats() to commands
        ).forEach { (scope, commands) ->
            execute(SetMyCommands.builder().commands(commands).scope(scope).build())
        }
    }

    private fun proceedPayment(update: Update) = routerScope.launch {
        paymentRouter.proceed(update).invoke(this@TelegramBot)
    }

    private fun proceedPollAnswer(update: Update) = routerScope.launch {
        runCatching { pollRouter.proceed(update) }
            .onFailure { log.warn("pollRouter.proceed failed", it) }
    }

    private suspend fun processIntent(intent: Intent) {
        channels.getOrPut(intent.chat.id) { createChannel() }.send(intent)
    }

    private fun createChannel(): Channel<Intent> = Channel<Intent>().also { channel ->
        routerScope.launch {
            for (incomingUpdate in channel) {
                proceed(incomingUpdate)
            }
        }
    }

    private suspend fun proceed(intent: Intent) {
        try {
            val user = intent.from
            MDC.put("chat", "${intent.chat.name}:${intent.chat.id}")
            MDC.put("user", "${user.name}:${user.id}")

            val action = intentsRouter.processIntent(intent)
            log.info("Executing action: $action")

            if (action != null) {
                actionProcessor.handle(action, this@TelegramBot)
            }
        } catch (e: TelegramApiRequestException) {
            val logMessage = "Telegram error: ${e.apiResponse}, ${e.errorCode}, update is ${intent.toJson()}"

            if (e.errorCode in 400..499) {
                log.warn(logMessage, e)
                if (e.apiResponse.contains("CHAT_WRITE_FORBIDDEN")) {
                    disableChattingFunctions(intent)
                }
            } else {
                log.error(logMessage, e)
            }
        } catch (e: Exception) {
            log.error("Unexpected error while processing intent: ${intent.toJson()}", e)
        } finally {
            MDC.clear()
        }
    }

    private fun disableChattingFunctions(intent: Intent) {
        listOf(FunctionId.Chatting, FunctionId.Huificate, FunctionId.TalkBack).forEach { function ->
            easyKeyValueService.put(function, ChatEasyKey(intent.chat.id), false)
        }
    }

    class InternalException(override val message: String) : RuntimeException(message)
}
