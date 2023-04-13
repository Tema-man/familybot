package dev.storozhenko.familybot.feature.vestnik

import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.UkrainianLanguage
import dev.storozhenko.familybot.core.services.talking.TranslateService
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.ask_world.AskWorldRepository
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.telegram.sendDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class VestnikCommandExecutor(
    private val askWorldRepository: AskWorldRepository,
    private val translateService: TranslateService,
    private val easyKeyValueService: EasyKeyValueService
) : CommandExecutor() {

    private val chat = Chat(id = -1001351771258L, name = null)

    override fun command() = Command.VESTNIK

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        return { sender ->
            val question = coroutineScope {
                async {
                    val isUkrainian =
                        async { easyKeyValueService.get(UkrainianLanguage, context.chatKey, false) }
                    val question =
                        askWorldRepository.searchQuestion("вестник", chat).randomOrNull()?.message
                            ?: "Выпусков нет :("
                    if (isUkrainian.await()) {
                        translateService.translate(question)
                    } else {
                        question
                    }
                }
            }

            sender.send(context, context.phrase(Phrase.RANDOM_VESTNIK))
            sender.sendDeferred(context, question, shouldTypeBeforeSend = true)
            null
        }
    }
}