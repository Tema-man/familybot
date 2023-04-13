package dev.storozhenko.familybot

import dev.storozhenko.familybot.FamilyBotApplication.Profile.NOT_TESTING_PROFILE_NAME
import dev.storozhenko.familybot.core.bot.AbstractBot
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.telegram.TelegramBot
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(
    AppConfig::class,
    TelegramConfig::class
)
@Profile(NOT_TESTING_PROFILE_NAME)
class FamilyBotApplication(
    private val env: ConfigurableEnvironment
) {
    private val logger = getLogger()

    @[Bean Suppress("unused")]
    fun injectTimedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)

    @[Bean Suppress("unused")]
    fun injectBotConfig(appConfig: AppConfig, telegramConfig: TelegramConfig): BotConfig {
        val botNameAliases = if (appConfig.botNameAliases.isNullOrEmpty()) {
            logger.warn("No bot aliases provided, using botName")
            listOf(telegramConfig.botName)
        } else {
            appConfig.botNameAliases.split(",")
        }

        return BotConfig(
            botToken = requireValue(telegramConfig.botToken, "botToken"),
            botName = requireValue(telegramConfig.botName, "botName"),
            developer = requireValue(telegramConfig.developer, "developer"),
            developerId = requireValue(telegramConfig.developerId, "developerId"),
            paymentToken = optionalValue(
                telegramConfig::paymentToken,
                "Payment token is not found, payment API won't work"
            ),

            botNameAliases = botNameAliases,
            yandexKey = optionalValue(
                appConfig::yandexKey,
                "Yandex API key is not found, language API won't work"
            ),
            ytdlLocation = optionalValue(
                appConfig::ytdlLocation,
                "yt-dlp is missing, downloading function won't work"
            ),
            openAiToken = optionalValue(
                appConfig::openAiToken,
                "OpenAI token is missing, API won't work"
            ),
            testEnvironment = env.activeProfiles.contains(TESTING_PROFILE_NAME)
        )
    }

    @EventListener(ApplicationReadyEvent::class)
    fun startBots(event: ApplicationReadyEvent) {
        val bots = event.applicationContext.getBeansOfType(AbstractBot::class.java).values

        bots.forEach { bot ->
            bot.start(event.applicationContext)
        }
    }

    private fun requireValue(value: String, valueName: String): String =
        value.ifBlank { throw TelegramBot.InternalException("Value of '$valueName' must be not empty") }

    private fun optionalValue(value: () -> String?, log: String): String? =
        value()
            ?.takeIf(String::isNotBlank)
            .also { if (it == null) logger.warn(log) }

    companion object Profile {
        const val TESTING_PROFILE_NAME = "testing"
        const val NOT_TESTING_PROFILE_NAME = "!$TESTING_PROFILE_NAME"
    }
}

@ConfigurationProperties("telegram", ignoreInvalidFields = false)
data class TelegramConfig @ConstructorBinding constructor(
    val botToken: String,
    val botName: String,
    val developer: String,
    val developerId: String,
    val paymentToken: String?,
)

@ConfigurationProperties("application", ignoreInvalidFields = false)
data class AppConfig(
    val yandexKey: String?,
    val botNameAliases: String?,
    val ytdlLocation: String?,
    val openAiToken: String?
)

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun main() {
    SpringApplication(FamilyBotApplication::class.java).apply {
        webApplicationType = WebApplicationType.NONE
        run()
    }
}
