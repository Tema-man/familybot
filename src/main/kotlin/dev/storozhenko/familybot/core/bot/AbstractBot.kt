package dev.storozhenko.familybot.core.bot

import org.springframework.context.ConfigurableApplicationContext

interface AbstractBot {
    suspend fun start(context: ConfigurableApplicationContext)
}
