package dev.storozhenko.familybot.core.bot

import org.springframework.context.ConfigurableApplicationContext

interface AbstractBot {
    fun start(context: ConfigurableApplicationContext)
}
