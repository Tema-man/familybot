package dev.storozhenko.familybot.feature.keyword

import dev.storozhenko.familybot.common.extensions.randomInt
import dev.storozhenko.familybot.core.executor.Configurable
import dev.storozhenko.familybot.core.executor.Executor
import dev.storozhenko.familybot.core.services.router.model.ExecutorContext
import dev.storozhenko.familybot.core.services.router.model.FunctionId
import dev.storozhenko.familybot.core.services.router.model.Priority
import dev.storozhenko.familybot.getLogger
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class KeyWordExecutor(val processors: List<KeyWordProcessor>) : Executor, Configurable {

    private val log = getLogger()

    private val processorsForMessage = HashMap<Int, KeyWordProcessor>()

    override fun priority(context: ExecutorContext) = Priority.HIGH

    override fun getFunctionId(context: ExecutorContext) = FunctionId.TALK_BACK

    override fun execute(context: ExecutorContext): suspend (AbsSender) -> Unit =
        processorsForMessage.remove(context.message.messageId)?.process(context) ?: {}

    override fun canExecute(context: ExecutorContext): Boolean {
        val message = context.message
        if (message.from.isBot) {
            return false
        }
        val keyWordProcessor = processors
            .find { it.canProcess(context) }
            ?.takeIf { isPassingRandomCheck(it, context) }
        return if (keyWordProcessor != null) {
            log.info("Key word processor is found: ${keyWordProcessor::class.simpleName}")
            processorsForMessage[message.messageId] = keyWordProcessor
            true
        } else {
            false
        }
    }

    private fun isPassingRandomCheck(processor: KeyWordProcessor, context: ExecutorContext): Boolean =
        if (processor.isRandom(context)) randomInt(0, 5) == 0 else true
}
