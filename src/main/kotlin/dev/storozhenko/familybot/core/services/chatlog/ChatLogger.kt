package dev.storozhenko.familybot.core.services.chatlog

import dev.storozhenko.familybot.common.extensions.key
import dev.storozhenko.familybot.common.extensions.prettyFormat
import dev.storozhenko.familybot.common.extensions.toJson
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.CommandByUser
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.intent.*
import dev.storozhenko.familybot.core.repository.ChatLogRepository
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.repository.RawChatLogRepository
import dev.storozhenko.familybot.core.services.settings.*
import dev.storozhenko.familybot.getLogger
import dev.storozhenko.familybot.telegram.toChat
import dev.storozhenko.familybot.telegram.toUser
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@Component
class ChatLogger(
    private val repository: CommonRepository,
    private val commandHistoryRepository: CommandHistoryRepository,
    private val chatLogRepository: ChatLogRepository,
    private val botConfig: BotConfig,
    private val meterRegistry: MeterRegistry,
    private val easyKeyValueService: EasyKeyValueService,
    private val rawChatLogRepository: RawChatLogRepository
) {

    private val logger = getLogger()
    private val chatLogRegex = Regex("[а-яА-Яё\\s,.!?]+")
    private val loggingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val loggingExceptionHandler = CoroutineExceptionHandler { _, exception ->
        logger.error("Exception in logging job", exception)
    }

    fun registerIntent(intent: Intent) {
        loggingScope.launch(loggingExceptionHandler) {
            val chat = intent.chat

            repository.addChat(chat)
            val key = chat.key()
            val firstBotInteractionDate = easyKeyValueService.get(FirstBotInteraction, key)
            if (firstBotInteractionDate == null) {
                easyKeyValueService.put(FirstBotInteraction, key, Instant.now().prettyFormat())
                easyKeyValueService.put(FirstTimeInChat, key, true, 1.days)
            }

            when (intent) {
                is TextMessageIntent -> registerTextMessage(intent)
                is UserLeftIntent -> registerMemberLeft(intent)
                is UserJoinedIntent -> registerMemberJoined(intent)
                is CommandIntent -> registerCommand(intent)
            }

            if (intent.from.role != User.Role.BOT || intent.from.nickname == "GroupAnonymousBot") repository.addUser(intent.from)

            logRawIntent(intent)
        }
    }

    private suspend fun registerTextMessage(intent: TextMessageIntent) {
        val key = intent.userAndChatKey
        if (easyKeyValueService.get(MessageCounter, key) != null) {
            easyKeyValueService.increment(MessageCounter, key)
        }

        val text = intent.text
            .takeIf {
                botConfig.botNameAliases.none { alias ->
                    it.contains(alias, ignoreCase = true)
                }
            }
            ?.takeIf { it.split(" ").size in (3..7) }
            ?.takeIf { it.length < 600 }
            ?.takeIf { chatLogRegex.matches(it) } ?: return

        chatLogRepository.add(intent.from, text)
    }

    private suspend fun registerCommand(intent: CommandIntent) {
        if (intent.command in setOf(Command.ROULETTE, Command.BET)) return

        val key = intent.userAndChatKey
        val currentValue = easyKeyValueService.get(CommandLimit, key)
        if (currentValue == null) {
            easyKeyValueService.put(CommandLimit, key, 1, 5.minutes)
        } else {
            easyKeyValueService.increment(CommandLimit, key)
        }

        commandHistoryRepository.add(CommandByUser(intent.from, intent.command, Instant.now()))
    }

    private suspend fun registerMemberLeft(intent: UserLeftIntent) {
        val leftChatMember = intent.from
        if (leftChatMember.role == User.Role.BOT && leftChatMember.nickname == botConfig.botName) {
            logger.info("Bot was removed from ${intent.chat}")
            repository.changeChatActiveStatus(intent.chat, false)
            repository.disableUsersInChat(intent.chat)
        } else {
            logger.info("User $leftChatMember has left")
            repository.changeUserActiveStatusNew(leftChatMember, false)
        }
    }

    private suspend fun registerMemberJoined(intent: UserJoinedIntent) {
        val user = intent.from
        if (user.role == User.Role.BOT && user.nickname == botConfig.botName) {
            logger.info("Bot was added to ${intent.chat}")
            repository.changeChatActiveStatus(intent.chat, true)
        } else {
            logger.info("New user added: $user")
            if (user.role != User.Role.BOT) repository.addUser(user)
        }
    }

    private suspend fun logRawIntent(intent: Intent) {
        if (intent.from.role == User.Role.BOT) return

        //TODO: add attachment intent
        val fileId = null
        /*when {
            rawMessage.hasPhoto() -> rawMessage.photo.joinToString { it.filePath ?: it.fileId }
            rawMessage.hasDocument() -> rawMessage.document.fileId
            else -> null
        }*/

        val text = (intent as? TextMessageIntent)?.text.orEmpty()
        rawChatLogRepository.add(intent.chat, intent.from, text, fileId, intent.toJson(), intent.date)
    }
}
