package dev.storozhenko.familybot.feature.ask_world

import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.feature.ask_world.model.AskWorldQuestion
import dev.storozhenko.familybot.feature.ban.service.BanService
import dev.storozhenko.familybot.getLogger
import dev.storozhenko.familybot.telegram.send
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class BanAskWorldExecutor(
    private val askWorldRepository: AskWorldRepository,
    private val banService: BanService
) : CommandExecutor() {
    private val log = getLogger()

    override fun command() = Command.BAN

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val message = context.message
        if (message.isReply.not()) return { null }

        val replyToMessage = message.replyToMessage
        val questions =
            askWorldRepository.getQuestionsFromDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .filter {
                    replyToMessage.text.contains(it.message, ignoreCase = true)
                }
        log.info("Trying to ban, questions found: {}", questions)
        when (questions.size) {
            0 -> return { it.send(context, "Can't find anyone, sorry, my master"); null }
            1 -> return ban(context, questions.first())
            else -> return { sender ->
                questions
                    .distinctBy { question -> question.user.id }
                    .map { question -> ban(context, question) }
                    .forEach { it.invoke(sender) }

                null
            }
        }
    }

    override fun canExecute(context: ExecutorContext) = context.isFromDeveloper && super.canExecute(context)

    private fun ban(
        context: ExecutorContext,
        question: AskWorldQuestion
    ): suspend (AbsSender) -> Message? {
        val tokens = context.update.message.text.split(" ")
        val banReason = tokens[1]
        val isChat = tokens.getOrNull(2) == "chat"
        if (isChat) {
            banService.banChat(question.chat, banReason)
            return {
                it.send(
                    context,
                    "${question.chat} is banned, my master",
                    replyToUpdate = true
                )
                null
            }
        } else {
            banService.banUser(question.user, banReason)
            return {
                it.send(context, "${question.user} is banned, my master", replyToUpdate = true)
                null
            }
        }
        null
    }
}
