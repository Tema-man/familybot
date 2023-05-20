package dev.storozhenko.familybot.feature.vestnik

import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.CompositeAction
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.UkrainianLanguage
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.TranslateService
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.ask_world.AskWorldRepository
import org.springframework.stereotype.Component

@Component
class VestnikCommandExecutor(
    private val askWorldRepository: AskWorldRepository,
    private val translateService: TranslateService,
    private val easyKeyValueService: EasyKeyValueService,
    private val dictionary: Dictionary
) : CommandIntentExecutor() {

    private val chat = Chat(id = -1001351771258L, name = null)

    override val command = Command.VESTNIK

    override fun execute(intent: Intent): Action? {
        val isUkrainian = easyKeyValueService.get(UkrainianLanguage, intent.chat.key, false)
        val question = askWorldRepository.searchQuestion("вестник", chat).randomOrNull()?.message ?: "Выпусков нет :("

        val text = if (isUkrainian) {
            translateService.translate(question)
        } else {
            question
        }

        return CompositeAction(
            actions = buildList {
                add(
                    SendTextAction(
                        chat = intent.chat,
                        text = dictionary.get(Phrase.RANDOM_VESTNIK, intent.chat.key)
                    )
                )
                add(
                    SendTextAction(
                        chat = intent.chat,
                        text = text
                    )
                )
            },
            chat = intent.chat
        )
    }
}
