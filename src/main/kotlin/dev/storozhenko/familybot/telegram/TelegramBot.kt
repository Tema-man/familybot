package dev.storozhenko.familybot.telegram

import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.bot.AbstractBot
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.PaymentRouter
import dev.storozhenko.familybot.core.services.router.PollRouter
import dev.storozhenko.familybot.core.services.router.Router
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.telegram.mappers.MessageHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val router: Router,
    val pollRouter: PollRouter,
    val paymentRouter: PaymentRouter,
    val easyKeyValueService: EasyKeyValueService,
    val messageHandlers: List<MessageHandler>
) : TelegramLongPollingBot(config.botToken), AbstractBot {

    private val log = LoggerFactory.getLogger(TelegramBot::class.java)
    private val routerScope = CoroutineScope(Dispatchers.Default)
    private val channels = HashMap<Long, Channel<Update>>()

    override fun getBotUsername(): String = config.botName

    override fun onUpdateReceived(tgUpdate: Update?) {
        val update = tgUpdate ?: throw InternalException("Update should not be null")

        when {
            update.hasPollAnswer() -> proceedPollAnswer(update)
            update.hasPreCheckoutQuery() || update.message?.hasSuccessfulPayment() == true -> proceedPayment(update)
            update.hasMessage() || update.hasCallbackQuery() || update.hasEditedMessage() -> proceedMessage(update)
            update.hasPoll() -> {}
        }
    }

    override fun start(context: ConfigurableApplicationContext) {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)

        val commands = context.getBeansOfType(CommandExecutor::class.java).values.asSequence()
            .map { it.command() }.sorted().distinct()
            .map { BotCommand(it.command, it.description) }.toList()

        telegramBotsApi.registerBot(this)
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

    private fun proceedMessage(update: Update) = routerScope.launch {
        val chat = update.toChat()
        val channel = channels.computeIfAbsent(chat.id) { createChannel() }
        channel.send(update)
    }

    private fun createChannel(): Channel<Update> {
        val channel = Channel<Update>()
        routerScope.launch {
            for (incomingUpdate in channel) {
                proceed(incomingUpdate)
            }
        }
        return channel
    }

    private suspend fun proceed(update: Update) {
        try {
            val user = update.toUser()
            MDC.put("chat", "${user.chat.name}:${user.chat.id}")
            MDC.put("user", "${user.name}:${user.id}")
            val message = router.processUpdate(update).invoke(this@TelegramBot)

            if (message != null) {
                messageHandlers.any {
                    it.handle(message, this@TelegramBot)
                }
            }

        } catch (e: TelegramApiRequestException) {
            val logMessage = "Telegram error: ${e.apiResponse}, ${e.errorCode}, update is ${update.toJson()}"
            if (e.errorCode in 400..499) {
                log.warn(logMessage, e)
                if (e.apiResponse.contains("CHAT_WRITE_FORBIDDEN")) {
                    listOf(FunctionId.Chatting, FunctionId.Huificate, FunctionId.TalkBack)
                        .forEach { function ->
                            easyKeyValueService.put(function, ChatEasyKey(update.toChat().id), false)
                        }
                }
            } else {
                log.error(logMessage, e)
            }
        } catch (e: Exception) {
            log.error("Unexpected error, update is ${update.toJson()}", e)
        } finally {
            MDC.clear()
        }
    }

    class InternalException(override val message: String) : RuntimeException(message)
}
