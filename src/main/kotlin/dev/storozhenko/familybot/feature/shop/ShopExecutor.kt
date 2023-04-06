package dev.storozhenko.familybot.feature.shop

import dev.storozhenko.familybot.telegram.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.bot.BotConfig
import dev.storozhenko.familybot.core.model.Command
import dev.storozhenko.familybot.core.model.message.Message
import dev.storozhenko.familybot.feature.shop.model.ShopItem
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class ShopExecutor(
    botConfig: BotConfig
) : CommandExecutor() {
    private val isEnabled = botConfig.paymentToken != null

    override fun command() = Command.SHOP

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Message? {
        if (isEnabled.not()) {
            return { sender ->
                sender.send(context, context.phrase(Phrase.SHOP_DISABLED))
                null
            }
        }

        return { sender ->
            sender.send(
                context,
                context.phrase(Phrase.SHOP_KEYBOARD),
                replyToUpdate = true,
                customization = customization(context)
            )
            null
        }
    }

    private fun customization(context: ExecutorContext): SendMessage.() -> Unit = {
        replyMarkup = ShopItem.toKeyBoard(context)
    }
}
