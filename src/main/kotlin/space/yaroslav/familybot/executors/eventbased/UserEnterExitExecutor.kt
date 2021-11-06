package space.yaroslav.familybot.executors.eventbased

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.bots.AbsSender
import space.yaroslav.familybot.common.extensions.send
import space.yaroslav.familybot.executors.Configurable
import space.yaroslav.familybot.executors.Executor
import space.yaroslav.familybot.models.dictionary.Phrase
import space.yaroslav.familybot.models.router.ExecutorContext
import space.yaroslav.familybot.models.router.FunctionId
import space.yaroslav.familybot.models.router.Priority
import space.yaroslav.familybot.telegram.BotConfig

@Component
class UserEnterExitExecutor(private val botConfig: BotConfig) :
    Executor,
    Configurable {
    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        val message = context.message
        val phrase = when {
            isUserLeft(message) -> Phrase.USER_LEAVING_CHAT
            isNewChat(message) -> Phrase.BOT_WELCOME_MESSAGE
            else -> Phrase.USER_ENTERING_CHAT
        }
        return {
            it.send(
                context, context.phrase(phrase),
                replyToUpdate = true,
                shouldTypeBeforeSend = true
            )
        }
    }

    override fun canExecute(context: ExecutorContext) =
        isUserLeft(context.message) || isUserEntered(context.message)

    override fun priority(context: ExecutorContext) = Priority.LOW

    private fun isUserLeft(message: Message) = message.leftChatMember != null
    private fun isUserEntered(message: Message) = message.newChatMembers?.isNotEmpty() ?: false
    private fun isNewChat(message: Message) =
        message.newChatMembers.any { it.isBot && it.userName == botConfig.botName }

    override fun getFunctionId(context: ExecutorContext) = FunctionId.GREETINGS
}