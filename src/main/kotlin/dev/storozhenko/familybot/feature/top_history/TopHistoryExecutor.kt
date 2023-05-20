package dev.storozhenko.familybot.feature.top_history

import com.fasterxml.jackson.annotation.JsonProperty
import dev.storozhenko.familybot.common.extensions.parseJson
import dev.storozhenko.familybot.core.executor.CommandIntentExecutor
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.action.Action
import dev.storozhenko.familybot.core.model.action.SendTextAction
import dev.storozhenko.familybot.core.model.intent.Intent
import dev.storozhenko.familybot.telegram.TelegramBot
import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Component

@Component
class TopHistoryExecutor : CommandIntentExecutor() {
    private  companion object {
        val mamoeb: Mamoeb = this::class.java.classLoader
            .getResourceAsStream("static/curses")
            ?.readAllBytes()
            ?.let { Base64.decodeBase64(it) }
            ?.decodeToString()
            ?.parseJson<Mamoeb>()
            ?: throw TelegramBot.InternalException("curses is missing")
    }

    override val command = Command.TOP_HISTORY

    override fun execute(intent: Intent): Action? = SendTextAction(
        text = mamoeb.curses.random(),
        chat = intent.chat
    )

    private data class Mamoeb(
        @JsonProperty("Templates") val curses: List<String>
    )
}
