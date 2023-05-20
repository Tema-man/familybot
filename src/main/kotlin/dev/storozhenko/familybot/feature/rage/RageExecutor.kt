package dev.storozhenko.familybot.feature.rage

import dev.storozhenko.familybot.common.extensions.untilNextDay
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.model.intent.MessageIntent
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.FirstTimeInChat
import dev.storozhenko.familybot.core.services.settings.RageMode
import dev.storozhenko.familybot.core.services.settings.RageTolerance
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.minutes

@Component
class RageExecutor(
    private val easyKeyValueService: EasyKeyValueService,
    private val dictionary: Dictionary
) : CommandIntentExecutor(), Configurable {

    private val log = getLogger()

    companion object {
        const val AMOUNT_OF_RAGE_MESSAGES = 20L
    }

    override fun getFunctionId(context: ExecutorContext) = FunctionId.RAGE

    override val command = Command.RAGE

    override fun execute(intent: Intent): Action? {
        val key = intent.chat.key
        if (isRageForced(intent)) {
            log.warn("Someone forced $command")

            easyKeyValueService.put(RageMode, key, AMOUNT_OF_RAGE_MESSAGES, 10.minutes)
            return SendTextAction(
                chat = intent.chat,
                text = dictionary.get(Phrase.RAGE_INITIAL, intent.chat.key)
            )
        }

        if (isFirstLaunch(intent)) {
            log.info("First launch of $command was detected, avoiding that")
            return SendTextAction(
                chat = intent.chat,
                text = dictionary.get(Phrase.TECHNICAL_ISSUE, intent.chat.key)
            )
        }

        if (isCooldown(intent)) {
            log.info("There is a cooldown of $;command")
            return SendTextAction(
                chat = intent.chat,
                text = dictionary.get(Phrase.RAGE_DONT_CARE_ABOUT_YOU, intent.chat.key)
            )
        }

        easyKeyValueService.put(RageMode, key, AMOUNT_OF_RAGE_MESSAGES, 10.minutes)
        easyKeyValueService.put(RageTolerance, key, true, untilNextDay())
        return SendTextAction(
            chat = intent.chat,
            text = dictionary.get(Phrase.RAGE_INITIAL, intent.chat.key)
        )
    }

    private fun isCooldown(intent: Intent): Boolean =
        easyKeyValueService.get(RageTolerance, intent.chat.key, false)

    private fun isFirstLaunch(intent: Intent): Boolean =
        easyKeyValueService.get(FirstTimeInChat, intent.chat.key, false)

    private fun isRageForced(intent: Intent): Boolean =
        (intent as? MessageIntent)?.text.orEmpty().contains("FORCED" + intent.from.id.toString().takeLast(4))
}
