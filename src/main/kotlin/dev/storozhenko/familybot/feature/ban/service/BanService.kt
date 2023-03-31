package dev.storozhenko.familybot.feature.ban.service

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.common.extensions.prettyFormat
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.Ban
import dev.storozhenko.familybot.core.services.settings.EasyKey
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.telegram.model.Chat
import dev.storozhenko.familybot.core.telegram.model.User
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Component
class BanService(
    private val easyKeyValueService: EasyKeyValueService
) {

    fun getUserBan(context: ExecutorContext): String? {
        return findBanByKey(context.userKey)
    }

    fun getChatBan(context: ExecutorContext): String? {
        return findBanByKey(context.chatKey)
    }

    fun banUser(user: User, description: String, isForever: Boolean = false) {
        banByKey(user.key(), description, calculateDuration(isForever))
    }

    fun banChat(chat: Chat, description: String, isForever: Boolean = false) {
        banByKey(chat.key(), description, calculateDuration(isForever))
    }

    fun findBanByKey(easyKey: EasyKey): String? {
        return easyKeyValueService.get(
            Ban,
            easyKey
        )
    }

    fun removeBan(easyKey: EasyKey) {
        easyKeyValueService.remove(Ban, easyKey)
    }

    private fun calculateDuration(isForever: Boolean): Duration {
        return if (isForever) 9999.days else 7.days
    }

    private fun banByKey(easyKey: EasyKey, description: String, duration: Duration) {
        val until = Instant.now().plusSeconds(duration.inWholeSeconds)
        easyKeyValueService.put(
            Ban,
            easyKey,
            "Бан нахуй по причине \"$description\" до ${until.prettyFormat()}",
            duration = duration
        )
    }
}
