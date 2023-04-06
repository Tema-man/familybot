package dev.storozhenko.familybot.telegram

import dev.storozhenko.familybot.core.bot.AbstractBot
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener

@Configuration
@Profile(BotStarter.NOT_TESTING_PROFILE_NAME)
class BotStarter {

    companion object Profile {
        const val TESTING_PROFILE_NAME = "testing"
        const val NOT_TESTING_PROFILE_NAME = "!$TESTING_PROFILE_NAME"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun telegramBot(event: ApplicationReadyEvent) {
        val bots = event.applicationContext.getBeansOfType(AbstractBot::class.java).values

        bots.forEach { bot ->
            bot.start(event.applicationContext)
        }
    }
}
