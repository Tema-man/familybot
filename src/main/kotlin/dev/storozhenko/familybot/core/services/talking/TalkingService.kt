package dev.storozhenko.familybot.core.services.talking

import dev.storozhenko.familybot.core.repository.ChatLogRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.UkrainianLanguage
import dev.storozhenko.familybot.core.telegram.model.User
import io.micrometer.core.annotation.Timed
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class TalkingService(
    private val chatLogRepository: ChatLogRepository,
    private val translateService: TranslateService,
    private val easyKeyValueService: EasyKeyValueService
) {

    companion object {
        private const val minimalDatabaseSizeThreshold = 300
    }

    @Timed("service.TalkingService.getReplyToUser")
    suspend fun getReplyToUser(
        context: ExecutorContext,
        shouldBeQuestion: Boolean = false
    ): String = coroutineScope {
        val message = async {
            getMessagesForUser(context.user)
                .also { if (shouldBeQuestion) it.filter { message -> message.endsWith("?") } }
                .randomOrNull() ?: "Хуй соси, губой тряси"
        }

        if (easyKeyValueService.get(UkrainianLanguage, context.chatKey) == true) {
            translateService.translate(message.await())
        } else {
            message.await()
        }
    }

    private fun getMessagesForUser(user: User): List<String> {
        return chatLogRepository
            .get(user)
            .takeIf { messages -> messages.size > minimalDatabaseSizeThreshold }
            ?: chatLogRepository.getRandomMessagesFromCommonPool()
    }
}
