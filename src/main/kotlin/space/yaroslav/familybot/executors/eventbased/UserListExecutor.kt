package space.yaroslav.familybot.executors.eventbased

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import space.yaroslav.familybot.common.utils.send
import space.yaroslav.familybot.executors.Executor
import space.yaroslav.familybot.models.Phrase
import space.yaroslav.familybot.models.Priority
import space.yaroslav.familybot.services.dictionary.Dictionary
import space.yaroslav.familybot.telegram.BotConfig

@Component
class UserListExecutor(private val dictionary: Dictionary, private val botConfig: BotConfig) : Executor {
    override fun execute(update: Update): suspend (AbsSender) -> Unit {
        val message = update.message
        val phrase = when {
            isUserLeft(message) -> Phrase.USER_LEAVING_CHAT
            isSucharaEntered(message) -> Phrase.SUCHARA_HELLO_MESSAGE
            else -> Phrase.USER_ENTERING_CHAT
        }
        return { it.send(update, dictionary.get(phrase), replyToUpdate = true, shouldTypeBeforeSend = true) }
    }

    override fun canExecute(message: Message) = isUserLeft(message) || isUserEntered(message)

    override fun priority(update: Update) = Priority.LOW

    private fun isUserLeft(message: Message) = message.leftChatMember != null
    private fun isUserEntered(message: Message) = message.newChatMembers?.isNotEmpty() ?: false
    private fun isSucharaEntered(message: Message) =
        message.newChatMembers.any { it.bot && it.userName == botConfig.botname }
}
