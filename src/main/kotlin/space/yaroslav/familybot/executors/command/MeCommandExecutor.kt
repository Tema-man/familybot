package space.yaroslav.familybot.executors.command

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import space.yaroslav.familybot.common.extensions.DateConstants
import space.yaroslav.familybot.common.extensions.PluralizedWordsProvider
import space.yaroslav.familybot.common.extensions.pluralize
import space.yaroslav.familybot.common.extensions.send
import space.yaroslav.familybot.common.extensions.toChat
import space.yaroslav.familybot.common.extensions.toUser
import space.yaroslav.familybot.models.dictionary.Phrase
import space.yaroslav.familybot.models.telegram.Chat
import space.yaroslav.familybot.models.telegram.Command
import space.yaroslav.familybot.models.telegram.User
import space.yaroslav.familybot.repos.CommandHistoryRepository
import space.yaroslav.familybot.repos.CommonRepository
import space.yaroslav.familybot.repos.RawChatLogRepository
import space.yaroslav.familybot.services.settings.EasyKeyValueService
import space.yaroslav.familybot.services.settings.MessageCounter
import space.yaroslav.familybot.services.settings.UserAndChatEasyKey
import space.yaroslav.familybot.services.talking.Dictionary
import space.yaroslav.familybot.services.talking.DictionaryContext
import space.yaroslav.familybot.telegram.BotConfig

@Component
class MeCommandExecutor(
    private val commonRepository: CommonRepository,
    private val commandHistoryRepository: CommandHistoryRepository,
    private val rawChatLogRepository: RawChatLogRepository,
    private val easyKeyValueService: EasyKeyValueService,
    private val dictionary: Dictionary,
    config: BotConfig
) : CommandExecutor(config) {

    override fun command(): Command {
        return Command.ME
    }

    override fun execute(update: Update): suspend (AbsSender) -> Unit {
        val chat = update.toChat()
        val user = update.toUser()
        val context = dictionary.createContext(chat)

        return {
            val message = coroutineScope {
                val messageCount = async { getMessageCount(chat, user, context) }
                val pidorCount = async { getPidorsCount(chat, user, context) }
                val commandCount = async { getCommandCount(user, context) }
                setOf(
                    pidorCount.await(),
                    commandCount.await(),
                    messageCount.await()
                ).joinToString("\n")
            }
            it.send(update, message, replyToUpdate = true)
        }
    }

    private fun getMessageCount(chat: Chat, user: User, context: DictionaryContext): String {
        val key = UserAndChatEasyKey(user.id, chat.id)
        val messageCounter = easyKeyValueService.get(MessageCounter, key)
            ?: rawChatLogRepository.getMessageCount(chat, user).toLong()
                .also { count -> easyKeyValueService.put(MessageCounter, key, count) }

        val word = pluralize(
            messageCounter,
            PluralizedWordsProvider(
                one = { context.get(Phrase.PLURALIZED_MESSAGE_ONE) },
                few = { context.get(Phrase.PLURALIZED_MESSAGE_FEW) },
                many = { context.get(Phrase.PLURALIZED_MESSAGE_MANY) }
            )
        )
        return context.get(Phrase.YOU_TALKED) + " $messageCounter $word."
    }

    private fun getCommandCount(user: User, context: DictionaryContext): String {
        val commandCount =
            commandHistoryRepository.get(user, from = DateConstants.theBirthDayOfFamilyBot).size
        val word = pluralize(
            commandCount,
            PluralizedWordsProvider(
                one = { context.get(Phrase.PLURALIZED_COUNT_ONE) },
                few = { context.get(Phrase.PLURALIZED_COUNT_FEW) },
                many = { context.get(Phrase.PLURALIZED_COUNT_MANY) }
            )
        )
        return context.get(Phrase.YOU_USED_COMMANDS) + " $commandCount $word."
    }

    private fun getPidorsCount(chat: Chat, user: User, context: DictionaryContext): String {
        val pidorCount = commonRepository
            .getPidorsByChat(chat, startDate = DateConstants.theBirthDayOfFamilyBot)
            .filter { (pidor) -> pidor.id == user.id }
            .size
        val word = pluralize(
            pidorCount,
            PluralizedWordsProvider(
                one = { context.get(Phrase.PLURALIZED_COUNT_ONE) },
                few = { context.get(Phrase.PLURALIZED_COUNT_FEW) },
                many = { context.get(Phrase.PLURALIZED_COUNT_MANY) }
            )
        )
        return pidorCount
            .takeIf { count -> count > 0 }
            ?.let { count -> context.get(Phrase.YOU_WAS_PIDOR) + " $count $word." }
            ?: context.get(Phrase.YOU_WAS_NOT_PIDOR)
    }
}
