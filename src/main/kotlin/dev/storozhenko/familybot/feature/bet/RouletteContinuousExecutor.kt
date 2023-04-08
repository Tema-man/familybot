package dev.storozhenko.familybot.feature.bet

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.core.executor.ContinuousConversationExecutor
import dev.storozhenko.familybot.core.repository.CommonRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.Pidor
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.feature.pidor.services.PidorCompetitionService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Component
@Deprecated(message = "Replaced with BetContinious")
class RouletteContinuousExecutor(
    private val botConfig: BotConfig,
    private val pidorRepository: CommonRepository,
    private val pidorCompetitionService: PidorCompetitionService
) : ContinuousConversationExecutor(botConfig) {

    private val log = LoggerFactory.getLogger(RouletteContinuousExecutor::class.java)

    override fun getDialogMessages(context: ExecutorContext): Set<String> {
        return setOf(ROULETTE_MESSAGE)
    }

    override fun command(): Command {
        return Command.ROULETTE
    }

    override fun canExecute(context: ExecutorContext): Boolean {
        val message = context.message
        return message.isReply &&
                message.replyToMessage.from.userName == botConfig.botName &&
                (message.replyToMessage.text ?: "") in getDialogMessages(context)
    }

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Action? {
        val user = context.user
        val chatId = context.chat.idString

        val number = context.message.text.split(" ")[0].toIntOrNull()
        if (number !in 1..6) {
            return {
                it.execute(SendMessage(chatId, "Мушку спили и в следующий раз играй по правилам"))
                coroutineScope {
                    launch {
                        pidorRepository.addPidor(
                            Pidor(
                                user,
                                Instant.now()
                            )
                        )
                    }
                }
                delay(1.seconds)
                it.execute(SendMessage(chatId, "В наказание твое пидорское очко уходит к остальным"))
                null
            }
        }
        val rouletteNumber = randomInt(1, 7)
        log.info("Roulette win number is $rouletteNumber and guessed number is $number")
        return {
            if (rouletteNumber == number) {
                it.execute(SendMessage(chatId, "Ты ходишь по охуенно тонкому льду"))
                coroutineScope { launch { repeat(5) { pidorRepository.removePidorRecord(user) } } }
                delay(2.seconds)
                it.execute(SendMessage(chatId, "Но он пока не треснул. Свое пидорское очко можешь забрать. "))
            } else {
                it.execute(SendMessage(chatId, "Ты ходишь по охуенно тонкому льду"))
                coroutineScope {
                    launch {
                        repeat(3) {
                            pidorRepository.addPidor(
                                Pidor(
                                    user,
                                    Instant.now()
                                )
                            )
                        }
                    }
                }
                delay(2.seconds)
                it.execute(
                    SendMessage(
                        chatId,
                        "Сорян, но ты проиграл. Твое пидорское очко уходит в зрительный зал трижды. Правильный ответ был $rouletteNumber."
                    )
                )
            }
            delay(2.seconds)
            pidorCompetitionService.pidorCompetition(context.chat, context.chatKey).invoke(it)
            null
        }
    }
}
