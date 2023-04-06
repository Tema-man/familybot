package dev.storozhenko.familybot.core.services.payment.processors

import dev.storozhenko.familybot.common.extensions.isToday
import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.common.extensions.startOfDay
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.payment.PaymentProcessor
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.PidorTolerance
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.feature.shop.model.PreCheckOutResponse
import dev.storozhenko.familybot.feature.shop.model.ShopItem
import dev.storozhenko.familybot.feature.shop.model.ShopPayload
import dev.storozhenko.familybot.feature.shop.model.SuccessPaymentResponse
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class ResetPidorPaymentProcessor(
    private val easyKeyValueService: EasyKeyValueService,
    private val commonRepository: CommonRepository
) : PaymentProcessor {
    private val log = getLogger()
    override fun itemType() = ShopItem.DROP_PIDOR

    override fun preCheckOut(shopPayload: ShopPayload): PreCheckOutResponse {
        val chat = Chat(shopPayload.chatId, null)
        val isNonePidorToday = commonRepository
            .getPidorsByChat(chat)
            .none { pidor -> pidor.date.isToday() }
        log.info("Doing pre checkout, shopPayload=$shopPayload, isNonePidorsToday is $isNonePidorToday")
        return if (isNonePidorToday) {
            PreCheckOutResponse.Error(Phrase.DROP_PIDOR_INVALID)
        } else {
            PreCheckOutResponse.Success()
        }
    }

    override fun processSuccess(shopPayload: ShopPayload): SuccessPaymentResponse {
        val chat = Chat(shopPayload.chatId, null)
        val now = Instant.now()
        val amountOfRemovedPidors = commonRepository.removePidorRecords(
            chat,
            from = now.startOfDay(),
            until = now.plus(1, ChronoUnit.DAYS).startOfDay()
        )
        easyKeyValueService.remove(PidorTolerance, chat.key())
        log.info("Removed $amountOfRemovedPidors pidors for $shopPayload")
        return SuccessPaymentResponse(Phrase.DROP_PIDOR_DONE)
    }
}
