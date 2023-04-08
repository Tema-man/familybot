package dev.storozhenko.familybot.feature.shop

import dev.storozhenko.familybot.common.extensions.rubles
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.ContinuousConversationExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.shop.model.ShopItem
import dev.storozhenko.familybot.feature.shop.model.ShopPayload
import dev.storozhenko.familybot.telegram.from
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ShopContinuousExecutor(
    private val botConfig: BotConfig
) : ContinuousConversationExecutor(botConfig) {

    override fun getDialogMessages(context: ExecutorContext): Set<String> =
        context.allPhrases(Phrase.SHOP_KEYBOARD)

    override fun command() = Command.SHOP

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val providerToken = botConfig.paymentToken ?: return { null }
        val chat = context.chat

        val callbackQuery = context.update.callbackQuery
        val shopItem = ShopItem.values().find { item -> callbackQuery.data == item.name }
            ?: return { null }

        return {
            val additionalTax = if (context.update.from().isPremium == true) {
                10.rubles()
            } else {
                0
            }
            it.execute(AnswerCallbackQuery(callbackQuery.id))
            it.execute(
                SendInvoice(
                    chat.idString,
                    context.phrase(shopItem.title),
                    context.phrase(shopItem.description),
                    createPayload(context, shopItem),
                    providerToken,
                    "help",
                    "RUB",
                    listOf(LabeledPrice(context.phrase(Phrase.SHOP_PAY_LABEL), shopItem.price + additionalTax))
                ).apply {
                    maxTipAmount = 100.rubles()
                    suggestedTipAmounts =
                        listOf(10.rubles(), 20.rubles(), 50.rubles(), 100.rubles())
                }
            )
            null
        }
    }

    private fun createPayload(
        context: ExecutorContext,
        shopItem: ShopItem
    ): String = ShopPayload(
        context.chat.id,
        context.user.id,
        shopItem
    ).toJson()
}
