package dev.storozhenko.familybot.feature.user_enter_exit

import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.BotAddedToChatIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.model.intent.UserJoinedIntent
import dev.storozhenko.familybot.core.model.intent.UserLeftIntent
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import org.springframework.stereotype.Component

@Component
class UserEnterExitExecutor(
    private val botConfig: BotConfig,
    private val dictionary: Dictionary
) : IntentExecutor, Configurable {

    override fun getFunctionId(context: ExecutorContext) = FunctionId.GREETINGS

    override val priority: Priority = Priority.LOW

    override fun execute(intent: Intent): Action? {
        val phrase = when (intent) {
            is UserLeftIntent -> Phrase.USER_LEAVING_CHAT
            is UserJoinedIntent -> Phrase.USER_ENTERING_CHAT
            is BotAddedToChatIntent -> Phrase.BOT_WELCOME_MESSAGE
            else -> return null
        }

        return SendTextAction(
            text = dictionary.get(phrase, intent.chat.key),
            chat = intent.chat,
            asReplyToIntentId = intent.id
        )
    }

    override fun canExecute(intent: Intent): Boolean = intent is UserJoinedIntent
        || intent is UserLeftIntent
        || intent is BotAddedToChatIntent

}
