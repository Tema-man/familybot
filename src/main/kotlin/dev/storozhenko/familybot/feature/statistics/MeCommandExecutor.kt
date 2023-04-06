package dev.storozhenko.familybot.feature.statistics

import dev.storozhenko.familybot.common.extensions.DateConstants
import dev.storozhenko.familybot.common.extensions.PluralizedWordsProvider
import dev.storozhenko.familybot.common.extensions.pluralize
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.repository.RawChatLogRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.MessageCounter
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.message.Message
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class MeCommandExecutor(
    private val commonRepository: CommonRepository,
    private val commandHistoryRepository: CommandHistoryRepository,
    private val rawChatLogRepository: RawChatLogRepository,
    private val easyKeyValueService: EasyKeyValueService
) : CommandExecutor() {

    override fun command() = Command.ME

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val chat = context.chat
        val user = context.user
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
            it.send(context, message, replyToUpdate = true)
            null
        }
    }

    private fun getMessageCount(chat: Chat, user: User, context: ExecutorContext): String {
        val key = UserAndChatEasyKey(user.id, chat.id)
        val messageCounter = easyKeyValueService.get(MessageCounter, key)
            ?: rawChatLogRepository.getMessageCount(chat, user).toLong()
                .also { count -> easyKeyValueService.put(MessageCounter, key, count) }

        val word = pluralize(
            messageCounter,
            PluralizedWordsProvider(
                one = { context.phrase(Phrase.PLURALIZED_MESSAGE_ONE) },
                few = { context.phrase(Phrase.PLURALIZED_MESSAGE_FEW) },
                many = { context.phrase(Phrase.PLURALIZED_MESSAGE_MANY) }
            )
        )
        return context.phrase(Phrase.YOU_TALKED) + " $messageCounter $word."
    }

    private fun getCommandCount(user: User, context: ExecutorContext): String {
        val commandCount =
            commandHistoryRepository.get(user, from = DateConstants.theBirthDayOfFamilyBot).size
        val word = pluralize(
            commandCount,
            PluralizedWordsProvider(
                one = { context.phrase(Phrase.PLURALIZED_COUNT_ONE) },
                few = { context.phrase(Phrase.PLURALIZED_COUNT_FEW) },
                many = { context.phrase(Phrase.PLURALIZED_COUNT_MANY) }
            )
        )
        return context.phrase(Phrase.YOU_USED_COMMANDS) + " $commandCount $word."
    }

    private fun getPidorsCount(chat: Chat, user: User, context: ExecutorContext): String {
        val pidorCount = commonRepository
            .getPidorsByChat(chat, startDate = DateConstants.theBirthDayOfFamilyBot)
            .filter { (pidor) -> pidor.id == user.id }
            .size
        val word = pluralize(
            pidorCount,
            PluralizedWordsProvider(
                one = { context.phrase(Phrase.PLURALIZED_COUNT_ONE) },
                few = { context.phrase(Phrase.PLURALIZED_COUNT_FEW) },
                many = { context.phrase(Phrase.PLURALIZED_COUNT_MANY) }
            )
        )
        return pidorCount
            .takeIf { count -> count > 0 }
            ?.let { count -> context.phrase(Phrase.YOU_WAS_PIDOR) + " $count $word." }
            ?: context.phrase(Phrase.YOU_WAS_NOT_PIDOR)
    }
}