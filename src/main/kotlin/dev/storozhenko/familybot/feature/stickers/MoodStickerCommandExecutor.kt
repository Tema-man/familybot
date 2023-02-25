package dev.storozhenko.familybot.feature.stickers

import dev.storozhenko.familybot.core.telegram.model.Command
import dev.storozhenko.familybot.core.telegram.stickers.StickerPack
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import org.springframework.stereotype.Component

@Component
class MoodStickerCommandExecutor(
    historyRepository: CommandHistoryRepository
) : SendRandomStickerExecutor(historyRepository) {
    override fun getMessage() = "Какой ты сегодня?"

    override fun getStickerPack() = StickerPack.YOU_ARE_TODAY

    override fun command() = Command.WHATS_MOOD_TODAY
}
