package dev.storozhenko.familybot.feature.rage

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.common.extensions.untilNextDay
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.FirstTimeInChat
import dev.storozhenko.familybot.core.services.settings.RageMode
import dev.storozhenko.familybot.core.services.settings.RageTolerance
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import kotlin.time.Duration.Companion.minutes

@Component
class RageExecutor(
    private val easyKeyValueService: EasyKeyValueService
) : CommandExecutor(), Configurable {

    private val log = getLogger()

    companion object {
        const val AMOUNT_OF_RAGE_MESSAGES = 20L
    }

    override fun getFunctionId(context: ExecutorContext) = FunctionId.RAGE

    override fun command() = Command.RAGE

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val key = context.chatKey
        if (isRageForced(context)) {
            log.warn("Someone forced ${command()}")
            easyKeyValueService.put(RageMode, key, AMOUNT_OF_RAGE_MESSAGES, 10.minutes)
            return {
                it.send(context, context.phrase(Phrase.RAGE_INITIAL), shouldTypeBeforeSend = true)
                null
            }
        }

        if (isFirstLaunch(context)) {
            log.info("First launch of ${command()} was detected, avoiding that")
            return {
                it.send(
                    context,
                    context.phrase(Phrase.TECHNICAL_ISSUE),
                    shouldTypeBeforeSend = true
                )
                null
            }
        }

        if (isCooldown(context)) {
            log.info("There is a cooldown of ${command()}")
            return {
                it.send(
                    context,
                    context.phrase(Phrase.RAGE_DONT_CARE_ABOUT_YOU),
                    shouldTypeBeforeSend = true
                )
                null
            }
        }
        easyKeyValueService.put(RageMode, key, AMOUNT_OF_RAGE_MESSAGES, 10.minutes)
        easyKeyValueService.put(RageTolerance, key, true, untilNextDay())
        return {
            it.send(context, context.phrase(Phrase.RAGE_INITIAL), shouldTypeBeforeSend = true)
            null
        }
    }

    private fun isCooldown(context: ExecutorContext): Boolean =
        easyKeyValueService.get(RageTolerance, context.chatKey, false)

    private fun isFirstLaunch(context: ExecutorContext): Boolean =
        easyKeyValueService.get(FirstTimeInChat, context.chatKey, false)

    private fun isRageForced(context: ExecutorContext): Boolean =
        context.message.text.contains("FORCED" + context.user.id.toString().takeLast(4))
}
