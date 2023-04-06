package dev.storozhenko.familybot.core.services.router

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.common.extensions.parseJson
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.payment.PaymentService
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.shop.model.PreCheckOutResponse
import dev.storozhenko.familybot.feature.shop.model.ShopPayload
import dev.storozhenko.familybot.feature.shop.model.SuccessPaymentResponse
import dev.storozhenko.familybot.getLogger
import dev.storozhenko.familybot.telegram.from
import dev.storozhenko.familybot.telegram.toChat
import dev.storozhenko.familybot.telegram.toUser
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class PaymentRouter(
    private val paymentService: PaymentService,
    private val commonRepository: CommonRepository,
    private val dictionary: Dictionary,
    private val botConfig: BotConfig
) {
    private val log = getLogger()

    fun proceed(update: Update): suspend (AbsSender) -> Unit = when {
        update.hasPreCheckoutQuery() -> handlePreCheckoutQuery(update)
        update.message?.hasSuccessfulPayment() == true -> handleSuccessfulPayment(update)
        else -> Result.failure(IllegalStateException("Message does not contain any payment information"))
    }.getOrDefault { }

    private fun handlePreCheckoutQuery(update: Update) =
        runCatching { proceedPreCheckoutQuery(update) }
            .onFailure { log.error("paymentRouter.proceedPreCheckoutQuery failed", it) }

    private fun handleSuccessfulPayment(update: Update) =
        runCatching { proceedSuccessfulPayment(update) }
            .onFailure { log.warn("paymentRouter.proceedSuccessfulPayment failed", it) }

    private fun proceedPreCheckoutQuery(update: Update): suspend (AbsSender) -> Unit {
        val shopPayload = getPayload(update.preCheckoutQuery.invoicePayload)
            .copy(userId = update.from().id)
        val settingsKey = ChatEasyKey(shopPayload.chatId)
        val chatId = shopPayload.chatId.toString()
        return { sender ->
            runCatching { paymentService.processPreCheckoutCheck(shopPayload) }
                .onFailure { e ->
                    log.error("Can not check pre checkout query", e)
                    val message = dictionary.get(Phrase.SHOP_PRE_CHECKOUT_FAIL, settingsKey)
                    sender.execute(
                        AnswerPreCheckoutQuery(
                            update.preCheckoutQuery.id,
                            false,
                            message
                        )
                    )
                    sender.execute(SendMessage(chatId, message))
                }
                .onSuccess { response ->
                    when (response) {
                        is PreCheckOutResponse.Success -> {
                            sender.execute(AnswerPreCheckoutQuery(update.preCheckoutQuery.id, true))
                            log.info("Pre checkout query is valid")
                        }

                        is PreCheckOutResponse.Error -> {
                            val message = dictionary.get(response.explainPhrase, settingsKey)
                            sender.execute(
                                AnswerPreCheckoutQuery(
                                    update.preCheckoutQuery.id,
                                    false,
                                    message
                                )
                            )
                            sender.execute(SendMessage(chatId, message))
                        }
                    }
                }
        }
    }

    private fun proceedSuccessfulPayment(update: Update): suspend (AbsSender) -> Unit {
        val shopPayload = getPayload(update.message.successfulPayment.invoicePayload)
        return { sender ->
            runCatching { paymentService.processSuccessfulPayment(shopPayload) }
                .onFailure { e ->
                    log.error("Can not process payment", e)
                    onFailure(sender, update, shopPayload)
                }
                .onSuccess { result ->
                    log.info("Wow, payment!")
                    onSuccess(sender, update, result, shopPayload)
                }
        }
    }

    private fun onSuccess(
        sender: AbsSender,
        update: Update,
        successPaymentResponse: SuccessPaymentResponse,
        shopPayload: ShopPayload
    ) {
        val developerId = botConfig.developerId
        val user = update.toUser()
        val chatKey = update.toChat().key()
        val text = dictionary.get(Phrase.SHOP_THANKS, chatKey)
            .replace("$0", user.getGeneralName())
            .replace("$1", "@" + botConfig.developer)
        val chatId = shopPayload.chatId.toString()
        sender.execute(SendMessage(chatId, text))
        sender.execute(SendMessage(chatId, dictionary.get(successPaymentResponse.phrase, chatKey)))
        successPaymentResponse.customCall(sender)
        val chat = commonRepository
            .getChatsByUser(user)
            .find { shopPayload.chatId == it.id }
            ?.name ?: "[???]"
        val message =
            "<b>+${shopPayload.shopItem.price / 100}₽</b> от ${user.getGeneralName()} из чата <b>$chat</b> за <b>${shopPayload.shopItem}</b>"
        sender.execute(
            SendMessage(developerId, message).apply {
                enableHtml(true)
            }
        )
    }

    private fun onFailure(
        sender: AbsSender,
        update: Update,
        shopPayload: ShopPayload
    ) {
        val developerId = botConfig.developerId
        val text = dictionary.get(Phrase.SHOP_ERROR, update.toChat().key())
            .replace("$1", "@" + botConfig.developer)
        sender.execute(SendMessage(shopPayload.chatId.toString(), text))

        sender.execute(SendMessage(developerId, "Payment gone wrong: $update"))
    }

    private fun getPayload(invoicePayload: String): ShopPayload {
        return invoicePayload.parseJson()
    }
}
