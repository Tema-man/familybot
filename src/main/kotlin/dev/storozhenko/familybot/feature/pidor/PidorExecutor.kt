package dev.storozhenko.familybot.feature.pidor

import dev.storozhenko.familybot.common.extensions.*
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.IntentExecutor
import dev.storozhenko.familybot.core.model.Chat
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.CompositeAction
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.CommandIntent
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.core.services.settings.*
import dev.storozhenko.familybot.core.services.talking.Dictionary
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.feature.pidor.services.PidorCompetitionService
import dev.storozhenko.familybot.feature.pidor.services.PidorStrikesService
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class PidorExecutor(
    private val repository: CommonRepository,
    private val pidorCompetitionService: PidorCompetitionService,
    private val pidorStrikesService: PidorStrikesService,
    private val easyKeyValueService: EasyKeyValueService,
    private val botConfig: BotConfig,
    private val dictionary: Dictionary
) : IntentExecutor /*CommandExecutor()*/, Configurable {

    private val log = getLogger()

    override fun getFunctionId(context: ExecutorContext) = FunctionId.PIDOR

    val command: Command = Command.PIDOR

    override val priority: Priority = Priority.MEDIUM

    override fun canExecute(intent: Intent): Boolean = (intent as? CommandIntent)?.command == command

    override fun execute(intent: Intent): Action? {
        val chat = intent.chat
        // TODO: Add ReplyIntent.
//        if (intent.message.isReply) {
//            return pickPidor(intent)
//        }
        log.info("Getting pidors from chat $chat")

        return selectPidor(intent).first
    }

    fun selectPidor(
        intent: Intent,
        silent: Boolean = false
    ): Pair<Action?, Boolean> {
        val chat = intent.chat
        val key = intent.chat.key
        val users = repository.getUsers(chat, activeOnly = true)

        val pidorToleranceValue = easyKeyValueService.get(PidorTolerance, key)
        if (isLimitOfPidorsExceeded(users, pidorToleranceValue ?: 0)) {
            log.info("Pidors are already found")
            if (!silent) {
                val message = getMessageForPidors(chat, key)
                if (message != null) return Pair(message, false)
            } else {
                return Pair(null, false)
            }
        }

        log.info("Pidor is not found, initiating search procedure")
        val pidor = getNextPidor(chat)

        val messages = listOf(
            Phrase.PIDOR_SEARCH_START,
            Phrase.PIDOR_SEARCH_MIDDLE,
            Phrase.PIDOR_SEARCH_FINISHER
        )
            .map { phrase ->
                SendTextAction(
                    text = dictionary.get(phrase, key).bold(),
                    enableRichFormatting = true,
                    chat = chat
                )
            }
            .toMutableList()
            .apply {
                add(
                    SendTextAction(
                        text = pidor.getGeneralName(),
                        enableRichFormatting = true,
                        chat = chat
                    )
                )
            }

        if (pidorToleranceValue == null) {
            easyKeyValueService.put(PidorTolerance, key, 1, untilNextDay())
        } else {
            easyKeyValueService.increment(PidorTolerance, key)
        }

        //TODO: Add to intent hooks?
        /*
        pidorStrikesService.calculateStrike(chat, key, pidor).invoke(sender)
        pidorCompetitionService.pidorCompetition(chat, key).invoke(sender)
        */

        val action = CompositeAction(messages, chat)
        return Pair(action, true)
    }

    private fun getNextPidor(chat: Chat): User {
        return runCatching {
            val users = repository.getUsers(chat, activeOnly = true).also { users ->
                if (easyKeyValueService.get(BotOwnerPidorSkip, chat.key, false)) users.filter { user ->
                    botConfig.developerId != user.id.toString()
                }
            }
            log.info("Users to roll: {}", users)

            val nextPidor = users.randomOrNull() ?: getFallbackPidor(chat)
            log.info("Pidor is rolled to $nextPidor")

            val newPidor = Pidor(nextPidor, Instant.now())
            repository.addPidor(newPidor)
            nextPidor
        }
            .onFailure { e ->
                log.error("Something bad is happened on rolling, investigate", e)
            }
            .getOrNull() ?: getFallbackPidor(chat)
    }

    private fun getFallbackPidor(chat: Chat): User {
        log.error("Can't find pidor due to empty user list for $chat, switching to fallback pidor")
        return repository
            .getPidorsByChat(chat, startDate = Instant.now().minus(10, ChronoUnit.DAYS))
            .random()
            .user
    }

    // TODO: Extract to users actualizer service
    /*
    private fun checkIfUserStillThere(user: User) {
        val userFromChat = getUserFromChat(user, sender)
        if (userFromChat == null) {
            log.warn("Some user {} has left without notification", user)
            repository.changeUserActiveStatusNew(user, false)
        } else {
            repository.addUser(userFromChat)
        }
    }

    private fun getUserFromChat(user: User, absSender: AbsSender): User? {
        val getChatMemberCall = GetChatMember(user.chat.idString, user.id)
        return runCatching {
            absSender.execute(getChatMemberCall)
                .apply { log.info("Chat member status: $this ") }
                .takeIf { member -> member.status != "left" && member.status != "kicked" }
                ?.user()
                ?.toUser(user.chat)
        }.getOrNull()
    }

    */

    private fun getMessageForPidors(chat: Chat, key: ChatEasyKey): Action? {
        val pidorsByChat: List<List<Pidor>> = repository
            .getPidorsByChat(chat)
            .filter { pidor -> pidor.date.isToday() }
            .groupBy { (user) -> user.id }
            .map(Map.Entry<Long, List<Pidor>>::value)

        return when (pidorsByChat.size) {
            0 -> null
            1 -> SendTextAction(
                text = dictionary.get(Phrase.PIROR_DISCOVERED_ONE, key) + " " + formatName(pidorsByChat.first(), key),
                enableRichFormatting = true,
                chat = chat
            )

            else -> SendTextAction(
                text = dictionary.get(Phrase.PIROR_DISCOVERED_MANY, key) + " " +
                    pidorsByChat.joinToString { formatName(it, key) },
                enableRichFormatting = true,
                chat = chat
            )
        }
    }

    private fun formatName(statEntity: List<Pidor>, key: ChatEasyKey): String {
        val pidorCount = statEntity.size
        val pidorName = statEntity.first()
        var message = pidorName.user.getGeneralName()
        if (pidorCount > 1) {
            val pidorCountPhrase =
                when (pidorCount) {
                    2 -> Phrase.PIDOR_COUNT_TWICE
                    3 -> Phrase.PIDOR_COUNT_THRICE
                    4 -> Phrase.PIDOR_COUNT_FOUR_TIMES
                    5 -> Phrase.PIDOR_COUNT_FIVE_TIMES
                    else -> Phrase.PIDOR_COUNT_DOHUYA
                }
            message = "$message (${dictionary.get(pidorCountPhrase, key)})"
        }
        return message
    }

    private fun isLimitOfPidorsExceeded(
        usersInChat: List<User>,
        pidorToleranceValue: Long
    ): Boolean {
        val limit = if (usersInChat.size >= 50) 2 else 1
        log.info("Limit of pidors is $limit, tolerance is $pidorToleranceValue")
        return pidorToleranceValue >= limit
    }

//TODO: Enable after ReplyIntent implementation
    /*private fun pickPidor(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val abilityCount = easyKeyValueService.get(PickPidorAbilityCount, context.userKey, 0L)
        if (abilityCount <= 0L) {
            return {
                it.send(
                    context,
                    context.phrase(Phrase.PICK_PIDOR_PAYMENT_REQUIRED),
                    shouldTypeBeforeSend = true,
                    replyToUpdate = true
                )
                null
            }
        }

        if (replyMessage.from.isBot) {
            if (replyMessage.from.userName == botConfig.botName) {
                return {
                    it.send(
                        context,
                        context.phrase(Phrase.PICK_PIDOR_CURRENT_BOT),
                        shouldTypeBeforeSend = true,
                        replyToUpdate = true
                    )
                    null
                }
            } else {
                return {
                    it.send(
                        context,
                        context.phrase(Phrase.PICK_PIDOR_ANY_BOT),
                        shouldTypeBeforeSend = true,
                        replyToUpdate = true
                    )
                    null
                }
            }
        }
        val pickedUser = replyMessage.from.toUser(context.chat)
        repository.addUser(pickedUser)
        repository.addPidor(Pidor(pickedUser, Instant.now()))
        easyKeyValueService.decrement(PickPidorAbilityCount, context.userKey)
        return {
            it.send(
                context,
                context.phrase(Phrase.PICK_PIDOR_PICKED).replace("{}", pickedUser.getGeneralName()),
                shouldTypeBeforeSend = true,
                replyMessageId = replyMessage.messageId
            )
            val newAbilityCount = easyKeyValueService.get(
                PickPidorAbilityCount,
                context.userKey
            )
            if (newAbilityCount == 0L) {
                it.send(
                    context,
                    context.phrase(Phrase.PICK_PIDOR_ABILITY_COUNT_LEFT_NONE),
                    shouldTypeBeforeSend = true,
                    replyToUpdate = true,
                    enableHtml = true
                )
                null
            } else {
                it.send(
                    context,
                    context.phrase(Phrase.PICK_PIDOR_ABILITY_COUNT_LEFT)
                        .replace("{}", newAbilityCount.toString()),
                    shouldTypeBeforeSend = true,
                    replyToUpdate = true
                )
                null
            }
        }
    }*/
}
