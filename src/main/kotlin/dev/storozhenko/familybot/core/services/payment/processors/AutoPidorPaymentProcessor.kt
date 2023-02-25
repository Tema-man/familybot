package dev.storozhenko.familybot.core.services.payment.processors

import dev.storozhenko.familybot.core.services.payment.PaymentProcessor
import dev.storozhenko.familybot.core.services.settings.AutoPidorTimesLeft
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.shop.model.PreCheckOutResponse
import dev.storozhenko.familybot.feature.shop.model.ShopItem
import dev.storozhenko.familybot.feature.shop.model.ShopPayload
import dev.storozhenko.familybot.feature.shop.model.SuccessPaymentResponse
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@Component
class AutoPidorPaymentProcessor(
    private val keyValueService: EasyKeyValueService,
    private val dictionary: Dictionary
) : PaymentProcessor {
    override fun itemType() = ShopItem.AUTO_PIDOR

    override fun preCheckOut(shopPayload: ShopPayload): PreCheckOutResponse =
        PreCheckOutResponse.Success()

    override fun processSuccess(shopPayload: ShopPayload): SuccessPaymentResponse {
        val chatKey = shopPayload.chatKey()
        val autoPidorLeft = keyValueService.get(AutoPidorTimesLeft, chatKey, defaultValue = 0)
        val value = autoPidorLeft + 30
        keyValueService.put(AutoPidorTimesLeft, chatKey, value)
        val balanceComment = dictionary.get(Phrase.AUTO_PIDOR_TIMES_LEFT, chatKey) + "$value"
        return SuccessPaymentResponse(Phrase.AUTO_PIDOR_SUCCESS) {
            it.execute(SendMessage(chatKey.chatId.toString(), balanceComment))
        }
    }
}
