package dev.storozhenko.familybot.feature.global_time

import dev.storozhenko.familybot.common.extensions.DateConstants
import dev.storozhenko.familybot.common.extensions.bold
import dev.storozhenko.familybot.common.extensions.code
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class TimeExecutor : CommandIntentExecutor() {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    companion object {
        private val times = mapOf(
            "Время в Берлине:          " to "Europe/Berlin",
            "Время в Москве:           " to "Europe/Moscow",
            "Время в Перми:            " to "Asia/Yekaterinburg",
        )
            .map { (prefix, zone) -> prefix.code() to ZoneId.of(zone) }
            .toMap()
    }

    override val command = Command.TIME

    override fun execute(intent: Intent): Action? {
        val now = Instant.now()
        val result = times.map { (prefix, zone) -> prefix to now.atZone(zone) }
            .sortedBy { (_, time) -> time }
            .joinToString(separator = "\n") { (prefix, time) ->
                prefix + time.format(timeFormatter).bold()
            }

        return SendTextAction(
            text = "$result\n${getMortgageDate()}",
            chat = intent.chat,
            asReplyToIntentId = intent.id,
            enableRichFormatting = true
        )
    }

    fun getMortgageDate(): String {
        val duration = Duration.between(Instant.ofEpochSecond(DateConstants.VITYA_MORTGAGE_DATE), Instant.now())
        return "Время в Ипотечной Кабале: ".code() + "${duration.toHours()}:${duration.toMinutes() % 60}".bold()
    }
}
