package dev.storozhenko.familybot.feature.tiktok

import dev.storozhenko.familybot.core.executor.PrivateMessageExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TikTokDownload
import org.springframework.stereotype.Component

@Component
class TikTokDownloadPMExecutor(
    private val tikTokDownloadExecutor: TikTokDownloadExecutor,
    private val easyKeyValueService: EasyKeyValueService
) : PrivateMessageExecutor {
    override fun execute(context: ExecutorContext) = tikTokDownloadExecutor.execute(context)

    override fun canExecute(context: ExecutorContext): Boolean {
        easyKeyValueService.put(TikTokDownload, context.chatKey, true)
        return tikTokDownloadExecutor.canExecute(context)
    }

    override fun priority(context: ExecutorContext) = Priority.HIGH
}
