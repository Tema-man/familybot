package dev.storozhenko.familybot.feature.settings.processors

import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.TalkingDensity
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.telegram.getMessageTokens
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class TalkingDensitySettingProcessor(
    private val easyKeyValueService: EasyKeyValueService
) : SettingProcessor {

    private val commands = setOf("разговорчики", "балачки")
    override fun canProcess(context: ExecutorContext): Boolean {
        val command = context.update.getMessageTokens()[1]
        return commands.contains(command)
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val value = context.update.getMessageTokens()[2]
        val amountOfDensity = value.toLongOrNull() ?: return {
            it.send(
                context,
                context.phrase(Phrase.ADVANCED_SETTINGS_FAILED_TALKING_DENSITY_NOT_NUMBER)
                    .replace("#value", value)
            )
        }

        if (amountOfDensity < 0) {
            return {
                it.send(
                    context,
                    context.phrase(Phrase.ADVANCED_SETTINGS_FAILED_TALKING_DENSITY_NEGATIVE)
                )
            }
        }

        easyKeyValueService.put(TalkingDensity, context.chatKey, amountOfDensity)
        return {
            it.send(context, context.phrase(Phrase.ADVANCED_SETTINGS_OK))
        }
    }
}
