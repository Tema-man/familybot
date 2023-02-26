package dev.storozhenko.familybot.feature.shop

import dev.storozhenko.familybot.common.extensions.send
import dev.storozhenko.familybot.core.executor.CommandExecutor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.talking.model.Phrase
import dev.storozhenko.familybot.core.telegram.BotConfig
import dev.storozhenko.familybot.core.telegram.model.Command
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

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit {
        if (isEnabled.not()) {
            return { sender ->
                sender.send(context, context.phrase(Phrase.SHOP_DISABLED))
            }
        }

        return { sender ->
            sender.send(
                context,
                context.phrase(Phrase.SHOP_KEYBOARD),
                replyToUpdate = true,
                customization = customization(context)
            )
        }
    }

    private fun customization(context: ExecutorContext): SendMessage.() -> Unit = {
        replyMarkup = ShopItem.toKeyBoard(context)
    }
}
