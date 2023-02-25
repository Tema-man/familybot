package dev.storozhenko.familybot.core.services.payment.processors

import dev.storozhenko.familybot.core.services.payment.PaymentProcessor
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.shop.model.PreCheckOutResponse
import dev.storozhenko.familybot.feature.shop.model.ShopItem
import dev.storozhenko.familybot.feature.shop.model.ShopPayload
import dev.storozhenko.familybot.feature.shop.model.SuccessPaymentResponse
import org.springframework.stereotype.Component

@Component
class IAmRichPaymentProcessor : PaymentProcessor {
    override fun itemType() = ShopItem.I_AM_RICH

    override fun preCheckOut(shopPayload: ShopPayload): PreCheckOutResponse =
        PreCheckOutResponse.Success()

    override fun processSuccess(shopPayload: ShopPayload): SuccessPaymentResponse {
        return SuccessPaymentResponse(Phrase.I_AM_RICH_DONE)
    }
}
