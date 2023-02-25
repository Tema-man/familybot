package dev.storozhenko.familybot.feature.stickers

import dev.storozhenko.familybot.core.telegram.model.Command
import dev.storozhenko.familybot.core.telegram.stickers.StickerPack
import dev.storozhenko.familybot.core.repository.CommandHistoryRepository
import org.springframework.stereotype.Component

@Component
class HampikExecutor(
    historyRepository: CommandHistoryRepository
) : SendRandomStickerExecutor(historyRepository) {

    override fun getMessage() = "Какой ты сегодня Андрей?"

    override fun getStickerPack() = StickerPack.HAMPIK_PACK

    override fun command() = Command.HAMPIK
}
