package dev.storozhenko.familybot.core.bot

import dev.storozhenko.familybot.core.model.message.Message
import org.springframework.context.ConfigurableApplicationContext

interface AbstractBot {
    fun start(context: ConfigurableApplicationContext)
}
