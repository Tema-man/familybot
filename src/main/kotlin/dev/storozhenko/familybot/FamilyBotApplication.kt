package dev.storozhenko.familybot

import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.core.telegram.BotConfigInjector
import dev.storozhenko.familybot.core.telegram.BotStarter
import dev.storozhenko.familybot.core.telegram.FamilyBot
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BotConfigInjector::class)
class FamilyBotApplication(
    private val env: ConfigurableEnvironment
) {
    private val logger = getLogger()

    @Bean
    fun timedAspect(registry: MeterRegistry): TimedAspect {
        return TimedAspect(registry)
    }

    @Bean
    fun injectBotConfig(botConfigInjector: BotConfigInjector): BotConfig {
        val botNameAliases = if (botConfigInjector.botNameAliases.isNullOrEmpty()) {
            logger.warn("No bot aliases provided, using botName")
            listOf(botConfigInjector.botName)
        } else {
            botConfigInjector.botNameAliases.split(",")
        }

        return BotConfig(
            botToken = requireValue(botConfigInjector.botToken, "botToken"),
            botName = requireValue(botConfigInjector.botName, "botName"),
            developer = requireValue(botConfigInjector.developer, "developer"),
            developerId = requireValue(botConfigInjector.developerId, "developerId"),
            botNameAliases = botNameAliases,
            yandexKey = optionalValue(
                botConfigInjector::yandexKey,
                "Yandex API key is not found, language API won't work"
            ),
            paymentToken = optionalValue(
                botConfigInjector::paymentToken,
                "Payment token is not found, payment API won't work"
            ),
            testEnvironment = env.activeProfiles.contains(BotStarter.TESTING_PROFILE_NAME),
            ytdlLocation = optionalValue(
                botConfigInjector::ytdlLocation,
                "yt-dlp is missing, downloading function won't work"
            )
        )
    }

    private fun requireValue(value: String, valueName: String): String =
        value.ifBlank { throw FamilyBot.InternalException("Value of '$valueName' must be not empty") }

    private fun optionalValue(value: () -> String?, log: String): String? =
        value()
            ?.takeIf(String::isNotBlank)
            .also { if (it == null) logger.warn(log) }
}

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    SpringApplication(FamilyBotApplication::class.java).apply {
        webApplicationType = WebApplicationType.NONE
        run()
    }
}
