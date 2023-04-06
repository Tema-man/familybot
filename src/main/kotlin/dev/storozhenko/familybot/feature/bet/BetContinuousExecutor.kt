package dev.storozhenko.familybot.feature.bet

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.common.extensions.untilNextMonth
import dev.storozhenko.familybot.core.executor.ContinuousConversationExecutor
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.settings.BetTolerance
import dev.storozhenko.familybot.core.services.settings.EasyKeyValueService
import dev.storozhenko.familybot.core.services.settings.UserAndChatEasyKey
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.services.talking.model.Pluralization
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.User
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.feature.pidor.services.PidorCompetitionService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendDice
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds

@Component
class BetContinuousExecutor(
    private val pidorRepository: CommonRepository,
    private val pidorCompetitionService: PidorCompetitionService,
    private val easyKeyValueService: EasyKeyValueService,
    private val botConfig: BotConfig
) : ContinuousConversationExecutor(botConfig) {
    private val diceNumbers = listOf(1, 2, 3, 4, 5, 6)

    override fun getDialogMessages(context: ExecutorContext): Set<String> {
        return context.allPhrases(Phrase.BET_INITIAL_MESSAGE)
    }

    override fun command() = Command.BET

    override fun canExecute(context: ExecutorContext): Boolean {
        val message = context.message
        return message.isReply &&
                message.replyToMessage.from.userName == botConfig.botName &&
                (message.replyToMessage.text ?: "") in getDialogMessages(context)
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val user = context.user
        val chatId = context.message.chatId
        val key = context.userAndChatKey

        if (isBetAlreadyDone(key)) {
            return {
                it.send(
                    context,
                    context.phrase(Phrase.BET_ALREADY_WAS),
                    shouldTypeBeforeSend = true
                )
                null
            }
        }
        val number = extractBetNumber(context)
        if (number == null || number !in 1..3) {
            return {
                it.send(
                    context,
                    context.phrase(Phrase.BET_BREAKING_THE_RULES_FIRST),
                    shouldTypeBeforeSend = true
                )
                it.send(
                    context,
                    context.phrase(Phrase.BET_BREAKING_THE_RULES_SECOND),
                    shouldTypeBeforeSend = true
                )
                null
            }
        }
        val winnableNumbers = diceNumbers.shuffled().subList(0, 3)
        return {
            it.send(
                context,
                "${context.phrase(Phrase.BET_WINNABLE_NUMBERS_ANNOUNCEMENT)} ${
                    formatWinnableNumbers(
                        winnableNumbers
                    )
                }",
                shouldTypeBeforeSend = true
            )
            it.send(context, context.phrase(Phrase.BET_ZATRAVOCHKA), shouldTypeBeforeSend = true)
            val diceMessage = it.execute(SendDice(chatId.toString()))
            delay(4.seconds)
            val isItWinner = winnableNumbers.contains(diceMessage.dice.value)
            if (isItWinner) {
                coroutineScope { launch { repeat(number) { pidorRepository.removePidorRecord(user) } } }
                it.send(context, context.phrase(Phrase.BET_WIN), shouldTypeBeforeSend = true)
                it.send(context, winEndPhrase(number, context), shouldTypeBeforeSend = true)
            } else {
                coroutineScope { launch { addPidorsMultiplyTimesWithDayShift(number, user) } }
                it.send(context, context.phrase(Phrase.BET_LOSE), shouldTypeBeforeSend = true)
                it.send(context, explainPhrase(number, context), shouldTypeBeforeSend = true)
            }
            easyKeyValueService.put(BetTolerance, key, true, untilNextMonth())
            delay(2.seconds)
            pidorCompetitionService.pidorCompetition(context.chat, context.chatKey).invoke(it)
            null
        }
    }

    private fun addPidorsMultiplyTimesWithDayShift(number: Int, user: User) {
        var i: Int = number
        while (i != 0) {
            pidorRepository.addPidor(
                Pidor(
                    user,
                    LocalDateTime
                        .now()
                        .toLocalDate()
                        .atStartOfDay()
                        .plusDays(i.toLong())
                        .toInstant(ZoneOffset.UTC)
                )
            )
            i--
        }
    }

    private fun extractBetNumber(context: ExecutorContext) =
        context.message.text.split(" ")[0].toIntOrNull()

    private fun isBetAlreadyDone(key: UserAndChatEasyKey) =
        easyKeyValueService.get(BetTolerance, key, false)

    private fun winEndPhrase(betNumber: Int, context: ExecutorContext): String {
        val plur = Pluralization.getPlur(betNumber)
        val winPhraseTemplate = context.phrase(Phrase.BET_WIN_END)
        return when (plur) {
            Pluralization.ONE -> {
                winPhraseTemplate
                    .replace("$0", betNumber.toString())
                    .replace("$1", context.phrase(Phrase.PLURALIZED_PIDORSKOE_ONE))
                    .replace("$2", context.phrase(Phrase.PLURALIZED_OCHKO_ONE))
            }

            Pluralization.FEW -> {
                winPhraseTemplate
                    .replace("$0", betNumber.toString())
                    .replace("$1", context.phrase(Phrase.PLURALIZED_PIDORSKOE_FEW))
                    .replace("$2", context.phrase(Phrase.PLURALIZED_OCHKO_FEW))
            }

            Pluralization.MANY -> {
                winPhraseTemplate
                    .replace("$0", betNumber.toString())
                    .replace("$1", context.phrase(Phrase.PLURALIZED_PIDORSKOE_MANY))
                    .replace("$2", context.phrase(Phrase.PLURALIZED_OCHKO_MANY))
            }
        }
    }

    private fun explainPhrase(betNumber: Int, context: ExecutorContext): String {
        val plur = Pluralization.getPlur(betNumber)
        val explainTemplate = context.phrase(Phrase.BET_EXPLAIN)
        return when (plur) {
            Pluralization.ONE -> {
                context.phrase(Phrase.BET_EXPLAIN_SINGLE_DAY)
            }

            else -> {
                explainTemplate
                    .replace("$0", betNumber.toString())
                    .replace("$1", context.phrase(Phrase.PLURALIZED_NEXT_MANY))
                    .replace("$2", context.phrase(Phrase.PLURALIZED_DAY_MANY))
            }
        }
    }

    private fun formatWinnableNumbers(numbers: List<Int>): String {
        val orderedNumbers = numbers.sorted()
        return "${orderedNumbers[0]}, ${orderedNumbers[1]} и ${orderedNumbers[2]}"
    }
}
