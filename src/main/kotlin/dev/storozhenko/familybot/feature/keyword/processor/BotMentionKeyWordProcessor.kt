package dev.storozhenko.familybot.feature.keyword.processor

import dev.storozhenko.familybot.common.extensions.randomBoolean
import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.FuckOffOverride
import dev.storozhenko.familybot.core.services.settings.FuckOffTolerance
import dev.storozhenko.familybot.core.services.talking.TalkingService
import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.feature.keyword.KeyWordProcessor
import dev.storozhenko.familybot.common.extensions.sendDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Component
class BotMentionKeyWordProcessor(
    private val botConfig: BotConfig,
    @Qualifier("Picker") private val talkingService: TalkingService,
    private val easyKeyValueService: EasyKeyValueService
) : KeyWordProcessor {

    private val defaultFuckOffDuration = 15.minutes
    private val defaultToleranceDuration = 24.hours

    private val fuckOffPhrases = setOf(
        Regex(".*завали.{0,10}ебало.*", RegexOption.IGNORE_CASE),
        Regex(".*ебало.{0,10}завали.*", RegexOption.IGNORE_CASE),
        Regex(".*стули.{0,10}пельку.*", RegexOption.IGNORE_CASE),
        Regex(".*пельку.{0,10}стули.*", RegexOption.IGNORE_CASE)
    )

    override fun canProcess(context: ExecutorContext): Boolean {
        val message = context.message
        return isReplyToBot(message) || isBotMention(message) || isBotNameMention(message)
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Unit {
        if (isFuckOff(context)) {
            return fuckOff(context)
        }
        val shouldBeQuestion = isBotMention(context.message) || isBotNameMention(context.message)
        return {
            coroutineScope {
                val reply = async {
                    talkingService.getReplyToUser(
                        context,
                        randomBoolean() && shouldBeQuestion
                    )
                }
                it.sendDeferred(context, reply, replyToUpdate = true, shouldTypeBeforeSend = true, enableHtml = true)
            }
        }
    }

    private fun isBotMention(message: Message): Boolean {
        return message.text?.contains("@${botConfig.botName}") ?: false
    }

    private fun isBotNameMention(message: Message): Boolean {
        val text = message.text ?: return false

        return botConfig
            .botNameAliases.any { alias -> text.contains(alias, ignoreCase = true) }
    }

    private fun isReplyToBot(message: Message): Boolean {
        return message.isReply && message.replyToMessage.from.userName == botConfig.botName
    }

    fun isFuckOff(context: ExecutorContext): Boolean {
        val text = context.message.text ?: return false
        return if (!isUserUnderTolerance(context)) {
            fuckOffPhrases.any { it.matches(text) }
        } else {
            false
        }
    }

    fun fuckOff(context: ExecutorContext): suspend (AbsSender) -> Unit {
        easyKeyValueService.put(FuckOffOverride, context.chatKey, true, defaultFuckOffDuration)
        easyKeyValueService.put(FuckOffTolerance, context.userAndChatKey, true, defaultToleranceDuration)
        return {}
    }

    private fun isUserUnderTolerance(context: ExecutorContext) =
        easyKeyValueService.get(FuckOffTolerance, context.userAndChatKey, defaultValue = false)
}
