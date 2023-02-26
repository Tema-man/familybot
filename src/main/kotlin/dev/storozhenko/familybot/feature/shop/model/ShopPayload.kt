package dev.storozhenko.familybot.feature.shop.model

import com.fasterxml.jackson.annotation.JsonProperty
import dev.storozhenko.familybot.core.services.settings.ChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.settings.UserEasyKey

data class ShopPayload(
    @JsonProperty("chatId") val chatId: Long,
    @JsonProperty("userId") val userId: Long,
    @JsonProperty("shopItem") val shopItem: ShopItem
) {

    fun chatKey() = ChatEasyKey(this.chatId)

    fun userAndChatKey() = UserAndChatEasyKey(this.userId, this.chatId)

    fun userKey() = UserEasyKey(this.userId)
}
