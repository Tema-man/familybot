package dev.storozhenko.familybot.feature.global_time

import dev.storozhenko.familybot.common.extensions.DateConstants
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.code
import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.core.model.message.SimpleTextMessage
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class TimeExecutor : CommandExecutor() {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    companion object {
        private val times = mapOf(
            "Время в Лондоне:          " to "Europe/London",
            "Время в Москве:           " to "Europe/Moscow",
            "Время в Ульяновске:       " to "Europe/Samara",
            "Время в Ташкенте:         " to "Asia/Tashkent",
            "Время в Аргентине:        " to "America/Argentina/Buenos_Aires",
        )
            .map { (prefix, zone) -> prefix.code() to ZoneId.of(zone) }
            .toMap()
    }

    override fun command() = Command.TIME

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        val now = Instant.now()
        val result = times.map { (prefix, zone) -> prefix to now.atZone(zone) }
            .sortedBy { (_, time) -> time }
            .joinToString(separator = "\n") { (prefix, time) ->
                prefix + time.format(timeFormatter).bold()
            }
        return {
            it.me
            it.send(context, "$result\n${getMortgageDate()}", replyToUpdate = true, enableHtml = true)
            null
        }
    }

    fun getMortgageDate(): String {
        val duration = Duration.between(Instant.ofEpochSecond(DateConstants.VITYA_MORTGAGE_DATE), Instant.now())
        return "Время в Ипотечной Кабале: ".code() + "${duration.toHours()}:${duration.toMinutes() % 60}".bold()
    }
}