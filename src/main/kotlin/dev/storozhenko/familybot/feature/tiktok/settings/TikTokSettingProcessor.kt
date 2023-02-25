package dev.storozhenko.familybot.feature.tiktok.settings

import dev.storozhenko.familybot.common.extensions.getMessageTokens
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TikTokDownload
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.settings.processors.SettingProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TikTokSettingProcessor(
    private val easyKeyValueService: EasyKeyValueService
) : SettingProcessor {
    override fun canProcess(context: ExecutorContext): Boolean {
        return context.update.getMessageTokens()[1] == "тикток"
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Unit {
        when (context.update.getMessageTokens()[2]) {
            "вкл" -> easyKeyValueService.put(TikTokDownload, context.chatKey, true)
            "выкл" -> easyKeyValueService.put(TikTokDownload, context.chatKey, false)
            else -> return { it.send(context, context.phrase(Phrase.ADVANCED_SETTINGS_ERROR)) }
        }
        return { it.send(context, context.phrase(Phrase.ADVANCED_SETTINGS_OK)) }
    }
}
