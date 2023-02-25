package dev.storozhenko.familybot.feature.ask_world

import dev.storozhenko.familybot.common.extensions.getMessageTokens
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.repository.FunctionsConfigureRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.AskWorldDensity
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.settings.processors.SettingProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class AskWorldSettingProcessor(
    private val easyKeyValueService: EasyKeyValueService,
    private val functionsConfigureRepository: FunctionsConfigureRepository
) : SettingProcessor {

    override fun canProcess(context: ExecutorContext): Boolean {
        return context.update.getMessageTokens()[1] == "вопросики"
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val arg = context.update.getMessageTokens()[2]
        val density = AskWorldDensityValue.values().find { mode -> mode.text == arg }
            ?: return { sender ->
                sender.send(
                    context,
                    context.phrase(Phrase.ADVANCED_SETTINGS_ASK_WORLD_BAD_USAGE)
                )
            }
        return { sender ->
            functionsConfigureRepository.setStatus(
                FunctionId.ASK_WORLD,
                context.chat,
                isEnabled = density != AskWorldDensityValue.NONE
            )
            easyKeyValueService.put(AskWorldDensity, context.chatKey, density.text)
            sender.send(context, context.phrase(Phrase.ADVANCED_SETTINGS_OK))
        }
    }
}

enum class AskWorldDensityValue(val text: String) {
    ALL("все"),
    LESS("поменьше"),
    NONE("отключить")
}
