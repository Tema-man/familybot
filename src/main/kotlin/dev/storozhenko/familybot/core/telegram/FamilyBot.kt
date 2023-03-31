package dev.storozhenko.familybot.core.telegram

import dev.storozhenko.familybot.common.extensions.toChat
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.common.extensions.toUser
import dev.storozhenko.familybot.core.services.router.PaymentRouter
import dev.storozhenko.familybot.core.services.router.PollRouter
import dev.storozhenko.familybot.core.services.router.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException

@Component
class FamilyBot(
    val config: BotConfig,
    val router: Router,
    val pollRouter: PollRouter,
    val paymentRouter: PaymentRouter
) : TelegramLongPollingBot(config.botToken) {

    private val log = LoggerFactory.getLogger(FamilyBot::class.java)
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

    private fun proceedPayment(update: Update) = routerScope.launch {
        paymentRouter.proceed(update).invoke(this@FamilyBot)
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
            router.processUpdate(update).invoke(this@FamilyBot)
        } catch (e: TelegramApiRequestException) {
            val logMessage = "Telegram error: ${e.apiResponse}, ${e.errorCode}, update is ${update.toJson()}"
            if (e.errorCode in 400..499) {
                log.warn(logMessage, e)
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
