package dev.storozhenko.familybot.core.telegram

import dev.storozhenko.familybot.core.executor.CommandExecutor
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import org.tomlj.TomlParseResult

@Configuration
@Profile(BotStarter.NOT_TESTING_PROFILE_NAME)
class BotStarter {

    companion object Profile {
        const val TESTING_PROFILE_NAME = "testing"
        const val NOT_TESTING_PROFILE_NAME = "!$TESTING_PROFILE_NAME"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun telegramBot(event: ApplicationReadyEvent) {
        val telegramBotsApi = TelegramBotsApi(DefaultBotSession::class.java)
        val bot = event.applicationContext.getBean(FamilyBot::class.java)

        val commands = event.applicationContext.getBeansOfType(CommandExecutor::class.java).values.asSequence()
            .map { it.command() }.sorted().distinct()
            .map { BotCommand(it.command, it.description) }.toList()

        telegramBotsApi.registerBot(bot)
        listOf(
            BotCommandScopeAllGroupChats() to commands,
            BotCommandScopeAllPrivateChats() to commands
        ).forEach { (scope, commands) ->
            bot.execute(SetMyCommands.builder().commands(commands).scope(scope).build())
        }
    }
}
