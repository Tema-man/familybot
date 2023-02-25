package dev.storozhenko.familybot.feature.stickers

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.common.extensions.sendRandomSticker
import dev.storozhenko.familybot.common.extensions.startOfDay
import dev.storozhenko.familybot.common.extensions.toUser
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.telegram.model.CommandByUser
import dev.storozhenko.familybot.core.telegram.model.User
import dev.storozhenko.familybot.core.telegram.stickers.StickerPack
import kotlinx.coroutines.delay
import org.telegram.telegrambots.meta.bots.AbsSender

abstract class SendRandomStickerExecutor(
    private val historyRepository: CommandHistoryRepository
) : CommandExecutor() {

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        if (isInvokedToday(context.update.toUser())) {
            return {}
        }

        return {
            it.send(context, getMessage())
            delay(1000)
            it.sendRandomSticker(context, getStickerPack())
        }
    }

    private fun isInvokedToday(user: User): Boolean {
        val commandsFromUserToday = historyRepository.get(
            user,
            from = startOfDay()
        ).map(CommandByUser::command)
        return commandsFromUserToday.any { it == command() }
    }

    abstract fun getMessage(): String
    abstract fun getStickerPack(): StickerPack
}
