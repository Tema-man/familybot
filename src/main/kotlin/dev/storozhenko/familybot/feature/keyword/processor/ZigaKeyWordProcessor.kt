package dev.storozhenko.familybot.feature.keyword.processor

import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.telegram.sendSticker
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.telegram.stickers.Sticker
import dev.storozhenko.familybot.telegram.stickers.StickerPack
import dev.storozhenko.familybot.feature.keyword.KeyWordProcessor
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ZigaKeyWordProcessor : KeyWordProcessor {
    private val zigaStickers = listOf(Sticker.LEFT_ZIGA, Sticker.RIGHT_ZIGA)

    override fun canProcess(context: ExecutorContext): Boolean {
        val incomeSticker = context.message.sticker ?: return false
        val isRightPack = StickerPack.FAMILY_PACK.packName == incomeSticker.setName
        return isRightPack && zigaStickers.any { it.stickerEmoji == incomeSticker.emoji }
    }

    override fun process(context: ExecutorContext): suspend (AbsSender) -> Message? {
        return {
            val stickerToSend =
                if (context.message.sticker?.emoji == Sticker.LEFT_ZIGA.stickerEmoji) {
                    Sticker.RIGHT_ZIGA
                } else {
                    Sticker.LEFT_ZIGA
                }
            it.sendSticker(context, stickerToSend, replyToUpdate = true)
            null
        }
    }
}